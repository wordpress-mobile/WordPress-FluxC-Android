package org.wordpress.android.fluxc.example;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoverPayload;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.AccountStore.SignOutPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.FetchAllSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.RemoveWpcomAndJetpackSitesPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteRequestPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class MainFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;


    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        view.findViewById(R.id.sign_in_fetch_sites_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // show signin dialog
                showSigninDialog();
            }
        });

        view.findViewById(R.id.signout).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        view.findViewById(R.id.signed_out_actions).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SignedOutActionsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.account).setOnClickListener(getOnClickListener(new AccountFragment()));
        view.findViewById(R.id.sites).setOnClickListener(getOnClickListener(new SitesFragment()));
        view.findViewById(R.id.posts).setOnClickListener(getOnClickListener(new PostsFragment()));
        view.findViewById(R.id.comments).setOnClickListener(getOnClickListener(new CommentsFragment()));
        view.findViewById(R.id.media).setOnClickListener(getOnClickListener(new MediaFragment()));
        view.findViewById(R.id.taxonomies).setOnClickListener(getOnClickListener(new TaxonomiesFragment()));

        return view;
    }

    // Private methods

    private OnClickListener getOnClickListener(final Fragment fragment) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceFragment(fragment);
            }
        };
    }

    private void replaceFragment(Fragment fragment) {
        if (mSiteStore.getSitesCount() == 0 && !mAccountStore.hasAccessToken()) {
            ToastUtils.showToast(getActivity(), "You must be logged in");
            return;
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showSSLWarningDialog(final RefreshSitesXMLRPCPayload selfhostedPayload, final String url,
                                      String certifString) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SSLWarningDialog newFragment = SSLWarningDialog.newInstance(
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add the certificate to our list
                        mMemorizingTrustManager.storeLastFailure();
                        // Retry login action
                        if (selfhostedPayload != null) {
                            signInAction(selfhostedPayload.username, selfhostedPayload.password, url);
                        }
                    }
                }, certifString);
        newFragment.show(ft, "dialog");
    }

    private void showSigninDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String password, String url) {
                signInAction(username, password, url);
            }
        }, "Username", "Password", "XMLRPC Url");
        newFragment.show(ft, "dialog");
    }

    private void show2faDialog(final AuthenticatePayload authenticatePayload) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String text1, String text2, String text3) {
                if (TextUtils.isEmpty(text3)) {
                    prependToLog("2FA code required to login");
                    return;
                }
                signIn2fa(authenticatePayload, text3);
            }
        }, "", "", "2FA Code");
        newFragment.show(ft, "2fadialog");
    }

    private void signIn2fa(AuthenticatePayload authenticatePayload, String twoStepCode) {
        authenticatePayload.twoStepCode = twoStepCode;
        mDispatcher.dispatchAsk(AuthenticationActionBuilder.newAuthenticateAction(authenticatePayload));
    }

    private void showHTTPAuthDialog(final RefreshSitesXMLRPCPayload selfhostedPayload, final String url) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String password, String unused) {
                // Add credentials
                mHTTPAuthManager.addHTTPAuthCredentials(username, password, url, null);
                // Retry login action
                signInAction(selfhostedPayload.username, selfhostedPayload.password, url);
            }
        }, "Username", "Password", "unused");
        newFragment.show(ft, "dialog");
    }

    /**
     * Called when the user tap OK on the SignIn Dialog. It authenticates and list user sites, on wpcom or self hosted
     * depending if the user filled the URL field.
     */
    private void signInAction(final String username, final String password, final String url) {
        if (TextUtils.isEmpty(url)) {
            wpcomFetchSites(username, password);
        } else {
            DiscoverPayload discoverPayload = new DiscoverPayload(url);
            discoverPayload.extra = new RefreshSitesXMLRPCPayload(username, password, url);
            mDispatcher.dispatchAsk(AuthenticationActionBuilder.newDiscoverEndpointAction(discoverPayload));
        }
    }

    private void signOut() {
        mDispatcher.dispatchAsk(AccountActionBuilder.newSignOutAction(new SignOutPayload()));
        mDispatcher.dispatchAsk(
                SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction(new RemoveWpcomAndJetpackSitesPayload()));
        // Remove all remaining sites
        for (SiteModel site : mSiteStore.getSites()) {
            mDispatcher.dispatchAsk(SiteActionBuilder.newRemoveSiteAction(new SiteRequestPayload(site)));
        }
    }

    private void wpcomFetchSites(String username, String password) {
        AuthenticatePayload authenticatePayload = new AuthenticatePayload(username, password);
        mDispatcher.dispatchAsk(AuthenticationActionBuilder.newAuthenticateAction(authenticatePayload));
    }

    private void selfHostedFetchSites(String username, String password, String xmlrpcEndpoint) {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload(username, password, xmlrpcEndpoint);
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatchAsk(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
    }

    // Event listeners

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            prependToLog("Signed Out");
            return;
        }

        if (event.isError()) {
            prependToLog("Account error: " + event.error.type);
        } else {
            if (!mSiteStore.hasSite() && event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                AppLog.d(T.API, "Account data fetched - fetching sites");
                mDispatcher.dispatchAsk(SiteActionBuilder.newFetchSitesAction(new FetchAllSitesPayload()));
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            prependToLog("Authentication error: " + event.error.type);

            switch (event.error.type) {
                case AUTHORIZATION_REQUIRED:
                case ACCESS_DENIED:
                    // You're not authorized to do that
                    break;
                case INVALID_CLIENT:
                case INVALID_GRANT:
                case UNSUPPORTED_GRANT_TYPE:
                    // You should fix your gradle.properties
                    break;
                case UNKNOWN_TOKEN:
                case INVALID_TOKEN:
                case NOT_AUTHENTICATED:
                case INCORRECT_USERNAME_OR_PASSWORD:
                    showSigninDialog();
                    break;
                case HTTP_AUTH_ERROR:
                    RefreshSitesXMLRPCPayload requestPayload = (RefreshSitesXMLRPCPayload) event.getRequestPayload();
                    // Show a Dialog prompting for http username and password
                    showHTTPAuthDialog(requestPayload, requestPayload.url);
                    break;
                case INVALID_SSL_CERTIFICATE:
                    requestPayload = (RefreshSitesXMLRPCPayload) event.getRequestPayload();
                    // Show a SSL Warning Dialog
                    showSSLWarningDialog(requestPayload, requestPayload.url,
                            mMemorizingTrustManager.getLastFailure().toString());
                    break;
                case NEEDS_2FA:
                    AuthenticatePayload authenticatePayload = (AuthenticatePayload) event.getRequestPayload();
                    show2faDialog(authenticatePayload);
                    break;
                case INVALID_OTP:
                    break;
                case INVALID_REQUEST:
                case UNSUPPORTED_RESPONSE_TYPE:
                case GENERIC_ERROR:
                    // Show Toast "Network Error"?
                    break;
            }
        } else {
            if (mAccountStore.hasAccessToken()) {
                AppLog.d(T.API, "Signed in to WordPress.com successfully, fetching account");
                mDispatcher.dispatchAsk(AccountActionBuilder.newFetchAccountAction(new FetchAccountPayload()));
                mDispatcher.dispatchAsk(AccountActionBuilder.newFetchSettingsAction(new FetchSettingsPayload()));
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryResponse(OnDiscoveryResponse event) {
        DiscoverPayload discoverPayload = (DiscoverPayload) event.getRequestPayload();
        RefreshSitesXMLRPCPayload requestPayload = (RefreshSitesXMLRPCPayload) discoverPayload.extra;

        if (event.isError()) {
            if (event.error == DiscoveryError.WORDPRESS_COM_SITE) {
                wpcomFetchSites(requestPayload.username, requestPayload.password);
            } else if (event.error == DiscoveryError.HTTP_AUTH_REQUIRED) {
                showHTTPAuthDialog(requestPayload, event.failedEndpoint);
            } else if (event.error == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                showSSLWarningDialog(requestPayload, event.failedEndpoint,
                        mMemorizingTrustManager.getLastFailure().toString());
            }
            prependToLog("Discovery failed with error: " + event.error);
            AppLog.e(T.API, "Discover error: " + event.error);
        } else {
            if (event.wpRestEndpoint != null && !event.wpRestEndpoint.isEmpty()) {
                prependToLog("Discovery succeeded, found WP-API endpoint: " + event.wpRestEndpoint);
            } else {
                prependToLog("Discovery succeeded, found XML-RPC endpoint: " + event.xmlRpcEndpoint);
            }
            selfHostedFetchSites(requestPayload.username, requestPayload.password, event.xmlRpcEndpoint);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            prependToLog("SiteChanged error: " + event.error.type);
            return;
        }
        if (mSiteStore.hasSite()) {
            SiteModel firstSite = mSiteStore.getSites().get(0);
            prependToLog("First site name: " + firstSite.getName() + " - Total sites: " + mSiteStore.getSitesCount()
                         + " - rowsAffected: " + event.rowsAffected);
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
