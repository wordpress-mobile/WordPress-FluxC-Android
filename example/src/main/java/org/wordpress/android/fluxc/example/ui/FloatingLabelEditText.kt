package org.wordpress.android.fluxc.example.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_floating_edittext.view.*
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.utils.onTextChanged

class FloatingLabelEditText@JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : ConstraintLayout(
        ctx,
        attrs
) {
    init {
        View.inflate(context, R.layout.view_floating_edittext, this)
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.FloatingLabelEditText)
            try {
                val hintText = attrArray.getString(R.styleable.FloatingLabelEditText_textHint).orEmpty()
                txt_hint.text = hintText
                edit_text.hint = hintText
            } finally {
                attrArray.recycle()
            }
        }
    }

    fun onTextChanged(cb: (String) -> Unit) {
        edit_text.onTextChanged(cb)
    }

    fun setText(text: String) {
        edit_text.setText(text)
    }

    override fun setEnabled(isEnabled: Boolean) {
        edit_text.isEnabled = isEnabled
    }
}
