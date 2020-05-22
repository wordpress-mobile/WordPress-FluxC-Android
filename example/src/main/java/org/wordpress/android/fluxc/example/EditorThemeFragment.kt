package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_editor_theme.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.EditorThemeStore.FetchEditorThemePayload
import org.wordpress.android.fluxc.store.EditorThemeStore.OnEditorThemeChanged
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class EditorThemeFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var editorThemeStore: EditorThemeStore
    @Inject internal lateinit var dispatcher: Dispatcher

    val site: SiteModel? by lazy {
        siteStore.sites.firstOrNull {
            it.url != null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_editor_theme, container, false)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetch_theme.setOnClickListener(::onFetchThemeClick)
        fetch_cached_theme.setOnClickListener(::onFetchCachedThemeClick)
        clear_cache.setOnClickListener(::onDeleteCacheClick)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEditorThemeChanged(event: OnEditorThemeChanged) {
        if (event.isError) {
            prependToLog("Error from FETCH_EDITOR_THEME - error: " + event.error)
            return
        }

        val theme = event.editorTheme ?: return
        logTheme(theme)
        prependToLog("Fetched Theme")
    }

    private fun onFetchThemeClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val site = this.site ?: return
        if (editorThemeStore.getEditorThemeForSite(site) != null) {
            prependToLog("Has Cached Theme")
        }

        val payload = FetchEditorThemePayload(site)
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))
        prependToLog("Fetching Theme")
    }

    private fun onDeleteCacheClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val site = this.site ?: return
        EditorThemeSqlUtils().deleteEditorThemeForSite(site)
    }

    private fun onFetchCachedThemeClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val site = this.site ?: return
        val theme = editorThemeStore.getEditorThemeForSite(site)

        if (theme != null) {
            logTheme(theme)
            prependToLog("Found Cached Theme")
        } else {
            prependToLog("No theme Cached")
        }
    }

    private fun logTheme(theme: EditorTheme) {
        val colors = theme.themeSupport.colors?.map { it.slug }?.joinToString(", ")
        val gradients = theme.themeSupport.gradients?.map { it.slug }?.joinToString(",")
        prependToLog("Found: \n colors: [${colors}] \n gradients: [${gradients}]")
    }
}
