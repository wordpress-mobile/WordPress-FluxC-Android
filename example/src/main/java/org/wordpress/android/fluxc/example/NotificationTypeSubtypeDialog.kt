package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import org.wordpress.android.fluxc.example.databinding.DialogNotificationTypeSubtypeBinding
import org.wordpress.android.fluxc.model.notification.NotificationModel.Kind
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind

class NotificationTypeSubtypeDialog : DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(listener: Listener) = NotificationTypeSubtypeDialog().apply { this.listener = listener }
    }

    interface Listener {
        fun onSubmitted(type: String, subtype: String)
    }

    var listener: Listener? = null

    override fun onResume() {
        super.onResume()

        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogNotificationTypeSubtypeBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(DialogNotificationTypeSubtypeBinding.bind(view)) {
            notifType.adapter =
                ArrayAdapter<Kind>(requireActivity(), android.R.layout.simple_dropdown_item_1line, Kind.values())
            notifSubtype.adapter =
                ArrayAdapter<Subkind>(requireActivity(), android.R.layout.simple_dropdown_item_1line, Subkind.values())

            notifDialogOk.setOnClickListener {
                listener?.let {
                    val type = notifType.selectedItem.toString()
                    val subtype = notifSubtype.selectedItem.toString()
                    it.onSubmitted(type, subtype)
                }
                dismiss()
            }

            notifDialogCancel.setOnClickListener {
                dismiss()
            }
        }
    }
}
