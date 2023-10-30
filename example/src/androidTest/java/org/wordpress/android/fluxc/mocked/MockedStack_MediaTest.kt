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
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@Suppress("ClassNaming")
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
    fun testCancelImageUpload() {
        interceptor.respondWithSticky("media-upload-response-success.json")

        // First, try canceling an image with the default behavior (canceled image is deleted from the store)
        newMediaModel("Test Title", sampleImagePath)?.let { testMedia ->
            countDownLatch = CountDownLatch(1)
            nextEvent = TestEvents.CANCELED_MEDIA
            val payload = UploadMediaPayload(testSite, testMedia, true)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))

            // Wait a bit and issue the cancel command
            TestUtils.waitFor(300)
            val cancelPayload = CancelMediaPayload(testSite, testMedia)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload))

            Assert.assertTrue(
                countDownLatch.await(
                    TestUtils.DEFAULT_TIMEOUT_MS.toLong(),
                    TimeUnit.MILLISECONDS
                )
            )
            Assert.assertEquals(0, mediaStore.getSiteMediaCount(testSite))
        }

        // Now, try canceling with delete=false (canceled image should be marked as failed and kept in the store)
        newMediaModel("Test Title", sampleImagePath)?.let { testMedia ->
            countDownLatch = CountDownLatch(1)
            nextEvent = TestEvents.CANCELED_MEDIA
            val payload = UploadMediaPayload(testSite, testMedia, true)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))

            // Wait a bit and issue the cancel command
            TestUtils.waitFor(300)
            val cancelPayload = CancelMediaPayload(testSite, testMedia, false)
            dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(cancelPayload))

            Assert.assertTrue(
                countDownLatch.await(
                    TestUtils.DEFAULT_TIMEOUT_MS.toLong(),
                    TimeUnit.MILLISECONDS
                )
            )
            Assert.assertEquals(1, mediaStore.getSiteMediaCount(testSite))

            val canceledMedia = mediaStore.getMediaWithLocalId(testMedia.id)
            Assert.assertEquals(MediaUploadState.FAILED.toString(), canceledMedia.uploadState)
        }
    }

    @Test
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
                Assert.assertNotNull(
                    mediaStore.getSiteMediaWithId(testSite, uploadedMediaModel.mediaId)
                )
            }
        }
    }

    @Test
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
        Assert.assertEquals(
            uploadedIds.size,
            mediaStore.getSiteMediaWithState(testSite, MediaUploadState.UPLOADED).size
        )
    }

    @Test
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
        Assert.assertEquals(
            uploadedIds.size,
            mediaStore.getSiteMediaWithState(testSite, MediaUploadState.UPLOADED).size
        )

        // All cancelled media should have a FAILED state
        Assert.assertEquals(
            amountToCancel,
            mediaStore.getSiteMediaWithState(testSite, MediaUploadState.FAILED).size
        )
    }

    @Subscribe
    @Suppress("unused", "ThrowsCount", "CyclomaticComplexMethod", "NestedBlockDepth")
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError || event.media == null) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        event.media?.let {
            if (event.canceled) {
                if (nextEvent == TestEvents.CANCELED_MEDIA ||
                    nextEvent == TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL) {
                    countDownLatch.countDown()
                } else {
                    throw AssertionError("Unexpected cancellation for media: " + it.id)
                }
            } else if (event.completed) {
                when (nextEvent) {
                    TestEvents.UPLOADED_MULTIPLE_MEDIA_WITH_CANCEL -> {
                        uploadedIds.add(it.mediaId)
                        // Update our own map object with the new media id
                        val media = uploadedMediaModels[it.id]?.apply {
                            mediaId = it.mediaId
                        } ?: AppLog.e(T.MEDIA, "MediaModel not found: " + it.id)
                        Assert.assertNotNull(media)
                    }

                    TestEvents.UPLOADED_MULTIPLE_MEDIA -> {
                        uploadedIds.add(it.mediaId)
                        // Update our own map object with the new media id
                        val media = uploadedMediaModels[it.id]?.apply {
                            mediaId = it.mediaId
                        } ?: AppLog.e(T.MEDIA, "MediaModel not found: " + it.id)
                        Assert.assertNotNull(media)
                    }

                    else -> {
                        throw AssertionError("Unexpected completion for media: " + it.id)
                    }
                }
                countDownLatch.countDown()
            }
        }
    }

    private fun addMediaModelToUploadArray(title: String) {
        val mediaModel = newMediaModel(title, sampleImagePath)
        mediaModel?.let { uploadedMediaModels[mediaModel.id] = it }
    }

    private fun newMediaModel(testTitle: String, mediaPath: String): MediaModel? {
        val testMedia = MediaModel(
            testSite.id,
            null,
            mediaPath.substring(mediaPath.lastIndexOf("/")),
            mediaPath,
            mediaPath.substring(mediaPath.lastIndexOf(".") + 1),
            "image/jpeg",
            testTitle,
            null
        )
        testMedia.description = "Test Description"
        testMedia.caption = "Test Caption"
        testMedia.alt = "Test Alt"

        return mediaStore.instantiateMediaModel(testMedia)
    }

    private fun uploadMultipleMedia(
        mediaList: List<MediaModel>,
        howManyFirstToCancel: Int = 0,
        delete: Boolean = false
    ) {
        // List of unique sequential ids the same size as the mediaList: 100, 101, 102, ...
        val remoteIdQueue = ConcurrentLinkedQueue(List(mediaList.size) { i -> (i + 100) })

        interceptor.respondWithSticky("media-upload-response-success.json") {
            // To imitate a real set of media upload requests as much as possible, each one should return a unique
            // remote media id. This also makes sure the MediaModel table doesn't treat these as duplicate entries and
            // deletes them, failing the test.
            defaultId: String ->
            defaultId.replace("9999", remoteIdQueue.poll()?.toString() ?: "")
        }

        countDownLatch = CountDownLatch(mediaList.size)
        for (media in mediaList) {
            // Don't strip location, as all media are the same file and we end up with concurrent read/writes
            val payload = UploadMediaPayload(testSite, media, false)
            dispatcher.dispatch(MediaActionBuilder.newUploadMediaAction(payload))
        }

        if (howManyFirstToCancel > 0 && howManyFirstToCancel <= mediaList.size) {
            // Wait a bit and issue the cancel command
            TestUtils.waitFor(300)

            // We're only cancelling the first n=howManyFirstToCancel uploads
            for (i in 0 until howManyFirstToCancel) {
                val media = mediaList[i]
                val payload = CancelMediaPayload(testSite, media, delete)
                dispatcher.dispatch(MediaActionBuilder.newCancelMediaUploadAction(payload))
            }
        }

        Assert.assertTrue(
            countDownLatch.await(
                TestUtils.MULTIPLE_UPLOADS_TIMEOUT_MS.toLong(),
                TimeUnit.MILLISECONDS
            )
        )
    }
}
