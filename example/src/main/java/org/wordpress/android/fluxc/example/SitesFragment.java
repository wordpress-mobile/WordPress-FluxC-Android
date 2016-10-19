package org.wordpress.android.fluxc.example;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;

public class SitesFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    private Button mLogSites;
    private Button mUpdateFirstSite;
    private Button mNewSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sites, container, false);

        mUpdateFirstSite = (Button) view.findViewById(R.id.update_first_site);
        mUpdateFirstSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Fetch site
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
                // Fetch site's post formats
                mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(site));
            }
        });


        mLogSites = (Button) view.findViewById(R.id.log_sites);
        mLogSites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SiteModel site : mSiteStore.getSites()) {
                    AppLog.i(T.API, LogUtils.toString(site));
                }
            }
        });
        mNewSite = (Button) view.findViewById(R.id.new_site);
        mNewSite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewSiteDialog();
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

    private void newSiteAction(String name, String title) {
        // Default language "en" (english)
        NewSitePayload newSitePayload = new NewSitePayload(name, title, "en", SiteVisibility.PUBLIC, true);
        mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload));
    }

    private void showNewSiteDialog() {
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String name, String title, String unused) {
                newSiteAction(name, title);
            }
        }, "Site Name", "Site Title", "Unused");
        newFragment.show(ft, "dialog");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            AppLog.e(T.TESTS, "SiteChanged error: " + event.error.type);
            return;
        }
        if (mSiteStore.hasSite()) {
            mUpdateFirstSite.setEnabled(true);
        } else {
            mUpdateFirstSite.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError()) {
            prependToLog("New site " + message + ": error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("New site " + message + ": success!");
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
