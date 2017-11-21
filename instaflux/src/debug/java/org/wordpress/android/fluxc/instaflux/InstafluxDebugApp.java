package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;

public class InstafluxDebugApp extends InstafluxApp {
    @Override
    public void onCreate() {
        super.onCreate();
        initDaggerComponent();
    }

    protected void initDaggerComponent() {
        mComponent = DaggerAppComponentDebug.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
    }
}
