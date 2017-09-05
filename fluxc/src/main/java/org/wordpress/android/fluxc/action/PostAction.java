package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostRequest;
import org.wordpress.android.fluxc.store.PostStore.RemotePostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.RemoveAllPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.RemovePostPayload;
import org.wordpress.android.fluxc.store.PostStore.SearchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.SearchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.UpdatePostPayload;

@ActionEnum
public enum PostAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_POSTS,
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_PAGES,
    @Action(payloadType = RemotePostRequest.class)
    FETCH_POST,
    @Action(payloadType = RemotePostRequest.class)
    PUSH_POST,
    @Action(payloadType = RemotePostRequest.class)
    DELETE_POST,
    @Action(payloadType = SearchPostsPayload.class)
    SEARCH_POSTS,
    @Action(payloadType = SearchPostsPayload.class)
    SEARCH_PAGES,

    // Remote responses
    @Action(payloadType = FetchPostsResponsePayload.class)
    FETCHED_POSTS,
    @Action(payloadType = FetchPostResponsePayload.class)
    FETCHED_POST,
    @Action(payloadType = RemotePostResponsePayload.class)
    PUSHED_POST,
    @Action(payloadType = RemotePostResponsePayload.class)
    DELETED_POST,
    @Action(payloadType = SearchPostsResponsePayload.class)
    SEARCHED_POSTS,

    // Local actions
    @Action(payloadType = UpdatePostPayload.class)
    UPDATE_POST,
    @Action(payloadType = RemovePostPayload.class)
    REMOVE_POST,
    @Action(payloadType = RemoveAllPostsPayload.class)
    REMOVE_ALL_POSTS
}

