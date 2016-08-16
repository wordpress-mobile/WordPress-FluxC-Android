package org.wordpress.android.fluxc.store;

import com.wellsql.generated.MediaModelTable;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.media.MediaRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient;
import org.wordpress.android.fluxc.persistence.MediaSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaStore extends Store
        implements MediaRestClient.MediaRestListener, MediaXMLRPCClient.MediaXmlRpcListener {
    //
    // Payloads
    //

    /**
     * Used for PULL_ALL_MEDIA and PULL_MEDIA actions
     */
    public static class PullMediaPayload implements Payload {
        public SiteModel site;
        public List<Long> mediaIds;
        public PullMediaPayload(SiteModel site, List<Long> mediaIds) {
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

    public class OnMediaProgress extends OnChanged {
        public MediaModel media;
        public float progress;
        public OnMediaProgress(MediaModel media, float progress) {
            this.media = media;
            this.progress = progress;
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
    private MediaXMLRPCClient mMediaXmlrpcClient;

    @Inject
    public MediaStore(Dispatcher dispatcher, MediaRestClient restClient, MediaXMLRPCClient xmlrpcClient) {
        super(dispatcher);
        mMediaRestClient = restClient;
        mMediaRestClient.setListener(this);
        mMediaXmlrpcClient = xmlrpcClient;
        mMediaXmlrpcClient.setListener(this);
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        if (action.getType() == MediaAction.PUSH_MEDIA) {
            performPushMedia((ChangeMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.UPLOAD_MEDIA) {
            performUploadMedia((ChangeMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.PULL_ALL_MEDIA) {
            performPullAllMedia((PullMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.PULL_MEDIA) {
            performPullMedia((PullMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.DELETE_MEDIA) {
            performDeleteMedia((ChangeMediaPayload) action.getPayload());
        } else if (action.getType() == MediaAction.UPDATE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
            updateMedia(payload.media);
        } else if (action.getType() == MediaAction.REMOVE_MEDIA) {
            ChangeMediaPayload payload = (ChangeMediaPayload) action.getPayload();
            removeMedia(payload.media);
        }
    }

    @Override
    public void onRegister() {
    }

    @Override
    public void onMediaError(MediaAction cause, Exception error) {
        AppLog.d(AppLog.T.MEDIA, cause + " caused error: " + error);
    }

    @Override
    public void onMediaPulled(MediaAction cause, List<MediaModel> pulledMedia, List<Exception> errors) {
        if (cause == MediaAction.PULL_ALL_MEDIA || cause == MediaAction.PULL_MEDIA) {
            emitChange(new OnMediaChanged(cause, pulledMedia));
        }
    }

    @Override
    public void onMediaPushed(MediaAction cause, List<MediaModel> pushedMedia, List<Exception> errors) {
        emitChange(new OnMediaChanged(cause, pushedMedia));
    }

    @Override
    public void onMediaDeleted(MediaAction cause, List<MediaModel> deletedMedia, List<Exception> errors) {
        if (cause == MediaAction.DELETE_MEDIA) {
            emitChange(new OnMediaChanged(cause, deletedMedia));
        }
    }

    @Override
    public void onMediaUploadProgress(MediaAction cause, MediaModel media, float progress) {
        AppLog.v(AppLog.T.MEDIA, "Progress update on upload of " + media.getTitle() + ": " + progress);
        emitChange(new OnMediaProgress(media, progress));
    }

    public List<MediaModel> getAllSiteMedia(long siteId) {
        return MediaSqlUtils.getAllSiteMedia(siteId);
    }

    public int getSiteMediaCount(long siteId) {
        return getAllSiteMedia(siteId).size();
    }

    public boolean hasSiteMediaWithId(long siteId, long mediaId) {
        return getSiteMediaWithId(siteId, mediaId) != null;
    }

    public MediaModel getSiteMediaWithId(long siteId, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteId, mediaId);
        return media.size() > 0 ? media.get(0) : null;
    }

    public List<MediaModel> getSiteMediaWithIds(long siteId, List<Long> mediaIds) {
        return MediaSqlUtils.getSiteMediaWithIds(siteId, mediaIds);
    }

    public List<MediaModel> getSiteImages(long siteId) {
        return MediaSqlUtils.getSiteImages(siteId);
    }

    public int getSiteImageCount(long siteId) {
        return getSiteImages(siteId).size();
    }

    public List<MediaModel> getSiteImagesExcludingIds(long siteId, List<Long> filter) {
        return MediaSqlUtils.getSiteImagesExcluding(siteId, filter);
    }

    public List<MediaModel> getUnattachedSiteMedia(long siteId) {
        return MediaSqlUtils.matchSiteMedia(siteId, MediaModelTable.POST_ID, 0);
    }

    public int getUnattachedSiteMediaCount(long siteId) {
        return getUnattachedSiteMedia(siteId).size();
    }

    public List<MediaModel> getLocalSiteMedia(long siteId) {
        return MediaSqlUtils.matchSiteMedia(siteId, MediaModelTable.UPLOAD_DATE, null);
    }

    public String getUrlForSiteVideoWithVideoPressGuid(long siteId, String videoPressGuid) {
        List<MediaModel> media =
                MediaSqlUtils.matchSiteMedia(siteId, MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid);
        return media.size() > 0 ? media.get(0).getVideoPressGuid() : null;
    }

    public String getThumbnailUrlForSiteMediaWithId(long siteId, long mediaId) {
        List<MediaModel> media = MediaSqlUtils.getSiteMediaWithId(siteId, mediaId);
        return media.size() > 0 ? media.get(0).getThumbnailUrl() : null;
    }

    public List<MediaModel> searchSiteMediaByTitle(long siteId, String titleSearch) {
        return MediaSqlUtils.searchSiteMedia(siteId, MediaModelTable.TITLE, titleSearch);
    }

    public MediaModel getPostMediaWithPath(long postId, String filePath) {
        List<MediaModel> media = MediaSqlUtils.matchPostMedia(postId, MediaModelTable.FILE_PATH, filePath);
        return media.size() > 0 ? media.get(0) : null;
    }

    public MediaModel getNextSiteMediaToDelete(long siteId) {
        List<MediaModel> media = MediaSqlUtils.matchSiteMedia(siteId,
                MediaModelTable.UPLOAD_STATE, MediaModel.UPLOAD_STATE.DELETE.toString());
        return media.size() > 0 ? media.get(0) : null;
    }

    public boolean hasSiteMediaToDelete(long siteId) {
        return getNextSiteMediaToDelete(siteId) != null;
    }

    private void updateMedia(List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        OnMediaChanged event = new OnMediaChanged(MediaAction.UPDATE_MEDIA, new ArrayList<MediaModel>());
        for (MediaModel mediaItem : media) {
            if (MediaSqlUtils.insertOrUpdateMedia(mediaItem) > 0) {
                event.media.add(mediaItem);
            }
        }
        emitChange(event);
    }

    private void removeMedia(List<MediaModel> media) {
        if (media == null || media.isEmpty()) return;

        OnMediaChanged event = new OnMediaChanged(MediaAction.REMOVE_MEDIA, new ArrayList<MediaModel>());
        for (MediaModel mediaItem : media) {
            if (MediaSqlUtils.deleteMedia(mediaItem) > 0) {
                event.media.add(mediaItem);
            }
        }
        emitChange(event);

    }

    private OnChanged getErrorOrChangedEvent(MediaAction cause, ChangedMediaPayload payload) {
        return payload.isError() ? new OnMediaError(cause, payload.error) :
                                   new OnMediaChanged(cause, payload.media);
    }
}
