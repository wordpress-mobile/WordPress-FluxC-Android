package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.MEDIA
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class WPV2MediaRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine,
    @Named("regular") private val okHttpClient: OkHttpClient,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    private val gson: Gson by lazy { Gson() }

    fun uploadMedia(site: SiteModel, media: MediaModel) {
        coroutineEngine.launch(T.MEDIA, this, "Upload Media using WPCom's /wp/v2 API") {
            syncUploadMedia(site, media)
        }
    }

    suspend fun syncUploadMedia(site: SiteModel, media: MediaModel): ProgressPayload {
        return suspendCancellableCoroutine { cont ->
            val url = WPAPI.media.getWPComUrl(site.siteId)
            val body = WPRestUploadRequestBody(media) { media, progress ->
                val payload = ProgressPayload(media, progress, false, null)
                mDispatcher.emitChange(UploadActionBuilder.newUploadedMediaAction(payload))
            }

            val request = Request.Builder()
                    .url(url)
                    .post(body = body)
                    .header(WPComGsonRequest.REST_AUTHORIZATION_HEADER, accessToken.get())
                    .build()

            val call = okHttpClient.newCall(request)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // If the continuation has been canceled, then ignore errors
                    if (cont.isActive) {
                        val message = "media upload failed: $e"
                        AppLog.w(MEDIA, message)
                        val error = MediaError.fromIOException(e)
                        error.logMessage = message
                        cont.handleFailure(media, error)
                    }
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        try {
                            val res = gson.fromJson(response.body!!.string(), MediaWPRESTResponse::class.java)
                            val uploadedMedia = res.toMediaModel()
                            cont.resume(ProgressPayload(uploadedMedia, 1f, true, false))
                        } catch (e: JsonSyntaxException) {
                            val error = MediaError(PARSE_ERROR)
                            cont.handleFailure(media, error)
                        } catch (e: NullPointerException) {
                            val error = MediaError(PARSE_ERROR)
                            cont.handleFailure(media, error)
                        }
                    } else {
                        TODO()
                    }
                }
            })

            cont.invokeOnCancellation {
                call.cancel()
            }
        }
    }

    private fun CancellableContinuation<ProgressPayload>.handleFailure(media: MediaModel, error: MediaError) {
        media.setUploadState(FAILED)
        val payload = ProgressPayload(media, 1f, false, error)
        mDispatcher.emitChange(payload)
        resume(payload)
    }
}
