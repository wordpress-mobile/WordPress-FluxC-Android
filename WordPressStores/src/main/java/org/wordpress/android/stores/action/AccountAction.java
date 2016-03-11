package org.wordpress.android.stores.action;

public enum AccountAction implements IAction {
    FETCH,
    FETCH_SETTINGS,
    FETCHED,
    FETCHED_ACCOUNT,
    FETCHED_SETTINGS,
    POSTED_SETTINGS,
    UPDATE,
    ERROR_FETCH_ACCOUNT,
    ERROR_FETCH_ACCOUNT_SETTINGS,
    ERROR_POST_ACCOUNT_SETTINGS,
}
