package org.wordpress.android.fluxc.instaflux;

import android.app.Application;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

public class InstafluxApp extends Application {
    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerAppComponent.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
        component().inject(this);
        WellSql.init(new WellSqlConfig(getApplicationContext()));
    }

    public AppComponent component() {
        return component;
    }
}
