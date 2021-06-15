package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.DatabaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.OkHttpClientModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        OkHttpClientModule.class,
        ReleaseNetworkModule.class,
        DatabaseModule.class
})
public interface AppComponent {
    void inject(InstafluxApp application);
    void inject(MainInstafluxActivity homeActivity);
    void inject(PostActivity postActivity);
}
