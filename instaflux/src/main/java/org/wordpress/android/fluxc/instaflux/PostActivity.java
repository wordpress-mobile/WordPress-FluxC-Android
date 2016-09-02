package org.wordpress.android.fluxc.instaflux;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PostActivity extends AppCompatActivity {
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    private final int RESULT_PICK_MEDIA = 1;

    private EditText mTitleText;
    private EditText mContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InstafluxApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_post);

        mTitleText = (EditText) findViewById(R.id.edit_text_title);
        mContentText = (EditText) findViewById(R.id.edit_text_content);
        Button postButton = (Button) findViewById(R.id.button_post);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post();
            }
        });
        Button signOutButton = (Button) findViewById(R.id.button_sign_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        Button imagePostButton = (Button) findViewById(R.id.image_post);
        imagePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickMedia();
            }
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out");
        builder.setPositiveButton("SIGN OUT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }});
        builder.setNegativeButton("CANCEL", null);
        builder.show();
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

    private void pickMedia() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, RESULT_PICK_MEDIA);
    }

    private void post() {
        String title = mTitleText.getText().toString();
        String content = mContentText.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            ToastUtils.showToast(this, R.string.error_no_title_or_content);
            return;
        }

        PostStore.InstantiatePostPayload payload = new PostStore.InstantiatePostPayload(mSiteStore.getSites().get(0), false);
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(payload));

        AppLog.i(AppLog.T.API, "Create a new post with title: " + title + " content: " + content);
    }

    private void signOut() {
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
            mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomSitesAction());
        } else {
            SiteModel firstSite = mSiteStore.getSites().get(0);
            mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(firstSite));
        }
    }

    private void uploadMedia(String imagePath, String mimeType) {
        SiteModel site = mSiteStore.getSites().get(0);
        MediaModel mediaModel = new MediaModel();
        mediaModel.setFilePath(imagePath);
        mediaModel.setFileExtension(imagePath.substring(imagePath.lastIndexOf(".") + 1, imagePath.length()));
        mediaModel.setMimeType(mimeType);
        mediaModel.setFileName(imagePath.substring(imagePath.lastIndexOf("/"), imagePath.length()));
        mediaModel.setSiteId(site.getSiteId());
        List<MediaModel> media = new ArrayList<>();
        media.add(mediaModel);
        MediaStore.ChangeMediaPayload payload = new MediaStore.ChangeMediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(AccountStore.OnAccountChanged event) {
        if (!mAccountStore.hasAccessToken()) {
            // Signed Out
            finish();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(SiteStore.OnSiteRemoved event) {
        if (!mSiteStore.hasSite()) {
            // Signed Out
            finish();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostInstantiated(PostStore.OnPostInstantiated event) {
        // upload the post if there is no error
        if (mSiteStore.hasSite() && event.post != null) {
            event.post.setTitle(mTitleText.getText().toString());
            event.post.setContent(mContentText.getText().toString());
            PostStore.RemotePostPayload payload = new PostStore.RemotePostPayload(event.post, mSiteStore.getSites().get(0));
            mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(PostStore.OnPostUploaded event) {
        mTitleText.setText("");
        mContentText.setText("");
        ToastUtils.showToast(this, event.post.getTitle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        switch (event.causeOfChange) {
            case UPLOAD_MEDIA:
                AppLog.i(AppLog.T.API, "Media uploaded!");
                break;
        }
    }
}
