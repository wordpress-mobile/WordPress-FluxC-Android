package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_jetpackai.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse.Success
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import javax.inject.Inject

class JetpackAIFragment : StoreSelectingFragment() {
    @Inject internal lateinit var store: JetpackAIStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_jetpackai, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        generate_haiku.setOnClickListener {
            selectedSite?.let {
                lifecycleScope.launch {
                    val result = store.fetchJetpackAICompletions(
                        site = it,
                        prompt = "Please make me a haiku",
                        feature = "fluxc-example"
                    )

                    when (result) {
                        is Success -> {
                            prependToLog("Haiku:\n${result.completion}")
                        }
                        is Error -> {
                            prependToLog("Error fetching haiku: ${result.message}")
                        }
                    }
                }
            }
        }
    }
}
