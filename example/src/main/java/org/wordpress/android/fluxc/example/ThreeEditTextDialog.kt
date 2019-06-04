package org.wordpress.android.fluxc.example

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.text.TextUtils
import android.view.View
import android.widget.EditText

class ThreeEditTextDialog : androidx.fragment.app.DialogFragment() {
    private lateinit var editText1: EditText
    private lateinit var editText2: EditText
    private lateinit var editText3: EditText

    interface Listener {
        fun onClick(text1: String, text2: String, text3: String)
    }

    private var listener: Listener? = null
    var hint1 = ""
    var hint2 = ""
    var hint3 = ""

    fun setListener(onClickListener: Listener) {
        listener = onClickListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.signin_dialog, null)
            editText1 = view.findViewById(R.id.text1) as EditText
            editText2 = view.findViewById(R.id.text2) as EditText
            editText3 = view.findViewById(R.id.text3) as EditText

            editText1.hint = hint1
            editText2.hint = hint2
            editText3.hint = hint3
            if (TextUtils.isEmpty(hint1)) {
                editText1.visibility = View.GONE
            }
            if (TextUtils.isEmpty(hint2)) {
                editText2.visibility = View.GONE
            }
            if (TextUtils.isEmpty(hint3)) {
                editText3.visibility = View.GONE
            }
            builder.setView(view)
                    .setPositiveButton(android.R.string.ok) { dialog, id ->
                        listener?.onClick(editText1.text.toString(), editText2.text.toString(),
                                editText3.text.toString())
                    }
                    .setNegativeButton(android.R.string.cancel, null)
            return builder.create()
        } ?: throw IllegalStateException("Not attached to an activity!")
    }

    companion object {
        @JvmStatic
        fun newInstance(
            onClickListener: Listener,
            text1Hint: String,
            text2Hint: String,
            text3Hint: String
        ): ThreeEditTextDialog {
            val fragment = ThreeEditTextDialog()
            fragment.setListener(onClickListener)
            fragment.hint1 = text1Hint
            fragment.hint2 = text2Hint
            fragment.hint3 = text3Hint
            return fragment
        }
    }
}
