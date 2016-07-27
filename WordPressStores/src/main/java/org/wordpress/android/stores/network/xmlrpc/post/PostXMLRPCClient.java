package org.wordpress.android.stores.network.xmlrpc.post;

import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.generated.PostActionBuilder;
import org.wordpress.android.stores.model.PostLocation;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.model.PostsModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.stores.network.xmlrpc.XMLRPC;
import org.wordpress.android.stores.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.stores.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PostXMLRPCClient extends BaseXMLRPCClient {
    private static final int NUM_POSTS_TO_REQUEST = 20;

    public PostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void getPosts(final SiteModel site, final boolean getPages, final int offset) {
        int numPostsToRequest = offset + NUM_POSTS_TO_REQUEST;

        List<Object> params = new ArrayList<>(4);
        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(numPostsToRequest);

        XMLRPC method = (getPages ? XMLRPC.GET_PAGES : XMLRPC.GET_POSTS);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        boolean canLoadMore;
                        int startPosition = 0;
                        if (response != null && response.length > 0) {
                            canLoadMore = true;

                            // If we're loading more posts, only save the posts at the end of the array.
                            // NOTE: Switching to wp.getPosts wouldn't require janky solutions like this
                            // since it allows for an offset parameter.
                            if (offset > 0 && response.length > NUM_POSTS_TO_REQUEST) {
                                startPosition = response.length - NUM_POSTS_TO_REQUEST;
                            }
                        } else {
                            canLoadMore = false;
                        }

                        PostsModel posts = postsResponseToPostsModel(response, site, getPages, startPosition);

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(posts, site, getPages,
                                offset > 0, canLoadMore);

                        if (posts != null) {
                            mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                        } else {
                            // TODO: do nothing or dispatch error?
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Implement lower-level catching in BaseXMLRPCClient
                    }
                }
        );

        add(request);
    }

    public void deletePost(final PostModel post, final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        XMLRPC method = (post.isPage() ? XMLRPC.DELETE_PAGE : XMLRPC.DELETE_POST);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params, new Listener() {
            @Override
            public void onResponse(Object response) {}
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Implement lower-level catching in BaseXMLRPCClient
            }
        });

        add(request);
    }

    private PostsModel postsResponseToPostsModel(Object[] response, SiteModel site, boolean isPage, int startPosition) {
        List<Map<?, ?>> postsList = new ArrayList<>();
        for (int ctr = startPosition; ctr < response.length; ctr++) {
            Map<?, ?> postMap = (Map<?, ?>) response[ctr];
            postsList.add(postMap);
        }

        PostsModel posts = new PostsModel();

        for (Object postObject : postsList) {
            // Sanity checks
            if (!(postObject instanceof Map)) {
                continue;
            }

            Map<?, ?> postMap = (Map<?, ?>) postObject;
            PostModel post = new PostModel();

            String postID = MapUtils.getMapStr(postMap, (isPage) ? "page_id" : "postid");
            if (TextUtils.isEmpty(postID)) {
                // If we don't have a post or page ID, move on
                continue;
            }

            post.setLocalSiteId(site.getId());
            post.setRemotePostId(Integer.valueOf(postID));
            post.setTitle(MapUtils.getMapStr(postMap, "title"));

            Date dateCreated = MapUtils.getMapDate(postMap, "dateCreated");
            if (dateCreated != null) {
                post.setDateCreated(dateCreated.getTime());
            } else {
                Date now = new Date();
                post.setDateCreated(now.getTime());
            }

            Date dateCreatedGmt = MapUtils.getMapDate(postMap, "date_created_gmt");
            if (dateCreatedGmt != null) {
                post.setDateCreatedGmt(dateCreatedGmt.getTime());
            } else {
                dateCreatedGmt = new Date(post.getDateCreated());
                post.setDateCreatedGmt(dateCreatedGmt.getTime() + (dateCreatedGmt.getTimezoneOffset() * 60000));
            }


            post.setDescription(MapUtils.getMapStr(postMap, "description"));
            post.setLink(MapUtils.getMapStr(postMap, "link"));
            post.setPermaLink(MapUtils.getMapStr(postMap, "permaLink"));

            Object[] postCategories = (Object[]) postMap.get("categories");
            JSONArray jsonCategoriesArray = new JSONArray();
            if (postCategories != null) {
                for (Object postCategory : postCategories) {
                    jsonCategoriesArray.put(postCategory.toString());
                }
            }
            post.setCategories(jsonCategoriesArray.toString());

            Object[] custom_fields = (Object[]) postMap.get("custom_fields");
            JSONArray jsonCustomFieldsArray = new JSONArray();
            if (custom_fields != null) {
                PostLocation postLocation = new PostLocation();
                for (Object custom_field : custom_fields) {
                    jsonCustomFieldsArray.put(custom_field.toString());
                    // Update geo_long and geo_lat from custom fields
                    if (!(custom_field instanceof Map))
                        continue;
                    Map<?, ?> customField = (Map<?, ?>) custom_field;
                    if (customField.get("key") != null && customField.get("value") != null) {
                        if (customField.get("key").equals("geo_longitude"))
                            postLocation.setLongitude(Long.valueOf(customField.get("value").toString()));
                        if (customField.get("key").equals("geo_latitude"))
                            postLocation.setLatitude(Long.valueOf(customField.get("value").toString()));
                    }
                }
                post.setPostLocation(postLocation);
            }
            post.setCustomFields(jsonCustomFieldsArray.toString());

            post.setExcerpt(MapUtils.getMapStr(postMap, (isPage) ? "excerpt" : "mt_excerpt"));
            post.setMoreText(MapUtils.getMapStr(postMap, (isPage) ? "text_more" : "mt_text_more"));

            post.setAllowComments((MapUtils.getMapInt(postMap, "mt_allow_comments", 0)) != 0);
            post.setAllowPings((MapUtils.getMapInt(postMap, "mt_allow_pings", 0)) != 0);
            post.setSlug(MapUtils.getMapStr(postMap, "wp_slug"));
            post.setPassword(MapUtils.getMapStr(postMap, "wp_password"));
            post.setAuthorId(MapUtils.getMapStr(postMap, "wp_author_id"));
            post.setAuthorDisplayName(MapUtils.getMapStr(postMap, "wp_author_display_name"));
            post.setFeaturedImageId(MapUtils.getMapInt(postMap, "wp_post_thumbnail"));
            post.setStatus(MapUtils.getMapStr(postMap, (isPage) ? "page_status" : "post_status"));
            post.setUserId(Integer.valueOf(MapUtils.getMapStr(postMap, "userid")));

            if (isPage) {
                post.setIsPage(true);
                post.setPageParentId(MapUtils.getMapStr(postMap, "wp_page_parent_id"));
                post.setPageParentTitle(MapUtils.getMapStr(postMap, "wp_page_parent_title"));
            } else {
                post.setKeywords(MapUtils.getMapStr(postMap, "mt_keywords"));
                post.setPostFormat(MapUtils.getMapStr(postMap, "wp_post_format"));
            }

            posts.add(post);
        }

        if (posts.isEmpty()) {
            return null;
        }

        return posts;
    }
}
