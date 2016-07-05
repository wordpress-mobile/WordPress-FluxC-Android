package org.wordpress.android.stores.action;

public enum AccountAction implements IAction {
    FETCH, // request fetch of both Account and Account Settings
    FETCH_ACCOUNT, // request fetch of Account information
    FETCHED_ACCOUNT, // response received from Account fetch request
    FETCH_SETTINGS, // request fetch of Account Settings
    FETCHED_SETTINGS, // response received from Account Settings fetch
    POST_SETTINGS, // request saving Account Settings remotely
    POSTED_SETTINGS, // response received from Account Settings post
    UPDATE, // update in-memory and persisted Account in AccountStore
    SIGN_OUT, // delete persisted Account, reset in-memory Account, delete access token
    VALIDATE_NEW_ACCOUNT, // validate new account informations
    VALIDATED_NEW_ACCOUNT, // validate new account response
    CREATE_NEW_ACCOUNT, // create a new account
    CREATED_NEW_ACCOUNT, // create a new account response
}
