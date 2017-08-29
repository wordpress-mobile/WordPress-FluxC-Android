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


        return view;
    }
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        prependToLog("onThemesChanged: ");
        if (event.isError()) {
            prependToLog("error: " + event.error.message);
        } else {
            prependToLog("success: WP.com theme count = " + mThemeStore.getWpThemes().size());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
    }

    private void prependToLog(final String s) {
        ((MainExampleActivity) getActivity()).prependToLog(s);
    }
}
