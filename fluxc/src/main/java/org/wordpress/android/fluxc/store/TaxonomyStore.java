package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.RequestPayload;
import org.wordpress.android.fluxc.ResponsePayload;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.TermsModel;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TaxonomyRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.taxonomy.TaxonomyXMLRPCClient;
import org.wordpress.android.fluxc.persistence.TaxonomySqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class TaxonomyStore extends Store {
    public static final String DEFAULT_TAXONOMY_CATEGORY = "category";
    public static final String DEFAULT_TAXONOMY_TAG = "post_tag";

    public static class FetchCategoriesPayload extends RequestPayload {
        public SiteModel site;

        public FetchCategoriesPayload(SiteModel site) {
            this.site = site;
        }
    }

    public static class FetchTermsPayload extends RequestPayload {
        public SiteModel site;
        public TaxonomyModel taxonomy;

        public FetchTermsPayload(SiteModel site, TaxonomyModel taxonomy) {
            this.site = site;
            this.taxonomy = taxonomy;
        }
    }

    public static class FetchTermsResponsePayload extends ResponsePayload {
        public TaxonomyError error;
        public TermsModel terms;
        public SiteModel site;
        public String taxonomy;

        public FetchTermsResponsePayload(RequestPayload requestPayload, TermsModel terms, SiteModel site,
                                         String taxonomy) {
            super(requestPayload);
            this.terms = terms;
            this.site = site;
            this.taxonomy = taxonomy;
        }

        public FetchTermsResponsePayload(RequestPayload requestPayload, TaxonomyError error, String taxonomy) {
            super(requestPayload);
            this.error = error;
            this.taxonomy = taxonomy;
        }
    }

    public static class RemoteTermRequestPayload extends RequestPayload {
        public TermModel term;
        public SiteModel site;

        public RemoteTermRequestPayload(TermModel term, SiteModel site) {
            this.term = term;
            this.site = site;
        }
    }

    public static class RemoteTermResponsePayload extends ResponsePayload {
        public final TaxonomyError error;
        public TermModel term;
        public SiteModel site;

        public RemoteTermResponsePayload(RequestPayload requestPayload, TermModel term, SiteModel site,
                                         TaxonomyError error) {
            super(requestPayload);
            this.error = error;
            this.term = term;
            this.site = site;
        }
    }

    public static class RemoveAllTermsPayload extends RequestPayload {}

    public static class FetchTermResponsePayload extends RemoteTermResponsePayload {
        public TaxonomyAction origin = TaxonomyAction.FETCH_TERM; // Used to track fetching newly uploaded XML-RPC terms

        public FetchTermResponsePayload(RequestPayload requestPayload, TermModel term, SiteModel site,
                                        TaxonomyError error) {
            super(requestPayload, term, site, error);
        }
    }

    // OnChanged events
    public static class OnTaxonomyChanged extends OnChanged<TaxonomyError> {
        public int rowsAffected;
        public String taxonomyName;
        public TaxonomyAction causeOfChange;

        public OnTaxonomyChanged(RequestPayload requestPayload, int rowsAffected, String taxonomyName) {
            super(requestPayload);
            this.rowsAffected = rowsAffected;
            this.taxonomyName = taxonomyName;
        }

        public OnTaxonomyChanged(RequestPayload requestPayload, int rowsAffected) {
            super(requestPayload);
            this.rowsAffected = rowsAffected;
        }
    }

    public static class OnTermUploaded extends OnChanged<TaxonomyError> {
        public TermModel term;

        public OnTermUploaded(RequestPayload requestPayload, TermModel term) {
            super(requestPayload);
            this.term = term;
        }
    }

    public static class TaxonomyError implements OnChangedError {
        public TaxonomyErrorType type;
        public String message;

        public TaxonomyError(TaxonomyErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        public TaxonomyError(@NonNull String type, @NonNull String message) {
            this.type = TaxonomyErrorType.fromString(type);
            this.message = message;
        }

        public TaxonomyError(TaxonomyErrorType type) {
            this(type, "");
        }
    }

    public enum TaxonomyErrorType {
        INVALID_TAXONOMY,
        DUPLICATE,
        UNAUTHORIZED,
        INVALID_RESPONSE,
        GENERIC_ERROR;

        public static TaxonomyErrorType fromString(String string) {
            if (string != null) {
                for (TaxonomyErrorType v : TaxonomyErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    private final TaxonomyRestClient mTaxonomyRestClient;
    private final TaxonomyXMLRPCClient mTaxonomyXMLRPCClient;

    @Inject
    public TaxonomyStore(Dispatcher dispatcher, TaxonomyRestClient taxonomyRestClient,
                         TaxonomyXMLRPCClient taxonomyXMLRPCClient) {
        super(dispatcher);
        mTaxonomyRestClient = taxonomyRestClient;
        mTaxonomyXMLRPCClient = taxonomyXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "TaxonomyStore onRegister");
    }

    public TermModel instantiateCategory(SiteModel site) {
        return instantiateTermModel(site, DEFAULT_TAXONOMY_CATEGORY);
    }

    public TermModel instantiateTag(SiteModel site) {
        return instantiateTermModel(site, DEFAULT_TAXONOMY_TAG);
    }

    public TermModel instantiateTerm(SiteModel site, TaxonomyModel taxonomy) {
        return instantiateTermModel(site, taxonomy.getName());
    }

    private TermModel instantiateTermModel(SiteModel site, String taxonomyName) {
        TermModel newTerm = new TermModel();
        newTerm.setLocalSiteId(site.getId());
        newTerm.setTaxonomy(taxonomyName);

        // Insert the term into the db, updating the object to include the local ID
        newTerm = TaxonomySqlUtils.insertTermForResult(newTerm);

        // id is set to -1 if insertion fails
        if (newTerm.getId() == -1) {
            return null;
        }
        return newTerm;
    }

    /**
     * Returns all categories for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getCategoriesForSite(SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns all tags for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getTagsForSite(SiteModel site) {
        return TaxonomySqlUtils.getTermsForSite(site, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns all the terms of a taxonomy for the given site as a {@link TermModel} list.
     */
    public List<TermModel> getTermsForSite(SiteModel site, String taxonomyName) {
        return TaxonomySqlUtils.getTermsForSite(site, taxonomyName);
    }

    /**
     * Returns a category as a {@link TermModel} given its remote id.
     */
    public TermModel getCategoryByRemoteId(SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns a tag as a {@link TermModel} given its remote id.
     */
    public TermModel getTagByRemoteId(SiteModel site, long remoteId) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns a term as a {@link TermModel} given its remote id.
     */
    public TermModel getTermByRemoteId(SiteModel site, long remoteId, String taxonomyName) {
        return TaxonomySqlUtils.getTermByRemoteId(site, remoteId, taxonomyName);
    }

    /**
     * Returns a category as a {@link TermModel} given its name.
     */
    public TermModel getCategoryByName(SiteModel site, String categoryName) {
        return TaxonomySqlUtils.getTermByName(site, categoryName, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns a tag as a {@link TermModel} given its name.
     */
    public TermModel getTagByName(SiteModel site, String tagName) {
        return TaxonomySqlUtils.getTermByName(site, tagName, DEFAULT_TAXONOMY_TAG);
    }

    /**
     * Returns a term as a {@link TermModel} given its name.
     */
    public TermModel getTermByName(SiteModel site, String termName, String taxonomyName) {
        return TaxonomySqlUtils.getTermByName(site, termName, taxonomyName);
    }

    /**
     * Returns all the categories for the given post as a {@link TermModel} list.
     */
    public List<TermModel> getCategoriesForPost(PostModel post, SiteModel site) {
        return TaxonomySqlUtils.getTermsFromRemoteIdList(post.getCategoryIdList(), site, DEFAULT_TAXONOMY_CATEGORY);
    }

    /**
     * Returns all the tags for the given post as a {@link TermModel} list.
     */
    public List<TermModel> getTagsForPost(PostModel post, SiteModel site) {
        return TaxonomySqlUtils.getTermsFromRemoteNameList(post.getTagNameList(), site, DEFAULT_TAXONOMY_TAG);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof TaxonomyAction)) {
            return;
        }

        switch ((TaxonomyAction) actionType) {
            case FETCH_CATEGORIES:
                fetchTerms((FetchCategoriesPayload) action.getPayload(),
                        ((FetchCategoriesPayload) action.getPayload()).site, DEFAULT_TAXONOMY_CATEGORY);
                break;
            case FETCH_TAGS:
                fetchTerms((FetchCategoriesPayload) action.getPayload(),
                        ((FetchCategoriesPayload) action.getPayload()).site, DEFAULT_TAXONOMY_TAG);
                break;
            case FETCH_TERMS:
                fetchTerms((FetchTermsPayload) action.getPayload());
                break;
            case FETCHED_TERMS:
                handleFetchTermsCompleted((FetchTermsResponsePayload) action.getPayload());
                break;
            case FETCH_TERM:
                fetchTerm((RemoteTermRequestPayload) action.getPayload());
                break;
            case FETCHED_TERM:
                handleFetchSingleTermCompleted((FetchTermResponsePayload) action.getPayload());
                break;
            case PUSH_TERM:
                pushTerm((RemoteTermRequestPayload) action.getPayload());
                break;
            case PUSHED_TERM:
                handlePushTermCompleted((RemoteTermResponsePayload) action.getPayload());
                break;
            case REMOVE_ALL_TERMS:
                removeAllTerms((RemoveAllTermsPayload) action.getPayload());
                break;
        }
    }

    private void fetchTerm(RemoteTermRequestPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.fetchTerm(payload, payload.term, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mTaxonomyXMLRPCClient.fetchTerm(payload, payload.term, payload.site);
        }
    }

    private void fetchTerms(RequestPayload payload, SiteModel site, String taxonomyName) {
        // TODO: Support large number of terms (currently pulling 100 from REST, and ? from XML-RPC) - pagination?
        if (site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.fetchTerms(payload, site, taxonomyName);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
             mTaxonomyXMLRPCClient.fetchTerms(payload, site, taxonomyName);
        }
    }

    private void fetchTerms(FetchTermsPayload payload) {
        fetchTerms(payload, payload.site, payload.taxonomy.getName());
    }

    private void handleFetchTermsCompleted(FetchTermsResponsePayload payload) {
        OnTaxonomyChanged onTaxonomyChanged;

        if (payload.isError()) {
            onTaxonomyChanged = new OnTaxonomyChanged(payload.getRequestPayload(), 0, payload.taxonomy);
            onTaxonomyChanged.error = payload.error;
        } else {
            // Clear existing terms for this taxonomy
            // This is the simplest way of keeping our local terms in sync with their remote versions
            // (in case of deletions,or if the user manually changed some term IDs)
            // TODO: This may have to change when we support large numbers of terms and require multiple requests
            TaxonomySqlUtils.clearTaxonomyForSite(payload.site, payload.taxonomy);

            int rowsAffected = 0;
            for (TermModel term : payload.terms.getTerms()) {
                rowsAffected += TaxonomySqlUtils.insertOrUpdateTerm(term);
            }

            onTaxonomyChanged = new OnTaxonomyChanged(payload.getRequestPayload(), rowsAffected, payload.taxonomy);
        }

        switch (payload.taxonomy) {
            case DEFAULT_TAXONOMY_CATEGORY:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_CATEGORIES;
                break;
            case DEFAULT_TAXONOMY_TAG:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_TAGS;
                break;
            default:
                onTaxonomyChanged.causeOfChange = TaxonomyAction.FETCH_TERMS;
        }

        emitChange(onTaxonomyChanged);
    }

    private void handleFetchSingleTermCompleted(FetchTermResponsePayload payload) {
        if (payload.origin == TaxonomyAction.PUSH_TERM) {
            OnTermUploaded onTermUploaded = new OnTermUploaded(payload.getRequestPayload(), payload.term);
            if (payload.isError()) {
                onTermUploaded.error = payload.error;
            } else {
                updateTerm(payload.getRequestPayload(), payload.term);
            }
            emitChange(onTermUploaded);
            return;
        }

        if (payload.isError()) {
            OnTaxonomyChanged event = new OnTaxonomyChanged(payload.getRequestPayload(), 0, payload.term.getTaxonomy());
            event.error = payload.error;
            event.causeOfChange = TaxonomyAction.UPDATE_TERM;
            emitChange(event);
        } else {
            updateTerm(payload.getRequestPayload(), payload.term);
        }
    }

    private void handlePushTermCompleted(RemoteTermResponsePayload payload) {
        if (payload.isError()) {
            OnTermUploaded onTermUploaded = new OnTermUploaded(payload.getRequestPayload(), payload.term);
            onTermUploaded.error = payload.error;
            emitChange(onTermUploaded);
        } else {
            if (payload.site.isUsingWpComRestApi()) {
                // The WP.COM REST API response contains the modified term, so we're already in sync with the server
                // All we need to do is store it and emit OnTaxonomyChanged
                updateTerm(payload.getRequestPayload(), payload.term);
                emitChange(new OnTermUploaded(payload.getRequestPayload(), payload.term));
            } else {
                // XML-RPC does not respond to new/edit term calls with the resulting term - request it from the server
                // This needs to complete for us to obtain the slug for a newly created term
                TaxonomySqlUtils.insertOrUpdateTerm(payload.term);
                mTaxonomyXMLRPCClient.fetchTerm(payload.getRequestPayload(), payload.term, payload.site,
                        TaxonomyAction.PUSH_TERM);
            }
        }
    }

    private void pushTerm(RemoteTermRequestPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mTaxonomyRestClient.pushTerm(payload, payload.term, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mTaxonomyXMLRPCClient.pushTerm(payload, payload.term, payload.site);
        }
    }

    private void updateTerm(RequestPayload requestPayload, TermModel term) {
        int rowsAffected = TaxonomySqlUtils.insertOrUpdateTerm(term);

        OnTaxonomyChanged onTaxonomyChanged = new OnTaxonomyChanged(requestPayload, rowsAffected, term.getTaxonomy());
        onTaxonomyChanged.causeOfChange = TaxonomyAction.UPDATE_TERM;
        emitChange(onTaxonomyChanged);
    }

    private void removeAllTerms(RemoveAllTermsPayload payload) {
        int rowsAffected = TaxonomySqlUtils.deleteAllTerms();

        OnTaxonomyChanged onTaxonomyChanged = new OnTaxonomyChanged(payload, rowsAffected);
        onTaxonomyChanged.causeOfChange = TaxonomyAction.REMOVE_ALL_TERMS;
        emitChange(onTaxonomyChanged);
    }
}
