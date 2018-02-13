package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload;

@ActionEnum
public enum WCOrderAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrdersPayload.class)
    FETCH_ORDERS,

    // Remote responses
    @Action(payloadType = FetchOrdersResponsePayload.class)
    FETCHED_ORDERS
}
