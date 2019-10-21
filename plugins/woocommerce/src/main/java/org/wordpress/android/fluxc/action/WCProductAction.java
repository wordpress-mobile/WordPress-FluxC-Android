package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductReviewPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.SearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload;
import org.wordpress.android.fluxc.store.WCProductStore.UpdateProductReviewStatusPayload;

@ActionEnum
public enum WCProductAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchSingleProductPayload.class)
    FETCH_SINGLE_PRODUCT,
    @Action(payloadType = FetchProductsPayload.class)
    FETCH_PRODUCTS,
    @Action(payloadType = SearchProductsPayload.class)
    SEARCH_PRODUCTS,
    @Action(payloadType = FetchProductVariationsPayload.class)
    FETCH_PRODUCT_VARIATIONS,
    @Action(payloadType = FetchProductReviewsPayload.class)
    FETCH_PRODUCT_REVIEWS,
    @Action(payloadType = FetchSingleProductReviewPayload.class)
    FETCH_SINGLE_PRODUCT_REVIEW,
    @Action(payloadType = UpdateProductReviewStatusPayload.class)
    UPDATE_PRODUCT_REVIEW_STATUS,

    // Remote responses
    @Action(payloadType = RemoteProductPayload.class)
    FETCHED_SINGLE_PRODUCT,
    @Action(payloadType = RemoteProductListPayload.class)
    FETCHED_PRODUCTS,
    @Action(payloadType = RemoteSearchProductsPayload.class)
    SEARCHED_PRODUCTS,
    @Action(payloadType = RemoteProductVariationsPayload.class)
    FETCHED_PRODUCT_VARIATIONS,
    @Action(payloadType = FetchProductReviewsResponsePayload.class)
    FETCHED_PRODUCT_REVIEWS,
    @Action(payloadType = RemoteProductReviewPayload.class)
    FETCHED_SINGLE_PRODUCT_REVIEW,
    @Action(payloadType = RemoteProductReviewPayload.class)
    UPDATED_PRODUCT_REVIEW_STATUS
}
