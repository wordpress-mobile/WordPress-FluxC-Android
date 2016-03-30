package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.ActionAnnotation;
import org.wordpress.android.stores.annotations.ActionEnumAnnotation;
import org.wordpress.android.stores.annotations.IAction;

public enum SiteAction implements IAction {
    FETCH_SITE,
    FETCH_SITES,
    FETCH_SITES_XMLRPC,
    UPDATE_SITE,
    UPDATE_SITES,
    REMOVE_SITE,
    LOGOUT_WPCOM,
    SHOW_SITES,
    HIDE_SITES;
}
