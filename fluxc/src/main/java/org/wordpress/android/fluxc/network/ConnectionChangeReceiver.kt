package org.wordpress.android.fluxc.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global network connection change receiver - The implementing Application must register this receiver in either it's
 * Application class (if it wants to manage connectivity status globally, or inside the activity that wishes to
 * monitor connectivity.
 */
@Singleton
class ConnectionChangeReceiver @Inject constructor(val dispatcher: Dispatcher) : BroadcastReceiver() {
    companion object {
        private var isFirstReceive = true
        private var wasConnected = true
    }

    class OnNetworkStatusChanged(var isConnected: Boolean)

    /**
     * Listens for device connection changes.
     *
     * This method is called whenever something about the device connection has changed, not just
     * it's network connectivity. Only dispatch a [OnNetworkStatusChanged] event
     * when connection availability has changed.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val isConnected = NetworkUtils.isNetworkAvailable(context)
        if (isFirstReceive || isConnected != wasConnected) {
            AppLog.i(T.MAIN, "Connection Changed to $isConnected")
            wasConnected = isConnected
            isFirstReceive = false
            dispatcher.emitChange(OnNetworkStatusChanged(isConnected))
        }
    }
}
