package org.wordpress.android.fluxc.store.jetpackai

import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIQueryErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIQueryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsErrorType.AUTH_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.ResponseFormat
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackAIStore @Inject constructor(
    private val jetpackAIRestClient: JetpackAIRestClient,
    private val jetpackAITranscriptionRestClient: JetpackAITranscriptionRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    companion object {
        private const val OPENAI_GPT4_MODEL_NAME = "gpt-4o"
    }

    private var token: JWTToken? = null

    /**
     * Fetches Jetpack AI completions for a given prompt to be used on a particular post.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param postId    Used to mark the post as having content generated by Jetpack AI.
     */
    suspend fun fetchJetpackAICompletionsForPost(
        site: SiteModel,
        prompt: String,
        postId: Long,
        feature: String,
        skipCache: Boolean = false
    ) = fetchJetpackAICompletions(site, prompt, feature, skipCache, postId)

    /**
     * Fetches Jetpack AI completions for a given prompt used globally by a site.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     */
    suspend fun fetchJetpackAICompletionsForSite(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean = false
    ) = fetchJetpackAICompletions(site, prompt, feature, skipCache)

    private suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean,
        postId: Long? = null
    ) = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI completions"
    ) {
        jetpackAIRestClient.fetchJetpackAICompletions(site, prompt, feature, skipCache, postId)
    }

    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String,
        responseFormat: ResponseFormat? = null,
        model: String? = null
    ): JetpackAICompletionsResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI completions"
    ) {
        val token = token?.validateExpiryDate()?.validateBlogId(site.siteId)
            ?: fetchJetpackAIJWTToken(site).let { tokenResponse ->
                when (tokenResponse) {
                    is Error -> {
                        return@withDefaultContext JetpackAICompletionsResponse.Error(
                            type = AUTH_ERROR,
                            message = tokenResponse.message,
                        )
                    }

                    is Success -> {
                        token = tokenResponse.token
                        tokenResponse.token
                    }
                }
            }

        val result = jetpackAIRestClient.fetchJetpackAITextCompletion(
            token,
            prompt,
            feature,
            responseFormat,
            model
        )

        return@withDefaultContext when {
            // Fetch token anew if using existing token returns AUTH_ERROR
            result is JetpackAICompletionsResponse.Error && result.type == AUTH_ERROR -> {
                // Remove cached token
                this@JetpackAIStore.token = null
                fetchJetpackAICompletions(site, prompt, feature, responseFormat, model)
            }

            else -> result
        }
    }

    private suspend fun fetchJetpackAIJWTToken(site: SiteModel): JetpackAIJWTTokenResponse =
        coroutineEngine.withDefaultContext(
            tag = AppLog.T.API,
            caller = this,
            loggedMessage = "fetch Jetpack AI JWT token"
        ) {
            jetpackAIRestClient.fetchJetpackAIJWTToken(site)
        }

    private fun JWTToken.validateBlogId(blogId: Long): JWTToken? =
        if (getPayloadItem("blog_id")?.toLong() == blogId) this else null

    /**
     * Fetches Jetpack AI Transcription for the specified audio file.
     *
     * @param site      The site used to create the JWT token
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param audioFile The audio File to be transcribed.
     * @param retryCount The number of times the JWTToken request was called
     * @param maxRetries The max number of times JWTToken can be requested
     */
    suspend fun fetchJetpackAITranscription(
        site: SiteModel,
        feature: String?,
        audioFile: File,
        retryCount: Int = 0,
        maxRetries: Int = 1
    ): JetpackAITranscriptionResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI Transcription"
    ) {
        val token = token?.validateExpiryDate()?.validateBlogId(site.siteId)
            ?: fetchJetpackAIJWTToken(site).let { tokenResponse ->
                when (tokenResponse) {
                    is Error -> {
                        return@withDefaultContext JetpackAITranscriptionResponse.Error(
                            type = JetpackAITranscriptionErrorType.AUTH_ERROR,
                            message = tokenResponse.message,
                        )
                    }

                    is Success -> {
                        token = tokenResponse.token
                        tokenResponse.token
                    }
                }
            }

        val result = jetpackAITranscriptionRestClient.fetchJetpackAITranscription(
            jwtToken = token,
            feature = feature,
            audioFile = audioFile
        )

        return@withDefaultContext when {
            // Fetch token anew if using existing token returns AUTH_ERROR
            result is JetpackAITranscriptionResponse.Error &&
                    result.type == JetpackAITranscriptionErrorType.AUTH_ERROR -> {
                // Remove cached token and retry getting the token another time
                this@JetpackAIStore.token = null
                if (retryCount <= maxRetries) {
                    fetchJetpackAITranscription(
                        site,
                        feature,
                        audioFile,
                        retryCount + 1,
                        maxRetries
                    )
                } else {
                    result // Return the error after max retries
                }
            }

            else -> result
        }
    }

    /**
     * Fetches Jetpack AI Query for the specified audio file.
     *
     * @param site      The site used to create the JWT token
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param role      A special marker to indicate that the message needs to be expanded by the Jetpack AI BE.
     * @param message   The message to be expanded by the Jetpack AI BE.
     * @param type      An indication of which kind of post-processing action will be executed over the content.
     * @param stream    When true, the response is a set of EventSource events, otherwise a single response
     * @param retryCount The number of times the JWTToken request was called
     * @param maxRetries The max number of times JWTToken can be requested
     */
    @Suppress("LongParameterList")
    suspend fun fetchJetpackAIQuery(
        site: SiteModel,
        feature: String?,
        role: String,
        message: String,
        type: String,
        stream: Boolean,
        retryCount: Int = 0,
        maxRetries: Int = 1
    ): JetpackAIQueryResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI Query"
    ) {
        val token = token?.validateExpiryDate()?.validateBlogId(site.siteId)
            ?: fetchJetpackAIJWTToken(site).let { tokenResponse ->
                when (tokenResponse) {
                    is Error -> {
                        return@withDefaultContext JetpackAIQueryResponse.Error(
                            type = JetpackAIQueryErrorType.AUTH_ERROR,
                            message = tokenResponse.message,
                        )
                    }

                    is Success -> {
                        token = tokenResponse.token
                        tokenResponse.token
                    }
                }
            }

        val result = jetpackAIRestClient.fetchJetpackAiMessageQuery(
            jwtToken = token,
            message = message,
            feature = feature,
            role = role,
            type = type,
            stream = stream
        )

        return@withDefaultContext when {
            // Fetch token anew if using existing token returns AUTH_ERROR
            result is JetpackAIQueryResponse.Error &&
                    result.type == JetpackAIQueryErrorType.AUTH_ERROR -> {
                // Remove cached token and retry getting the token another time
                this@JetpackAIStore.token = null
                if (retryCount <= maxRetries) {
                    fetchJetpackAIQuery(
                        site = site,
                        feature = feature,
                        role = role,
                        message = message,
                        type = type,
                        stream = stream,
                        retryCount = retryCount + 1,
                        maxRetries = maxRetries
                    )
                } else {
                    result // Return the error after max retries
                }
            }

            else -> result
        }
    }

    /**
     * Fetches Jetpack AI Query for the specific prompt/question
     *
     * @param site      The site used to create the JWT token.
     * @param question  The question to be expanded by the Jetpack AI BE.
     * @param feature   Used by backend to track AI-generation usage and measure costs.
     * @param stream    When true, the response is a set of EventSource events, otherwise a single response
     * @param format    The format of the response: 'text' or 'json_object'. Default "text"
     * @param model     The model to be used for the query: 'gpt-4o' or 'gpt-3.5-turbo-1106'. Optional
     * @param fields    The fields to be requested in the response
     */
    @Suppress("LongParameterList")
    suspend fun fetchJetpackAIQuery(
        site: SiteModel,
        question: String,
        feature: String,
        stream: Boolean,
        model: String = OPENAI_GPT4_MODEL_NAME,
        format: ResponseFormat = ResponseFormat.TEXT,
        fields: String? = null
    ): JetpackAIQueryResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI Query"
    ) {
        val token = token?.validateExpiryDate()?.validateBlogId(site.siteId)
            ?: fetchJetpackAIJWTToken(site).let { tokenResponse ->
                when (tokenResponse) {
                    is Error -> {
                        return@withDefaultContext JetpackAIQueryResponse.Error(
                            type = JetpackAIQueryErrorType.AUTH_ERROR,
                            message = tokenResponse.message,
                        )
                    }

                    is Success -> {
                        token = tokenResponse.token
                        tokenResponse.token
                    }
                }
            }

        val result = jetpackAIRestClient.fetchJetpackAiQuestionQuery(
            jwtToken = token,
            question = question,
            feature = feature,
            format = format,
            model = model,
            stream = stream,
            fields = fields
        )

        return@withDefaultContext when {
            // Fetch token anew if using existing token returns AUTH_ERROR
            result is JetpackAIQueryResponse.Error &&
                    result.type == JetpackAIQueryErrorType.AUTH_ERROR -> {
                // Remove cached token and retry getting the token another time
                this@JetpackAIStore.token = null
                result
            }

            else -> result
        }
    }

    @Suppress("LongParameterList")
    suspend fun fetchJetpackAIAssistantFeature(
        site: SiteModel,
    ): JetpackAIAssistantFeatureResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI Assistant Feature"
    ) {
        jetpackAIRestClient.fetchJetpackAiAssistantFeature(site)
    }
}
