package org.wordpress.android.fluxc.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.AppLog.T.DB
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCInboxStore @Inject constructor(
    private val restClient: InboxRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val database: WCAndroidDatabase,
    private val inboxNotesDao: InboxNotesDao
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    suspend fun fetchInboxNotes(
        site: SiteModel,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupons") {
            val response = restClient.fetchInboxNotes(site, page, pageSize)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    saveInboxNotes(response.result, site)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeInboxNotes(site: SiteModel): Flow<List<InboxNoteWithActions>> =
        inboxNotesDao.observeInboxNotes(site.siteId)
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    private suspend fun saveInboxNotes(result: Array<InboxNoteDto>, site: SiteModel) {
        result.forEach { dto ->
            database.runInTransaction {
                coroutineEngine.launch(
                    DB,
                    this,
                    "fetchInboxNotes DB transaction"
                ) {
                    inboxNotesDao.insertOrUpdateInboxNote(dto.toDataModel(site.siteId))
                    saveInboxNoteActions(dto, site.id.toLong())
                }
            }
        }
    }

    private suspend fun saveInboxNoteActions(dto: InboxNoteDto, siteId: Long) {
        dto.actions.forEach { action ->
            inboxNotesDao.insertOrUpdateInboxNoteAction(
                action.toDataModel(dto.id, siteId)
            )
        }
    }
}
