package org.wordpress.android.fluxc.example

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_woocommerce.*
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

class WooCommerceFragment : Fragment() {
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    override fun onAttach(context: Context?) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woocommerce, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log_sites.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                prependToLog(site.name + ": " + if (site.isWpComStore) "WP.com store" else "Self-hosted store")
                AppLog.i(T.API, LogUtils.toString(site))
            }
        }
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
