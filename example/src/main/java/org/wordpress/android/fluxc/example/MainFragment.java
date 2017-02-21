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
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
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


    // Would be great to not have to keep this state, but it makes HTTPAuth and self signed SSL management easier
    private RefreshSitesXMLRPCPayload mSelfhostedPayload;

    // used for 2fa
    private AuthenticatePayload mAuthenticatePayload;

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

    private void showSSLWarningDialog(String certifString) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SSLWarningDialog newFragment = SSLWarningDialog.newInstance(
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add the certificate to our list
                        mMemorizingTrustManager.storeLastFailure();
                        // Retry login action
                        if (mSelfhostedPayload != null) {
                            signInAction(mSelfhostedPayload.username, mSelfhostedPayload.password,
                                    mSelfhostedPayload.url);
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

    private void show2faDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String text1, String text2, String text3) {
                if (TextUtils.isEmpty(text3)) {
                    prependToLog("2FA code required to login");
                    return;
                }
                signIn2fa(text3);
            }
        }, "", "", "2FA Code");
        newFragment.show(ft, "2fadialog");
    }

    private void signIn2fa(String twoStepCode) {
        mAuthenticatePayload.twoStepCode = twoStepCode;
        mAuthenticatePayload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(mAuthenticatePayload));
    }

    private void showHTTPAuthDialog(final String url) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String password, String unused) {
                // Add credentials
                mHTTPAuthManager.addHTTPAuthCredentials(username, password, url, null);
                // Retry login action
                if (mSelfhostedPayload != null) {
                    signInAction(mSelfhostedPayload.username, mSelfhostedPayload.password, url);
                }
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
            mSelfhostedPayload = new RefreshSitesXMLRPCPayload();
            mSelfhostedPayload.url = url;
            mSelfhostedPayload.username = username;
            mSelfhostedPayload.password = password;

            mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(url));
        }
    }

    private void signOut() {
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());
        // Remove all remaining sites
        for (SiteModel site : mSiteStore.getSites()) {
            mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(site));
        }
    }

    private void wpcomFetchSites(String username, String password) {
        mAuthenticatePayload = new AuthenticatePayload(username, password);
        // Next action will be dispatched if authentication is successful
        mAuthenticatePayload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(mAuthenticatePayload));
    }

    private void selfHostedFetchSites(String username, String password, String xmlrpcEndpoint) {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = xmlrpcEndpoint;
        mSelfhostedPayload = payload;
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
    }

    // Event listeners

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            prependToLog("Signed Out");
        }
    }

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
                    // Show a Dialog prompting for http username and password
                    showHTTPAuthDialog(mSelfhostedPayload.url);
                    break;
                case INVALID_SSL_CERTIFICATE:
                    // Show a SSL Warning Dialog
                    showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
                    break;
                case NEEDS_2FA:
                    show2faDialog();
                    break;
                case INVALID_OTP:
                    break;
                case INVALID_REQUEST:
                case UNSUPPORTED_RESPONSE_TYPE:
                case GENERIC_ERROR:
                    // Show Toast "Network Error"?
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryResponse(OnDiscoveryResponse event) {
        if (event.isError()) {
            if (event.error == DiscoveryError.WORDPRESS_COM_SITE) {
                wpcomFetchSites(mSelfhostedPayload.username, mSelfhostedPayload.password);
            } else if (event.error == DiscoveryError.HTTP_AUTH_REQUIRED) {
                showHTTPAuthDialog(event.failedEndpoint);
            } else if (event.error == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                mSelfhostedPayload.url = event.failedEndpoint;
                showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
            }
            prependToLog("Discovery failed with error: " + event.error);
            AppLog.e(T.API, "Discover error: " + event.error);
        } else {
            if (event.wpRestEndpoint != null && !event.wpRestEndpoint.isEmpty()) {
                prependToLog("Discovery succeeded, found WP-API endpoint: " + event.wpRestEndpoint);
            } else {
                prependToLog("Discovery succeeded, found XML-RPC endpoint: " + event.xmlRpcEndpoint);
            }
            selfHostedFetchSites(mSelfhostedPayload.username, mSelfhostedPayload.password, event.xmlRpcEndpoint);
        }
    }

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
