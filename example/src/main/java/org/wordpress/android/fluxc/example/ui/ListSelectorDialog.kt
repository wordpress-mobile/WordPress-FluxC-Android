package org.wordpress.android.fluxc.example.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

class ListSelectorDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(listItems: List<String>, listener: Listener) = ListSelectorDialog().apply {
            this.listener = listener
            this.listItems = listItems
        }
    }

    interface Listener {
        fun onListItemSelected(selectedItem: String?)
    }

    var listener: Listener? = null
    var listItems: List<String>? = null

    override fun onResume() {
        super.onResume()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select a list item")
                    .setSingleChoiceItems(listItems?.toTypedArray(), 0) { dialog, which ->
                        listener?.onListItemSelected(listItems?.get(which))
                        dialog.dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
