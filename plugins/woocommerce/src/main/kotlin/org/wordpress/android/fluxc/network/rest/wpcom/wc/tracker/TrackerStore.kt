package org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.STATS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerStore @Inject internal constructor(
    private val restClient: TrackerRestClient,
    private val appLogWrapper: AppLogWrapper
) {
    private companion object {
        val ISO8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
    }

    suspend fun sendTelemetry(
        appVersion: String,
        site: SiteModel,
        installationDate: Date? = null
    ): WooPayload<Unit> {
        val formattedInstallationDate = installationDate?.let { ISO8601_FORMAT.format(it) }
        appLogWrapper.d(
            STATS,
            "Sending Telemetry. Values: appVersion=$appVersion, site=${site.id}, " +
                    "installationDate=$formattedInstallationDate"
        )
        return restClient.sendTelemetry(appVersion, site, formattedInstallationDate)
    }
}
