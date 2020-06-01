package org.wordpress.android.fluxc.example.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import java.util.ArrayList

class ListSelectorDialog : DialogFragment() {
    companion object {
        const val LIST_SELECTOR_REQUEST_CODE = 1000
        const val ARG_LIST_SELECTED_ITEM = "ARG_LIST_SELECTED_ITEM"
        const val ARG_LIST_ITEMS = "ARG_LIST_ITEMS"
        const val ARG_RESULT_CODE = "ARG_RESULT_CODE"
        @JvmStatic
        fun newInstance(
            fragment: Fragment,
            listItems: List<String>,
            resultCode: Int,
            selectedListItem: String?
        ) = ListSelectorDialog().apply {
            setTargetFragment(fragment, LIST_SELECTOR_REQUEST_CODE)
            this.resultCode = resultCode
            this.listItems = listItems
            this.selectedListItem = selectedListItem
        }
    }

    var resultCode: Int = -1
    var listItems: List<String>? = null
    var selectedListItem: String? = null

    override fun onResume() {
        super.onResume()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            listItems = it.getStringArrayList(ARG_LIST_ITEMS)
            resultCode = it.getInt(ARG_RESULT_CODE)
            selectedListItem = it.getString(ARG_LIST_SELECTED_ITEM)
        }

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select a list item")
                    .setSingleChoiceItems(listItems?.toTypedArray(), getListIndex()) { dialog, which ->
                        val intent = activity?.intent
                        intent?.putExtra(ARG_LIST_SELECTED_ITEM, listItems?.get(which))
                        targetFragment?.onActivityResult(LIST_SELECTOR_REQUEST_CODE, resultCode, intent)
                        dialog.dismiss()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getListIndex(): Int {
        return listItems?.indexOfFirst { it == selectedListItem } ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(ARG_LIST_ITEMS, listItems as ArrayList<String>?)
        outState.putInt(ARG_RESULT_CODE, resultCode)
        outState.putString(ARG_LIST_SELECTED_ITEM, selectedListItem)
    }
}
