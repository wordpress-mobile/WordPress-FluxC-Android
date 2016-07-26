package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.PostModel;
import org.wordpress.android.stores.store.PostStore;

@ActionEnum
public enum PostAction implements IAction {
    // TODO: Should this be distinguished into FETCH_POSTS and FETCH_PAGES? what about handling fetchMore this way too?
    @Action(payloadType = PostStore.FetchPostsPayload.class)
    FETCH_POSTS,
    // TODO: Rename to UPDATE_POSTS?
    @Action(payloadType = PostStore.FetchPostsResponsePayload.class)
    FETCHED_POSTS,
    @Action(payloadType = PostModel.class)
    UPDATE_POST
}
