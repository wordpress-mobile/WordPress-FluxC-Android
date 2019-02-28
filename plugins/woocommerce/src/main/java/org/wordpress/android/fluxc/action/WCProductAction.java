package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
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
