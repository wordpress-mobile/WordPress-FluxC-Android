package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.example.databinding.FragmentReactnativeBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class ReactNativeFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var reactNativeStore: ReactNativeStore

    val site: SiteModel? by lazy {
        siteStore.sites.firstOrNull {
            it.url != null
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentReactnativeBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentReactnativeBinding.bind(view)) {
            topTextView.text = site?.url?.let { url ->
                """All calls are made to 
                |$url.
                |Only enter the rest endpoint you are trying to hit.
                |For example, with a self-hosted site instead of entering 
                |'https://mysite.com/wp-json/wp/v2/media',
                |just enter 'wp/v2/media'.
                |For a WP.com site, instead of entering
                |'https://public-api.wordpress.com/wp/v2/sites/12345689/media',
                |just enter 'wp/v2/media' (site id is handled automatically)""".trimMargin()
            } ?: "Site with url not loaded. All calls will fail."

            pathField.setOnEditorActionListener { v, actionId, event ->
                when (actionId) {
                    EditorInfo.IME_ACTION_SEND -> {
                        onClick(v)
                        true
                    }
                    else -> false
                }
            }

            requestButton.setOnClickListener { onClick(it) }
        }
    }

    private fun FragmentReactnativeBinding.onClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val path = pathField.text.toString()
        prependToLog("Making request: $path")
        lifecycleScope.launch(Dispatchers.IO) {
            val response = site?.let { reactNativeStore.executeGetRequest(it, path) }
            withContext(Dispatchers.Main) {
                when (response) {
                    is Success -> {
                        prependToLog("Request succeeded")
                        AppLog.i(AppLog.T.API, "Request result: ${response.result}")
                    }
                    is Error -> prependToLog("Request to '$path' failed: ${response.error.message}")
                    null -> Unit // Do nothing
                }
            }
        }
    }
}
