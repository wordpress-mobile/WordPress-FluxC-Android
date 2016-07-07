package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.ActionEnum;
import org.wordpress.android.stores.annotations.action.IAction;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.model.SitesModel;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.stores.store.SiteStore.NewSitePayload;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;

@ActionEnum
public enum SiteAction implements IAction {
    @Action(payloadType = SiteModel.class)
    FETCH_SITE,
    @Action
    FETCH_SITES,
    @Action(payloadType = RefreshSitesXMLRPCPayload.class)
    FETCH_SITES_XML_RPC,
    @Action(payloadType = SiteModel.class)
    UPDATE_SITE,
    @Action(payloadType = SitesModel.class)
    UPDATE_SITES,
    @Action(payloadType = SiteModel.class)
    REMOVE_SITE,
    @Action
    REMOVE_WPCOM_SITES,
    @Action(payloadType = SitesModel.class)
    SHOW_SITES,
    @Action(payloadType = SitesModel.class)
    HIDE_SITES,
    @Action(payloadType = NewSitePayload.class)
    CREATE_NEW_SITE,
    @Action(payloadType = NewSiteResponsePayload.class)
    CREATED_NEW_SITE,
}
