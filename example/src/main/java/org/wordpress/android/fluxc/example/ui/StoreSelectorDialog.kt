package org.wordpress.android.fluxc.example.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class StoreSelectorDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(listener: Listener, selectedPos: Int) = StoreSelectorDialog().apply {
            this.listener = listener
            this.selectedPos = selectedPos
        }
    }

    interface Listener {
        fun onSiteSelected(site: SiteModel, pos: Int)
    }

    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    var listener: Listener? = null
    var selectedPos: Int = -1

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val adapter = SiteAdapter(it, wooCommerceStore.getWooCommerceSites())

            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select a site")
                    .setSingleChoiceItems(adapter, selectedPos) { dialog, which ->
                        val adapter = (dialog as AlertDialog).listView.adapter as SiteAdapter
                        val site = adapter.getItem(which)
                        if (site != null) {
                            listener?.onSiteSelected(site, which)
                        } else {
                            prependToLog("SiteChanged error: site at position $which was null.")
                        }
                        dialog.dismiss()
                    }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    class SiteAdapter(
        ctx: Context,
        items: MutableList<SiteModel>
    ) : ArrayAdapter<SiteModel>(ctx, android.R.layout.simple_list_item_1, items) {
        fun refreshSites(newItems: List<SiteModel>) {
            setNotifyOnChange(true)
            addAll(newItems)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val cv = convertView
                    ?: LayoutInflater.from(context)
                            .inflate(android.R.layout.simple_list_item_single_choice, parent, false)
            val site = getItem(position)
            (cv as TextView).text = site?.displayName ?: site?.name
            return cv
        }

        override fun getItemId(position: Int) = getItem(position)!!.id.toLong()

        override fun hasStableIds() = true
    }
}
