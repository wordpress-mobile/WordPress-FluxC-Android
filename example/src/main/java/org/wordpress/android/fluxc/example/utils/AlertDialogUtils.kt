package org.wordpress.android.fluxc.example.utils

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity

typealias AlertTextListener = (EditText) -> Unit

fun showSingleLineDialog(activity: FragmentActivity?, message: String, alertTextListener: AlertTextListener) {
    if (activity == null) return
    val alert = AlertDialog.Builder(activity)
    val editText = EditText(activity)
    editText.setSingleLine()
    alert.setMessage(message)
    alert.setView(editText)
    alert.setPositiveButton(android.R.string.ok) { _, _ -> alertTextListener(editText) }
    alert.show()
}
