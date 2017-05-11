package org.wordpress.android.fluxc.example;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.fluxc.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.OnURLChecked;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;

import javax.inject.Inject;


public class SignedOutActionsFragment extends Fragment {
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
        View view = inflater.inflate(R.layout.fragment_signed_out_actions, container, false);
        view.findViewById(R.id.new_account).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewAccountDialog();
            }
        });

        view.findViewById(R.id.magic_link_email).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMagicLinkDialog();
            }
        });

        view.findViewById(R.id.check_url_wpcom).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCheckWPComUrlDialog();
            }
        });
        view.findViewById(R.id.domain_suggestions).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDomainSuggestionsDialog();
            }
        });
        view.findViewById(R.id.connect_site_info).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String site = "http://www.example.com";
                mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(site));
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

    private void newAccountAction(String username, String email, String password) {
        NewAccountPayload newAccountPayload = new NewAccountPayload(username, password, email, true);
        mDispatcher.dispatch(AccountActionBuilder.newCreateNewAccountAction(newAccountPayload));
    }

    private void showNewAccountDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String email, String password) {
                newAccountAction(username, email, password);
            }
        }, "Username", "Email", "Password");
        newFragment.show(ft, "dialog");
    }

    private void showCheckWPComUrlDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage("Check if the following URL is wpcom or jetpack"
                + " (and can be contacted via wpcom credentials):");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String url = editText.getText().toString();
                mDispatcher.dispatch(SiteActionBuilder.newIsWpcomUrlAction(url));
            }
        });
        alert.show();
    }

    private void showDomainSuggestionsDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage("Suggest domain names (max 5), by keyword:");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String keyword = editText.getText().toString();
                SuggestDomainsPayload payload = new SuggestDomainsPayload(keyword, true, false, 5);
                mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
            }
        });
        alert.show();
    }


    private void showMagicLinkDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage("Send magic link login e-mail:");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String email = editText.getText().toString();
                mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(email));
            }
        });
        alert.show();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewUserValidated(OnNewUserCreated event) {
        String message = event.dryRun ? "validation" : "creation";
        if (event.isError()) {
            prependToLog("New user " + message + ": error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("New user " + message + ": success!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthEmailSent(OnAuthEmailSent event) {
        if (event.isError()) {
            prependToLog("Error sending magic link: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("Sent magic link e-mail!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUrlChecked(OnURLChecked event) {
        if (event.isError()) {
            prependToLog("OnUrlChecked: error");
        } else {
            prependToLog("OnUrlChecked: " + event.url + (event.isWPCom ? " is" : " is not")
                         + " accessible via WordPress.com API");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSuggestedDomains(OnSuggestedDomains event) {
        if (event.isError()) {
            prependToLog("OnSuggestedDomains: error: " + event.error.type + " - " + event.error.message);
        } else {
            for (DomainSuggestionResponse suggestion : event.suggestions) {
                prependToLog("Suggestion: " + suggestion.domain_name + " - " + suggestion.cost);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFetchedConnectSiteInfo(SiteStore.OnConnectSiteInfoChecked event) {
        if (event.isError()) {
            prependToLog("Connect Site Info: error: " + event.error.type);
        } else {
            prependToLog("Connect Site Info: success! " + event.info.description());
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
