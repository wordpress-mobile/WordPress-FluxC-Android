package org.wordpress.android.fluxc.network.rest.wpcom.site;

import org.wordpress.android.fluxc.network.Response;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.Plan;

import java.util.List;

public class SitePlansWPComRestResponse implements Response {
    public class SiteUnLocalisedPlansResponse {
        public List<SitePlansWPComRestResponse> sites;
    }

    public long ID;
    public Plan plan;
}
