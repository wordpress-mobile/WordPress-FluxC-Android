package org.wordpress.android.fluxc.example;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.SiteStore;

import javax.inject.Inject;

public class ThemeFragment extends Fragment {
    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getActivity().getApplication()).component().inject(this);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_themes, container, false);

        view.findViewById(R.id.fetch_wpcom_themes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispatcher.dispatch(ThemeActionBuilder.newFetchWpThemesAction());
            }
        });

        view.findViewById(R.id.fetch_installed_themes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel jetpackSite = getJetpackConnectedSite();
                if (jetpackSite == null) {
                    prependToLog("No Jetpack connected site found, unable to test.");
                } else {
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(jetpackSite));
                }
            }
        });

        view.findViewById(R.id.fetch_current_theme_jp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel jetpackSite = getJetpackConnectedSite();
                if (jetpackSite == null) {
                    prependToLog("No Jetpack connected site found, unable to test.");
                } else {
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(jetpackSite));
                }
            }
        });

        view.findViewById(R.id.fetch_current_theme_wpcom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SiteModel wpcomSite = getWPComSite();
                if (wpcomSite == null) {
                    prependToLog("No WP.com site found, unable to test.");
                } else {
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(wpcomSite));
                }
            }
        });

        return view;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemesFetched(ThemeStore.OnCurrentThemeFetched event) {
        prependToLog("onCurrentThemesFetched: ");
        if (event.isError()) {
            prependToLog("error: " + event.error.message);
        } else {
            prependToLog("success: theme = " + event.theme.getName());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        prependToLog("onThemesChanged: ");
        if (event.isError()) {
            prependToLog("error: " + event.error.message);
        } else {
            prependToLog("success: WP.com theme count = " + mThemeStore.getWpThemes().size());
            SiteModel jpSite = getJetpackConnectedSite();
            if (jpSite != null) {
                prependToLog("Installed theme count = " + mThemeStore.getThemesForSite(jpSite).size());
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
    }

    private SiteModel getWPComSite() {
        for (SiteModel site : mSiteStore.getSites()) {
            if (site != null && site.isWPCom()) {
                return site;
            }
        }
        return null;
    }

    private SiteModel getJetpackConnectedSite() {
        for (SiteModel site : mSiteStore.getSites()) {
            if (site != null && site.isJetpackConnected()) {
                return site;
            }
        }
        return null;
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
