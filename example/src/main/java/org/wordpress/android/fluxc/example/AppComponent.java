package org.wordpress.android.fluxc.example;

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
    void inject(ExampleApp object);
    void inject(MainExampleActivity object);
    void inject(SitesFragment object);
    void inject(MainFragment object);
    void inject(MediaFragment object);
    void inject(CommentsFragment object);
    void inject(PostsFragment object);
    void inject(AccountFragment object);
    void inject(SignedOutActionsFragment object);
    void inject(TaxonomiesFragment object);
    void inject(UploadsFragment object);
}
