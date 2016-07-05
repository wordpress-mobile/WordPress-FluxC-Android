package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.action.IAction;

public enum AccountAction implements IAction {
    FETCH,                  // request fetch of both Account and Account Settings
    FETCH_ACCOUNT,          // request fetch of Account information
    FETCHED_ACCOUNT,        // response received from Account fetch request
    FETCH_SETTINGS,         // request fetch of Account Settings
    FETCHED_SETTINGS,       // response received from Account Settings fetch
    POST_SETTINGS,          // request saving Account Settings remotely
    POSTED_SETTINGS,        // response received from Account Settings post
    UPDATE,                 // update in-memory and persisted Account in AccountStore
    UPDATE_ACCESS_TOKEN,    // update in-memory and persisted Access Token
    SIGN_OUT,               // delete persisted Account, reset in-memory Account, delete access token
}
