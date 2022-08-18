package org.wordpress.android.fluxc.example.ui.customer.search

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_woo_customers_search_builder.*
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.util.ToastUtils

class WooCustomersSearchBuilderFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_customers_search_builder, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCustomerSearch.setOnClickListener {
            replaceFragment(
                    WooCustomersSearchFragment.newInstance(
                            siteId = requireArguments().getInt(KEY_SELECTED_SITE_ID),
                            searchParams = buildParams()
                    )
            )
        }
    }

    private fun buildParams() = WooCustomersSearchFragment.SearchParams(
            searchQuery = etCustomerSearch.text?.toString(),
            includeIds = etCustomerInclude.text.toIds(),
            excludeIds = etCustomerExclude.text.toIds(),
            email = etCustomerEmail.text?.toString(),
            role = etCustomerRole.text?.toString()
    )

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun Editable?.toIds(): List<Long> {
        return try {
            if (this.isNullOrEmpty()) return emptyList()
            this.toString().split(",\\s*").map { it.toLong() }
        } catch (e: Exception) {
            ToastUtils.showToast(requireContext(), "Wrongly formatted ids")
            emptyList()
        }
    }

    companion object {
        fun newInstance(siteId: Int) = WooCustomersSearchBuilderFragment().apply {
            arguments = Bundle().apply { putInt(KEY_SELECTED_SITE_ID, siteId) }
        }
    }
}

private const val KEY_SELECTED_SITE_ID = "selected_site_id"
