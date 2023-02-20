package org.wordpress.android.fluxc.example.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_onboarding.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.store.OnboardingStore
import javax.inject.Inject

class WooOnboardingFragment : StoreSelectingFragment() {
    @Inject internal lateinit var onboardingStore: OnboardingStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_woo_onboarding, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
    }
}
