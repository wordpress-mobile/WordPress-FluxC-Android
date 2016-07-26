package org.wordpress.android.stores.store;

import android.database.Cursor;

import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.Payload;
import org.wordpress.android.stores.action.PostAction;
import org.wordpress.android.stores.annotations.action.Action;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.model.PostsModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.stores.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.stores.persistence.PostSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostStore extends Store {
    public static class FetchPostsPayload implements Payload {
        public SiteModel site;
        public boolean loadMore;

        public FetchPostsPayload(SiteModel site) {
            this.site = site;
        }

        public FetchPostsPayload(SiteModel site, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    public static class FetchPostsResponsePayload implements Payload {
        public PostsModel posts;
        public boolean canLoadMore;

        public FetchPostsResponsePayload(PostsModel posts, boolean canLoadMore) {
            this.posts = posts;
            this.canLoadMore = canLoadMore;
        }
    }

    private PostRestClient mPostRestClient;
    private PostXMLRPCClient mPostXMLRPCClient;

    @Inject
    public PostStore(Dispatcher dispatcher, PostRestClient postRestClient, PostXMLRPCClient postXMLRPCClient) {
        super(dispatcher);
        mPostRestClient = postRestClient;
        mPostXMLRPCClient = postXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PostStore onRegister");
    }

    /**
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPosts() {
        return WellSql.select(PostModel.class).getAsModel();
    }

    /**
     * Returns all posts in the store as a {@link Cursor}.
     */
    public Cursor getPostsCursor() {
        return WellSql.select(PostModel.class).getAsCursor();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPostsCount() {
        return getPostsCursor().getCount();
    }

    /**
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPostsForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, false);
    }

    /**
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPagesForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, true);
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPostsCountForSite(SiteModel site) {
        return getPostsForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPagesCountForSite(SiteModel site) {
        return getPagesForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public List<PostModel> getUploadedPostsForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, false);
    }

    /**
     * Returns the number of posts in the store.
     */
    public List<PostModel> getUploadedPagesForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, true);
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getUploadedPostsCountForSite(SiteModel site) {
        return getUploadedPostsForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getUploadedPagesCountForSite(SiteModel site) {
        return getUploadedPagesForSite(site).size();
    }

    public PostModel getPostByLocalPostId(long localId) {
        return WellSql.select(PostModel.class)
                .where().equals(PostModelTable.ID, localId).endWhere()
                .getAsModel().get(0);
    }

    @Subscribe
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType == PostAction.FETCH_POSTS) {
            FetchPostsPayload payload = (FetchPostsPayload) action.getPayload();
            if (payload.site.isWPCom() || payload.site.isJetpack()) {
                // TODO: Implement REST API posts fetch
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                if (payload.loadMore) {
                    mPostXMLRPCClient.getPosts(payload.site, false, getUploadedPostsCountForSite(payload.site));
                } else {
                    mPostXMLRPCClient.getPosts(payload.site, false, 0);
                }
            }
        } else if (actionType == PostAction.FETCH_PAGES) {
            FetchPostsPayload payload = (FetchPostsPayload) action.getPayload();
            if (payload.site.isWPCom() || payload.site.isJetpack()) {
                // TODO: Implement REST API posts fetch
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                if (payload.loadMore) {
                    mPostXMLRPCClient.getPosts(payload.site, true, getUploadedPagesCountForSite(payload.site));
                } else {
                    mPostXMLRPCClient.getPosts(payload.site, true, 0);
                }
            }
        } else if (actionType == PostAction.FETCHED_POSTS) {
            FetchPostsResponsePayload postsResponsePayload = (FetchPostsResponsePayload) action.getPayload();
            for (PostModel post : postsResponsePayload.posts) {
                PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(post);
            }
        } else if (actionType == PostAction.UPDATE_POST) {
            PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges((PostModel) action.getPayload());
        }
    }
}
