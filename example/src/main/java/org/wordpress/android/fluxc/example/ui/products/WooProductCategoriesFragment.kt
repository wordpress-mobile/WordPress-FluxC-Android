package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_product_categories.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesAdapter.OnProductCategoryClickListener
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesAdapter.ProductCategoryViewHolderModel
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment.ProductCategory
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooProductCategoriesFragment : Fragment(), OnProductCategoryClickListener {
    private var resultCode: Int = -1
    private var productCategories: List<ProductCategory>? = null
    private var selectedProductCategories: MutableList<ProductCategory>? = null
    private var selectedSitePosition: Int = 0

    private lateinit var productCategoriesAdapter: WooProductCategoriesAdapter

    @Inject lateinit var woocommerceStore: WooCommerceStore
    @Inject lateinit var wcProductStore: WCProductStore

    companion object {
        const val PRODUCT_CATEGORIES_REQUEST_CODE = 2000
        const val ARG_RESULT_CODE = "ARG_RESULT_CODE"
        const val ARG_PRODUCT_CATEGORIES = "ARG_PRODUCT_CATEGORIES"
        const val ARG_SELECTED_PRODUCT_CATEGORIES = "ARG_SELECTED_PRODUCT_CATEGORIES"
        const val ARG_SELECTED_SITE_POSITION = "ARG_SELECTED_SITE_POSITION"

        fun newInstance(
            fragment: Fragment,
            resultCode: Int,
            productCategories: List<ProductCategory>,
            selectedProductCategories: MutableList<ProductCategory>?,
            selectedSitePosition: Int
        ) = WooProductCategoriesFragment().apply {
            this.setTargetFragment(fragment, PRODUCT_CATEGORIES_REQUEST_CODE)
            this.resultCode = resultCode
            this.productCategories = productCategories
            this.selectedProductCategories = selectedProductCategories
            this.selectedSitePosition = selectedSitePosition
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_product_categories, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            resultCode = it.getInt(ARG_RESULT_CODE)
            productCategories = it.getParcelableArrayList(ARG_PRODUCT_CATEGORIES)
            selectedProductCategories = it.getParcelableArrayList(ARG_SELECTED_PRODUCT_CATEGORIES)
            selectedSitePosition = it.getInt(ARG_SELECTED_SITE_POSITION)
        }

        productCategoriesAdapter = WooProductCategoriesAdapter(requireContext(), this)
        with(category_list) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
        }

        val allCategories = productCategories?.toViewHolders() ?: emptyList()

        productCategoriesAdapter.setProductCategories(allCategories.toList())

        btn_add.setOnClickListener {
            handleCreatingProductCategories()
        }

        btn_done.setOnClickListener {
            val intent = activity?.intent
            intent?.putParcelableArrayListExtra(
                    ARG_SELECTED_PRODUCT_CATEGORIES, selectedProductCategories as? ArrayList
            )
            targetFragment?.onActivityResult(PRODUCT_CATEGORIES_REQUEST_CODE, resultCode, intent)
            fragmentManager?.popBackStack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_RESULT_CODE, resultCode)
        productCategories?.let {
            outState.putParcelableArrayList(ARG_PRODUCT_CATEGORIES, it as? ArrayList)
        }
        selectedProductCategories?.let {
            outState.putParcelableArrayList(ARG_SELECTED_PRODUCT_CATEGORIES, it as? ArrayList)
        }
        outState.putInt(ARG_SELECTED_SITE_POSITION, selectedSitePosition)
    }

    override fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel) {
        val found = selectedProductCategories?.find {
            it.name == productCategoryViewHolderModel.category.name
        }
        if (!productCategoryViewHolderModel.isSelected && found != null) {
            selectedProductCategories?.remove(found)
        } else if (productCategoryViewHolderModel.isSelected && found == null) {
            selectedProductCategories?.add(productCategoryViewHolderModel.category)
        }
    }

    private fun handleCreatingProductCategories() {
        lifecycleScope.launch {
            val input = showSingleLineDialog(
                activity = requireActivity(),
                message = "Enter categories separated by a comma",
                isNumeric = false
            )?.takeIf { it.isNotEmpty() } ?: return@launch

            val categories = input.split(",").map {
                WCProductCategoryModel().apply {
                    name = it
                }
            }
            val site = woocommerceStore.getWooCommerceSites()[selectedSitePosition]

            val result = wcProductStore.addProductCategories(site, categories)

            if (result.isError) {
                prependToLog("Error adding product categories: ${result.error?.message}")
                return@launch
            }

            prependToLog("Product categories created successfully: ${result.model?.map { it.name }}")

            productCategories = wcProductStore.getProductCategoriesForSite(site).map {
                ProductCategory(it.remoteCategoryId, it.name, it.slug)
            }
            productCategoriesAdapter.setProductCategories(productCategories!!.toViewHolders())
        }
    }

    private fun List<ProductCategory>.toViewHolders() = map { productCategory ->
        ProductCategoryViewHolderModel(
            category = productCategory,
            isSelected = selectedProductCategories?.any { it.name == productCategory.name } ?: false
        )
    }
}
