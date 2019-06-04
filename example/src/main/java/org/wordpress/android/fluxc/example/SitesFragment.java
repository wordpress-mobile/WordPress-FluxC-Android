package org.wordpress.android.fluxc.example;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnProfileFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteDeleted;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteExported;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class SitesFragment extends Fragment {
    @Inject SiteStore mSiteStore;
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
        View view = inflater.inflate(R.layout.fragment_sites, container, false);

        view.findViewById(R.id.fetch_profile).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Fetch site
                mDispatcher.dispatch(SiteActionBuilder.newFetchProfileXmlRpcAction(site));
            }
        });

        view.findViewById(R.id.new_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewSiteDialog();
            }
        });

        view.findViewById(R.id.update_first_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Fetch site
                mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
                // Fetch site's post formats
                mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(site));
            }
        });

        view.findViewById(R.id.update_all_sites).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SiteModel site : mSiteStore.getSites()) {
                    mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
                }
            }
        });

        view.findViewById(R.id.log_sites).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (SiteModel site : mSiteStore.getSites()) {
                    prependToLog(site.getName());
                    AppLog.i(T.API, LogUtils.toString(site));
                }
            }
        });

        view.findViewById(R.id.delete_first_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Delete site
                mDispatcher.dispatch(SiteActionBuilder.newDeleteSiteAction(site));
            }
        });

        view.findViewById(R.id.export_first_site).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Export site
                mDispatcher.dispatch(SiteActionBuilder.newExportSiteAction(site));
            }
        });

        view.findViewById(R.id.fetch_plans).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel site = mSiteStore.getSites().get(0);
                // Fetch site plans
                mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(site));
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
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new Listener() {
            @Override
            public void onClick(String name, String title, String unused) {
                newSiteAction(name, title);
            }
        }, "Site Name", "Site Title", "Unused");
        newFragment.show(ft, "dialog");
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProfileFetched(OnProfileFetched event) {
        if (event.isError()) {
            prependToLog("onProfileFetched error: " + event.error.type);
            AppLog.e(T.TESTS, "onProfileFetched error: " + event.error.type);
        } else {
            prependToLog("onProfileFetched: email = " + event.site.getEmail());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            prependToLog("SiteChanged error: " + event.error.type);
            AppLog.e(T.TESTS, "SiteChanged error: " + event.error.type);
        } else {
            prependToLog("SiteChanged: rowsAffected = " + event.rowsAffected);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        String message = event.dryRun ? "validated" : "created";
        if (event.isError()) {
            prependToLog("New site " + message + ": error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("New site " + message + ": success!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteDeleted(OnSiteDeleted event) {
        if (event.isError()) {
            prependToLog("Delete Site: error: " + event.error.type + " - " + event.error.message);
        } else {
            prependToLog("Delete Site: success!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteExported(OnSiteExported event) {
        if (event.isError()) {
            prependToLog("Export Site: error: " + event.error.type);
        } else {
            prependToLog("Export Site: success!");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlansFetched(OnPlansFetched event) {
        if (event.isError()) {
            prependToLog("Fetch Plans: error: " + event.error.type);
        } else {
            prependToLog("Fetch Plans: success, " + event.plans.size() + " Plans fetched");
        }
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
