package org.wordpress.android.stores.store;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.MediaAction;
import org.wordpress.android.stores.annotations.action.Action;
import org.wordpress.android.stores.generated.MediaActionBuilder;
import org.wordpress.android.stores.model.MediaModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.rest.wpcom.media.MediaRestClient;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store {
    //
    // Payloads
    //

    /**
     * Used for FETCH_ALL_MEDIA and FETCH_MEDIA actions
     */
    public static class FetchMediaPayload implements Payload {
        public SiteModel site;
        public List<Long> mediaIds;
        public FetchMediaPayload(SiteModel site, List<Long> mediaIds) {
            this.site = site;
            this.mediaIds = mediaIds;
        }
    }

    /**
     * Used for DELETE_MEDIA, REMOVE_MEDIA, PUSH_MEDIA, and UPDATE_MEDIA actions
     */
    public static class ChangeMediaPayload implements Payload {
        public SiteModel site;
        public List<MediaModel> media;
        public ChangeMediaPayload(SiteModel site, List<MediaModel> media) {
            this.site = site;
            this.media = media;
        }
    }

    public static class ChangedMediaPayload implements Payload {
        public List<MediaModel> media;
        public List<Exception> errors;
        public Exception error;
        public ChangedMediaPayload(List<MediaModel> media, List<Exception> errors, Exception error) {
            this.media = media;
            this.errors = errors;
            this.error = error;
        }
        public boolean isError() {
            if (errors != null) {
                for (Exception e : errors) {
                    if (e == null) return false;
                }
            }
            return error != null;
        }
    }

    //
    // OnChanged events
    //

    public class OnMediaChanged extends OnChanged {
        public MediaAction causeOfChange;
        public List<MediaModel> media;
        public OnMediaChanged(MediaAction cause, List<MediaModel> media) {
            this.causeOfChange = cause;
            this.media = media;
        }
    }

    public class OnMediaError extends OnChanged {
        public MediaAction causeOfError;
        public Exception error;
        public OnMediaError(MediaAction cause, Exception error) {
            this.causeOfError = cause;
            this.error = error;
        }
    }

    private MediaRestClient mMediaRestClient;

    @Inject
    public MediaStore(Dispatcher dispatcher, MediaRestClient mediaRestClient) {
        super(dispatcher);
        mMediaRestClient = mediaRestClient;
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        if (action.getType() == MediaAction.FETCH_ALL_MEDIA) {
            FetchMediaPayload payload = (FetchMediaPayload) action.getPayload();
            mMediaRestClient.fetchAllMedia(payload.site.getSiteId());
        } else if (action.getType() == MediaAction.FETCHED_ALL_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
            final OnChanged changeEvent = payload.isError() ?
                    new OnMediaError(MediaAction.FETCH_ALL_MEDIA, payload.error) :
                    new OnMediaChanged(MediaAction.FETCH_ALL_MEDIA, payload.media);
            emitChange(changeEvent);
        } else if (action.getType() == MediaAction.FETCH_MEDIA) {
            FetchMediaPayload payload = (FetchMediaPayload) action.getPayload();
            mMediaRestClient.fetchMedia(payload.site.getSiteId(), payload.mediaIds);
        } else if (action.getType() == MediaAction.FETCHED_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
            final OnChanged changeEvent = payload.isError() ?
                    // TODO: not using correct error here
                    new OnMediaError(MediaAction.FETCH_MEDIA, payload.error) :
                    new OnMediaChanged(MediaAction.FETCH_MEDIA, payload.media);
            emitChange(changeEvent);
        } else if (action.getType() == MediaAction.PUSH_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
        } else if (action.getType() == MediaAction.PUSHED_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
        } else if (action.getType() == MediaAction.DELETE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
            mMediaRestClient.deleteMedia(payload.site.getSiteId(), payload.media);
        } else if (action.getType() == MediaAction.DELETED_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
            final OnChanged changeEvent = payload.isError() ?
                    new OnMediaError(MediaAction.DELETE_MEDIA, payload.error) :
                    new OnMediaChanged(MediaAction.DELETE_MEDIA, payload.media);
            emitChange(changeEvent);
        } else if (action.getType() == MediaAction.UPDATE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
        } else if (action.getType() == MediaAction.UPDATED_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
        } else if (action.getType() == MediaAction.REMOVE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
        } else if (action.getType() == MediaAction.REMOVED_MEDIA) {
            ChangedMediaPayload payload = (ChangedMediaPayload) action.getPayload();
        }
    }

    @Override
    public void onRegister() {
    }
}
