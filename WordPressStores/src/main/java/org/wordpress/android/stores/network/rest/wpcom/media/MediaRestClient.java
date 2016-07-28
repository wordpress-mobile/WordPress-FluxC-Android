package org.wordpress.android.stores.network.rest.wpcom.media;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.generated.MediaActionBuilder;
import org.wordpress.android.stores.model.MediaModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.stores.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.stores.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.store.MediaStore.ChangedMediaPayload;

import java.util.ArrayList;
import java.util.List;

public class MediaRestClient extends BaseWPComRestClient {
    public MediaRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken, UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchAllMedia(final SiteModel site) {
        String url = WPCOMREST.MEDIA_ALL.getUrlV1_1(String.valueOf(site.getSiteId()));
        add(new WPComGsonRequest<>(Request.Method.GET, url, null, MediaWPComRestResponse.MultipleMediaResponse.class,
                new Response.Listener<MediaWPComRestResponse.MultipleMediaResponse>() {
                    @Override
                    public void onResponse(MediaWPComRestResponse.MultipleMediaResponse response) {
                        List<MediaModel> mediaList = responseToMediaModelList(response);
                        ChangedMediaPayload payload = new ChangedMediaPayload(mediaList, null, null);
                        mDispatcher.dispatch(MediaActionBuilder.newFetchedAllMediaAction(payload));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ChangedMediaPayload payload = new ChangedMediaPayload(null, null, error);
                        mDispatcher.dispatch(MediaActionBuilder.newFetchedAllMediaAction(payload));
                    }
                }
        ));
    }

    private List<MediaModel> responseToMediaModelList(MediaWPComRestResponse.MultipleMediaResponse from) {
        List<MediaModel> media = new ArrayList<>();
        for (int i = 0; i < from.found; ++i) {
            media.add(i, responseToMediaModel(from.media.get(i)));
        }
        return media;
    }

    private MediaModel responseToMediaModel(MediaWPComRestResponse from) {
        MediaModel media = new MediaModel();
        media.setMediaId(from.ID);
        media.setUploadDate(from.date);
        media.setPostId(from.post_ID);
        media.setAuthorId(from.author_ID);
        media.setUrl(from.URL);
        media.setGuid(from.guid);
        media.setFileName(from.file);
        media.setFileExtension(from.extension);
        media.setMimeType(from.mime_type);
        media.setTitle(from.title);
        media.setCaption(from.caption);
        media.setDescription(from.description);
        media.setAlt(from.alt);
        media.setThumbnailUrl(from.thumbnails.thumbnail);
        media.setHeight(from.height);
        media.setWidth(from.width);
        media.setLength(from.length);
        media.setVideoPressGuid(from.videopress_guid);
        media.setVideoPressProcessingDone(from.videopress_processing_done);
        // TODO: legacy fields
        return media;
    }
}
