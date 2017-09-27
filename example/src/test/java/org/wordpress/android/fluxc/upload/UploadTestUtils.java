package org.wordpress.android.fluxc.upload;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostUploadModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.persistence.UploadSqlUtils;

class UploadTestUtils {
    private static final int TEST_LOCAL_SITE_ID = 42;

    static MediaModel getTestMedia(long mediaId) {
        MediaModel media = new MediaModel();
        media.setLocalSiteId(TEST_LOCAL_SITE_ID);
        media.setMediaId(mediaId);
        return media;
    }

    static MediaModel getLocalTestMedia() {
        MediaModel media = new MediaModel();
        media.setLocalSiteId(TEST_LOCAL_SITE_ID);
        return media;
    }

    static PostModel getTestPost() {
        PostModel post = new PostModel();
        post.setLocalSiteId(TEST_LOCAL_SITE_ID);
        return post;
    }

    static SiteModel getTestSite() {
        SiteModel siteModel = new SiteModel();
        siteModel.setId(TEST_LOCAL_SITE_ID);
        return siteModel;
    }

    static MediaUploadModel getMediaUploadModelForMediaModel(MediaModel mediaModel) {
        return UploadSqlUtils.getMediaUploadModelForLocalId(mediaModel.getId());
    }

    static PostUploadModel getPostUploadModelForPostModel(PostModel postModel) {
        return UploadSqlUtils.getPostUploadModelForLocalId(postModel.getId());
    }
}
