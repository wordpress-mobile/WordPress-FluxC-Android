package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_notification_type_subtype.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater.inflate(R.layout.dialog_notification_type_subtype, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notif_type.adapter =
                ArrayAdapter<Kind>(requireActivity(), android.R.layout.simple_dropdown_item_1line, Kind.values())
        notif_subtype.adapter =
                ArrayAdapter<Subkind>(requireActivity(), android.R.layout.simple_dropdown_item_1line, Subkind.values())

        notif_dialog_ok.setOnClickListener {
            listener?.let {
                val type = notif_type.selectedItem.toString()
                val subtype = notif_subtype.selectedItem.toString()
                it.onSubmitted(type, subtype)
            }
            dismiss()
        }

        notif_dialog_cancel.setOnClickListener {
            dismiss()
        }
    }
}
