package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductVariationPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationPayload;

@ActionEnum
public enum WCProductAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchSingleProductPayload.class)
    FETCH_SINGLE_PRODUCT,
    @Action(payloadType = FetchSingleProductVariationPayload.class)
    FETCH_SINGLE_PRODUCT_VARIATION,

    // Remote responses
    @Action(payloadType = RemoteProductPayload.class)
    FETCHED_SINGLE_PRODUCT,
    @Action(payloadType = RemoteProductVariationPayload.class)
    FETCHED_SINGLE_PRODUCT_VARIATION
}
