package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.store.PostStore.RemotePostPayload;
import org.wordpress.android.stores.store.PostStore.FetchPostsPayload;
import org.wordpress.android.stores.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.stores.store.PostStore.InstantiatePostPayload;

@ActionEnum
public enum PostAction implements IAction {
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_POSTS,
    @Action(payloadType = FetchPostsPayload.class)
    FETCH_PAGES,
    @Action(payloadType = FetchPostsResponsePayload.class)
    FETCHED_POSTS,
    @Action(payloadType = RemotePostPayload.class)
    FETCH_POST,
    @Action(payloadType = InstantiatePostPayload.class)
    INSTANTIATE_POST,
    @Action(payloadType = RemotePostPayload.class)
    PUSH_POST,
    @Action(payloadType = RemotePostPayload.class)
    PUSHED_POST,
    @Action(payloadType = PostModel.class)
    UPDATE_POST,
    @Action(payloadType = RemotePostPayload.class)
    DELETE_POST,
    @Action(payloadType = PostModel.class)
    REMOVE_POST
}
