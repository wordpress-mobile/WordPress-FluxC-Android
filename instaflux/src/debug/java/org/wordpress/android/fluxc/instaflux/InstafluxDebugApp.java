package org.wordpress.android.fluxc.instaflux;

import com.facebook.flipper.android.AndroidFlipperClient;
import com.facebook.flipper.android.utils.FlipperUtils;
import com.facebook.flipper.core.FlipperClient;
import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin;
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin;
import com.facebook.soloader.SoLoader;

import org.wordpress.android.fluxc.module.AppContextModule;

public class InstafluxDebugApp extends InstafluxApp {
    @Override
    public void onCreate() {
        super.onCreate();
        initDaggerComponent();

        // Init Flipper
        if (FlipperUtils.shouldEnableFlipper(this)) {
            SoLoader.init(this, false);
            FlipperClient client = AndroidFlipperClient.getInstance(this);
            client.addPlugin(new InspectorFlipperPlugin(getApplicationContext(), DescriptorMapping.withDefaults()));
            client.addPlugin(new NetworkFlipperPlugin());
            client.addPlugin(new InspectorFlipperPlugin(this, DescriptorMapping.withDefaults()));
            client.start();
        }
    }

    protected void initDaggerComponent() {
        mComponent = DaggerAppComponentDebug.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
    }
}
