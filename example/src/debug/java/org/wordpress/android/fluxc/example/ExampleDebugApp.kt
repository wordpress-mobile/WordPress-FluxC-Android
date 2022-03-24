package org.wordpress.android.fluxc.example

import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.soloader.SoLoader
import org.wordpress.android.fluxc.example.di.AppComponent
import org.wordpress.android.fluxc.example.di.DaggerAppComponentDebug
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS

class ExampleDebugApp : ExampleApp() {
    override val component: AppComponent by lazy {
        DaggerAppComponentDebug.builder()
            .application(this)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)

        if (FlipperUtils.shouldEnableFlipper(this)) {
            runCatching { // Needs runCatching to avoid crashes in robolectric tests
                SoLoader.init(this, false)
                AndroidFlipperClient.getInstance(this).apply {
                    addPlugin(InspectorFlipperPlugin(applicationContext, DescriptorMapping.withDefaults()))
                    addPlugin(NetworkFlipperPlugin())
                }.start()
            }.also {
                AppLog.w(UTILS, "Failed to initialise Flipper. Probably due to trying to " +
                    "initialising Flipper during robolectric tests execution")
            }
        }
    }
}
