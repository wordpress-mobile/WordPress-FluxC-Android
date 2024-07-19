package org.wordpress.android.fluxc.example.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentWooOnboardingBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.store.OnboardingStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

internal class WooOnboardingFragment : StoreSelectingFragment() {
    @Inject lateinit var onboardingStore: OnboardingStore
    @Inject lateinit var siteStore: SiteStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooOnboardingBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooOnboardingBinding.bind(view)) {
            btnFetchOnboardingTasks.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            onboardingStore.fetchOnboardingTasks(site)
                        }

                        result.error?.let {
                            prependToLog("${it.type}: ${it.message}")
                            return@launch
                        }
                        result.model?.let {
                            prependToLog("Fetched data: $it")
                        }
                    }
                }
            }

            btnLaunchSite.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val result = siteStore.launchSite(site)
                        when {
                            result.isError -> {
                                prependToLog(
                                    "Error launching site. Type:${result.error.type} \n " +
                                        "Message: ${result.error.message}"
                                )
                            }
                            else -> prependToLog("Site launched success")
                        }
                    }
                }
            }
        }
    }
}
