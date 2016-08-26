package org.wordpress.android.fluxc.example;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.fluxc.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.store.MediaStore.ChangeMediaPayload;
import static org.wordpress.android.fluxc.store.MediaStore.PullMediaPayload;

public class MainExampleActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private final int RESULT_PICK_MEDIA = 1;

    private TextView mLogView;
    private Button mAccountInfos;
    private Button mListSites;
    private Button mLogSites;
    private Button mUpdateFirstSite;
    private Button mSignOut;
    private Button mAccountSettings;
    private Button mNewAccount;
    private Button mNewSite;
    private Button mFetchAllMedia;
    private Button mFetchMedia;
    private Button mUploadMedia;

    // Would be great to not have to keep this state, but it makes HTTPAuth and self signed SSL management easier
    private RefreshSitesXMLRPCPayload mSelfhostedPayload;

    // used for 2fa
    private AuthenticatePayload mDotComPayload;

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
                SiteModel site = mSiteStore.getSites().get(0);
                // Fetch site
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
                // Fetch site's post formats
                mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(site));
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

        mFetchAllMedia = (Button) findViewById(R.id.all_media);
        mFetchAllMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAllMedia();
            }
        });

        mFetchMedia = (Button) findViewById(R.id.media);
        mFetchMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showMediaItemInputDialog();
            }
        });

        mUploadMedia = (Button) findViewById(R.id.upload_media);
        mUploadMedia.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, RESULT_PICK_MEDIA);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case RESULT_PICK_MEDIA:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String mimeType = getContentResolver().getType(selectedImage);
                    String[] filePathColumn = {android.provider.MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String filePath = cursor.getString(columnIndex);
                            uploadMedia(filePath, mimeType);
                        }
                        cursor.close();
                    }
                }
        }
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

    private void fetchMediaItems(String commaSeparated) {
        if (!TextUtils.isEmpty(commaSeparated)) {
            String[] split = commaSeparated.split(",");
            List<Long> mediaIds = new ArrayList<>();
            for (String s : split) {
                Long lVal = Long.valueOf(s);
                if (lVal >= 0) mediaIds.add(lVal);
            }
            if (!mediaIds.isEmpty()) {
                PullMediaPayload payload = new MediaStore.PullMediaPayload(mSiteStore.getSites().get(0), mediaIds);
                mDispatcher.dispatch(MediaActionBuilder.newPullMediaAction(payload));
            }
        }
    }

    private void deleteMediaItems(String commaSeparated) {
        if (!TextUtils.isEmpty(commaSeparated)) {
            String[] split = commaSeparated.split(",");
            List<MediaModel> mediaIds = new ArrayList<>();
            for (String s : split) {
                Long lVal = Long.valueOf(s);
                if (lVal >= 0) {
                    MediaModel mediaModel = new MediaModel();
                    mediaModel.setMediaId(lVal);
                    mediaIds.add(mediaModel);
                }
            }
            if (!mediaIds.isEmpty()) {
                ChangeMediaPayload payload = new ChangeMediaPayload(mSiteStore.getSites().get(0), mediaIds);
                mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
            }
        }
    }

    private void showMediaItemInputDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String text1, String text2, String text3) {
                if (TextUtils.isEmpty(text2)) {
                    fetchMediaItems(text1);
                } else {
                    deleteMediaItems(text1);
                }
            }
        }, "Media Items (comma separated)", null, null);
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

    private void show2faDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
        mDotComPayload.twoStepCode = twoStepCode;
        mDotComPayload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(mDotComPayload));
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
        mDotComPayload = new AuthenticatePayload(username, password);
        // Next action will be dispatched if authentication is successful
        mDotComPayload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(mDotComPayload));
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

    private void fetchAllMedia() {
        PullMediaPayload payload = new PullMediaPayload(mSiteStore.getSites().get(0), null);
        mDispatcher.dispatch(MediaActionBuilder.newPullAllMediaAction(payload));
    }

    private void uploadMedia(String imagePath, String mimeType) {
        SiteModel site = mSiteStore.getSites().get(0);
        MediaModel mediaModel = new MediaModel();
        mediaModel.setFilePath(imagePath);
        mediaModel.setFileExtension(imagePath.substring(imagePath.lastIndexOf(".") + 1, imagePath.length()));
        mediaModel.setMimeType(mimeType);
        mediaModel.setFileName(imagePath.substring(imagePath.lastIndexOf("/"), imagePath.length()));
        mediaModel.setBlogId(site.getSiteId());
        List<MediaModel> media = new ArrayList<>();
        media.add(mediaModel);
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
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
        if (event.isError()) {
            prependToLog("Authentication error: " + event.error.type);

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
                    show2faDialog();
                    break;
                default:
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
            prependToLog("Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
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
            prependToLog("First site name: " + firstSite.getName() + " - Total sites: " + mSiteStore.getSitesCount());
            mUpdateFirstSite.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewUserValidated(OnNewUserCreated event) {
        String message = event.dryRun ? "validation" : "creation";
        if (event.isError()) {
            prependToLog("New user " + message + ": error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("New user " + message + ": success!");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError()) {
            prependToLog("New site " + message + ": error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("New site " + message + ": success!");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        mUpdateFirstSite.setEnabled(mSiteStore.hasSite());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        switch (event.causeOfChange) {
            case PULL_ALL_MEDIA:
                prependToLog("Begin parsing PULL_ALL_MEDIA response");
                if (event.media != null) {
                    for (MediaModel media : event.media) {
                        if (MediaUtils.isImageMimeType(media.getMimeType())) {
                            prependToLog("Image(" + media.getMediaId() + ") - " + media.getTitle());
                        } else if (MediaUtils.isVideoMimeType(media.getMimeType())) {
                            prependToLog("Video(" + media.getMediaId() + ") - " + media.getTitle());
                        } else {
                            prependToLog(media.getTitle());
                        }
                    }
                }
                prependToLog("End parsing PULL_ALL_MEDIA response");
                break;
            case PULL_MEDIA:
                if (event.media != null && !event.media.isEmpty()) {
                    for (MediaModel media : event.media) {
                        if (media != null) {
                            prependToLog("Fetched media(" + media.getMediaId() + ") - " + media.getTitle());
                        } else {
                            prependToLog("A media item failed to fetch. Does it exist?");
                        }
                    }
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaError(MediaStore.OnMediaError event) {
        prependToLog("Media error occurred: " + event.error.toString());
    }
}
