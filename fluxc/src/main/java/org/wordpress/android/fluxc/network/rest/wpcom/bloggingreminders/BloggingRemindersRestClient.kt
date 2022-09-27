package org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BloggingRemindersRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchSettings(site: SiteModel): BloggingRemindersPayload<BloggingRemindersResponse> {
        val url = WPCOMV2.sites.site(site.siteId).blogging_prompts.settings.url
        val response = wpComGsonRequestBuilder.syncGetRequest(
            this,
            url,
            mapOf(),
            BloggingRemindersResponse::class.java
        )
        return when (response) {
            is Success -> BloggingRemindersPayload(response.data)
            is Error -> BloggingRemindersPayload(response.error.toBloggingPromptsError())
        }
    }

    suspend fun setBloggingReminders(
        site: SiteModel,
        reminders: BloggingReminders
    ): BloggingRemindersPayload<BloggingRemindersResponse> {
        val url = WPCOMV2.sites.site(site.siteId).blogging_prompts.settings.url

        val hour = reminders.hour.toString()
        val minute = if (reminders.minute == 0) {
            "00"
        } else {
            reminders.minute.toString()
        }

        val params = mapOf(
            "prompts_reminders_opted_in" to reminders.isPromptRemindersOptedIn,
            "reminders_time" to "${hour}.${minute}",
            "reminders_days" to mapOf(
                "monday" to reminders.monday,
                "tuesday" to reminders.tuesday,
                "wednesday" to reminders.wednesday,
                "thursday" to reminders.thursday,
                "friday" to reminders.friday,
                "saturday" to reminders.saturday,
                "sunday" to reminders.sunday,
            )
        )

        val response = wpComGsonRequestBuilder.syncPostRequest(
            this,
            url,
            null,
            params,
            SetBloggingRemindersResponse::class.java
        )
        return when (response) {
            is Success -> BloggingRemindersPayload(response.data.content)
            is Error -> BloggingRemindersPayload(response.error.toBloggingPromptsError())
        }
    }

    data class SetBloggingRemindersResponse(
        @SerializedName("updated") val content: BloggingRemindersResponse
    ) {
        fun toDatabaseModel(localSiteId: Int) = content.toDatabaseModel(localSiteId)
    }

    data class BloggingRemindersResponse(
        @SerializedName("prompts_card_opted_in") val promptsCardOptedIn: Boolean,
        @SerializedName("prompts_reminders_opted_in") val promptsRemindersOptedIn: Boolean,
        @SerializedName("is_potential_blogging_site") val potentialBloggingSite: Boolean,
        @SerializedName("reminders_days") val reminderDays: Map<String, Boolean>,
        @SerializedName("reminders_time") val remindersTime: String
    ) {
        fun toDatabaseModel(localSiteId: Int): BloggingReminders {
            val hoursAndMinutes = remindersTime.removeSuffix(" UTC").split(".")
            val hour = hoursAndMinutes[0].toInt()
            val minute = hoursAndMinutes[1].toInt()

            return BloggingReminders(
                localSiteId = localSiteId,
                isPromptRemindersOptedIn = promptsRemindersOptedIn,
                monday = reminderDays["monday"] ?: false,
                tuesday = reminderDays["tuesday"] ?: false,
                wednesday = reminderDays["wednesday"] ?: false,
                thursday = reminderDays["thursday"] ?: false,
                friday = reminderDays["friday"] ?: false,
                saturday = reminderDays["saturday"] ?: false,
                sunday = reminderDays["sunday"] ?: false,
                hour = hour,
                minute = minute
            )
        }
    }

    data class BloggingRemindersPayload<T>(
        val response: T? = null
    ) : Payload<BloggingRemindersError>() {
        constructor(error: BloggingRemindersError) : this() {
            this.error = error
        }
    }

    class BloggingRemindersError(
        val type: BloggingRemindersErrorType,
        val message: String? = null
    ) : OnChangedError

    enum class BloggingRemindersErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        API_ERROR,
        TIMEOUT
    }

    private fun WPComGsonNetworkError.toBloggingPromptsError(): BloggingRemindersError {
        val type = when (type) {
            GenericErrorType.TIMEOUT -> BloggingRemindersErrorType.TIMEOUT
            GenericErrorType.NO_CONNECTION,
            GenericErrorType.SERVER_ERROR,
            GenericErrorType.INVALID_SSL_CERTIFICATE,
            GenericErrorType.NETWORK_ERROR -> BloggingRemindersErrorType.API_ERROR
            GenericErrorType.PARSE_ERROR,
            GenericErrorType.NOT_FOUND,
            GenericErrorType.CENSORED,
            GenericErrorType.INVALID_RESPONSE -> BloggingRemindersErrorType.INVALID_RESPONSE
            GenericErrorType.HTTP_AUTH_ERROR,
            GenericErrorType.AUTHORIZATION_REQUIRED,
            GenericErrorType.NOT_AUTHENTICATED -> BloggingRemindersErrorType.AUTHORIZATION_REQUIRED
            GenericErrorType.UNKNOWN,
            null -> BloggingRemindersErrorType.GENERIC_ERROR
        }
        return BloggingRemindersError(type, message)
    }
}
