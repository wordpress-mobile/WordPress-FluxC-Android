package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderNotePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload;
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload;
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload;

@ActionEnum
public enum WCOrderAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchOrdersPayload.class)
    FETCH_ORDERS,
    @Action(payloadType = FetchOrderListPayload.class)
    FETCH_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsPayload.class)
    FETCH_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrdersCountPayload.class)
    FETCH_ORDERS_COUNT,
    @Action(payloadType = UpdateOrderStatusPayload.class)
    UPDATE_ORDER_STATUS,
    @Action(payloadType = PostOrderNotePayload.class)
    POST_ORDER_NOTE,
    @Action(payloadType = FetchHasOrdersPayload.class)
    FETCH_HAS_ORDERS,
    @Action(payloadType = SearchOrdersPayload.class)
    SEARCH_ORDERS,
    @Action(payloadType = FetchOrderStatusOptionsPayload.class)
    FETCH_ORDER_STATUS_OPTIONS,
    @Action(payloadType = AddOrderShipmentTrackingPayload.class)
    ADD_ORDER_SHIPMENT_TRACKING,
    @Action(payloadType = DeleteOrderShipmentTrackingPayload.class)
    DELETE_ORDER_SHIPMENT_TRACKING,
    @Action(payloadType = FetchOrderShipmentProvidersPayload.class)
    FETCH_ORDER_SHIPMENT_PROVIDERS,

    // Remote responses
    @Action(payloadType = FetchOrdersResponsePayload.class)
    FETCHED_ORDERS,
    @Action(payloadType = FetchOrderListResponsePayload.class)
    FETCHED_ORDER_LIST,
    @Action(payloadType = FetchOrdersByIdsResponsePayload.class)
    FETCHED_ORDERS_BY_IDS,
    @Action(payloadType = FetchOrdersCountResponsePayload.class)
    FETCHED_ORDERS_COUNT,
    @Action(payloadType = RemoteOrderPayload.class)
    UPDATED_ORDER_STATUS,
    @Action(payloadType = RemoteOrderNotePayload.class)
    POSTED_ORDER_NOTE,
    @Action(payloadType = FetchHasOrdersResponsePayload.class)
    FETCHED_HAS_ORDERS,
    @Action(payloadType = SearchOrdersResponsePayload.class)
    SEARCHED_ORDERS,
    @Action(payloadType = FetchOrderStatusOptionsResponsePayload.class)
    FETCHED_ORDER_STATUS_OPTIONS,
    @Action(payloadType = AddOrderShipmentTrackingResponsePayload.class)
    ADDED_ORDER_SHIPMENT_TRACKING,
    @Action(payloadType = DeleteOrderShipmentTrackingResponsePayload.class)
    DELETED_ORDER_SHIPMENT_TRACKING,
    @Action(payloadType = FetchOrderShipmentProvidersResponsePayload.class)
    FETCHED_ORDER_SHIPMENT_PROVIDERS
}
