package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wordpress.android.fluxc.store.EncryptedLogStore
import javax.inject.Inject

private const val TEST_UUID = "TEST-UUID"

class ReleaseStack_EncryptedLogTest: ReleaseStack_Base() {
    @Inject lateinit var encryptedLogStore: EncryptedLogStore

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Test
    fun testSomething() {
        runBlocking {
            encryptedLogStore.queueLogForUpload(TEST_UUID, createTempFile(suffix = TEST_UUID))
        }
    }
}
