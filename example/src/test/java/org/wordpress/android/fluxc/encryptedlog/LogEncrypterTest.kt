package org.wordpress.android.fluxc.encryptedlog

import com.goterl.lazycode.lazysodium.utils.KeyPair
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptionUtils
import org.wordpress.android.fluxc.model.encryptedlogging.LogEncrypter
import java.io.File
import java.util.UUID

class EncryptionUtilsTest {
    private lateinit var keypair: KeyPair

    @Before
    fun setup() {
        keypair = EncryptionUtils.sodium.cryptoBoxKeypair()
    }

    @Test
    @Throws
    fun testThatEncryptedLogsMatchV1FileFormat() {
        val testLogString = UUID.randomUUID().toString()
        val file = createTempFile()

        val json = JSONObject(LogEncrypter(file, testLogString, keypair.publicKey).encrypt())
        assertEquals(
                "`keyedWith` must ALWAYS be v1 in this version of the file format",
                "v1",
                json.getString("keyedWith")
        )

        assertNotNull(
                "The UUID must be valid",
                UUID.fromString(json.getString("uuid"))
        )

        assertEquals(
                "The header must be 32 bytes long",
                32,
                json.getString("header").count()
        )

        assertEquals(
                "The encrypted key should be 108 bytes long",
                108,
                json.getString("encryptedKey").count()
        )

        assertEquals(
                "There should be one message and the closing tag",
                2,
                json.getJSONArray("messages").length()
        )
    }

    // Helpers
    private fun logWithContent(string: String): File {
        val file = createTempFile()
        file.writeText(string)
        return file
    }
}
