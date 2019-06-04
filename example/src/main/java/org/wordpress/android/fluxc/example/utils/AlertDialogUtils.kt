package org.wordpress.android.fluxc.example.utils

import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AlertDialog
import android.widget.EditText

typealias AlertTextListener = (EditText) -> Unit

fun showSingleLineDialog(activity: androidx.fragment.app.FragmentActivity?, message: String, alertTextListener: AlertTextListener) {
    if (activity == null) return
    val alert = AlertDialog.Builder(activity)
    val editText = EditText(activity)
    editText.setSingleLine()
    alert.setMessage(message)
    alert.setView(editText)
    alert.setPositiveButton(android.R.string.ok, { _, _ -> alertTextListener(editText) })
    alert.show()
}
