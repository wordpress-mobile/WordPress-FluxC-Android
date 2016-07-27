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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.PostAction;
import org.wordpress.android.stores.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.stores.generated.AccountActionBuilder;
import org.wordpress.android.stores.generated.AuthenticationActionBuilder;
import org.wordpress.android.stores.generated.PostActionBuilder;
import org.wordpress.android.stores.generated.SiteActionBuilder;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.MemorizingTrustManager;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.stores.store.AccountStore.AuthenticationError;
import org.wordpress.android.stores.store.AccountStore.NewAccountPayload;
import org.wordpress.android.stores.store.AccountStore.OnAccountChanged;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.stores.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.stores.store.PostStore;
import org.wordpress.android.stores.store.PostStore.FetchPostsPayload;
import org.wordpress.android.stores.store.PostStore.OnPostChanged;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.NewSitePayload;
import org.wordpress.android.stores.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.stores.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.stores.store.SiteStore.SiteVisibility;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;

import javax.inject.Inject;

public class MainExampleActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private TextView mLogView;
    private Button mAccountInfos;
    private Button mListSites;
    private Button mLogSites;
    private Button mUpdateFirstSite;
    private Button mFetchFirstSitePosts;
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
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
                mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
            }
        });
        mUpdateFirstSite = (Button) findViewById(R.id.update_first_site);
        mUpdateFirstSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSiteStore.getSites().get(0)));
            }
        });

        mFetchFirstSitePosts = (Button) findViewById(R.id.fetch_first_site_posts);
        mFetchFirstSitePosts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FetchPostsPayload payload = new FetchPostsPayload(mSiteStore.getSites().get(0));
                mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload));
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
        mFetchFirstSitePosts.setEnabled(mSiteStore.hasSite());
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
        String output = s + "\n" + mLogView.getText();
        mLogView.setText(output);
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
                                    mSelfhostedPayload.url);
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
            mSelfhostedPayload.url = url;
            mSelfhostedPayload.username = username;
            mSelfhostedPayload.password = password;

            mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mSelfhostedPayload));
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
        payload.url = xmlrpcEndpoint;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            prependToLog("Signed Out");
        } else {
            if (event.accountInfosChanged) {
                prependToLog("Display Name: " + mAccountStore.getAccount().getDisplayName());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        mAccountInfos.setEnabled(mAccountStore.hasAccessToken());
        mAccountSettings.setEnabled(mAccountStore.hasAccessToken());
        mNewSite.setEnabled(mAccountStore.hasAccessToken());
        if (event.isError) {
            prependToLog("Authentication error: " + event.errorType + " - " + event.errorMessage);
            if (event.errorType == AuthenticationError.HTTP_AUTH_ERROR) {
                // Show a Dialog prompting for http username and password
                showHTTPAuthDialog(mSelfhostedPayload.url);
            }
            if (event.errorType == AuthenticationError.INVALID_SSL_CERTIFICATE) {
                // Show a SSL Warning Dialog
                showSSLWarningDialog(mMemorizingTrustManager.getLastFailure().toString());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoverySucceeded(AccountStore.OnDiscoverySucceeded event) {
        prependToLog("Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
        selfHostedFetchSites(mSelfhostedPayload.username, mSelfhostedPayload.password, event.xmlRpcEndpoint);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryFailed(AccountStore.OnDiscoveryFailed event) {
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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (mSiteStore.hasSite()) {
            SiteModel firstSite = mSiteStore.getSites().get(0);
            prependToLog("First site name: " + firstSite.getName() + " - Total sites: " + mSiteStore.getSitesCount());
            mUpdateFirstSite.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewUserValidated(OnNewUserCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError) {
            prependToLog("New user " + message + ", error: " + event.errorMessage + " - " + event.errorType);
        } else {
            prependToLog("New user " + message + ": success!");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError) {
            prependToLog("New site " + message + ", error: " + event.errorMessage + " - " + event.errorType);
        } else {
            prependToLog("New site " + message + ": success!");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        mUpdateFirstSite.setEnabled(mSiteStore.hasSite());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (!mPostStore.getPosts().isEmpty()) {
            if (event.causeOfChange.equals(PostAction.FETCH_POSTS) ||
                    event.causeOfChange.equals(PostAction.FETCH_PAGES)) {
                SiteModel firstSite = mSiteStore.getSites().get(0);
                prependToLog("Fetched " + event.numFetched + "posts from: " + firstSite.getName());
            }
            mDeleteLatestPost.setEnabled(true);
        } else {
            mDeleteLatestPost.setEnabled(false);
        }
    }
}
