package org.wordpress.android.fluxc.example.utils

import android.app.Activity
import android.app.AlertDialog
import android.widget.EditText

typealias AlertTextListener = (EditText) -> Unit

fun showSingleLineDialog(activity: Activity, message: String, alertTextListener: AlertTextListener) {
    val alert = AlertDialog.Builder(activity)
    val editText = EditText(activity)
    editText.setSingleLine()
    alert.setMessage(message)
    alert.setView(editText)
    alert.setPositiveButton(android.R.string.ok, { _, _ -> alertTextListener(editText) })
    alert.show()
}
