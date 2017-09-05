package org.wordpress.android.fluxc.example;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.FetchAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.VerificationEmailPayload;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.HashMap;

import javax.inject.Inject;

public class AccountFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        view.findViewById(R.id.account_settings).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAccountSettings();
            }
        });

        view.findViewById(R.id.account_infos_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction(new FetchAccountPayload()));
                mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction(new FetchSettingsPayload()));
            }
        });

        view.findViewById(R.id.account_email_verification).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(AccountActionBuilder.newSendVerificationEmailAction(new VerificationEmailPayload()));
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            prependToLog("Signed Out");
        } else {
            if (event.causeOfChange == AccountAction.SEND_VERIFICATION_EMAIL) {
                if (!event.isError()) {
                    prependToLog("Verification email sent, check your inbox.");
                } else {
                    prependToLog("Error sending verification email. Are you already verified?");
                }
            } else if (event.accountInfosChanged) {
                prependToLog("Display Name: " + mAccountStore.getAccount().getDisplayName());
            }
        }
    }

    private void changeAccountSettings() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText edittext = new EditText(getActivity());
        alert.setMessage("Update your display name:");
        alert.setView(edittext);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String displayName = edittext.getText().toString();
                PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
                payload.params = new HashMap<>();
                payload.params.put("display_name", displayName);
                mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
            }
        });
        alert.show();
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
