package org.wordpress.android.fluxc.instaflux;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
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
    @Inject MediaStore mMediaStore;

    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    private static final int RESULT_PICK_MEDIA = 2;

    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private MediaModel mMedia;
    private SiteModel mSite;
    private boolean mCreatePost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InstafluxApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_post);

        // Pick the first site.
        if (mSiteStore.getSitesCount() == 0) {
            // We can't do anything if the user doesn't have sites.
            ToastUtils.showToast(this, "Die");
            finish();
        }
        mSite = mSiteStore.getSites().get(0);

        fetchPosts();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                layoutManager.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        hideProgress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_post) {
            pickMedia();
        } else if (item.getItemId() == R.id.sign_out) {
            signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out");
        builder.setPositiveButton("SIGN OUT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }
        });
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

        switch (requestCode) {
            case RESULT_PICK_MEDIA:
                if (resultCode == RESULT_OK) {
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
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickMedia();
                }
                break;
        }
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

    private void fetchMedia(SiteModel site, long mediaId) {
        List<MediaModel> mediaList = new ArrayList<>();
        MediaModel media = new MediaModel();
        media.setMediaId(mediaId);
        mediaList.add(media);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(new MediaListPayload(MediaAction.FETCH_MEDIA, site,
                mediaList)));
    }

    private void createMediaPost(MediaModel media) {
        mMedia = media;
        PostStore.InstantiatePostPayload payload = new PostStore.InstantiatePostPayload(mSite, false, null, "image");
        mDispatcher.dispatch(PostActionBuilder.newInstantiatePostAction(payload));
        AppLog.i(AppLog.T.API, "Create a new media post for " + mMedia.getUrl());
    }

    private void signOut() {
        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
            mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomSitesAction());
        } else {
            mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(mSite));
        }
    }

    private void uploadMedia(String imagePath, String mimeType) {
        showProgress();
        MediaModel mediaModel = new MediaModel();
        mediaModel.setFilePath(imagePath);
        mediaModel.setFileExtension(MediaUtils.getExtension(imagePath));
        mediaModel.setMimeType(mimeType);
        mediaModel.setFileName(MediaUtils.getFileName(imagePath));
        mediaModel.setSiteId(mSite.getSiteId());
        MediaStore.UploadMediaPayload payload = new MediaStore.UploadMediaPayload(mSite, mediaModel);
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
        if (event.post != null) {
            String post = "<img src=\"" + mMedia.getUrl()
                    + "\" width=\""
                    + mMedia.getWidth()
                    + "\" height=\""
                    + mMedia.getHeight() + "\" />";
            event.post.setContent(post);

            PostStore.RemotePostPayload payload = new PostStore.RemotePostPayload(event.post, mSite);
            mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
        } else {
            hideProgress();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(PostStore.OnPostChanged event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.POSTS, "Error from " + event.causeOfChange + " - error: " + event.error.type);
            return;
        }

        if (event.causeOfChange.equals(PostAction.FETCH_POSTS)) {
            ArrayList<String> postFormat = new ArrayList<>();
            postFormat.add("image");
            List<PostModel> postList = mPostStore.getPostsForSiteWithFormat(mSite, postFormat);
            PostAdapter postAdapter = new PostAdapter(this, postList);
            mRecyclerView.setAdapter(postAdapter);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(PostStore.OnPostUploaded event) {
        ToastUtils.showToast(this, "Post uploaded!");
        hideProgress();
        fetchPosts();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.isError()) {
            AppLog.w(AppLog.T.MEDIA, "OnMediaChanged error: " + event.error);
        }
        if (mCreatePost) {
            mCreatePost = false;
            createMediaPost(event.media.get(0));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (event.completed && event.media != null) {
            fetchMedia(mSite, event.media.getMediaId());
            mCreatePost = true;
        }
    }

    private void fetchPosts() {
        PostStore.FetchPostsPayload payload = new PostStore.FetchPostsPayload(mSite);
        mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload));
    }

    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
    }
}
