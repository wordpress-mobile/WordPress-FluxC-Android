package org.wordpress.android.fluxc.example.utils

import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity

typealias AlertTextListener = (EditText) -> Unit

fun showSingleLineDialog(
    activity: FragmentActivity?,
    message: String,
    isNumeric: Boolean = false,
    alertTextListener: AlertTextListener
) {
    if (activity == null) return
    val alert = AlertDialog.Builder(activity)
    val editText = EditText(activity)
    editText.setSingleLine()
    if (isNumeric) editText.inputType = InputType.TYPE_CLASS_NUMBER
    alert.setMessage(message)
    alert.setView(editText)
    alert.setPositiveButton(android.R.string.ok) { _, _ -> alertTextListener(editText) }
    alert.show()
}
