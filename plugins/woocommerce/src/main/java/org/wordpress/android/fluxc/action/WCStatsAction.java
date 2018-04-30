package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload;

@ActionEnum
public enum WCStatsAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrderStatsPayload.class)
    FETCH_ORDER_STATS,

    // Remote responses
    @Action(payloadType = FetchOrderStatsResponsePayload.class)
    FETCHED_ORDER_STATS
}
