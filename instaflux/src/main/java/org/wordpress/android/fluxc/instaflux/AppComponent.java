package org.wordpress.android.fluxc.instaflux;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseOkHttpClientModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class
})
public interface AppComponent {
    void inject(InstafluxApp application);
    void inject(MainInstafluxActivity homeActivity);
    void inject(PostActivity postActivity);
}
