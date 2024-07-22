package org.wordpress.android.fluxc.example.ui.helpsupport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.databinding.FragmentWooHelpSupportBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooHelpSupportFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooHelpSupportBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooHelpSupportBinding.bind(view)) {
            fetchSsr.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.Default) {
                            wooCommerceStore.fetchSSR(site)
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
}
