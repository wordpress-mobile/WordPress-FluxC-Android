package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.network.rest.wpcom.media.MediaRestClient;

@ActionEnum
public enum MediaAction implements IAction {
    @Action(payloadType = MediaRestClient.MediaFetchPayload.class)
    FETCH_ALL_MEDIA,
    @Action(payloadType = MediaRestClient.MediaFetchPayload.class)
    FETCH_MEDIA,
    @Action
    SAVE_MEDIA
}
