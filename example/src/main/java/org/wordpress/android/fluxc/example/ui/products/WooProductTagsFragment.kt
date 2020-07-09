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
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsAdapter.OnProductTagClickListener
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsAdapter.ProductTagViewHolderModel
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment.ProductTag

class WooProductTagsFragment : Fragment(), OnProductTagClickListener {
    private var resultCode: Int = -1
    private var productTags: List<ProductTag>? = null
    private var selectedProductTags: MutableList<ProductTag>? = null

    private lateinit var productTagsAdapter: WooProductTagsAdapter

    companion object {
        const val PRODUCT_TAGS_REQUEST_CODE = 3000
        const val ARG_RESULT_CODE = "ARG_RESULT_CODE"
        const val ARG_PRODUCT_TAGS = "ARG_PRODUCT_TAGS"
        const val ARG_SELECTED_PRODUCT_TAGS = "ARG_SELECTED_PRODUCT_TAGS"

        fun newInstance(
            fragment: Fragment,
            resultCode: Int,
            productTags: List<ProductTag>,
            selectedProductTags: MutableList<ProductTag>?
        ) = WooProductTagsFragment().apply {
            this.setTargetFragment(fragment, PRODUCT_TAGS_REQUEST_CODE)
            this.resultCode = resultCode
            this.productTags = productTags
            this.selectedProductTags = selectedProductTags
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
            productTags = it.getParcelableArrayList(ARG_PRODUCT_TAGS)
            selectedProductTags = it.getParcelableArrayList(ARG_SELECTED_PRODUCT_TAGS)
        }

        productTagsAdapter = WooProductTagsAdapter(requireContext(), this)
        with(category_list) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productTagsAdapter
        }

        val allTags = productTags?.map { productTag ->
            ProductTagViewHolderModel(
                    tag = productTag,
                    isSelected = selectedProductTags?.any { it.name == productTag.name } ?: false
            )
        } ?: emptyList()

        productTagsAdapter.setProductTags(allTags.toList())

        btn_done.setOnClickListener {
            val intent = activity?.intent
            intent?.putParcelableArrayListExtra(
                    ARG_SELECTED_PRODUCT_TAGS, selectedProductTags as? ArrayList
            )
            targetFragment?.onActivityResult(PRODUCT_TAGS_REQUEST_CODE, resultCode, intent)
            fragmentManager?.popBackStack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_RESULT_CODE, resultCode)
        productTags?.let {
            outState.putParcelableArrayList(ARG_PRODUCT_TAGS, it as? ArrayList)
        }
        selectedProductTags?.let {
            outState.putParcelableArrayList(ARG_SELECTED_PRODUCT_TAGS, it as? ArrayList)
        }
    }

    override fun onProductTagClick(productTagViewHolderModel: ProductTagViewHolderModel) {
        val found = selectedProductTags?.find {
            it.name == productTagViewHolderModel.tag.name
        }
        if (!productTagViewHolderModel.isSelected && found != null) {
            selectedProductTags?.remove(found)
        } else if (productTagViewHolderModel.isSelected && found == null) {
            selectedProductTags?.add(productTagViewHolderModel.tag)
        }
    }
}
