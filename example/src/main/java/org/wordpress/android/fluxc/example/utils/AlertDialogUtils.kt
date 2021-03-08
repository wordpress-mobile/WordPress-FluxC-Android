package org.wordpress.android.fluxc.example.utils

import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

suspend fun showSingleLineDialog(
    activity: FragmentActivity,
    message: String,
    isNumeric: Boolean = false,
    defaultValue: String = ""
): String? = suspendCancellableCoroutine { continuation ->
    val alert = AlertDialog.Builder(activity)
    val editText = EditText(activity)
    editText.setSingleLine()
    editText.setText(defaultValue)
    if (isNumeric) editText.inputType = InputType.TYPE_CLASS_NUMBER
    alert.setMessage(message)
    alert.setView(editText)
    alert.setPositiveButton(android.R.string.ok) { _, _ ->
        continuation.resume(editText.text.toString().ifEmpty { null })
    }
    alert.setOnDismissListener {
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }
    alert.show()
}
