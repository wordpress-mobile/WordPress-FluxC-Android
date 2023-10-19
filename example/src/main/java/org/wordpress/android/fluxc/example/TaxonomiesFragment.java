package org.wordpress.android.fluxc.example;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TaxonomiesFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_taxonomies, container, false);
        view.findViewById(R.id.fetch_categories).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchCategoriesFirstSite();
            }
        });
        view.findViewById(R.id.fetch_tags).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchTagsFirstSite();
            }
        });
        view.findViewById(R.id.create_category).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createCategory();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    private SiteModel getFirstSite() {
        return mSiteStore.getSites().get(0);
    }

    private void fetchCategoriesFirstSite() {
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(getFirstSite()));
    }

    private void fetchTagsFirstSite() {
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(getFirstSite()));
    }

    private void createCategory() {
        TermModel newCategory = new TermModel(
                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                "FluxC-Category-" + new Random().nextLong(),
                0
        );
        RemoteTermPayload payload = new RemoteTermPayload(newCategory, getFirstSite());
        mDispatcher.dispatch(TaxonomyActionBuilder.newPushTermAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(@NonNull OnTaxonomyChanged event) {
        AppLog.i(T.API, "OnTaxonomyChanged: rowsAffected=" + event.rowsAffected);
        if (event.isError()) {
            String error = "Error: " + event.error.type + " - " + event.error.message;
            prependToLog(error);
            AppLog.i(T.TESTS, error);
        } else {
            if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES) {
                List<TermModel> terms = mTaxonomyStore.getCategoriesForSite(getFirstSite());
                for (TermModel term : terms) {
                    prependToLog("category" + " " + term.getRemoteTermId() + ": " + term.getName());
                }
            } else if (event.causeOfChange == TaxonomyAction.FETCH_TAGS) {
                List<TermModel> terms = mTaxonomyStore.getTagsForSite(getFirstSite());
                for (TermModel term : terms) {
                    prependToLog("tag" + " " + term.getRemoteTermId() + ": " + term.getName());
                }
            } else {
                prependToLog(event.causeOfChange + " " + event.rowsAffected);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTermUploaded(@NonNull OnTermUploaded event) {
        prependToLog("Term uploaded! Remote category id: " + event.term.getRemoteTermId());
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
