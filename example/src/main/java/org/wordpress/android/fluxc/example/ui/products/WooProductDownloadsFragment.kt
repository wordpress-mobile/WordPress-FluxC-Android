package org.wordpress.android.fluxc.example.ui.products

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_woo_product_categories.*
import kotlinx.android.synthetic.main.fragment_woo_product_categories.btn_done
import kotlinx.android.synthetic.main.fragment_woo_product_downloads.*
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.model.WCProductFileModel

class WooProductDownloadsFragment : Fragment() {
    companion object {
        const val PRODUCT_DOWNLOADS_REQUEST_CODE = 4000
        const val ARG_PRODUCT_DOWNLOADS = "ARG_PRODUCT_DOWNLOADS"

        fun newInstance(
            fragment: Fragment,
            productDownloads: List<ProductFile>
        ) = WooProductDownloadsFragment().apply {
            this.setTargetFragment(fragment, PRODUCT_DOWNLOADS_REQUEST_CODE)
            productDownloadsAdapter.filesList = productDownloads
        }
    }

    private val productDownloadsAdapter: WooProductDownloadsAdapter by lazy { WooProductDownloadsAdapter() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_product_downloads, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            productDownloadsAdapter.filesList = it.getParcelableArrayList(ARG_PRODUCT_DOWNLOADS) ?: mutableListOf()
        }

        with(files_list) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productDownloadsAdapter
        }

        btn_add.setOnClickListener { productDownloadsAdapter.filesList = productDownloadsAdapter.filesList + ProductFile(null, "", "") }

        btn_done.setOnClickListener {
            val intent = activity?.intent
            intent?.putParcelableArrayListExtra(
                    ARG_PRODUCT_DOWNLOADS, productDownloadsAdapter.filesList as ArrayList
            )
            targetFragment?.onActivityResult(
                    PRODUCT_DOWNLOADS_REQUEST_CODE,
                    Activity.RESULT_OK,
                    intent
            )
            fragmentManager?.popBackStack()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        productDownloadsAdapter.filesList.let {
            outState.putParcelableArrayList(ARG_PRODUCT_DOWNLOADS, it as ArrayList)
        }
    }

    @Parcelize
    data class ProductFile(
        val id: String?,
        var name: String,
        var url: String
    ) : Parcelable {
        fun toWCProductFileModel(): WCProductFileModel {
            return WCProductFileModel(this.id, this.name, this.url)
        }
    }
}
