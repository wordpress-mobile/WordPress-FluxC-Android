package org.wordpress.android.fluxc.model;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.BasePayload;

import java.util.ArrayList;
import java.util.List;

public class SitesModel extends BasePayload {
    private List<SiteModel> mSites;

    public SitesModel() {
        mSites = new ArrayList<>();
    }

    public SitesModel(@NonNull List<SiteModel> sites) {
        mSites = sites;
    }

    public List<SiteModel> getSites() {
        return mSites;
    }

    public void setSites(List<SiteModel> sites) {
        this.mSites = sites;
    }
}
