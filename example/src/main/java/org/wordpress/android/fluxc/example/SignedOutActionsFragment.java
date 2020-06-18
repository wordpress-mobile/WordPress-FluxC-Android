package org.wordpress.android.fluxc.example;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PlanOffersAction;
import org.wordpress.android.fluxc.annotations.action.ActionBuilder;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.WhatsNewActionBuilder;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.fluxc.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.fluxc.store.PlanOffersStore;
import org.wordpress.android.fluxc.store.PlanOffersStore.OnPlanOffersFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.OnURLChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.fluxc.store.WhatsNewStore;
import org.wordpress.android.fluxc.store.WhatsNewStore.OnWhatsNewFetched;
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewAppId;
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewFetchPayload;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;


public class SignedOutActionsFragment extends Fragment {
    @Inject AccountStore mAccountStore;
    @Inject WhatsNewStore mWhatsNewStore;
    @Inject PlanOffersStore mPlanOffersStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
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
                showMagicLinkDialog(false);
            }
        });

        view.findViewById(R.id.magic_link_signup).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMagicLinkDialog(true);
            }
        });

        view.findViewById(R.id.check_url_wpcom).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCheckWPComUrlDialog();
            }
        });
        view.findViewById(R.id.fetch_wpcom_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showFetchWPComSiteDialog();
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
                showFetchConnectSiteInfoDialog();
            }
        });
        view.findViewById(R.id.plans_info).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchPlanOffers();
            }
        });
        view.findViewById(R.id.whats_new_info).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchWhatsNew();
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
            public void onClick(@NonNull String username, @NonNull String email, @NonNull String password) {
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
                SuggestDomainsPayload payload = new SuggestDomainsPayload(keyword, false, true, false, 5, false);
                mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
            }
        });
        alert.show();
    }

    private void showMagicLinkDialog(final boolean isSignup) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage(
                isSignup ? "Send magic link signup to e-mail:" : "Send magic link login to (e-mail or username):");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String emailOrUsername = editText.getText().toString();
                AuthEmailPayload authEmailPayload = new AuthEmailPayload(emailOrUsername, isSignup, null, null);
                mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(authEmailPayload));
            }
        });
        alert.show();
    }

    private void showFetchConnectSiteInfoDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage("Fetch site info about the following URL");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String url = editText.getText().toString();
                mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(url));
            }
        });
        alert.show();
    }

    private void showFetchWPComSiteDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine();
        alert.setMessage("Fetch the WordPress.com site with URL:");
        alert.setView(editText);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String url = editText.getText().toString();
                mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(url));
            }
        });
        alert.show();
    }

    private void fetchPlanOffers() {
        mDispatcher.dispatch(ActionBuilder.generateNoPayloadAction(PlanOffersAction.FETCH_PLAN_OFFERS));
    }

    private void fetchWhatsNew() {
        mDispatcher.dispatch(WhatsNewActionBuilder.newFetchRemoteAnnouncementAction(new WhatsNewFetchPayload("14.7",
                WhatsNewAppId.WP_ANDROID)));
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
    public void onFetchedConnectSiteInfo(OnConnectSiteInfoChecked event) {
        if (event.isError()) {
            prependToLog("Connect Site Info: error: " + event.error.type);
        } else {
            prependToLog("Connect Site Info: success! " + event.info.description());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFetchedConnectSiteInfo(OnWPComSiteFetched event) {
        if (event.isError()) {
            prependToLog("Fetch WP.com site for URL " + event.checkedUrl + ": error: " + event.error.type);
        } else {
            prependToLog("Fetch WP.com site for URL " + event.checkedUrl + ": success! Site name: "
                    + event.site.getName());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlanOffersFetched(OnPlanOffersFetched event) {
        if (event.isError()) {
            prependToLog("Fetch Plan Offers error: " + event.error.getType());
        } else {
            prependToLog("Fetch Plan Offers success!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWhatsNewFetched(OnWhatsNewFetched event) {
        if (event.isError()) {
            prependToLog("Fetch Whats New error: " + event.error.getType());
        } else {
            prependToLog("Fetch Whats New success!");
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
