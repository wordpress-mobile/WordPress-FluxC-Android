package org.wordpress.android.fluxc

import android.util.Base64
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.model.JWTToken

class JWTTokenTests {
    @Test
    fun given_a_valid_token__when_takeIfValid_is_called__then_return_it() {
        val token = generateToken(expired = false)

        val result = token.takeIfValid()

        Assert.assertNotNull(result)
    }

    @Test
    fun given_an_expired_token__when_takeIfValid_is_called__then_return_null() {
        val token = generateToken(expired = true)

        val result = token.takeIfValid()

        Assert.assertNull(result)
    }

    private fun generateToken(expired: Boolean): JWTToken {
        val expirationTime = System.currentTimeMillis() / 1000 + if (expired) -100 else 100

        // Sample token from https://jwt.io/ modifier with an expiration time
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = Base64.encode(
            """
                {
                    "sub": "1234567890",
                    "name": "John Doe",
                    "iat": 1516239022,
                    "exp": $expirationTime,
                    "expires": $expirationTime
                }
            """.trimIndent().toByteArray(), Base64.DEFAULT
        ).decodeToString()
        val signature = "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        return JWTToken("$header.$payload.$signature")
    }
}
