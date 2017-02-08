package org.wordpress.android.fluxc.example;

import com.facebook.stetho.Stetho;

import org.wordpress.android.fluxc.module.AppContextModule;

public class ExampleDebugApp extends ExampleApp {
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
