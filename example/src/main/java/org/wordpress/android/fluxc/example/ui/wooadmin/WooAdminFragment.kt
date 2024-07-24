package org.wordpress.android.fluxc.example.ui.wooadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.databinding.FragmentWooAdminBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.admin.WooAdminStore
import javax.inject.Inject

class WooAdminFragment : StoreSelectingFragment() {
    @Inject internal lateinit var wooAdminStore: WooAdminStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooAdminBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooAdminBinding.bind(view)) {
            fetchOptions.setOnClickListener {
                lifecycleScope.launch {
                    selectedSite?.let { site ->
                        val keys = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please enter the keys to fetch separated by a comma",
                            isNumeric = false
                        )?.split(",")?.map { it.trim() } ?: return@launch
                        wooAdminStore.getOptions(site, keys).let { result ->
                            when {
                                result.isError -> prependToLog("Fetching Admin Options failed, " +
                                    "${result.error.type}: ${result.error.message}")
                                else -> prependToLog(
                                    "Admin Options: ${
                                        result.model!!.entries.joinToString { entry ->
                                            "${entry.key}:${entry.value}"
                                        }
                                    }"
                                )
                            }
                        }
                    }
                }
            }

            updateOptions.setOnClickListener {
                lifecycleScope.launch {
                    selectedSite?.let { site ->
                        val keyValues = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please enter the options to update in the format key:value separated by a comma",
                            isNumeric = false
                        )?.split(",")?.map { keyValue ->
                            keyValue.trim().split(":").let { Pair(it[0], it[1]) }
                        } ?: return@launch

                        wooAdminStore.updateOptions(site, keyValues.toMap()).let { result ->
                            when {
                                result.isError -> prependToLog("Updating Admin Options failed, " +
                                    "${result.error.type}: ${result.error.message}")
                                else -> prependToLog("Admin options updated successfully")
                            }
                        }
                    }
                }
            }
        }
    }
}
