package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.DatabaseModule;
import org.wordpress.android.fluxc.module.OkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        OkHttpClientModule.class,
        InterceptorModule.class,
        ReleaseNetworkModule.class
})
public interface AppComponentDebug extends AppComponent {}
