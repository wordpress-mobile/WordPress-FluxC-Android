package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_product_categories.*
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesAdapter.OnProductCategoryClickListener
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesAdapter.ProductCategoryViewHolderModel
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment.ProductCategory

class WooProductCategoriesFragment : Fragment(), OnProductCategoryClickListener {
    private var resultCode: Int = -1
    private var productCategories: List<ProductCategory>? = null
    private var selectedProductCategories: MutableList<ProductCategory>? = null

    private lateinit var productCategoriesAdapter: WooProductCategoriesAdapter

    companion object {
        const val PRODUCT_CATEGORIES_REQUEST_CODE = 2000
        const val ARG_RESULT_CODE = "ARG_RESULT_CODE"
        const val ARG_PRODUCT_CATEGORIES = "ARG_PRODUCT_CATEGORIES"
        const val ARG_SELECTED_PRODUCT_CATEGORIES = "ARG_SELECTED_PRODUCT_CATEGORIES"

        fun newInstance(
            fragment: Fragment,
            resultCode: Int,
            productCategories: List<ProductCategory>,
            selectedProductCategories: MutableList<ProductCategory>?
        ) = WooProductCategoriesFragment().apply {
            this.setTargetFragment(fragment, PRODUCT_CATEGORIES_REQUEST_CODE)
            this.resultCode = resultCode
            this.productCategories = productCategories
            this.selectedProductCategories = selectedProductCategories
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
        }

        productCategoriesAdapter = WooProductCategoriesAdapter(requireContext(), this)
        with(category_list) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productCategoriesAdapter
        }

        val allCategories = productCategories?.map { productCategory ->
            ProductCategoryViewHolderModel(
                    category = productCategory,
                    isSelected = selectedProductCategories?.any { it.name == productCategory.name } ?: false
            )
        } ?: emptyList()

        productCategoriesAdapter.setProductCategories(allCategories.toList())

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
}
