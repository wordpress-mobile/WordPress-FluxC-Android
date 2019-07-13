package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsV4Payload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsV4ResponsePayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsResponsePayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsPayload;
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsResponsePayload;

@ActionEnum
public enum WCStatsAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrderStatsPayload.class)
    FETCH_ORDER_STATS,

    @Action(payloadType = FetchOrderStatsV4Payload.class)
    FETCH_ORDER_STATS_V4,

    @Action(payloadType = FetchVisitorStatsPayload.class)
    FETCH_VISITOR_STATS,

    @Action(payloadType = FetchTopEarnersStatsPayload.class)
    FETCH_TOP_EARNERS_STATS,

    // Remote responses
    @Action(payloadType = FetchOrderStatsResponsePayload.class)
    FETCHED_ORDER_STATS,

    @Action(payloadType = FetchOrderStatsV4ResponsePayload.class)
    FETCHED_ORDER_STATS_V4,

    @Action(payloadType = FetchVisitorStatsResponsePayload.class)
    FETCHED_VISITOR_STATS,

    @Action(payloadType = FetchTopEarnersStatsResponsePayload.class)
    FETCHED_TOP_EARNERS_STATS
}
