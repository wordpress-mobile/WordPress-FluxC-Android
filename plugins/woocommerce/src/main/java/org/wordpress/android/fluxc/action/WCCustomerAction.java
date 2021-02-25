package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.WCCustomerStore.FetchSingleCustomerPayload;
import org.wordpress.android.fluxc.store.WCCustomerStore.RemoteCustomerPayload;

@ActionEnum
public enum WCCustomerAction implements IAction {
    // region remote actions
    @Action(payloadType = FetchSingleCustomerPayload.class)
    FETCH_SINGLE_CUSTOMER,
    // endregion

    // region remote responses
    @Action(payloadType = RemoteCustomerPayload.class)
    FETCHED_SINGLE_CUSTOMER,
    // endregion
}
