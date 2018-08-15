package org.wordpress.android.fluxc.example

import android.app.Activity
import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import com.yarolegovich.wellsql.WellSql
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import org.wordpress.android.fluxc.example.di.AppComponent
import org.wordpress.android.fluxc.example.di.DaggerAppComponent
import org.wordpress.android.fluxc.network.ConnectionChangeReceiver
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

open class ExampleApp : Application(), HasActivityInjector {
    companion object {
        var sAppIsInTheBackground = true
    }
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var connectionReceiver: ConnectionChangeReceiver

    protected open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        val wellSqlConfig = WellSqlConfig(applicationContext, WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(wellSqlConfig)

        val lifecycleMonitor = ApplicationLifecycleMonitor(connectionReceiver)
        registerActivityLifecycleCallbacks(lifecycleMonitor)
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    /**
     * Monitors the lifecycle of this application.
     *
     * The two methods ([startActivityTransitionTimer] and [stopActivityTransitionTimer])
     * are used to track when the app goes to background.
     *
     * Our implementation uses [onActivityPaused] and [onActivityResumed] of this [ApplicationLifecycleMonitor]
     * to start and stop the timer that detects when the app goes to background.
     *
     * So when the user is simply navigating between the activities, the [onActivityPaused]
     * calls [startActivityTransitionTimer] and starts the timer, but almost immediately the new activity being
     * entered, the [ApplicationLifecycleMonitor] cancels the timer in its [onActivityResumed] method, that in order
     * calls [stopActivityTransitionTimer] and so mIsInBackground would be false.
     *
     * In the case the app is sent to background, the [TimerTask] is instead executed, and the code that handles all
     * the background logic is run.
     */
    inner class ApplicationLifecycleMonitor(
        val connectionReceiver: ConnectionChangeReceiver
    ) : Application.ActivityLifecycleCallbacks {
        private val MAX_ACTIVITY_TRANSITION_TIME_MS: Long = 2000
        private var connectionReceiverRegistered = false
        private var activityTransitionTimer: Timer? = null
        private var activityTransitionTimerTask: TimerTask? = null

        private fun onAppGoesToBackground() {
            AppLog.i(T.MAIN, "App goes to background")

            // Methods onAppComesFromBackground / onAppGoesToBackground are only workarounds to track when the
            // app goes to or comes from background, but they are not 100% reliable, we should avoid unregistering
            // the receiver twice.
            if (connectionReceiverRegistered) {
                connectionReceiverRegistered = false
                unregisterReceiver(connectionReceiver)
            }
        }

        private fun onAppComesFromBackground() {
            AppLog.i(T.MAIN, "App goes to foreground")
            if (!connectionReceiverRegistered) {
                connectionReceiverRegistered = true
                registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }

        private fun startActivityTransitionTimer() {
            this.activityTransitionTimer = Timer()
            this.activityTransitionTimerTask = object : TimerTask() {
                override fun run() {
                    onAppGoesToBackground()
                }
            }

            this.activityTransitionTimer?.schedule(activityTransitionTimerTask, MAX_ACTIVITY_TRANSITION_TIME_MS)
        }

        private fun stopActivityTransitionTimer() {
            this.activityTransitionTimerTask?.cancel()
            this.activityTransitionTimer?.cancel()

            sAppIsInTheBackground = false
        }

        override fun onActivityPaused(activity: Activity?) {
            startActivityTransitionTimer()
        }

        override fun onActivityResumed(activity: Activity?) {
            if (sAppIsInTheBackground) {
                onAppComesFromBackground()
            }
            stopActivityTransitionTimer()
            sAppIsInTheBackground = false
        }

        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivityDestroyed(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityStopped(activity: Activity?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    }
}
