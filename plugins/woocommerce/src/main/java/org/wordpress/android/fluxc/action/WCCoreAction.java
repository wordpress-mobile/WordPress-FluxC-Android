package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchApiVersionResponsePayload;

@ActionEnum
public enum WCCoreAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_SITE_API_VERSION,

    // Remote responses
    @Action(payloadType = FetchApiVersionResponsePayload.class)
    FETCHED_SITE_API_VERSION
}
