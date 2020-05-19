package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_editor_theme.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class EditorThemeFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var editorThemeStore: EditorThemeStore

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

    private fun onClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        val theme = site?.let { editorThemeStore.getEditorThemeForSite(it) }
        val colors = (theme?.getParcelableArrayList<Bundle>("colors")?.map { it.getString("slug") }?.joinToString(", "))
        val gradients = (theme?.getParcelableArrayList<Bundle>("gradients")?.map { it.getString("slug") }?.joinToString(", "))

        prependToLog("Found: \n colors: ${colors} \n gradients: ${gradients}")
    }
}
