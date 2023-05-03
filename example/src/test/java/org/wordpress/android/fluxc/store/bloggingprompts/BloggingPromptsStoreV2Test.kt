package org.wordpress.android.fluxc.store.bloggingprompts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsError
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsPayload
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponseV2
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsListResponseV2
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsRespondentAvatar
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val SITE_LOCAL_ID = 1
private const val ANSWERED_LINK_PREFIX = "https://wordpress.com/tag/dailyprompt-"

private val PROMPTS_RESPONSE: BloggingPromptsListResponseV2 = BloggingPromptsListResponseV2(
    prompts = listOf(
        BloggingPromptResponseV2(
            id = 1,
            title = "Title 1",
            content = "Content 1",
            text = "Cast the movie of your life.",
            date = "2015-01-12",
            attribution = "",
            isAnswered = false,
            respondentsCount = 0,
            respondentsAvatars = emptyList(),
        ),

        BloggingPromptResponseV2(
            id = 2,
            title = "Title 2",
            content = "Content 2",
            text = "Cast the movie of your life 2.",
            date = "2015-01-13",
            attribution = "dayone",
            isAnswered = true,
            respondentsCount = 1,
            respondentsAvatars = listOf(BloggingPromptsRespondentAvatar("http://site/avatar1.jpg")),
        ),
        BloggingPromptResponseV2(
            id = 3,
            title = "Title 3",
            content = "Content 3",
            text = "Cast the movie of your life 3.",
            date = "2015-01-14",
            attribution = "",
            isAnswered = false,
            respondentsCount = 3,
            respondentsAvatars = listOf(
                BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"),
                BloggingPromptsRespondentAvatar("http://site/avatar2.jpg"),
                BloggingPromptsRespondentAvatar("http://site/avatar3.jpg")
            ),
        )
    )
)

/* MODEL */

private val FIRST_PROMPT_MODEL = BloggingPromptModel(
    id = 1,
    text = "Cast the movie of your life.",
    date = BloggingPromptsUtils.stringToDate("2015-01-12"),
    isAnswered = false,
    attribution = "",
    respondentsCount = 0,
    respondentsAvatarUrls = emptyList(),
    answeredLink = ANSWERED_LINK_PREFIX + 1,
)

private val SECOND_PROMPT_MODEL = BloggingPromptModel(
    id = 2,
    text = "Cast the movie of your life 2.",
    date = BloggingPromptsUtils.stringToDate("2015-01-13"),
    isAnswered = true,
    attribution = "dayone",
    respondentsCount = 1,
    respondentsAvatarUrls = listOf("http://site/avatar1.jpg"),
    answeredLink = ANSWERED_LINK_PREFIX + 2,
)

private val THIRD_PROMPT_MODEL = BloggingPromptModel(
    id = 3,
    text = "Cast the movie of your life 3.",
    date = BloggingPromptsUtils.stringToDate("2015-01-14"),
    isAnswered = false,
    attribution = "",
    respondentsCount = 3,
    respondentsAvatarUrls = listOf(
        "http://site/avatar1.jpg",
        "http://site/avatar2.jpg",
        "http://site/avatar3.jpg"
    ),
    answeredLink = ANSWERED_LINK_PREFIX + 3,
)

private val PROMPT_MODELS = listOf(FIRST_PROMPT_MODEL, SECOND_PROMPT_MODEL, THIRD_PROMPT_MODEL)

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptsStoreV2Test {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: BloggingPromptsRestClient
    @Mock private lateinit var dao: BloggingPromptsDao

    private lateinit var promptsStore: BloggingPromptsStore

    private val numberOfPromptsToFetch = 40
    private val requestedPromptDate = Date()

    @Before
    fun setUp() {
        promptsStore = BloggingPromptsStore(
            restClient,
            dao,
            initCoroutineEngine()
        )
        setUpMocks()
    }

    private fun setUpMocks() {
        whenever(siteModel.id).thenReturn(SITE_LOCAL_ID)
    }

    @Test
    fun `when fetch prompts triggered, then all prompt models are inserted into db`() = test {
        val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
        whenever(
            restClient.fetchPromptsV2(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate
            )
        ).thenReturn(payload)

        promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate, true)

        verify(dao).insertForSite(siteModel.id, PROMPT_MODELS)
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then all prompt models are returned in the result`() =
        test {
            val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
            whenever(
                restClient.fetchPromptsV2(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(
                payload
            )

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate,
                true
            )

            assertThat(result.model).isEqualTo(PROMPT_MODELS)
            assertThat(result.error).isNull()
        }

    @Test
    fun `given prompts response with exception, when fetch prompts gets triggered, then prompts error is returned`() =
        test {
            val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
            whenever(
                restClient.fetchPromptsV2(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(
                payload
            )
            whenever(
                dao.insertForSite(
                    siteModel.id,
                    PROMPT_MODELS
                )
            ).thenThrow(IllegalStateException("Error"))

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate,
                true
            )

            assertThat(result.model).isNull()
            assertEquals(BloggingPromptsErrorType.GENERIC_ERROR, result.error.type)
            assertNull(result.error.message)
        }

    @Test
    fun `given prompts error, when fetch prompts gets triggered, then prompts error is returned`() =
        test {
            val errorType = BloggingPromptsErrorType.API_ERROR
            val payload = BloggingPromptsPayload<BloggingPromptsListResponseV2>(
                BloggingPromptsError(
                    errorType
                )
            )
            whenever(
                restClient.fetchPromptsV2(
                    siteModel,
                    numberOfPromptsToFetch,
                    requestedPromptDate
                )
            ).thenReturn(payload)

            val result = promptsStore.fetchPrompts(
                siteModel,
                numberOfPromptsToFetch,
                requestedPromptDate,
                true
            )

            assertThat(result.model).isNull()
            assertEquals(errorType, result.error.type)
            assertNull(result.error.message)
        }
}
