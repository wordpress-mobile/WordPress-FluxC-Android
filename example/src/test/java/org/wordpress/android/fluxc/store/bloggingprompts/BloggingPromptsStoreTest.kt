package org.wordpress.android.fluxc.store.bloggingprompts

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsRespondentAvatar
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.TodaysStatsResponse
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsError
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsErrorType
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsPayload
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsResult
import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

/* SITE */

const val SITE_LOCAL_ID = 1

/* TODAY'S STATS */

const val TODAYS_STATS_VIEWS = 100
const val TODAYS_STATS_VISITORS = 30
const val TODAYS_STATS_LIKES = 50
const val TODAYS_STATS_COMMENTS = 10

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
const val POST_FEATURED_IMAGE = "featuredImage"
const val POST_DATE = "2021-12-27 11:33:55"

/* CARD TYPES */

private val CARD_TYPES = listOf(CardModel.Type.TODAYS_STATS, CardModel.Type.POSTS)

/* RESPONSE */

private val TODAYS_STATS_RESPONSE = TodaysStatsResponse(
        views = TODAYS_STATS_VIEWS,
        visitors = TODAYS_STATS_VISITORS,
        likes = TODAYS_STATS_LIKES,
        comments = TODAYS_STATS_COMMENTS
)

private val POST_RESPONSE = PostResponse(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        featuredImage = POST_FEATURED_IMAGE,
        date = POST_DATE
)

private val PROMPTS_RESPONSE = BloggingPromptsResponse(
        prompts = listOf(
                BloggingPromptResponse(
                        id = 1,
                        text = "Cast the movie of your life.",
                        title = "Prompt Title",
                        content = "content of the prompt",
                        date = "2015-01-12",
                        isAnswered = false,
                        respondentsCount = 0,
                        respondentsAvatars = emptyList()
                ),

                BloggingPromptResponse(
                        id = 2,
                        text = "Cast the movie of your life 2.",
                        title = "Prompt Title 2",
                        content = "content of the prompt 2",
                        date = "2015-01-13",
                        isAnswered = true,
                        respondentsCount = 1,
                        respondentsAvatars = listOf(BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"))
                ),
                BloggingPromptResponse(
                        id = 3,
                        text = "Cast the movie of your life 3.",
                        title = "Prompt Title 3",
                        content = "content of the prompt 3",
                        date = "2015-01-14",
                        isAnswered = false,
                        respondentsCount = 3,
                        respondentsAvatars = listOf(
                                BloggingPromptsRespondentAvatar("http://site/avatar1.jpg"),
                                BloggingPromptsRespondentAvatar("http://site/avatar2.jpg"),
                                BloggingPromptsRespondentAvatar("http://site/avatar3.jpg")
                        )
                )
        )
)

/* MODEL */
private val PROMPT_MODELS = listOf(
        BloggingPromptModel(
                id = 1,
                text = "Cast the movie of your life.",
                title = "Prompt Title",
                content = "content of the prompt",
                date = BloggingPromptsUtils.stringToDate("2015-01-12"),
                isAnswered = false,
                respondentsCount = 0,
                respondentsAvatars = emptyList()
        ),

        BloggingPromptModel(
                id = 2,
                text = "Cast the movie of your life 2.",
                title = "Prompt Title 2",
                content = "content of the prompt 2",
                date = BloggingPromptsUtils.stringToDate("2015-01-13"),
                isAnswered = true,
                respondentsCount = 1,
                respondentsAvatars = listOf("http://site/avatar1.jpg")
        ),
        BloggingPromptModel(
                id = 3,
                text = "Cast the movie of your life 3.",
                title = "Prompt Title 3",
                content = "content of the prompt 3",
                date = BloggingPromptsUtils.stringToDate("2015-01-14"),
                isAnswered = false,
                respondentsCount = 3,
                respondentsAvatars = listOf(
                        "http://site/avatar1.jpg",
                        "http://site/avatar2.jpg",
                        "http://site/avatar3.jpg",
                )
        )
)

/* ENTITY */
private val PROMPT_ENTITIES = listOf(
        BloggingPromptEntity(
                id = 1,
                siteLocalId = SITE_LOCAL_ID,
                text = "Cast the movie of your life.",
                title = "Prompt Title",
                content = "content of the prompt",
                date = BloggingPromptsUtils.stringToDate("2015-01-12"),
                isAnswered = false,
                respondentsCount = 0,
                respondentsAvatars = emptyList()
        ),
        BloggingPromptEntity(
                id = 2,
                siteLocalId = SITE_LOCAL_ID,
                text = "Cast the movie of your life 2.",
                title = "Prompt Title 2",
                content = "content of the prompt 2",
                date = BloggingPromptsUtils.stringToDate("2015-01-13"),
                isAnswered = true,
                respondentsCount = 1,
                respondentsAvatars = listOf("http://site/avatar1.jpg")
        ),
        BloggingPromptEntity(
                id = 3,
                siteLocalId = SITE_LOCAL_ID,
                text = "Cast the movie of your life 3.",
                title = "Prompt Title 3",
                content = "content of the prompt 3",
                date =  BloggingPromptsUtils.stringToDate("2015-01-14"),
                isAnswered = false,
                respondentsCount = 3,
                respondentsAvatars = listOf(
                        "http://site/avatar1.jpg",
                        "http://site/avatar2.jpg",
                        "http://site/avatar3.jpg",
                )
        )
)

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptsStoreTest {
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
    fun `when fetch prompts triggered, then all prompt model are inserted into db`() = test {
        val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
        whenever(restClient.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)).thenReturn(payload)

        promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)

        verify(dao).insertForSite(siteModel.id, PROMPT_MODELS)
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then empty cards model is returned`() =
            test {
                val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
                whenever(restClient.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)).thenReturn(
                        payload
                )

                val result = promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)

                assertThat(result.model).isNull()
                assertThat(result.error).isNull()
            }

    @Test
    fun `given prompts response with exception, when fetch prompts gets triggered, then prompts error is returned`() =
            test {
                val payload = BloggingPromptsPayload(PROMPTS_RESPONSE)
                whenever(restClient.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)).thenReturn(
                        payload
                )
                whenever(
                        dao.insertForSite(
                                siteModel.id,
                                PROMPT_MODELS
                        )
                ).thenThrow(IllegalStateException("Error"))

                val result = promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)

                assertThat(result.model).isNull()
                assertEquals(BloggingPromptsErrorType.GENERIC_ERROR, result.error.type)
                assertNull(result.error.message)
            }

    @Test
    fun `when get prompts gets triggered, then a flow of prompt models is returned`() = test {
        whenever(dao.getAllPrompts(SITE_LOCAL_ID)).thenReturn(flowOf(PROMPT_ENTITIES))

        val result = promptsStore.getPrompts(siteModel).first()

        assertThat(result).isEqualTo(BloggingPromptsResult(PROMPT_MODELS))
    }

    @Test
    fun `given prompts error, when fetch prompts gets triggered, then prompts error is returned`() = test {
        val errorType = BloggingPromptsErrorType.API_ERROR
        val payload = BloggingPromptsPayload<BloggingPromptsResponse>(BloggingPromptsError(errorType))
        whenever(restClient.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)).thenReturn(payload)

        val result = promptsStore.fetchPrompts(siteModel, numberOfPromptsToFetch, requestedPromptDate)

        assertThat(result.model).isNull()
        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }
//
//    @Test
//    fun `given authorization required, when fetch cards gets triggered, then db is cleared of cards model`() = test {
//        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
//        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
//        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)
//
//        promptsStore.fetchCards(siteModel, CARD_TYPES)
//
//        verify(dao).clear()
//    }
//
//    @Test
//    fun `given authorization required, when fetch cards gets triggered, then empty cards model is returned`() = test {
//        val errorType = CardsErrorType.AUTHORIZATION_REQUIRED
//        val payload = CardsPayload<CardsResponse>(CardsError(errorType))
//        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)
//
//        val result = promptsStore.fetchCards(siteModel, CARD_TYPES)
//
//        assertThat(result.model).isNull()
//        assertThat(result.error).isNull()
//    }
//
//    @Test
//    fun `given empty cards payload, when fetch cards gets triggered, then cards error is returned`() = test {
//        val payload = CardsPayload<CardsResponse>()
//        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(payload)
//
//        val result = promptsStore.fetchCards(siteModel, CARD_TYPES)
//
//        assertThat(result.model).isNull()
//        assertEquals(CardsErrorType.INVALID_RESPONSE, result.error.type)
//        assertNull(result.error.message)
//    }
//
//    @Test
//    fun `when get cards gets triggered, then a flow of cards model is returned`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES)).thenReturn(flowOf(CARDS_ENTITY))
//
//        val result = promptsStore.getCards(siteModel, CARD_TYPES).single()
//
//        assertThat(result).isEqualTo(CardsResult(CARDS_MODEL))
//    }
//
//    @Test
//    fun `when get cards gets triggered for today's stats only, then a flow of today's stats card model is returned`() =
//            test {
//                whenever(dao.get(SITE_LOCAL_ID, listOf(CardModel.Type.TODAYS_STATS)))
//                        .thenReturn(flowOf(listOf(TODAYS_STATS_ENTITY)))
//
//                val result = promptsStore.getCards(siteModel, listOf(CardModel.Type.TODAYS_STATS)).single()
//
//                assertThat(result).isEqualTo(CardsResult(listOf(TODAYS_STATS_MODEL)))
//            }
//
//    @Test
//    fun `when get cards gets triggered for posts only, then a flow of post card model is returned`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, listOf(CardModel.Type.POSTS))).thenReturn(flowOf(listOf(POSTS_ENTITY)))
//
//        val result = promptsStore.getCards(siteModel, listOf(CardModel.Type.POSTS)).single()
//
//        assertThat(result).isEqualTo(CardsResult(listOf(POSTS_MODEL)))
//    }
//
//    /* TODAYS STATS CARD WITH ERROR */
//
//    @Test
//    fun `given todays stats card with error, when fetch cards triggered, then card with error inserted into db`() =
//            test {
//                whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(CardsPayload(cardsRespone))
//                whenever(cardsRespone.toCards()).thenReturn(listOf(TODAYS_STATS_WITH_ERROR_MODEL))
//
//                promptsStore.fetchCards(siteModel, CARD_TYPES)
//
//                verify(dao).insertWithDate(siteModel.id, listOf(TODAYS_STATS_WITH_ERROR_MODEL))
//            }
//
//    @Test
//    fun `given today's stats jetpack disconn error, when get cards triggered, then error exists in the card`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES))
//                .thenReturn(
//                        flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)))
//                )
//
//        val result = promptsStore.getCards(siteModel, CARD_TYPES).single()
//
//        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.JETPACK_DISCONNECTED)
//    }
//
//    @Test
//    fun `given today's stats jetpack disabled error, when get cards triggered, then error exists in the card`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES))
//                .thenReturn(flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.JETPACK_DISABLED))))
//
//        val result = promptsStore.getCards(siteModel, CARD_TYPES).single()
//
//        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.JETPACK_DISABLED)
//    }
//
//    @Test
//    fun `given today's stats jetpack unauth error, when get cards triggered, then error exists in the card`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES))
//                .thenReturn(flowOf(listOf(getTodaysStatsErrorCardEntity(TodaysStatsCardErrorType.UNAUTHORIZED))))
//
//        val result = promptsStore.getCards(siteModel, CARD_TYPES).single()
//
//        assertThat(result.findTodaysStatsCardError()?.type).isEqualTo(TodaysStatsCardErrorType.UNAUTHORIZED)
//    }
//
//    /* POSTS CARD WITH ERROR */
//
//    @Test
//    fun `given posts card with error, when fetch cards triggered, then card with error inserted into db`() = test {
//        whenever(restClient.fetchCards(siteModel, CARD_TYPES)).thenReturn(CardsPayload(cardsRespone))
//        whenever(cardsRespone.toCards()).thenReturn(listOf(POSTS_WITH_ERROR_MODEL))
//
//        promptsStore.fetchCards(siteModel, CARD_TYPES)
//
//        verify(dao).insertWithDate(siteModel.id, listOf(POSTS_WITH_ERROR_MODEL))
//    }
//
//    @Test
//    fun `given posts card unauth error, when get cards triggered, then error exists in the card`() = test {
//        whenever(dao.get(SITE_LOCAL_ID, CARD_TYPES)).thenReturn(flowOf(listOf(POSTS_WITH_ERROR_ENTITY)))
//
//        val result = promptsStore.getCards(siteModel, CARD_TYPES).single()
//
//        assertThat(result.findPostsCardError()?.type).isEqualTo(PostCardErrorType.UNAUTHORIZED)
//    }

    private fun CardsResult<List<CardModel>>.findTodaysStatsCardError(): TodaysStatsCardError? =
            model?.filterIsInstance(TodaysStatsCardModel::class.java)?.firstOrNull()?.error

    private fun CardsResult<List<CardModel>>.findPostsCardError(): PostCardError? =
            model?.filterIsInstance(PostsCardModel::class.java)?.firstOrNull()?.error
}
