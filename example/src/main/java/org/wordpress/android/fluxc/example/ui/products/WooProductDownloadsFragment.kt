package org.wordpress.android.fluxc.example.ui.products

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.example.databinding.FragmentWooProductDownloadsBinding
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooProductDownloadsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooProductDownloadsBinding.bind(view)) {
            savedInstanceState?.let {
                productDownloadsAdapter.filesList = it.getParcelableArrayList(ARG_PRODUCT_DOWNLOADS) ?: mutableListOf()
            }

            with(filesList) {
                layoutManager = LinearLayoutManager(activity)
                adapter = productDownloadsAdapter
            }

            btnAdd.setOnClickListener {
                productDownloadsAdapter.filesList = productDownloadsAdapter.filesList +
                    ProductFile(null, "", "")
            }

            btnDone.setOnClickListener {
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        productDownloadsAdapter.filesList.let {
            outState.putParcelableArrayList(ARG_PRODUCT_DOWNLOADS, it as ArrayList)
        }
    }

    @Parcelize
    @Suppress("DataClassShouldBeImmutable")
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
