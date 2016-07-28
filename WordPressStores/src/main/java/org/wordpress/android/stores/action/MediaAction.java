package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.store.MediaStore.FetchMediaPayload;
import org.wordpress.android.stores.store.MediaStore.ChangeMediaPayload;
import org.wordpress.android.stores.store.MediaStore.ChangedMediaPayload;

@ActionEnum
public enum MediaAction implements IAction {
    @Action(payloadType = FetchMediaPayload.class)      FETCH_ALL_MEDIA,
    @Action(payloadType = ChangedMediaPayload.class)    FETCHED_ALL_MEDIA,
    @Action(payloadType = FetchMediaPayload.class)      FETCH_MEDIA,
    @Action(payloadType = ChangedMediaPayload.class)    FETCHED_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class)     PUSH_MEDIA,// Update remotely
    @Action(payloadType = ChangedMediaPayload.class)    PUSHED_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class)     DELETE_MEDIA,// Delete remotely
    @Action(payloadType = ChangedMediaPayload.class)    DELETED_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class)     UPDATE_MEDIA,// Update locally
    @Action(payloadType = ChangedMediaPayload.class)    UPDATED_MEDIA,
    @Action(payloadType = ChangeMediaPayload.class)     REMOVE_MEDIA,// Delete locally
    @Action(payloadType = ChangedMediaPayload.class)    REMOVED_MEDIA
}
