package org.wordpress.android.fluxc.example.ui.leaderboards

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooLeaderboardsFragment : Fragment(), StoreSelectorDialog.Listener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcLeaderboardsStore: WCLeaderboardsStore

    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_leaderboards, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderboards_select_site.setOnClickListener(::onLeaderboardsSelectSiteButtonClicked)
    }

    private fun onLeaderboardsSelectSiteButtonClicked(view: View) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(this, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }

    override fun onSiteSelected(site: SiteModel, pos: Int) {
        selectedSite = site
        selectedPos = pos
        buttonContainer.toggleSiteDependentButtons()
        leaderboards_selected_site.text = site.name ?: site.displayName
    }
}
