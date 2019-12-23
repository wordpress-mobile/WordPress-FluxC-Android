package org.wordpress.android.fluxc.mocked

import android.annotation.SuppressLint
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

@SuppressLint("UseSparseArrays")
class MockedStack_MediaTest : MockedStack_Base() {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var mediaStore: MediaStore

    @Inject lateinit var interceptor: ResponseMockingInterceptor

    private var nextEvent: TestEvents? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val testSite: SiteModel
        get() {
            val site = SiteModel()
            site.id = 5
            site.setIsWPCom(true)
            site.siteId = 6426253
            return site
        }

    private enum class TestEvents {
        NONE
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)

        dispatcher.register(this)
        nextEvent = TestEvents.NONE
    }

    @Suppress("unused")
    @Subscribe
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
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
}
