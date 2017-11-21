package org.wordpress.android.fluxc.instaflux;

import com.facebook.stetho.Stetho;

import org.wordpress.android.fluxc.module.AppContextModule;

public class InstafluxDebugApp extends InstafluxApp {
    @Override
    public void onCreate() {
        super.onCreate();
        initDaggerComponent();
        Stetho.initializeWithDefaults(this);
    }

    protected void initDaggerComponent() {
        mComponent = DaggerAppComponentDebug.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
    }
}
