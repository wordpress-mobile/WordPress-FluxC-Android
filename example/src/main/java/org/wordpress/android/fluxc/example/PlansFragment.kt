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
import org.wordpress.android.fluxc.example.databinding.FragmentPlansBinding
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.store.PlansStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class PlansFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var plansStore: PlansStore
    @Inject internal lateinit var siteStore: SiteStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentPlansBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentPlansBinding.bind(view)) {
            fetchPlans.setOnClickListener {
                lifecycleScope.launch {
                    val plansResult = plansStore.fetchPlans()
                    when {
                        plansResult.isError -> {
                            prependToLog("Error fetching plans: ${plansResult.error}")
                        }
                        else -> {
                            val plans = plansResult.plans
                                ?.joinToString(separator = "\n") {
                                    "${it.productName} (${it.billPeriodLabel}): ${it.price}"
                                }
                            prependToLog("Plans:\n$plans")
                        }
                    }
                }
            }

            fetchSitePlan.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val plansResult = siteStore.fetchSitePlans(site)
                        when {
                            plansResult.isError -> {
                                prependToLog("Error fetching site plan: ${plansResult.error.type}")
                            }
                            else -> {
                                val plan = plansResult.plans?.firstOrNull { it.isCurrentPlan }
                                if (plan != null) {
                                    prependToLog("Current site plan: ${plan.productName}" +
                                        "; has domain credit: ${plan.hasDomainCredit}")
                                } else {
                                   prependToLog("The site has no active plan")
                                }
                            }
                        }
                    }
                } ?: prependToLog("Select a site first")
            }
        }
    }
}
