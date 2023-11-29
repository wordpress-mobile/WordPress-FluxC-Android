package org.wordpress.android.fluxc.example;

import static android.app.Activity.RESULT_OK;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.UploadStore;
import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class UploadsFragment extends Fragment {
    private static final int RESULT_PICK_MEDIA = 1;
    private static final int IMAGE_PERMISSION_REQUEST_CODE = 101;

    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject PostStore mPostStore;
    @Inject UploadStore mUploadStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;
    private MediaModel mCurrentMediaUpload;

    private View mUploadButton;
    private View mCancelButton;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if (mSiteStore.hasSite()) {
            mSite = mSiteStore.getSites().get(0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_uploads, container, false);

        mUploadButton = view.findViewById(R.id.upload_media_post);
        mUploadButton.setOnClickListener(v -> {
            if (!PermissionUtils.checkAndRequestPermissions(
                    getActivity(),
                    IMAGE_PERMISSION_REQUEST_CODE,
                    getRequiredPermissions())) {
                return;
            }

            if (mSite == null) {
                prependToLog("Site is null, cannot upload media.");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, RESULT_PICK_MEDIA);
        });

        mCancelButton = view.findViewById(R.id.cancel_upload);
        mCancelButton.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }

            if (mCurrentMediaUpload == null) {
                mCancelButton.setEnabled(false);
                return;
            }

            cancelMediaUpload(mSite, mCurrentMediaUpload);
        });

        return view;
    }

    private String[] getRequiredPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{permission.READ_MEDIA_IMAGES};
        } else {
            // For devices lower than API 33, storage permission is the equivalent of Photos and Videos permission
            permissions = new String[]{permission.READ_EXTERNAL_STORAGE};
        }
        return permissions;
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
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case RESULT_PICK_MEDIA:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePath = {android.provider.MediaStore.Images.Media.DATA};
                    Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(filePath[0]);
                            String fileToUpload = cursor.getString(columnIndex);
                            if (!TextUtils.isEmpty(fileToUpload)) {
                                uploadMediaInPost(mSite, fileToUpload);
                            }
                        }
                        cursor.close();
                    }
                }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media != null && !event.isError()) {
            if (event.canceled) {
                prependToLog("Upload canceled: " + event.media.getFileName());
                mUploadButton.setEnabled(true);
                mCancelButton.setEnabled(false);
                mCurrentMediaUpload = null;
            } else if (event.completed) {
                prependToLog("Successfully uploaded localId=" + mCurrentMediaUpload.getId()
                             + " - url=" + event.media.getUrl());
                mUploadButton.setEnabled(true);
                mCancelButton.setEnabled(false);
                mCurrentMediaUpload = null;
                if (event.media.getLocalPostId() > 0) {
                    PostModel associatedPost = mPostStore.getPostByLocalPostId(event.media.getLocalPostId());
                    if (mUploadStore.isPendingPost(associatedPost)) {
                        // This media was attached to a post waiting to be uploaded (it's registered in the
                        // UploadStore)
                        String postContent = associatedPost.getContent();
                        // Replace image placeholder in post content with remote URL of uploaded image
                        associatedPost.setContent(postContent.replace("[image]", "<img src=\""
                                                                                 + event.media.getUrl() + "\">"));
                        if (mUploadStore.getUploadingMediaForPost(associatedPost).isEmpty()) {
                            prependToLog("Post with localId=" + associatedPost.getId()
                                         + " has no more pending media - uploading!");
                            RemotePostPayload payload = new RemotePostPayload(associatedPost, mSite);
                            mDispatcher.dispatch(PostActionBuilder.newPushPostAction(payload));
                        }
                    }
                }
            } else {
                prependToLog("Upload progress: " + event.progress * 100);
            }
        } else {
            prependToLog("Upload error: " + event.error.type + ", message: " + event.error.message);
            mCurrentMediaUpload = null;
            mUploadButton.setEnabled(true);
            mCancelButton.setEnabled(false);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        prependToLog("Post uploaded! Remote post id: " + event.post.getRemotePostId());
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }

    private void uploadMediaInPost(@NonNull SiteModel site, @NonNull String mediaUri) {
        prependToLog("Uploading media to " + site.getName());

        PostModel examplePost = mPostStore.instantiatePostModel(mSite, false);
        examplePost.setTitle("From example activity");
        examplePost.setContent("Hi there, I'm a post from FluxC! [image]");
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(examplePost));

        MediaModel media = new MediaModel(
                site.getId(),
                null,
                MediaUtils.getFileName(mediaUri),
                mediaUri,
                null,
                MediaUtils.getMimeTypeForExtension(MediaUtils.getExtension(mediaUri)),
                "",
                null
        );
        media.setLocalPostId(examplePost.getId());
        mCurrentMediaUpload = mMediaStore.instantiateMediaModel(media);
        if (mCurrentMediaUpload != null) {
            List<MediaModel> associatedMediaList = new ArrayList<>();
            associatedMediaList.add(mCurrentMediaUpload);

            // Register this post with the UploadStore. This will track the upload activity of the media it contains,
            // as well as associate any media upload failures/error messages with the post
            mUploadStore.registerPostModel(examplePost, associatedMediaList);

            // Upload the media contained in the post
            UploadMediaPayload payload = new UploadMediaPayload(site, mCurrentMediaUpload, true);
            prependToLog("Dispatching upload event for media localId=" + mCurrentMediaUpload.getId());

            mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
            mUploadButton.setEnabled(false);
            mCancelButton.setEnabled(true);
        }
    }

    private void cancelMediaUpload(@NonNull SiteModel site, @NonNull MediaModel media) {
        CancelMediaPayload payload = new CancelMediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }
}
