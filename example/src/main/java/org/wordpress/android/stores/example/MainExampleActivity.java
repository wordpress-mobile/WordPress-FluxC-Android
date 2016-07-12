package org.wordpress.android.stores.example;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.stores.generated.AccountActionBuilder;
import org.wordpress.android.stores.generated.AuthenticationActionBuilder;
import org.wordpress.android.stores.generated.SiteActionBuilder;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.MemorizingTrustManager;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.NewAccountPayload;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.stores.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.NewSitePayload;
import org.wordpress.android.stores.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.stores.store.SiteStore.OnSitesRemoved;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.stores.store.SiteStore.SiteVisibility;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;

import javax.inject.Inject;


public class MainExampleActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;
    @Inject SelfHostedEndpointFinder mSelfHostedEndpointFinder;

    private TextView mLogView;
    private Button mAccountInfos;
    private Button mListSites;
    private Button mLogSites;
    private Button mUpdateFirstSite;
    private Button mSignOut;
    private Button mAccountSettings;
    private Button mNewAccount;
    private Button mNewSite;

    // Would be great to not have to keep this state, but it makes HTTPAuth and self signed SSL management easier
    private RefreshSitesXMLRPCPayload mSelfhostedPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_example);
        mListSites = (Button) findViewById(R.id.sign_in_fetch_sites_button);
        mListSites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // show signin dialog
                showSigninDialog();
            }
        });
        mAccountInfos = (Button) findViewById(R.id.account_infos_button);
        mAccountInfos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(AccountActionBuilder.newFetchAction());
            }
        });
        mUpdateFirstSite = (Button) findViewById(R.id.update_first_site);
        mUpdateFirstSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSiteStore.getSites().get(0)));
            }
        });

        mAccountSettings = (Button) findViewById(R.id.account_settings);
        mAccountSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAccountSettings();
            }
        });

        mLogSites = (Button) findViewById(R.id.log_sites);
        mLogSites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SiteModel site : mSiteStore.getSites()) {
                    AppLog.i(T.API, LogUtils.toString(site));
                }
            }
        });

        mSignOut = (Button) findViewById(R.id.signout);
        mSignOut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                signOutWpCom();
            }
        });

        mNewAccount = (Button) findViewById(R.id.new_account);
        mNewAccount.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewAccountDialog();
            }
        });

        mNewSite = (Button) findViewById(R.id.new_site);
        mNewSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewSiteDialog();
            }
        });
        mLogView = (TextView) findViewById(R.id.log);

        init();
    }

    private void init() {
        mAccountInfos.setEnabled(mAccountStore.hasAccessToken());
        mAccountSettings.setEnabled(mAccountStore.hasAccessToken());
        if (mAccountStore.hasAccessToken()) {
            prependToLog("You're signed in as: " + mAccountStore.getAccount().getUserName());
        }
        mUpdateFirstSite.setEnabled(mSiteStore.hasSite());
        mNewSite.setEnabled(mAccountStore.hasAccessToken());
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

    // Private methods

    private void prependToLog(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String output = s + "\n" + mLogView.getText();
                mLogView.setText(output);
            }
        });
    }

    private void showSSLWarningDialog(String certifString) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SSLWarningDialog newFragment = SSLWarningDialog.newInstance(
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Add the certificate to our list
                        mMemorizingTrustManager.storeLastFailure();
                        // Retry login action
                        if (mSelfhostedPayload != null) {
                            signInAction(mSelfhostedPayload.username, mSelfhostedPayload.password,
                                    mSelfhostedPayload.xmlrpcEndpoint);
                        }
                    }
                }, certifString);
        newFragment.show(ft, "dialog");
    }

    private void showSigninDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String password, String url) {
                signInAction(username, password, url);
            }
        }, "Username", "Password", "XMLRPC Url");
        newFragment.show(ft, "dialog");
    }

    private void showHTTPAuthDialog(final String url) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
            mSelfhostedPayload.username = username;
            mSelfhostedPayload.password = password;
            mSelfHostedEndpointFinder.findEndpoint(url, username, password,
                    new SelfHostedEndpointFinder.DiscoveryCallback() {
                @Override
                public void onError(DiscoveryError error, String lastEndpoint) {
                    if (error == DiscoveryError.WORDPRESS_COM_SITE) {
                        wpcomFetchSites(username, password);
                    } else if (error == DiscoveryError.HTTP_AUTH_REQUIRED) {
                        showHTTPAuthDialog(lastEndpoint);
                    } else if (error == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                        mSelfhostedPayload.xmlrpcEndpoint = lastEndpoint;
                        showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
                    }
                    prependToLog("Discovery failed with error: " + error);
                    AppLog.e(T.API, "Discover error: " + error);
                }

                @Override
                public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                    prependToLog("Discovery succeeded, endpoint: " + xmlrpcEndpoint);
                    selfHostedFetchSites(username, password, xmlrpcEndpoint);
                }
            });
        }
    }

    private void signOutWpCom() {
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomSitesAction());
    }

    private void wpcomFetchSites(String username, String password) {
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        // Next action will be dispatched if authentication is successful
        payload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    private void selfHostedFetchSites(String username, String password, String xmlrpcEndpoint) {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.xmlrpcEndpoint = xmlrpcEndpoint;
        mSelfhostedPayload = payload;
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
    }

    private void changeAccountSettings() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
        alert.setMessage("Update your display name:");
        alert.setView(edittext);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String displayName = edittext.getText().toString();
                PostAccountSettingsPayload payload = new PostAccountSettingsPayload();
                payload.params = new HashMap<>();
                payload.params.put("display_name", displayName);
                mDispatcher.dispatch(AccountActionBuilder.newPostSettingsAction(payload));
            }
        });
        alert.show();
    }

    private void showNewAccountDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String username, String email, String password) {
                newAccountAction(username, email, password);
            }
        }, "Username", "Email", "Password");
        newFragment.show(ft, "dialog");
    }

    private void newAccountAction(String username, String email, String password) {
        NewAccountPayload newAccountPayload = new NewAccountPayload(username, password, email, true);
        mDispatcher.dispatch(AccountActionBuilder.newCreateNewAccountAction(newAccountPayload));
    }

    private void showNewSiteDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String name, String title, String unused) {
                newSiteAction(name, title);
            }
        }, "Site Name", "Site Title", "Unused");
        newFragment.show(ft, "dialog");
    }

    private void newSiteAction(String name, String title) {
        // Default language "en" (english)
        NewSitePayload newSitePayload = new NewSitePayload(name, title, "en", SiteVisibility.PUBLIC, true);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
    }

    // Event listeners

    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.isSignedIn()) {
            prependToLog("Signed Out");
        } else {
            if (event.accountInfosChanged) {
                prependToLog("Display Name: " + mAccountStore.getAccount().getDisplayName());
            }
        }
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        mAccountInfos.setEnabled(mAccountStore.hasAccessToken());
        mAccountSettings.setEnabled(mAccountStore.hasAccessToken());
        mNewSite.setEnabled(mAccountStore.hasAccessToken());
        if (event.isError) {
            prependToLog("Authentication error: " + event.authError);
            if (event.authError == AuthError.HTTP_AUTH_ERROR) {
                // Show a Dialog prompting for http username and password
                showHTTPAuthDialog(mSelfhostedPayload.xmlrpcEndpoint);
            }
            if (event.authError == AuthError.INVALID_SSL_CERTIFICATE) {
                // Show a SSL Warning Dialog
                showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
            }
        }
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        if (mSiteStore.hasSite()) {
            SiteModel firstSite = mSiteStore.getSites().get(0);
            prependToLog("First site name: " + firstSite.getName() + " - Total sites: " + mSiteStore.getSitesCount());
            mUpdateFirstSite.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
        }
    }

    @Subscribe
    public void onNewUserValidated(OnNewUserCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError) {
            prependToLog("New user " + message + ", error: " + event.errorMessage + " - " + event.errorType);
        } else {
            prependToLog("New user " + message + ": success!");
        }
    }

    @Subscribe
    public void onNewSiteCreated(OnNewSiteCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError) {
            prependToLog("New site " + message + ", error: " + event.errorMessage + " - " + event.errorType);
        } else {
            prependToLog("New site " + message + ": success!");
        }
    }

    @Subscribe
    public void onSitesRemoved(OnSitesRemoved event) {
        mUpdateFirstSite.setEnabled(mSiteStore.hasSite());
    }
}
