package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WhatsNewAction
import org.wordpress.android.fluxc.action.WhatsNewAction.FETCH_CACHED_ANNOUNCEMENT
import org.wordpress.android.fluxc.action.WhatsNewAction.FETCH_REMOTE_ANNOUNCEMENT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient
import org.wordpress.android.fluxc.persistence.WhatsNewSqlUtils
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsNewStore @Inject constructor(
    private val whatsNewRestClient: WhatsNewRestClient,
    private val whatsNewSqlUtils: WhatsNewSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WhatsNewAction ?: return
        when (actionType) {
            FETCH_REMOTE_ANNOUNCEMENT -> {
                val versionName = (action.payload as WhatsNewFetchPayload).versionName
                val appId = (action.payload as WhatsNewFetchPayload).appId
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_REMOTE_ANNOUNCEMENT") {
                    emitChange(fetchRemoteAnnouncements(versionName, appId))
                }
            }
            FETCH_CACHED_ANNOUNCEMENT -> {
                coroutineEngine.launch(AppLog.T.API, this, "FETCH_CACHED_ANNOUNCEMENT") {
                    emitChange(fetchCachedAnnouncements())
                }
            }
        }
    }

    suspend fun fetchCachedAnnouncements() =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                return@withDefaultContext OnWhatsNewFetched(whatsNewSqlUtils.getAnnouncements(), true)
            }

    suspend fun fetchRemoteAnnouncements(versionName: String, appId: WhatsNewAppId) =
            coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
                val fetchedWhatsNewPayload = whatsNewRestClient.fetchWhatsNew(versionName, appId)

                return@withDefaultContext if (!fetchedWhatsNewPayload.isError) {
                    val fetchedAnnouncements = fetchedWhatsNewPayload.whatsNewItems
                    whatsNewSqlUtils.updateAnnouncementCache(fetchedAnnouncements)
                    OnWhatsNewFetched(fetchedAnnouncements)
                } else {
                    OnWhatsNewFetched(
                            fetchError = WhatsNewFetchError(GENERIC_ERROR, fetchedWhatsNewPayload.error.message)
                    )
                }
            }

    override fun onRegister() {
        AppLog.d(API, WhatsNewStore::class.java.simpleName + " onRegister")
    }

    class WhatsNewFetchPayload(
        val versionName: String,
        val appId: WhatsNewAppId
    ) : Payload<BaseNetworkError>()

    class WhatsNewFetchedPayload(
        val whatsNewItems: List<WhatsNewAnnouncementModel>? = null
    ) : Payload<BaseNetworkError>()

    data class OnWhatsNewFetched(
        val whatsNewItems: List<WhatsNewAnnouncementModel>? = null,
        val isFromCache: Boolean = false,
        val fetchError: WhatsNewFetchError? = null
    ) : Store.OnChanged<WhatsNewFetchError>() {
        init {
            // we allow setting error from constructor, so it will be a part of data class
            // and used during comparison, so we can test error events
            this.error = fetchError
        }
    }

    data class WhatsNewFetchError(
        val type: WhatsNewErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class WhatsNewErrorType {
        GENERIC_ERROR
    }

    enum class WhatsNewAppId(val id: Int) {
        WP_ANDROID(1),
        WOO_ANDROID(3),
        JP_ANDROID(5)
    }
}
