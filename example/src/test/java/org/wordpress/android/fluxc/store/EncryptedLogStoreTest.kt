package org.wordpress.android.fluxc.store

import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils

@RunWith(MockitoJUnitRunner::class)
class EncryptedLogStoreTest {
    @Mock private lateinit var encryptedLogRestClient: EncryptedLogRestClient
    @Mock private lateinit var encryptedLogSqlUtils: EncryptedLogSqlUtils
    private lateinit var encryptedLogStore: EncryptedLogStore

    @Before
    fun setUp() {
        encryptedLogStore = EncryptedLogStore(encryptedLogRestClient, encryptedLogSqlUtils)
    }

    @Test
    fun testSomething() {
        assertTrue(true)
    }
}
