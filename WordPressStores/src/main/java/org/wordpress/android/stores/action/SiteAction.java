package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.store.SiteStore;

@ActionEnum
public enum SiteAction implements IAction {
    @Action(payloadType = SiteModel.class)
    FETCH_SITE,
    @Action
    FETCH_SITES,
    FETCH_SITES_XMLRPC,
    @Action(payloadType = SiteStore.RefreshSitesXMLRPCPayload.class)
    @Action(payloadType = SiteModel.class)
    UPDATE_SITE,
    @Action(payloadType = SitesModel.class)
    UPDATE_SITES,
    @Action(payloadType = SiteModel.class)
    REMOVE_SITE,
    LOGOUT_WPCOM,
    @Action
    @Action(payloadType = SitesModel.class)
    SHOW_SITES,
    @Action(payloadType = SitesModel.class)
    HIDE_SITES;
}
