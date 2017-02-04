package org.wordpress.android.fluxc.example;

import android.app.Application;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

public class ExampleApp extends Application {
    protected AppComponent mComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        initDaggerComponent();
        component().inject(this);
        WellSql.init(new WellSqlConfig(getApplicationContext()));
    }

    public AppComponent component() {
        return mComponent;
    }

    protected void initDaggerComponent() {
        mComponent = DaggerAppComponent.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
    }
}
