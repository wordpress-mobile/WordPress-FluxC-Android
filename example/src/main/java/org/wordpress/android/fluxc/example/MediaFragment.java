package org.wordpress.android.fluxc.example;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class MediaFragment extends Fragment {
    private final int RESULT_PICK_MEDIA = 1;

    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;
    private Spinner mMediaList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
        if (mSiteStore.hasSite()) {
            mSite = mSiteStore.getSites().get(0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        mMediaList = (Spinner) view.findViewById(R.id.media_list);

        view.findViewById(R.id.fetch_all_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSite == null) {
                    prependToLog("Site is null, cannot request all media.");
                    return;
                }
                fetchAllMedia(mSite);
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

                        List<MediaModel> mediaList = new ArrayList<>();
                        String[] ids = text1.split(",");
                        for (String id : ids) {
                            MediaModel media = new MediaModel();
                            media.setMediaId(Long.valueOf(id));
                            mediaList.add(media);
                        }

                        fetchMedia(mSite, mediaList);
                    }
                }, "comma-separate media IDs", null, null);
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
                intent.setType("image/*");
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
        switch(requestCode) {
            case RESULT_PICK_MEDIA:
                if(resultCode == RESULT_OK){
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaError(OnMediaChanged event) {
        if (event.isError()) {
            prependToLog(event.error.message);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (!event.isError()) {
            prependToLog("Received successful response for " + event.cause + " event.");
            if (event.cause == MediaAction.FETCH_ALL_MEDIA) {
                prependToLog(event.media.size() + " media items fetched.");
                mMediaList.setAdapter(new MediaAdapter(getActivity(), R.layout.media_list_item, event.media));
            } else if (event.cause == MediaAction.FETCH_MEDIA) {
                prependToLog(event.media.size() + " media items fetched.");
            } else if (event.cause == MediaAction.UPLOAD_MEDIA) {
                prependToLog("Successfully uploaded " + event.media.get(0).getFileName() + "!");
            } else if (event.cause == MediaAction.DELETE_MEDIA) {
                prependToLog("Successfully deleted " + event.media.get(0).getTitle() + ".");
            }
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }

    private List<MediaModel> mediaListForSingleItem(MediaModel media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(media);
        return mediaList;
    }

    private void fetchAllMedia(@NonNull SiteModel site) {
        prependToLog("Fetching all media from " + site.getName());

        MediaListPayload payload = new MediaListPayload(MediaAction.FETCH_ALL_MEDIA, site, null);
        mDispatcher.dispatch(MediaActionBuilder.newFetchAllMediaAction(payload));
    }

    private void fetchMedia(@NonNull final SiteModel site, @NonNull List<MediaModel> media) {
        prependToLog("Fetching requested media from" + site.getName());

        MediaListPayload payload = new MediaListPayload(MediaAction.FETCH_MEDIA, mSite, media);
        mDispatcher.dispatch(MediaActionBuilder.newFetchMediaAction(payload));
    }

    private void uploadMedia(@NonNull SiteModel site, @NonNull String mediaUri) {
        prependToLog("Uploading media to " + site.getName());
    }

    private void deleteMedia(@NonNull SiteModel site, @NonNull MediaModel media) {
        prependToLog("Deleting requested media from " + site.getName());

        MediaListPayload payload = new MediaListPayload(MediaAction.DELETE_MEDIA, site, mediaListForSingleItem(media));
        mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(payload));
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
