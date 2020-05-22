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
        fetch_theme.setOnClickListener(::onClick)
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
    }

    private fun onClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val site = this.site ?: return
        val payload = FetchEditorThemePayload(site)
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))
    }

    private fun logTheme(theme: EditorTheme) {
        val colors = theme.themeSupport.colors?.map { it.slug }?.joinToString(",\n\t")
        val gradients = theme.themeSupport.gradients?.map { it.slug }?.joinToString(",\n\t")
        prependToLog("Found: \n colors:\n\t${colors}\n gradients:\n\t${gradients}")
    }
}
