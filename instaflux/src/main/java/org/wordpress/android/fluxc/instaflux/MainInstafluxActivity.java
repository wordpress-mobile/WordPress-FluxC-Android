package org.wordpress.android.fluxc.instaflux;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class MainInstafluxActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    // Would be great to not have to keep this state, but it makes HTTPAuth and self signed SSL management easier
    private RefreshSitesXMLRPCPayload mSelfhostedPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InstafluxApp) getApplication()).component().inject(this);

        // if the user is already logged in switch to PostActivity immediately
        if (mSiteStore.hasSite()) {
            launchPostActivity();
        }

        setContentView(R.layout.activity_main);

        final TextView usernameField = (TextView) findViewById(R.id.username);
        final TextView passwordField = (TextView) findViewById(R.id.password);
        final TextView urlField = (TextView) findViewById(R.id.url);
        Button signInBtn = (Button) findViewById(R.id.sign_in_button);
        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                String password = passwordField.getText().toString();
                String url = urlField.getText().toString();
                if (TextUtils.isEmpty(username)) {
                    usernameField.requestFocus();
                    ToastUtils.showToast(MainInstafluxActivity.this, R.string.error_empty_username);
                } else if (TextUtils.isEmpty(password)) {
                    passwordField.requestFocus();
                    ToastUtils.showToast(MainInstafluxActivity.this, R.string.error_empty_password);
                } else {
                    signInAction(username, password, url);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Order is important here since onRegister could fire onChanged events. "register(this)" should probably go
        // first everywhere.
        mDispatcher.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    private void showHTTPAuthDialog(final String url) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new ThreeEditTextDialog.Listener() {
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

    private void showSSLWarningDialog(String certifString) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SSLWarningDialog newFragment = SSLWarningDialog.newInstance(
                new DialogInterface.OnClickListener() {
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

    private void wpcomFetchSites(String username, String password) {
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
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

    private void launchPostActivity() {
        Intent intent = new Intent(this, PostActivity.class);
        startActivity(intent);
    }

    // Event listeners

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            // Signed out!
            return;
        }

        if (event.isError()) {
            AppLog.e(AppLog.T.API, "Account error: " + event.error.type);
        } else {
            if (!mSiteStore.hasSite() && event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                AppLog.d(AppLog.T.API, "Account data fetched - fetching sites");
                mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            switch (event.error.type) {
                case HTTP_AUTH_ERROR:
                    // Show a Dialog prompting for http username and password
                    showHTTPAuthDialog(mSelfhostedPayload.url);
                    break;
                case INVALID_SSL_CERTIFICATE:
                    // Show a SSL Warning Dialog
                    showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
                    break;
                case NEEDS_2FA:
                    // TODO: handle 2fa
                    break;
                default:
                    // Show Toast "Network Error"?
                    break;
            }
        } else {
            if (mAccountStore.hasAccessToken()) {
                AppLog.d(AppLog.T.API, "Signed in to WordPress.com successfully, fetching account");
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
                mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryResponse(OnDiscoveryResponse event) {
        if (event.isError()) {
            if (event.error == SelfHostedEndpointFinder.DiscoveryError.WORDPRESS_COM_SITE) {
                wpcomFetchSites(mSelfhostedPayload.username, mSelfhostedPayload.password);
            } else if (event.error == SelfHostedEndpointFinder.DiscoveryError.HTTP_AUTH_REQUIRED) {
                showHTTPAuthDialog(event.failedEndpoint);
            } else if (event.error == SelfHostedEndpointFinder.DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                mSelfhostedPayload.url = event.failedEndpoint;
                showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
            }
            AppLog.e(AppLog.T.API, "Discover error: " + event.error);
        } else {
            AppLog.i(AppLog.T.API, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
            selfHostedFetchSites(mSelfhostedPayload.username, mSelfhostedPayload.password, event.xmlRpcEndpoint);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (mSiteStore.hasSite()) {
            launchPostActivity();
        }
    }
}
