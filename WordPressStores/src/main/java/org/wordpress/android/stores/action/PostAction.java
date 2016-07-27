package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.store.PostStore;

@ActionEnum
public enum PostAction implements IAction {
    @Action(payloadType = PostStore.FetchPostsPayload.class)
    FETCH_POSTS,
    @Action(payloadType = PostStore.FetchPostsPayload.class)
    FETCH_PAGES,
    @Action(payloadType = PostStore.FetchPostsResponsePayload.class)
    FETCHED_POSTS,
    @Action(payloadType = PostModel.class)
    UPDATE_POST,
    @Action(payloadType = PostStore.ChangePostPayload.class)
    DELETE_POST,
    @Action(payloadType = PostModel.class)
    REMOVE_POST
}
