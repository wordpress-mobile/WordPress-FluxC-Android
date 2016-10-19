package org.wordpress.android.fluxc.example;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseStoreModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface AppComponent {
    void inject(ExampleApp object);
    void inject(MainExampleActivity object);
    void inject(SitesFragment object);
    void inject(MainFragment object);
    void inject(MediaFragment object);
    void inject(CommentsFragment object);
    void inject(PostsFragment object);
    void inject(AccountFragment object);
}
