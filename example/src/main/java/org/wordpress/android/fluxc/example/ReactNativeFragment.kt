package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_reactnative.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_reactnative, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        top_text_view.text = site?.url?.let { url ->
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

        path_field.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    onClick(v)
                    true
                }
                else -> false
            }
        }

        request_button.setOnClickListener(::onClick)
    }

    private fun onClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        GlobalScope.launch(Dispatchers.Main) {
            val path = path_field.text.toString()
            prependToLog("Making request: $path")
            val response = withContext(Dispatchers.IO) {
                site?.let { reactNativeStore.executeRequest(it, path) }
            }

            when (response) {
                is Success -> {
                    prependToLog("Request succeeded")
                    AppLog.i(AppLog.T.API, "Request result: ${response.result}")
                }
                is Error -> prependToLog("Request to '$path' failed: ${response.error.message}")
            }
        }
    }
}
