package org.wordpress.android.fluxc.instaflux;

import android.app.Application;

import org.wordpress.android.fluxc.module.AppContextModule;

public class InstafluxApp extends Application {
    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerAppComponent.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
        component().inject(this);
    }

    public AppComponent component() {
        return component;
    }
}
