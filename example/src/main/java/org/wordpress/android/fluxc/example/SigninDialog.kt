package org.wordpress.android.fluxc.example

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.wordpress.android.fluxc.example.databinding.SigningDialogBinding
import org.wordpress.android.fluxc.example.utils.onTextChanged

class SigninDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(
            onClickListener: SigningListener
        ): SigninDialog {
            val fragment = SigninDialog()
            fragment.listener = onClickListener
            return fragment
        }
    }

    interface SigningListener {
        fun onClick(username: String, password: String, url: String, isXmlrpc: Boolean)
    }

    private var listener: SigningListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        with(SigningDialogBinding.inflate(requireActivity().layoutInflater)) {
            val builder = AlertDialog.Builder(requireActivity())
            url.onTextChanged {
                xmlrpcCheck.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
            builder.setView(root)
                .setPositiveButton(android.R.string.ok) { dialog, id ->
                    listener?.onClick(
                        username = username.text.toString(),
                        password = password.text.toString(),
                        url = url.text.toString(),
                        isXmlrpc = xmlrpcCheck.isChecked
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
            return builder.create()
        }
    }
}
