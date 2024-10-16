package org.wordpress.android.fluxc.example

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.example.databinding.FragmentAccountBinding
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import javax.inject.Inject

class AccountFragment : Fragment() {
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var dispatcher: Dispatcher

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentAccountBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentAccountBinding.bind(view)) {
            accountSettings.setOnClickListener { changeAccountSettings() }

            accountInfosButton.setOnClickListener {
                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            }

            accountEmailVerification.setOnClickListener {
                dispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!accountStore.hasAccessToken()) {
            prependToLog("Signed Out")
        } else {
            if (event.causeOfChange == AccountAction.SEND_VERIFICATION_EMAIL) {
                if (!event.isError) {
                    prependToLog("Verification email sent, check your inbox.")
                } else {
                    prependToLog("Error sending verification email. Are you already verified?")
                }
            } else if (event.accountInfosChanged) {
                prependToLog("Display Name: " + accountStore.account.displayName)
            }
        }
    }

    private fun changeAccountSettings() {
        val alert = AlertDialog.Builder(activity)
        val editText = EditText(activity)
        alert.setMessage("Update your display name:")
        alert.setView(editText)
        alert.setPositiveButton(android.R.string.ok) { _, _ ->
            val displayName = editText.text.toString()
            val payload = PushAccountSettingsPayload()
            payload.params = HashMap<String, Any>()
            payload.params.put("display_name", displayName)
            dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
        }
        alert.show()
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
