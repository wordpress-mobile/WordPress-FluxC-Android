package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.DebugOkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        DebugOkHttpClientModule.class,
        InterceptorModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class
})
public interface AppComponentDebug extends AppComponent {}
