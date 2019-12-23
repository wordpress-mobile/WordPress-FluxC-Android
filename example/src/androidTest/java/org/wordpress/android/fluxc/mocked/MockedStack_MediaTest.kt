package org.wordpress.android.fluxc.mocked

import android.annotation.SuppressLint
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.CancelMediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.MediaStore.UploadMediaPayload
import org.wordpress.android.fluxc.utils.MediaUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@SuppressLint("UseSparseArrays")
class MockedStack_MediaTest : MockedStack_Base() {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var mediaStore: MediaStore

    @Inject lateinit var interceptor: ResponseMockingInterceptor

    private var nextEvent: TestEvents? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val uploadedIds: MutableList<Long> = mutableListOf()
    private val uploadedMediaModels: MutableMap<Int, MediaModel> = mutableMapOf()

    private val testSite: SiteModel
        get() {
            val site = SiteModel()
            site.id = 5
            site.setIsWPCom(true)
            site.siteId = 6426253
            return site
        }

    private enum class TestEvents {
        NONE,
        CANCELED_MEDIA,
        UPLOADED_MULTIPLE_MEDIA,
        UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)

        dispatcher.register(this)
        nextEvent = TestEvents.NONE
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUploadMultipleImages() {
        // Upload media to guarantee media exists
        uploadedIds.clear()
        nextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA

        uploadedMediaModels.clear()
        // Here we use the newMediaModel() with id builder, as we need it to identify uploads
        for (i in 1..5) {
            addMediaModelToUploadArray("Test media $i")
        }

        // Upload media, dispatching all at once (not waiting for each to finish)
        uploadMultipleMedia(uploadedMediaModels.values.toList())

        // Verify all have been uploaded
        Assert.assertEquals(uploadedMediaModels.size, uploadedIds.size)
        Assert.assertEquals(
                uploadedMediaModels.size,
                mediaStore.getSiteMediaWithState(testSite, MediaUploadState.UPLOADED).size
        )

        // Verify they exist in the MediaStore
        val iterator = uploadedMediaModels.values.iterator()
        while (iterator.hasNext()) {
            iterator.next().let { uploadedMediaModel ->
                Assert.assertNotNull(mediaStore.getSiteMediaWithId(testSite, uploadedMediaModel.mediaId))
            }
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUploadMultipleImagesAndCancel() {
        // Upload media to guarantee media exists
        uploadedIds.clear()
        nextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL

        uploadedMediaModels.clear()
        // Here we use the newMediaModel() with id builder, as we need it to identify uploads
        for (i in 1..5) {
            addMediaModelToUploadArray("Test media $i")
        }

        // Use this variable to test cancelling 1, 2, 3, 4 or all 5 uploads
        val amountToCancel = 4

        // Upload media, dispatching all at once (not waiting for each to finish)
        // Also cancel (and delete) the first n=`amountToCancel` media uploads
        uploadMultipleMedia(uploadedMediaModels.values.toList(), amountToCancel, true)

        // Verify how many have been uploaded
        Assert.assertEquals((uploadedMediaModels.size - amountToCancel), uploadedIds.size)

        // Verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (mediaId in uploadedIds) {
            Assert.assertNotNull(mediaStore.getSiteMediaWithId(testSite, mediaId))
        }

        // Only completed uploads should exist in the store
        Assert.assertEquals(uploadedIds.size, mediaStore.getSiteMediaCount(testSite))

        // The number of uploaded media in the store should match our records of how many were not cancelled
        Assert.assertEquals(uploadedIds.size,
                mediaStore.getSiteMediaWithState(testSite, MediaUploadState.UPLOADED).size)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testUploadMultipleImagesAndCancelWithoutDeleting() {
        // Upload media to guarantee media exists
        uploadedIds.clear()
        nextEvent = TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL

        uploadedMediaModels.clear()
        // Here we use the newMediaModel() with id builder, as we need it to identify uploads
        for (i in 1..5) {
            addMediaModelToUploadArray("Test media $i")
        }

        // Use this variable to test cancelling 1, 2, 3, 4 or all 5 uploads
        val amountToCancel = 4

        // Upload media, dispatching all at once (not waiting for each to finish)
        // Also cancel (without deleting) the first n=`amountToCancel` media uploads
        uploadMultipleMedia(uploadedMediaModels.values.toList(), amountToCancel, false)

        // Verify how many have been uploaded
        Assert.assertEquals((uploadedMediaModels.size - amountToCancel), uploadedIds.size)

        // Verify each one of the remaining, non-cancelled uploads exist in the MediaStore
        for (mediaId in uploadedIds) {
            Assert.assertNotNull(mediaStore.getSiteMediaWithId(testSite, mediaId))
        }

        // All the original uploads should exist in the store, whether cancelled or not
        Assert.assertEquals(uploadedMediaModels.size, mediaStore.getSiteMediaCount(testSite))

        // The number of uploaded media in the store should match our records of how many were not cancelled
        Assert.assertEquals(uploadedIds.size, mediaStore.getSiteMediaWithState(testSite,
                MediaUploadState.UPLOADED).size)

        // All cancelled media should have a FAILED state
        Assert.assertEquals(amountToCancel, mediaStore.getSiteMediaWithState(testSite, MediaUploadState.FAILED).size)
    }

    @Suppress("unused")
    @Subscribe
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        if (event.canceled) {
            if (nextEvent == TestEvents.CANCELED_MEDIA || nextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL) {
                countDownLatch.countDown()
            } else {
                throw AssertionError("Unexpected cancellation for media: " + event.media.id)
            }
        } else if (event.completed) {
            when (nextEvent) {
                TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL -> {
                    uploadedIds.add(event.media.mediaId)
                    // Update our own map object with the new media id
                    val media = uploadedMediaModels[event.media.id]?.apply {
                        mediaId = event.media.mediaId
                    } ?: AppLog.e(T.MEDIA, "MediaModel not found: " + event.media.id)
                    Assert.assertNotNull(media)
                }
                TestEvents.UPLOADED_MULTIPLE_MEDIA -> {
                    uploadedIds.add(event.media.mediaId)
                    // Update our own map object with the new media id
                    val media = uploadedMediaModels[event.media.id]?.apply {
                        mediaId = event.media.mediaId
                    } ?: AppLog.e(T.MEDIA, "MediaModel not found: " + event.media.id)
                    Assert.assertNotNull(media)
                }
                else -> {
                    throw AssertionError("Unexpected completion for media: " + event.media.id)
                }
            }
            countDownLatch.countDown()
        }
    }

    private fun addMediaModelToUploadArray(title: String) {
        val mediaModel = newMediaModel(title, sampleImagePath, MediaUtils.MIME_TYPE_IMAGE)
        uploadedMediaModels[mediaModel.id] = mediaModel
    }

    private fun newMediaModel(testTitle: String, mediaPath: String, mimeType: String): MediaModel {
        val testDescription = "Test Description"
        val testCaption = "Test Caption"
        val testAlt = "Test Alt"

        return mediaStore.instantiateMediaModel().apply {
            filePath = mediaPath
            fileExtension = mediaPath.substring(mediaPath.lastIndexOf(".") + 1)
            this.mimeType = mimeType + fileExtension
            fileName = mediaPath.substring(mediaPath.lastIndexOf("/"))
            title = testTitle
            description = testDescription
            caption = testCaption
            alt = testAlt
            localSiteId = testSite.id
        }
    }

    @Throws(InterruptedException::class)
    private fun uploadMultipleMedia(
        mediaList: List<MediaModel>,
        howManyFirstToCancel: Int = 0,
        delete: Boolean = false
    ) {
        interceptor.respondWithSticky("media-upload-response-success.json")
        countDownLatch = CountDownLatch(mediaList.size)
        for (media in mediaList) {
            // Don't strip location, as all media are the same file and we end up with concurrent read/writes
            val payload = UploadMediaPayload(testSite, media, false)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
        }

        if (howManyFirstToCancel > 0 && howManyFirstToCancel <= mediaList.size) {
            // Wait a bit and issue the cancel command
            TestUtils.waitFor(500)

            // We'e only cancelling the first n=howManyFirstToCancel uploads
            for (i in 0 until howManyFirstToCancel) {
                val media = mediaList[i]
                val payload = CancelMediaPayload(testSite, media, delete)
                dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
            }
        }

        Assert.assertTrue(countDownLatch.await(TestUtils.MULTIPLE_UPLOADS_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }
}
