package org.wordpress.android.fluxc.instaflux;

import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor;
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import okhttp3.Interceptor;

@Module
public class InterceptorModule {
    @Provides @IntoSet @Named("network-interceptors")
    public Interceptor provideFlipperInterceptor() {
        FlipperClient client = AndroidFlipperClient.getInstanceIfInitialized();
        NetworkFlipperPlugin plugin = null;
        if (client != null) {
            plugin = client.getPlugin(NetworkFlipperPlugin.ID);
        }
        return new FlipperOkhttpInterceptor(plugin);
    }
}
