package org.wordpress.android.stores.persistence;

import com.wellsql.generated.MediaModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.model.MediaModel;
import org.wordpress.android.stores.utils.MediaUtils;

import java.util.List;

public class MediaSqlUtils {
    public static List<MediaModel> getAllMedia() {
        return WellSql.select(MediaModel.class).getAsModel();
    }

    public static List<MediaModel> getAllMediaForSite(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.BLOG_ID, siteId).endWhere()
                .getAsModel();
    }

    public static List<MediaModel> getMediaById(long mediaId) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.MEDIA_ID, mediaId).endWhere()
                .getAsModel();
    }

    public static List<MediaModel> getMediaByVideoPressGuid(String videoPressGuid) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.VIDEO_PRESS_GUID, videoPressGuid).endWhere()
                .getAsModel();
    }

    public static int getNumberOfMedia() {
        return WellSql.select(MediaModel.class).getAsCursor().getCount();
    }

    public static int getNumberOfMediaForSite(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.BLOG_ID, siteId).endWhere()
                .getAsCursor().getCount();
    }

    public static int getNumberOfImages() {
        return WellSql.select(MediaModel.class)
                .where().contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE).endWhere()
                .getAsCursor().getCount();
    }

    public static int getNumberOfVideos() {
        return WellSql.select(MediaModel.class)
                .where().contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_VIDEO).endWhere()
                .getAsCursor().getCount();
    }

    public static int getNumberOfImagesForSite(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_IMAGE)
                .equals(MediaModelTable.BLOG_ID, siteId)
                .endGroup().endWhere().getAsCursor().getCount();
    }

    public static int getNumberOfVideosForSite(long siteId) {
        return WellSql.select(MediaModel.class)
                .where().beginGroup()
                .contains(MediaModelTable.MIME_TYPE, MediaUtils.MIME_TYPE_VIDEO)
                .equals(MediaModelTable.BLOG_ID, siteId)
                .endGroup().endWhere().getAsCursor().getCount();
    }

    public static int insertOrUpdateMedia(MediaModel media) {
        if (media == null) return 0;

        List<MediaModel> existingMedia = WellSql.select(MediaModel.class)
                .where().equals(MediaModelTable.MEDIA_ID, media.getMediaId()).endWhere()
                .getAsModel();

        if (existingMedia.isEmpty()) {
            // insert, media item does not exist
            WellSql.insert(media).asSingleTransaction(true).execute();
            return 0;
        } else {
            // update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaModel.class).whereId(oldId)
                    .put(media, new UpdateAllExceptId<MediaModel>()).execute();
        }
    }

    public static int deleteMedia(MediaModel media) {
        if (media == null) return 0;
        return WellSql.delete(MediaModel.class).whereId(media.getId());
    }
}
