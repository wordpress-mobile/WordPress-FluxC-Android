package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.BloggingRemindersMapper
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersPayload
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingreminders.BloggingRemindersRestClient.BloggingRemindersResponse
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingRemindersStore
@Inject constructor(
    private val bloggingRemindersDao: BloggingRemindersDao,
    private val mapper: BloggingRemindersMapper,
    private val restClient: BloggingRemindersRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    fun bloggingRemindersModel(siteId: Int): Flow<BloggingRemindersModel> {
        return bloggingRemindersDao.liveGetBySiteId(siteId).map {
            it?.let { dbModel -> mapper.toDomainModel(dbModel) } ?: BloggingRemindersModel(siteId)
        }
    }

    suspend fun fetchBloggingReminders(site: SiteModel): BloggingReminderSettingsResult<BloggingReminders> {
        return coroutineEngine.withDefaultContext(T.SETTINGS, this, "fetch blogging reminder settings") {
            val payload = restClient.fetchSettings(site)
            storeSettings(site, payload)
        }
    }

    suspend fun setBloggingReminders(
        site: SiteModel,
        reminders: BloggingRemindersModel
    ): BloggingReminderSettingsResult<BloggingReminders> {
        return coroutineEngine.withDefaultContext(T.SETTINGS, this, "fetch blogging reminder settings") {
            val payload = restClient.setBloggingReminders(site, mapper.toDatabaseModel(reminders))
            storeSettings(site, payload)
        }
    }

    suspend fun hasModifiedBloggingReminders(siteId: Int) =
            coroutineEngine.withDefaultContext(T.SETTINGS, this, "Has blogging reminders") {
                bloggingRemindersDao.getBySiteId(siteId).isNotEmpty()
            }


    suspend fun updateBloggingReminders(model: BloggingRemindersModel) =
            coroutineEngine.withDefaultContext(T.SETTINGS, this, "Updating blogging reminders") {
                bloggingRemindersDao.insert(mapper.toDatabaseModel(model))
            }

    private suspend fun storeSettings(
        site: SiteModel,
        payload: BloggingRemindersPayload<BloggingRemindersResponse>
    ): BloggingReminderSettingsResult<BloggingReminders> = when {
        payload.isError -> handlePayloadError(payload.error)
        payload.response != null -> handlePayloadResponse(site, payload.response)
        else -> BloggingReminderSettingsResult(BloggingRemindersError(INVALID_RESPONSE))
    }

    private fun handlePayloadError(
        error: BloggingRemindersError
    ): BloggingReminderSettingsResult<BloggingReminders> = when (error.type) {
        else -> BloggingReminderSettingsResult(error)
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private suspend fun handlePayloadResponse(
        site: SiteModel,
        response: BloggingRemindersResponse
    ): BloggingReminderSettingsResult<BloggingReminders> = try {
        bloggingRemindersDao.insert(response.toDatabaseModel(site.id))
        BloggingReminderSettingsResult()
    } catch (e: Exception) {
        BloggingReminderSettingsResult(BloggingRemindersError(GENERIC_ERROR))
    }

    data class BloggingReminderSettingsResult<T>(
        val model: T? = null
    ) : Store.OnChanged<BloggingRemindersError>() {
        constructor(error: BloggingRemindersError) : this() {
            this.error = error
        }
    }
}
