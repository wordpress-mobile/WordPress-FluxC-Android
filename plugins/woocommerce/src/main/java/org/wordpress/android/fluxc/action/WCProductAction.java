package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchSingleOrderPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload;

@ActionEnum
public enum WCProductAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchSingleProductPayload.class)
    FETCH_SINGLE_PRODUCT,

    // Remote responses
    @Action(payloadType = RemoteProductPayload.class)
    FETCHED_SINGLE_PRODUCT
}
