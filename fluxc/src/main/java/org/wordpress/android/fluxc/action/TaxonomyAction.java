package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchCategoriesPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermRequestPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoveAllTermsPayload;

@ActionEnum
public enum TaxonomyAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchCategoriesPayload.class)
    FETCH_CATEGORIES,
    @Action(payloadType = FetchCategoriesPayload.class)
    FETCH_TAGS,
    @Action(payloadType = FetchTermsPayload.class)
    FETCH_TERMS,
    @Action(payloadType = RemoteTermRequestPayload.class)
    FETCH_TERM,
    @Action(payloadType = RemoteTermRequestPayload.class)
    PUSH_TERM,

    // Remote responses
    @Action(payloadType = FetchTermsResponsePayload.class)
    FETCHED_TERMS,
    @Action(payloadType = FetchTermResponsePayload.class)
    FETCHED_TERM,
    @Action(payloadType = RemoteTermResponsePayload.class)
    PUSHED_TERM,

    // Local actions
    @Action(payloadType = TermModel.class)
    UPDATE_TERM,
    @Action(payloadType = RemoveAllTermsPayload.class)
    REMOVE_ALL_TERMS
}

