package org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts

import com.android.volley.RequestQueue
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptResponseV2
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsListResponseV2
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsRestClient.BloggingPromptsRespondentAvatar
import org.wordpress.android.fluxc.test
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class BloggingPromptsRestClientV2Test {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var site: SiteModel

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: BloggingPromptsRestClient

    private val siteId: Long = 1
    private val numberOfPromptsToFetch: Int = 40

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = BloggingPromptsRestClient(
            wpComGsonRequestBuilder,
            dispatcher,
            null,
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `when fetch prompts gets triggered, then the correct request url is used`() = test {
        val json = UnitTestUtils.getStringFromResourceFile(javaClass, PROMPTS_JSON)
        initFetchPrompts(data = getPromptsResponseFromJsonString(json))

        restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

        Assert.assertEquals(
            urlCaptor.firstValue,
            "$API_SITE_PATH/${site.siteId}/$API_BLOGGING_PROMPTS_PATH"
        )
    }

    @Test
    fun `given success call, when fetch prompts gets triggered, then prompts response is returned`() =
        test {
            val json = UnitTestUtils.getStringFromResourceFile(javaClass, PROMPTS_JSON)
            initFetchPrompts(data = getPromptsResponseFromJsonString(json))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertSuccess(PROMPTS_RESPONSE, result)
        }

    @Test
    fun `given unknown error, when fetch prompts gets triggered, then return prompts timeout error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(UNKNOWN)))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertError(GENERIC_ERROR, result)
        }

    @Test
    fun `given timeout, when fetch prompts gets triggered, then return prompts timeout error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(TIMEOUT)))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertError(BloggingPromptsErrorType.TIMEOUT, result)
        }

    @Test
    fun `given network error, when fetch prompts gets triggered, then return prompts api error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertError(API_ERROR, result)
        }

    @Test
    fun `given invalid response, when fetch prompts gets triggered, then return prompts invalid response error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(INVALID_RESPONSE)))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertError(BloggingPromptsErrorType.INVALID_RESPONSE, result)
        }

    @Test
    fun `given not authenticated, when fetch prompts gets triggered, then return prompts auth required error`() =
        test {
            initFetchPrompts(error = WPComGsonNetworkError(BaseNetworkError(NOT_AUTHENTICATED)))

            val result = restClient.fetchPromptsV2(site, numberOfPromptsToFetch, Date())

            assertError(AUTHORIZATION_REQUIRED, result)
        }

    private fun getPromptsResponseFromJsonString(json: String): BloggingPromptsListResponseV2 {
        val responseType = object : TypeToken<BloggingPromptsListResponseV2>() {}.type
        return GsonBuilder()
            .create().fromJson(json, responseType) as BloggingPromptsListResponseV2
    }

    private suspend fun initFetchPrompts(
        data: BloggingPromptsListResponseV2? = null,
        error: WPComGsonNetworkError? = null
    ): Response<BloggingPromptsListResponseV2> {
        val nonNullData = data ?: mock()
        val response = if (error != null) Error(error) else Success(nonNullData)
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                eq(restClient),
                urlCaptor.capture(),
                paramsCaptor.capture(),
                eq(BloggingPromptsListResponseV2::class.java),
                eq(false),
                any(),
                eq(false)
            )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }

    @Suppress("SameParameterValue")
    private fun assertSuccess(
        expected: BloggingPromptsListResponseV2,
        actual: BloggingPromptsPayload<BloggingPromptsListResponseV2>
    ) {
        with(actual) {
            Assert.assertEquals(site, this@BloggingPromptsRestClientV2Test.site)
            Assert.assertFalse(isError)
            Assert.assertEquals(BloggingPromptsPayload(expected), this)
        }
    }

    private fun assertError(
        expected: BloggingPromptsErrorType,
        actual: BloggingPromptsPayload<BloggingPromptsListResponseV2>
    ) {
        with(actual) {
            Assert.assertEquals(site, this@BloggingPromptsRestClientV2Test.site)
            Assert.assertTrue(isError)
            Assert.assertEquals(expected, error.type)
            Assert.assertEquals(null, error.message)
        }
    }

    companion object {
        private const val API_BASE_PATH = "https://public-api.wordpress.com/wpcom/v2"
        private const val API_SITE_PATH = "$API_BASE_PATH/sites"
        private const val API_BLOGGING_PROMPTS_PATH = "blogging-prompts/"

        private const val PROMPTS_JSON = "wp/bloggingprompts/promptsV2.json"

        private val PROMPT_ONE = BloggingPromptResponseV2(
            id = 1010,
            title = "Title 1",
            content = "Content 1",
            text = "You have 15 minutes to address the whole world live (on television or radio — " +
                "choose your format). What would you say?",
            date = "2022-01-04",
            attribution = "",
            isAnswered = false,
            respondentsCount = 0,
            respondentsAvatars = emptyList(),
        )

        private val PROMPT_TWO = BloggingPromptResponseV2(
            id = 1011,
            title = "Title 2",
            content = "Content 2",
            text = "Do you play in your daily life? What says “playtime” to you?",
            date = "2022-01-05",
            attribution = "dayone",
            isAnswered = true,
            respondentsCount = 1,
            respondentsAvatars = listOf(BloggingPromptsRespondentAvatar("http://site/avatar1.jpg")),
        )

        private val PROMPT_THREE = BloggingPromptResponseV2(
            id = 1012,
            title = "Title 3",
            content = "Content 3",
            text = "Are you good at what you do? What would you like to be better at.",
            date = "2022-01-06",
            isAnswered = false,
            attribution = "",
            respondentsCount = 2,
            respondentsAvatars = listOf(
                BloggingPromptsRespondentAvatar("http://site/avatar2.jpg"),
                BloggingPromptsRespondentAvatar("http://site/avatar3.jpg")
            ),
        )

        private val PROMPTS_RESPONSE: BloggingPromptsListResponseV2 = BloggingPromptsListResponseV2(
            listOf(
                PROMPT_ONE,
                PROMPT_TWO,
                PROMPT_THREE
            )
        )
    }
}
