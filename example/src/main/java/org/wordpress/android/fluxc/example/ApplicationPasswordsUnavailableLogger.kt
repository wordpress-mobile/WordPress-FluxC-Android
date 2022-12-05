package org.wordpress.android.fluxc.example

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsUnavailableListener
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsUnavailableLogger @Inject constructor(
    private val application: Application
) : ApplicationPasswordsUnavailableListener {
    private var activityReference = WeakReference<MainExampleActivity>(null)

    @Suppress("EmptyFunctionBlock")
    fun init() {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                (activity as? MainExampleActivity)?.let {
                    activityReference = WeakReference(it)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    override fun featureIsUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError) {
        activityReference.get()?.let {
            it.runOnUiThread {
                it.prependToLog(
                    "Application Passwords are not supported on site ${siteModel.url}\n" +
                        "Cause: ${networkError.errorCode} \n" +
                        "Status Code: ${networkError.volleyError?.networkResponse?.statusCode}"
                )
            }
        }
    }
}
