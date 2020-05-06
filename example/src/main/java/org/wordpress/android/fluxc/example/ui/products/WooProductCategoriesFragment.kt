package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_product_categories.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter.OnProductCategoryClickListener
import org.wordpress.android.fluxc.example.ProductCategoriesAdapter.ProductCategoryViewHolderModel
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductModel.ProductTriplet
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchAllProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductCategoriesFragment : Fragment(), OnProductCategoryClickListener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var productStore: WCProductStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSitePosition: Int = -1
    private var selectedRemoteProductId: Long = -1
    private var selectedProductModel: WCProductModel? = null
    private var productCategories: List<WCProductCategoryModel>? = null
    private lateinit var productCategoriesAdapter: ProductCategoriesAdapter

    companion object {
        const val ARG_SELECTED_SITE_POS = "ARG_SELECTED_SITE_POS"
        const val ARG_SELECTED_PRODUCT_ID = "ARG_SELECTED_PRODUCT_ID"

        fun newInstance(selectedSitePosition: Int, selectedProductId: Long): WooProductCategoriesFragment {
            val fragment = WooProductCategoriesFragment()
            val args = Bundle()
            args.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
            args.putLong(ARG_SELECTED_PRODUCT_ID, selectedProductId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POS, 0)
            selectedRemoteProductId = it.getLong(ARG_SELECTED_PRODUCT_ID, 0)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_SELECTED_SITE_POS, selectedSitePosition)
        outState.putLong(ARG_SELECTED_PRODUCT_ID, selectedRemoteProductId)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_product_categories, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productCategoriesAdapter = ProductCategoriesAdapter(context!!, this)
        with(category_list) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
        }

        savedInstanceState?.let { bundle ->
            selectedRemoteProductId = bundle.getLong(ARG_SELECTED_PRODUCT_ID)
            selectedSitePosition = bundle.getInt(ARG_SELECTED_SITE_POS)
        }

        updateSelectedProductId(selectedRemoteProductId)

        update_product_categories.setOnClickListener {
            getWCSite()?.let { site ->
                if (selectedProductModel?.remoteProductId != null) {
                    val payload = UpdateProductPayload(site, selectedProductModel!!)
                    dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
                } else {
                    prependToLog("No valid remoteProductId defined...doing nothing")
                }
            } ?: prependToLog("No site found...doing nothing")
        }
    }

    private fun showProductCategories() {
        getWCSite()?.let { site ->
            val product = requireNotNull(selectedProductModel)

            // Get the categories of the product
            val selectedCategories = product.getCategories()
            val allCategories =
                    productStore.getProductCategoriesForSite(site).map {
                        ProductCategoryViewHolderModel(it)
                    }

            // Mark the product categories as selected in the sorted list
            for (productCategoryViewHolderModel in allCategories) {
                for (selectedCategory in selectedCategories) {
                    if (productCategoryViewHolderModel.category.remoteCategoryId == selectedCategory.id &&
                            productCategoryViewHolderModel.category.name == selectedCategory.name)
                        productCategoryViewHolderModel.isSelected = true
                }
            }
            productCategoriesAdapter.setProductCategories(allCategories.toList())
        } ?: prependToLog("No valid site found...doing nothing")
    }

    private fun updateSelectedProductId(remoteProductId: Long) {
        getWCSite()?.let { siteModel ->
            selectedProductModel = productStore.getProductByRemoteId(siteModel, remoteProductId)
                    ?: WCProductModel().apply { this.remoteProductId = remoteProductId }

            prependToLog("Submitting request to fetch product categories for site ${siteModel.id}")
            val payload = FetchAllProductCategoriesPayload(siteModel)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
        } ?: prependToLog("No valid site found...doing nothing")
    }

    override fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel) {
        val product = requireNotNull(selectedProductModel)
        val selectedCategories = product.getCategories()

        val found = selectedCategories.find {
            it.id == productCategoryViewHolderModel.category.remoteCategoryId &&
                    it.name == productCategoryViewHolderModel.category.name }
        if (!productCategoryViewHolderModel.isSelected && found != null) {
            selectedCategories.remove(found)
            product.categories = JsonArray().also {
                for (category in selectedCategories) {
                    it.add(category.toJson())
                }
            }.toString()
        } else if (productCategoryViewHolderModel.isSelected && found == null) {
            selectedCategories.add(ProductTriplet(
                    productCategoryViewHolderModel.category.remoteCategoryId,
                    productCategoryViewHolderModel.category.name,
                    productCategoryViewHolderModel.category.slug
            ))
            product.categories = JsonArray().also {
                for (category in selectedCategories) {
                    it.add(category.toJson())
                }
            }.toString()
        }
    }

    private fun getWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(selectedSitePosition)

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesFetched(event: OnProductCategoryChanged) {
        if (event.isError) {
            prependToLog("Error fetching product categories - error: " + event.error.type)
        } else {
            productCategories = productStore.getProductCategoriesForSite(getWCSite()!!)
            prependToLog("Fetched ${event.rowsAffected} product categories")
            showProductCategories()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }
        prependToLog("Product updated ${event.rowsAffected}")
    }
}
