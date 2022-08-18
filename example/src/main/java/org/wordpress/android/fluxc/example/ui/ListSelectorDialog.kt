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
        const val LIST_MULTI_SELECTOR_REQUEST_CODE = 2000
        const val ARG_LIST_SELECTED_ITEM = "ARG_LIST_SELECTED_ITEM"
        const val ARG_LIST_SELECTED_ITEMS = "ARG_LIST_SELECTED_ITEMS"
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

        fun newInstance(
            fragment: Fragment,
            items: List<ListItem>,
            resultCode: Int
        ) = ListSelectorDialog().apply {
            setTargetFragment(fragment, LIST_SELECTOR_REQUEST_CODE)
            this.resultCode = resultCode
            this.items = items.toMutableList()
            this.isSingleChoice = false
        }
    }

    var resultCode: Int = -1
    var listItems: List<String>? = null
    var items = mutableListOf<ListItem>()
    var selectedListItem: String? = null
    var selected: String? = null
    var isSingleChoice = true

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            listItems = it.getStringArrayList(ARG_LIST_ITEMS)
            resultCode = it.getInt(ARG_RESULT_CODE)
            selectedListItem = it.getString(ARG_LIST_SELECTED_ITEM)
        }

        val builder = AlertDialog.Builder(requireActivity())

        if (isSingleChoice) {
            builder.setTitle("Select a list item")
                .setSingleChoiceItems(listItems?.toTypedArray(), getListIndex()) { dialog, which ->
                    val intent = activity?.intent
                    intent?.putExtra(ARG_LIST_SELECTED_ITEM, listItems?.get(which))
                    targetFragment?.onActivityResult(LIST_SELECTOR_REQUEST_CODE, resultCode, intent)
                    dialog.dismiss()
                }
        } else {
            val checked = items.map { item -> item.isSelected }.toBooleanArray()
            builder.setTitle("Select multiple items")
                .setMultiChoiceItems(items.map { it.title }.toTypedArray(), checked) { _, which, isChecked ->
                    items[which] = items[which].copy(isSelected = isChecked)
                }
                .setPositiveButton("OK") { dialog, _ ->
                    val intent = activity?.intent
                    val selectedIds = items.filter { it.isSelected }.map { it.id }.toLongArray()
                    intent?.putExtra(ARG_LIST_SELECTED_ITEMS, selectedIds)
                    targetFragment?.onActivityResult(LIST_MULTI_SELECTOR_REQUEST_CODE, resultCode, intent)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
        }

        return builder.create()
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

    data class ListItem(val id: Long, val title: String, val isSelected: Boolean)
}
