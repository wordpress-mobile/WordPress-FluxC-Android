package org.wordpress.android.fluxc

import android.util.Base64
import android.util.Base64.DEFAULT
import com.goterl.lazycode.lazysodium.interfaces.SecretStream
import com.goterl.lazycode.lazysodium.utils.KeyPair
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLoggingKey
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedSecretStreamKey
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptionUtils
import org.wordpress.android.fluxc.model.encryptedlogging.LogEncrypter
import org.wordpress.android.fluxc.model.encryptedlogging.SecretStreamKey
import java.util.UUID

class LogEncrypterTest {
    private lateinit var keypair: KeyPair
    private val logDecrypter: LogDecrypter = LogDecrypter()

    @Before
    fun setup() {
        keypair = EncryptionUtils.sodium.cryptoBoxKeypair()
    }

    @Test
    @Throws
    fun testThatEncryptedLogsMatchV1FileFormat() {
        val testLogString = UUID.randomUUID().toString()
        val encryptedLog = encryptContent(testLogString)

        val json = JSONObject(encryptedLog)
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

    @Test
    fun testThatLogsCanBeDecrypted() {
        val testLogString = UUID.randomUUID().toString()
        assertEquals(testLogString, decryptContent(encryptContent(testLogString)))
    }

    @Test
    fun testThatEmptyLogsCanBeEncrypted() {
        val testLogString = ""
        assertEquals(testLogString, decryptContent(encryptContent(testLogString)))
    }

    @Test
    fun testThatExplicitUUIDsCanBeRetrievedFromEncryptedLogs() {
        val testUuid = UUID.randomUUID().toString()

        val (_, uuid) = logDecrypter.decrypt(encryptContent("", testUuid), keypair)
        assertEquals(uuid, testUuid)
    }

    // Helpers

    private fun encryptContent(content: String, uuid: String = UUID.randomUUID().toString()): String {
        return LogEncrypter(EncryptedLoggingKey(keypair.publicKey)).encrypt(content, uuid)
    }

    private fun decryptContent(encryptedText: String): String {
        return logDecrypter.decrypt(encryptedText, keypair).first
    }
}

/**
 * EncryptedStream represents the encrypted stream once the key has been decrypted. It exists to separate
 * the key decryption from the stream decryption while decoding.
 *
 * @param key An unencrypted SecretStreamKey used to decrypt the remainder of the log.
 * @param header A `ByteArray` representing the stream header – it's used to initialize the decryption stream.
 * @param messages A `List<ByteArray>` of encrypted messages
 */
private class EncryptedStream(val key: SecretStreamKey, val header: ByteArray, val messages: List<ByteArray>)

private const val JSON_KEYED_WITH_KEY = "keyedWith"
private const val JSON_UUID_KEY = "uuid"
private const val JSON_HEADER_KEY = "header"
private const val JSON_ENCRYPTED_KEY_KEY = "encryptedKey"
private const val JSON_MESSAGES_KEY = "messages"

/**
 * LogDecrypter allows decrypting encrypted content.
 */
private class LogDecrypter {
    private val sodium = EncryptionUtils.sodium
    private val state = SecretStream.State.ByReference()

    private fun encryptedStream(encryptedText: String, keyPair: KeyPair): Pair<EncryptedStream, String> {
        val json = JSONObject(encryptedText)

        require(json.getString(JSON_KEYED_WITH_KEY) == "v1") {
            "This class can only parse files keyedWith the v1 implementation"
        }

        val uuid = json.getString(JSON_UUID_KEY)
        val header = json.getString(JSON_HEADER_KEY).base64Decode()
        val encryptedKey = EncryptedSecretStreamKey(json.getString(JSON_ENCRYPTED_KEY_KEY).base64Decode())
        val messagesJson = json.getJSONArray(JSON_MESSAGES_KEY)

        val messages = (0 until messagesJson.length()).map { messagesJson.getString(it).base64Decode() }

        val encryptedStream = EncryptedStream(encryptedKey.decrypt(keyPair), header, messages)
        check(sodium.cryptoSecretStreamInitPull(state, encryptedStream.header, encryptedStream.key.bytes))
        return Pair(encryptedStream, uuid)
    }

    /**
     * Decrypts and returns the log file as a String.
     *
     * @param encryptedText The encrypted text to decrypt.
     * @param keyPair The public and secret key pair associated with this file. Both are required to decrypt the file.
     */
    fun decrypt(encryptedText: String, keyPair: KeyPair): Pair<String, String> {
        val (encryptedStream, uuid) = encryptedStream(encryptedText, keyPair)
        val decryptedText = encryptedStream.messages.fold("") { accumulated: String, cipherBytes: ByteArray ->
            String
            val plainBytes = ByteArray(cipherBytes.size - SecretStream.ABYTES)

            val tag = ByteArray(1) // Stores the extracted tag. This implementation doesn't do anything with it.
            check(sodium.cryptoSecretStreamPull(state, plainBytes, tag, cipherBytes, cipherBytes.size.toLong()))

            accumulated + String(plainBytes)
        }
        return Pair(decryptedText, uuid)
    }
}

// On Android base64 has lots of options, so define an extension to make it easier to avoid decoding issues.
private fun String.base64Decode(): ByteArray {
    return Base64.decode(this, DEFAULT)
}
