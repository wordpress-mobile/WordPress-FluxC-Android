package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListPayload;
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaResponsePayload;
import org.wordpress.android.fluxc.store.MediaStore.MediaRequestPayload;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;
import org.wordpress.android.fluxc.store.MediaStore.RemoveAllMediaPayload;

@ActionEnum
public enum MediaAction implements IAction {
    // Remote actions
    @Action(payloadType = MediaRequestPayload.class)
    PUSH_MEDIA,
    @Action(payloadType = MediaRequestPayload.class)
    UPLOAD_MEDIA,
    @Action(payloadType = FetchMediaListPayload.class)
    FETCH_MEDIA_LIST,
    @Action(payloadType = MediaRequestPayload.class)
    FETCH_MEDIA,
    @Action(payloadType = MediaRequestPayload.class)
    DELETE_MEDIA,
    @Action(payloadType = CancelMediaPayload.class)
    CANCEL_MEDIA_UPLOAD,

    // Remote responses
    @Action(payloadType = MediaResponsePayload.class)
    PUSHED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    UPLOADED_MEDIA,
    @Action(payloadType = FetchMediaListResponsePayload.class)
    FETCHED_MEDIA_LIST,
    @Action(payloadType = MediaResponsePayload.class)
    FETCHED_MEDIA,
    @Action(payloadType = MediaResponsePayload.class)
    DELETED_MEDIA,
    @Action(payloadType = ProgressPayload.class)
    CANCELED_MEDIA_UPLOAD,

    // Local actions
    @Action(payloadType = MediaRequestPayload.class)
    UPDATE_MEDIA,
    @Action(payloadType = MediaRequestPayload.class)
    REMOVE_MEDIA,
    @Action(payloadType = RemoveAllMediaPayload.class)
    REMOVE_ALL_MEDIA
}
