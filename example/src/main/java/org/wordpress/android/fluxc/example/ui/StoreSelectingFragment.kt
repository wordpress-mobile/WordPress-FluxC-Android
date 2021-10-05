package org.wordpress.android.fluxc.example.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_woo_products.*
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.ui.common.showStoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.model.SiteModel

abstract class StoreSelectingFragment : DaggerFragment() {
    protected var selectedPos: Int = -1
    protected var selectedSite: SiteModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<LinearLayout>(R.id.buttonContainer).addView(
                Button(context).apply {
                    text = "Select Site"
                    setOnClickListener {
                        showStoreSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                            override fun onSiteSelected(site: SiteModel, pos: Int) {
                                selectedSite = site
                                selectedPos = pos
                                buttonContainer.toggleSiteDependentButtons(true)
                                text = site.name ?: site.displayName
                            }
                        })
                    }
                },
                0
        )
    }
}
