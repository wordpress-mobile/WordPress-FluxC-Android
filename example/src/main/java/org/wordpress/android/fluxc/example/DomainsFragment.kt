package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentDomainsBinding
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class DomainsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var store: SiteStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentDomainsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentDomainsBinding.bind(view)) {
            fetchDomains.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val result = store.fetchSiteDomains(site)
                        when {
                            result.isError -> {
                                prependToLog("Error fetching plans: ${result.error.type}")
                            }
                            else -> {
                                val plans = result.domains
                                    ?.joinToString(separator = "\n") {
                                        "${it.domain} (Renewal: ${it.autoRenewalDate}), primary: ${it.primaryDomain}"
                                    }
                                prependToLog("Domains:\n$plans")
                            }
                        }
                    }
                } ?: prependToLog("Select a site first")
            }

            fetchDomainPrice.setOnClickListener {
                showSingleLineDialog(activity, "Enter a domain name:") { editText ->
                    lifecycleScope.launch {
                        val domain = editText.text.toString()
                        when (val result = store.fetchDomainPrice(editText.text.toString())) {
                            is Error -> {
                                prependToLog("Error fetching domain price: ${result.error.type}")
                            }
                            is Success -> {
                                prependToLog("$domain price: ${result.data?.cost}")
                            }
                        }
                    }
                }
            }

            fetchAllDomains.setOnClickListener {
                lifecycleScope.launch {
                    // fetching wpcom too for debugging purposes
                    val result = store.fetchAllDomains(noWpCom = false, resolveStatus = true)
                    when {
                        result.isError -> {
                            prependToLog("Error fetching all domains: ${result.error.message}")
                        }
                        else -> {
                            prependToLog("All domains count: ${result.domains?.size}")
                            val domains = result.domains
                                ?.joinToString(separator = "\n") {
                                    "${it.domain} (type: ${it.type}, expiry: ${it.expiry}, " +
                                        "status: ${it.domainStatus?.statusType.toString()})"
                                }
                            prependToLog("Domains:\n$domains")
                        }
                    }
                }
            }
        }
    }
}
