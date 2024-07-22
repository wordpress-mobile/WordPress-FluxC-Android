package org.wordpress.android.fluxc.example.ui.customer.search

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wordpress.android.fluxc.example.databinding.FragmentWooCustomersSearchBuilderBinding
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.util.ToastUtils

class WooCustomersSearchBuilderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooCustomersSearchBuilderBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooCustomersSearchBuilderBinding.bind(view)) {
            btnCustomerSearch.setOnClickListener {
                replaceFragment(
                    WooCustomersSearchFragment.newInstance(
                        siteId = requireArguments().getInt(KEY_SELECTED_SITE_ID),
                        searchParams = buildParams()
                    )
                )
            }
        }
    }

    private fun FragmentWooCustomersSearchBuilderBinding.buildParams() =
        WooCustomersSearchFragment.SearchParams(
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
