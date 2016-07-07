package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient.AccountPostSettingsResponsePayload;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient.AccountRestPayload;
import org.wordpress.android.stores.store.AccountStore.PostAccountSettingsPayload;
import org.wordpress.android.stores.store.AccountStore.UpdateTokenPayload;

@ActionEnum
public enum AccountAction implements IAction {
    @Action
    FETCH,                  // request fetch of both Account and Account Settings
    @Action
    FETCH_ACCOUNT,          // request fetch of Account information
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_ACCOUNT,        // response received from Account fetch request
    @Action
    FETCH_SETTINGS,         // request fetch of Account Settings
    @Action(payloadType = AccountRestPayload.class)
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    @Action(payloadType = PostAccountSettingsPayload.class)
    POST_SETTINGS,          // request saving Account Settings remotely
    @Action(payloadType = AccountPostSettingsResponsePayload.class)
    POSTED_SETTINGS,        // response received from Account Settings post
    @Action(payloadType = AccountModel.class)
    UPDATE,                 // update in-memory and persisted Account in AccountStore
    @Action(payloadType = UpdateTokenPayload.class)
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    @Action
    SIGN_OUT,               // delete persisted Account, reset in-memory Account, delete access token
}
