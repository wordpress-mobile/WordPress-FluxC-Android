package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppConfigModule.class,
        ReleaseOkHttpClientModule.class,
        ReleaseNetworkModule.class
})
public interface AppComponent {
    void inject(InstafluxApp application);
    void inject(MainInstafluxActivity homeActivity);
    void inject(PostActivity postActivity);
}
