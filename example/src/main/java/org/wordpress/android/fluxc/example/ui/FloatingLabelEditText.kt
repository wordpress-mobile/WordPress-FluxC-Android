package org.wordpress.android.fluxc.example.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.databinding.ViewFloatingEdittextBinding
import org.wordpress.android.fluxc.example.utils.onTextChanged

class FloatingLabelEditText@JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : ConstraintLayout(
        ctx,
        attrs
) {
    private val binding: ViewFloatingEdittextBinding = ViewFloatingEdittextBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.FloatingLabelEditText)
            try {
                val hintText = attrArray.getString(R.styleable.FloatingLabelEditText_textHint).orEmpty()
                binding.txtHint.text = hintText
                binding.editText.hint = hintText
            } finally {
                attrArray.recycle()
            }
        }
    }

    fun onTextChanged(cb: (String) -> Unit) {
        binding.editText.onTextChanged(cb)
    }

    fun setText(text: String) {
        binding.editText.post { binding.editText.setText(text) }
    }

    fun getText() = binding.editText.text.toString()

    override fun setEnabled(isEnabled: Boolean) {
        binding.editText.isEnabled = isEnabled
    }
}
