package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload;

@ActionEnum
public enum WCOrderAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrdersPayload.class)
    FETCH_ORDERS,
    @Action(payloadType = FetchOrdersCountPayload.class)
    FETCH_ORDERS_COUNT,
    @Action(payloadType = UpdateOrderStatusPayload.class)
    UPDATE_ORDER_STATUS,
    @Action(payloadType = FetchOrderNotesPayload.class)
    FETCH_ORDER_NOTES,
    @Action(payloadType = PostOrderNotePayload.class)
    POST_ORDER_NOTE,

    // Remote responses
    @Action(payloadType = FetchOrdersResponsePayload.class)
    FETCHED_ORDERS,
    @Action(payloadType = FetchOrdersCountResponsePayload.class)
    FETCHED_ORDERS_COUNT,
    @Action(payloadType = RemoteOrderPayload.class)
    UPDATED_ORDER_STATUS,
    @Action(payloadType = FetchOrderNotesResponsePayload.class)
    FETCHED_ORDER_NOTES,
    @Action(payloadType = RemoteOrderNotePayload.class)
    POSTED_ORDER_NOTE
}
