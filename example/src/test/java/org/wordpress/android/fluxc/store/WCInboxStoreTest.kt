package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteActionDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxNoteDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox.InboxRestClient
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.entity.InboxNoteWithActions
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WCInboxStore.Companion.DEFAULT_PAGE_SIZE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

class WCInboxStoreTest {
    private val restClient: InboxRestClient = mock()
    private val inboxNotesDao: InboxNotesDao = mock()

    private val sut = WCInboxStore(
        restClient,
        initCoroutineEngine(),
        inboxNotesDao
    )

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then save them into DB`() =
        test {
            givenNotesAreFetchedSuccesfully()

            sut.fetchInboxNotes(ANY_SITE)

            verify(inboxNotesDao).insertInboxNotesAndActions(
                ANY_SITE.siteId, *INBOX_NOTES_WITH_ACTIONS_ENTITY.toTypedArray()
            )
        }

    @Test
    fun `Given notes are fetched successfully, when notes fetched, then return WooResult`() =
        test {
            givenNotesAreFetchedSuccesfully()

            val result = sut.fetchInboxNotes(ANY_SITE)

            Assertions.assertThat(result).isEqualTo(WooResult(Unit))
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then notes are not saved`() =
        test {
            givenErrorFetchingNotes()

            sut.fetchInboxNotes(ANY_SITE)

            verify(inboxNotesDao, never()).insertOrUpdateInboxNote(any())
        }

    @Test
    fun `Given notes fetching error, when notes fetched, then returns WooResult error`() =
        test {
            givenErrorFetchingNotes()

            val result = sut.fetchInboxNotes(ANY_SITE)

            Assertions.assertThat(result).isEqualTo(ANY_WOO_RESULT_WITH_ERROR)
        }

    private suspend fun givenNotesAreFetchedSuccesfully() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(WooPayload(arrayOf(ANY_INBOX_NOTE_DTO)))
    }

    private suspend fun givenErrorFetchingNotes() {
        whenever(
            restClient.fetchInboxNotes(
                ANY_SITE,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(WooPayload(ANY_WOO_API_ERROR))
    }

    private companion object {
        val ANY_WOO_API_ERROR = WooError(GENERIC_ERROR, UNKNOWN)
        val ANY_WOO_RESULT_WITH_ERROR: WooResult<Unit> = WooResult(ANY_WOO_API_ERROR)
        val ANY_INBOX_NOTE_ACTION_DTO = InboxNoteActionDto(
            id = 2,
            name = "action",
            label = "action",
            query = "",
            status = null,
            primary = true,
            actionedText = "",
            nonceAction = "",
            nonceName = "",
            url = "www.automattic.com"
        )
        val ANY_INBOX_NOTE_DTO = InboxNoteDto(
            id = 1,
            name = "",
            type = "",
            status = "",
            source = "",
            actions = listOf(ANY_INBOX_NOTE_ACTION_DTO),
            locale = "",
            title = "",
            content = "",
            layout = "",
            dateCreated = "",
            dateReminder = ""
        )
        val ANY_SITE = SiteModel().apply { siteId = 1 }
        val INBOX_NOTES_WITH_ACTIONS_ENTITY = listOf(
            InboxNoteWithActions(
                inboxNote = ANY_INBOX_NOTE_DTO.toDataModel(ANY_SITE.siteId),
                noteActions = listOf(
                    ANY_INBOX_NOTE_ACTION_DTO.toDataModel(
                        ANY_SITE.siteId
                    )
                )
            )
        )
    }
}
