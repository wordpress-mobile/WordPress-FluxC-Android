package org.wordpress.android.fluxc.network.rest.wpcom.post;

import android.text.TextUtils;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostsResponse;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostRestClient extends BaseWPComRestClient {
    @Inject
    public PostRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                          UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchPost(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_1();

        final WPComGsonRequest<PostWPComRestResponse> request = new WPComGsonRequest<>(Method.GET,
                url, null, PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel post = postResponseToPostModel(response);
                        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle errors
                    }
                }
        );
        add(request);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, final int offset) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.getUrlV1_1();

        Map<String, String> params = new HashMap<>();

        params.put("number", String.valueOf(PostStore.NUM_POSTS_PER_FETCH));
        if (getPages) {
            params.put("type", "page");
        }
        if (offset > 0) {
            params.put("offset", String.valueOf(offset));
        }

        final WPComGsonRequest<PostsResponse> request = new WPComGsonRequest<>(Method.GET,
                url, params, PostsResponse.class,
                new Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        PostsModel posts = new PostsModel();
                        for (PostWPComRestResponse postResponse : response.posts) {
                            PostModel post = postResponseToPostModel(postResponse);
                            post.setLocalSiteId(site.getId());
                            posts.add(post);
                        }

                        boolean canLoadMore = posts.size() == PostStore.NUM_POSTS_PER_FETCH;

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(posts, site, getPages,
                                offset > 0, canLoadMore);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        final String url;

        if (post.isLocalDraft()) {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.new_.getUrlV1_1();
        } else {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_1();
        }

        Map<String, String> params = postModelToParams(post);

        final WPComGsonRequest<PostWPComRestResponse> request = new WPComGsonRequest<>(Method.POST,
                url, params, PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel uploadedPost = postResponseToPostModel(response);

                        uploadedPost.setIsLocalDraft(false);
                        uploadedPost.setIsLocallyChanged(false);

                        RemotePostPayload payload = new RemotePostPayload(uploadedPost, site);
                        mDispatcher.dispatch(PostActionBuilder.newPushedPostAction(payload));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle errors
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    public void deletePost(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).delete.getUrlV1_1();

        final WPComGsonRequest<PostWPComRestResponse> request = new WPComGsonRequest<>(Method.POST,
                url, null, PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(postResponseToPostModel(response)));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle errors
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    private PostModel postResponseToPostModel(PostWPComRestResponse from) {
        PostModel post = new PostModel();
        post.setRemotePostId(from.ID);
        post.setRemoteSiteId(from.site_ID);
        post.setLink(from.URL); // Is this right?
        post.setDateCreated(from.date);
        post.setTitle(from.title);
        post.setContent(from.content);
        post.setExcerpt(from.excerpt);
        post.setSlug(from.slug);
        post.setStatus(from.status);
        post.setPassword(from.password);
        post.setIsPage(from.type.equals("page"));

        if (from.post_thumbnail != null) {
            post.setFeaturedImageId(from.post_thumbnail.id);
        }
        post.setPostFormat(from.format);
        if (from.geo != null) {
            post.setLatitude(from.geo.latitude);
            post.setLongitude(from.geo.longitude);
        }

        if (from.categories != null) {
            List<Long> categoryIds = new ArrayList<>();
            for (PostWPComRestResponse.Taxonomy value : from.categories.values()) {
                categoryIds.add(value.ID);
            }
            post.setCategoryIdList(categoryIds);
        }

        if (from.tags != null) {
            List<Long> tagIds = new ArrayList<>();
            for (PostWPComRestResponse.Taxonomy value : from.tags.values()) {
                tagIds.add(value.ID);
            }
            post.setTagIdList(tagIds);
        }

        if (from.capabilities != null) {
            post.setHasCapabilityPublishPost(from.capabilities.publish_post);
            post.setHasCapabilityEditPost(from.capabilities.edit_post);
            post.setHasCapabilityDeletePost(from.capabilities.delete_post);
        }

        if (from.parent != null) {
            post.setParentId(from.parent.ID);
            post.setParentTitle(from.parent.title);
        }

        return post;
    }

    private Map<String, String> postModelToParams(PostModel post) {
        Map<String, String> params = new HashMap<>();

        // TODO: Send empty strings where string values are null
        params.put("status", post.getStatus());
        params.put("title", post.getTitle());
        params.put("content", post.getContent());
        params.put("excerpt", post.getExcerpt());

        if (post.isLocalDraft() && post.getDateCreated() != null) {
            params.put("date", post.getDateCreated());
        }

        if (!post.isPage()) {
            if (!TextUtils.isEmpty(post.getPostFormat())) {
                params.put("format", post.getPostFormat());
            }
        } else {
            params.put("type", "page");
        }

        params.put("password", post.getPassword());

        params.put("categories_by_id", post.getCategoryIds());
        params.put("tags_by_id", post.getTagIds());

        // Will remove any existing featured image if this is the empty string
        params.put("featured_image", String.valueOf(post.getFeaturedImageId()));

        return params;
    }
}
