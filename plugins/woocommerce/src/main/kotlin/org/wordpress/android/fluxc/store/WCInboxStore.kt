package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCInboxStore @Inject constructor(
    private val restClient: InboxRestClient,
    private val coroutineEngine: CoroutineEngine,
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
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.fetchInboxNotes(site, page, pageSize)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    saveInboxNotes(response.result, site.siteId)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    fun observeInboxNotes(siteId: Long): Flow<List<InboxNoteWithActions>> =
        inboxNotesDao.observeInboxNotes(siteId)
            .distinctUntilChanged()

    suspend fun markInboxNoteAsActioned(
        site: SiteModel,
        noteId: Long,
        actionId: Long
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.markInboxNoteAsActioned(site, noteId, actionId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    markNoteAsActionedLocally(site.siteId, response.result)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    suspend fun deleteNote(
        site: SiteModel,
        noteId: Long
    ): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            val response = restClient.deleteNote(site, noteId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    inboxNotesDao.deleteInboxNote(noteId, site.siteId)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    suspend fun deleteAllNotesForSite(site: SiteModel): WooResult<Unit> =
        coroutineEngine.withDefaultContext(API, this, "fetchInboxNotes") {
            inboxNotesDao.deleteInboxNotesForSite(site.siteId)
            val response = restClient.deleteAllNotesForSite(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(Unit)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    private suspend fun saveInboxNotes(result: Array<InboxNoteDto>, siteId: Long) {
        val notesWithActions = result.map { it.toInboxNoteWithActionsEntity(siteId) }
        inboxNotesDao.deleteAllAndInsertInboxNotes(siteId, *notesWithActions.toTypedArray())
    }

    private suspend fun markNoteAsActionedLocally(siteId: Long, updatedNote: InboxNoteDto) {
        val noteWithActionsEntity = updatedNote.toInboxNoteWithActionsEntity(siteId)
        inboxNotesDao.updateNote(siteId, noteWithActionsEntity)
    }
}
