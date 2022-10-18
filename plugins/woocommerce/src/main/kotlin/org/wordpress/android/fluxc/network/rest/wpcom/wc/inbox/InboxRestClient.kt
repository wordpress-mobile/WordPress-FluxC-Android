package org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class InboxRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchInboxNotes(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        inboxNoteTypes: Array<String>
    ): WooPayload<Array<InboxNoteDto>> {
        val url = WOOCOMMERCE.admin.notes.pathV4Analytics

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString(),
                "type" to inboxNoteTypes.joinToString(separator = ",")
            ),
            Array<InboxNoteDto>::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun markInboxNoteAsActioned(
        site: SiteModel,
        inboxNoteId: Long,
        inboxNoteActionId: Long
    ): WooPayload<InboxNoteDto> {
        val url = WOOCOMMERCE.admin.notes.note(inboxNoteId)
            .action.item(inboxNoteActionId).pathV4Analytics

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
            this,
            site,
            url,
            emptyMap(),
            InboxNoteDto::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun deleteNote(
        site: SiteModel,
        inboxNoteId: Long
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.admin.notes.delete.note(inboxNoteId).pathV4Analytics

        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            this,
            site,
            url,
            Unit::class.java
        )
        return when (response) {
            is JetpackError -> WooPayload(response.error.toWooError())
            is JetpackSuccess -> WooPayload(Unit)
        }
    }

    suspend fun deleteAllNotesForSite(
        site: SiteModel,
        page: Int,
        pageSize: Int,
        inboxNoteTypes: Array<String>
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.admin.notes.delete.all.pathV4Analytics

        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
            this,
            site,
            url,
            Array<InboxNoteDto>::class.java,
            mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString(),
                "type" to inboxNoteTypes.joinToString(separator = ",")
            )
        )
        return when (response) {
            is JetpackError -> WooPayload(response.error.toWooError())
            is JetpackSuccess -> WooPayload(Unit)
        }
    }
}
