package org.wordpress.android.fluxc.example.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.*
import kotlinx.android.synthetic.main.fragment_woo_leaderboards.buttonContainer
import kotlinx.android.synthetic.main.fragment_woo_product_attribute.*
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel

class WooProductAttributeFragment : Fragment(), StoreSelectorDialog.Listener {
    private var selectedPos: Int = -1
    private var selectedSite: SiteModel? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onSiteSelected(site: SiteModel, pos: Int) {
        selectedSite = site
        selectedPos = pos
        buttonContainer.toggleSiteDependentButtons()
        leaderboards_selected_site.text = site.name ?: site.displayName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.fragment_woo_product_attribute, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attributes_select_site.setOnClickListener(::onProductAttributesSelectSiteButtonClicked)
    }

    private fun onProductAttributesSelectSiteButtonClicked(view: View) {
        fragmentManager?.let { fm ->
            StoreSelectorDialog.newInstance(this, selectedPos)
                    .show(fm, "StoreSelectorDialog")
        }
    }
}