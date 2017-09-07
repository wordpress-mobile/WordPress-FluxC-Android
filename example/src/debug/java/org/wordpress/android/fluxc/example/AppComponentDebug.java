package org.wordpress.android.fluxc.example;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        DebugOkHttpClientModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class
})
public interface AppComponentDebug extends AppComponent {
}
