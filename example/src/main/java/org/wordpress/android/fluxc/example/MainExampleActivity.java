package org.wordpress.android.fluxc.example;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
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
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.InstantiatePostPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostInstantiated;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.store.MediaStore.ChangeMediaPayload;
import static org.wordpress.android.fluxc.store.MediaStore.FetchMediaPayload;

public class MainExampleActivity extends AppCompatActivity {
    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject AccountStore mAccountStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    private final int RESULT_PICK_MEDIA = 1;

    private TextView mLogView;
    private Button mAccountInfos;
    private Button mListSites;
    private Button mLogSites;
    private Button mUpdateFirstSite;
    private Button mFetchFirstSitePosts;
    private Button mCreatePostOnFirstSite;
    private Button mDeleteLatestPost;
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
    private AuthenticatePayload mAuthenticatePayload;

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

        mFetchFirstSitePosts = (Button) findViewById(R.id.fetch_first_site_posts);
        mFetchFirstSitePosts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FetchPostsPayload payload = new FetchPostsPayload(mSiteStore.getSites().get(0));
                mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload));
            }
        });

        mCreatePostOnFirstSite = (Button) findViewById(R.id.create_new_post_first_site);
        mCreatePostOnFirstSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PostStore.InstantiatePostPayload payload = new InstantiatePostPayload(mSiteStore.getSites().get(0),
                        false);
                mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(payload));
            }
        });

        mDeleteLatestPost = (Button) findViewById(R.id.delete_a_post_from_first_site);
        mDeleteLatestPost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel firstSite = mSiteStore.getSites().get(0);
                List<PostModel> posts = mPostStore.getPostsForSite(firstSite);
                Collections.sort(posts, new Comparator<PostModel>() {
                    @Override
                    public int compare(PostModel lhs, PostModel rhs) {
                        return (int) (rhs.getRemotePostId() - lhs.getRemotePostId());
                    }
                });
                if (!posts.isEmpty()) {
                    RemotePostPayload payload = new RemotePostPayload(posts.get(0), firstSite);
                    mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload));
                }
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
                pickMedia();
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
        mCreatePostOnFirstSite.setEnabled(mSiteStore.hasSite());
        mDeleteLatestPost.setEnabled(mSiteStore.hasSite());
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
            List<MediaModel> mediaList = new ArrayList<>();
            for (String s : split) {
                Long lVal = Long.valueOf(s);
                if (lVal < 0) continue;
                MediaModel media = new MediaModel();
                media.setMediaId(lVal);
                mediaList.add(media);
            }
            if (!mediaList.isEmpty()) {
                MediaStore.FetchMediaPayload payload = new MediaStore.FetchMediaPayload(mSiteStore.getSites().get(0), mediaList);
                mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload));
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
        mAuthenticatePayload.twoStepCode = twoStepCode;
        mAuthenticatePayload.nextAction = SiteActionBuilder.newFetchSitesAction();
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(mAuthenticatePayload));
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

    private void changeAccountSettings() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(this);
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
        FetchMediaPayload payload = new MediaStore.FetchMediaPayload(mSiteStore.getSites().get(0), null);
        mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(payload));
    }

    private void pickMedia() {
        if (checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, RESULT_PICK_MEDIA);
        }
    }

    private boolean checkAndRequestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    private void uploadMedia(String imagePath, String mimeType) {
        prependToLog("Uploading new media...");
        SiteModel site = mSiteStore.getSites().get(0);
        MediaModel media = new MediaModel();
        media.setFilePath(imagePath);
        media.setFileExtension(imagePath.substring(imagePath.lastIndexOf(".") + 1, imagePath.length()));
        media.setMimeType(mimeType);
        media.setFileName(imagePath.substring(imagePath.lastIndexOf("/"), imagePath.length()));
        media.setSiteId(site.getSiteId());
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(site, media);
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
            mFetchFirstSitePosts.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
            mFetchFirstSitePosts.setEnabled(false);
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
        if (event.isError()) {
            prependToLog("Media error occurred: " + event.error.type);
            return;
        }

        switch (event.cause) {
            case FETCH_ALL_MEDIA:
                prependToLog("Begin parsing FETCH_ALL_MEDIA response");
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
                prependToLog("End parsing FETCH_ALL_MEDIA response");
                break;
            case FETCH_MEDIA:
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
            case UPLOAD_MEDIA:
                prependToLog("Media uploaded!");
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (event.isError()) {
            prependToLog("Media upload error occurred: " + event.error.type);
            return;
        }

        prependToLog("Media progress: " + event.progress * 100 + "%");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (event.isError()) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type);
            return;
        }

        SiteModel firstSite = mSiteStore.getSites().get(0);
        if (!mPostStore.getPostsForSite(firstSite).isEmpty()) {
            if (event.causeOfChange.equals(PostAction.FETCH_POSTS)
                || event.causeOfChange.equals(PostAction.FETCH_PAGES)) {
                prependToLog("Fetched " + event.rowsAffected + " posts from: " + firstSite.getName());
            } else if (event.causeOfChange.equals(PostAction.DELETE_POST)) {
                prependToLog("Post deleted!");
            }
            mCreatePostOnFirstSite.setEnabled(true);
            mDeleteLatestPost.setEnabled(true);
        } else {
            mCreatePostOnFirstSite.setEnabled(false);
            mDeleteLatestPost.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostInstantiated(OnPostInstantiated event) {
        PostModel examplePost = event.post;
        examplePost.setTitle("From example activity");
        examplePost.setContent("Hi there, I'm a post from FluxC!");
        examplePost.setFeaturedImageId(0);

        RemotePostPayload payload = new RemotePostPayload(examplePost, mSiteStore.getSites().get(0));
        mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        prependToLog("Post uploaded! Remote post id: " + event.post.getRemotePostId());
    }
}
