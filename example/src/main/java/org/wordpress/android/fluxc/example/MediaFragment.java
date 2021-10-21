package org.wordpress.android.fluxc.example;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.example.ui.common.FragmentExtKt;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class MediaFragment extends Fragment {
    private static final int RESULT_PICK_MEDIA = 1;

    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;
    private Spinner mMediaList;
    private View mCancelButton;

    private MediaModel mCurrentUpload;

    private List<MediaModel> mMedia = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if (mSiteStore.hasSite()) {
            mSite = mSiteStore.getSites().get(0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        mMediaList = (Spinner) view.findViewById(R.id.media_list);

        view.findViewById(R.id.media_select_site).setOnClickListener(v -> FragmentExtKt.showSiteSelectorDialog(
                MediaFragment.this,
                mSiteStore.getSites().indexOf(mSite),
                (site, pos) -> mSite = site));

        mCancelButton = view.findViewById(R.id.cancel_upload);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAdded()) {
                    return;
                }

                if (mCurrentUpload == null) {
                    mCancelButton.setEnabled(false);
                    return;
                }

                cancelMediaUpload(mSite, mCurrentUpload);
            }
        });

        view.findViewById(R.id.fetch_media_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSite == null) {
                    prependToLog("Site is null, cannot request first media page.");
                    return;
                }
                fetchMediaList(mSite);
            }
        });

        view.findViewById(R.id.fetch_specified_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSite == null) {
                    prependToLog("Site is null, cannot request media.");
                    return;
                }
                ThreeEditTextDialog dialog = ThreeEditTextDialog.newInstance(new ThreeEditTextDialog.Listener() {
                    @Override
                    public void onClick(String text1, String text2, String text3) {
                        if (TextUtils.isEmpty(text1)) {
                            prependToLog("You must specify media IDs to fetch.");
                            return;
                        }

                        String[] ids = text1.split(",");
                        for (String id : ids) {
                            MediaModel media = new MediaModel();
                            media.setMediaId(Long.valueOf(id));
                            fetchMedia(mSite, media);
                        }
                    }
                }, "comma-separate media IDs", "", "");
                dialog.show(getFragmentManager(), "media-ids-dialog");
            }
        });

        view.findViewById(R.id.upload_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSite == null) {
                    prependToLog("Site is null, cannot request upload media.");
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("*/*");
                startActivityForResult(intent, RESULT_PICK_MEDIA);
            }
        });

        view.findViewById(R.id.delete_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSite == null) {
                    prependToLog("Site is null, cannot request delete media.");
                    return;
                }

                if (mMediaList.getCount() <= 0) {
                    prependToLog("Please fetch media before attempting to delete.");
                    return;
                }

                MediaModel media = (MediaModel) mMediaList.getSelectedItem();
                deleteMedia(mSite, media);
            }
        });

        return view;
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
                                uploadMedia(mSite, fileToUpload);
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
    public void onMediaChanged(OnMediaChanged event) {
        if (!event.isError()) {
            prependToLog("Received successful response for " + event.cause + " event.");
            if (event.cause == MediaAction.FETCH_MEDIA) {
                prependToLog(event.mediaList.size() + " media items fetched.");
            } else if (event.cause == MediaAction.DELETE_MEDIA) {
                prependToLog("Successfully deleted " + event.mediaList.get(0).getTitle() + ".");
            }
        } else {
            prependToLog(event.error.message);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaListFetched(OnMediaListFetched event) {
        if (!event.isError()) {
            prependToLog("Received successful response for media list fetch.");
            mMedia = mMediaStore.getAllSiteMedia(mSite);
            mMediaList.setAdapter(new MediaAdapter(getActivity(), R.layout.media_list_item, mMedia));
        }
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (!event.isError()) {
            if (event.canceled) {
                prependToLog("Upload canceled: " + event.media.getFileName());
                mCancelButton.setEnabled(false);
                mCurrentUpload = null;
            } else if (event.completed) {
                prependToLog("Successfully uploaded localId=" + mCurrentUpload.getId()
                             + " - url=" + event.media.getUrl());
                mCancelButton.setEnabled(false);
                mCurrentUpload = null;
            } else {
                prependToLog("Upload progress: " + event.progress * 100);
            }
        } else {
            prependToLog("Upload error: " + event.error.type + ", message: " + event.error.message);
            mCurrentUpload = null;
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }

    private void fetchMediaList(@NonNull SiteModel site) {
        FetchMediaListPayload payload = new FetchMediaListPayload(
                site, MediaStore.DEFAULT_NUM_MEDIA_PER_FETCH, false);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaListAction(payload));
    }

    private void fetchMedia(@NonNull final SiteModel site, @NonNull MediaModel media) {
        prependToLog("Fetching requested media from" + site.getName());

        MediaPayload payload = new MediaPayload(mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload));
    }

    private void uploadMedia(@NonNull SiteModel site, @NonNull String mediaUri) {
        prependToLog("Uploading media to " + site.getName());

        mCurrentUpload = mMediaStore.instantiateMediaModel();
        mCurrentUpload.setFileName(MediaUtils.getFileName(mediaUri));
        mCurrentUpload.setFilePath(mediaUri);
        mCurrentUpload.setMimeType(MediaUtils.getMimeTypeForExtension(MediaUtils.getExtension(mediaUri)));

        // Upload
        UploadMediaPayload payload = new UploadMediaPayload(site, mCurrentUpload, true);
        prependToLog("Dispatching upload event for media localId=" + mCurrentUpload.getId());

        mDispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload));
        mCancelButton.setEnabled(true);
    }

    private void deleteMedia(@NonNull SiteModel site, @NonNull MediaModel media) {
        prependToLog("Deleting requested media from " + site.getName());

        MediaPayload payload = new MediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
    }

    private void cancelMediaUpload(@NonNull SiteModel site, @NonNull MediaModel media) {
        CancelMediaPayload payload = new CancelMediaPayload(site, media);
        mDispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload));
    }

    private class MediaAdapter extends ArrayAdapter<MediaModel> {
        private MediaAdapter(Context context, int resource, List<MediaModel> objects) {
            super(context, resource, objects);
        }

        @Override
        @NonNull
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, convertView, parent);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getItemView(position, convertView, parent);
        }

        @NonNull
        private View getItemView(int position, View convertView, @NonNull ViewGroup parent) {
            final MediaModel mediaItem = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.media_list_item, parent, false);
            }

            if (mediaItem != null) {
                ((TextView) convertView.findViewById(R.id.media_name)).setText(mediaItem.getTitle());
                ((TextView) convertView.findViewById(R.id.media_id)).setText(String.valueOf(mediaItem.getMediaId()));
            }

            return convertView;
        }
    }
}
