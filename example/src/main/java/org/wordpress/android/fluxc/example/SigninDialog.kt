package org.wordpress.android.fluxc.example

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.signing_dialog.*
import kotlinx.android.synthetic.main.signing_dialog.view.*
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
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.signing_dialog, null)
        view.url.onTextChanged {
            view.xmlrpc_check.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        builder.setView(view)
            .setPositiveButton(android.R.string.ok) { dialog, id ->
                listener?.onClick(
                    username = view.username.text.toString(),
                    password = view.password.text.toString(),
                    url = view.url.text.toString(),
                    isXmlrpc = view.xmlrpc_check.isChecked
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
        return builder.create()
    }
}
