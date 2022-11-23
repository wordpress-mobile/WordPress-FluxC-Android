package org.wordpress.android.fluxc.example.utils

object RandomStringUtils {
    @JvmStatic
    fun randomAlphanumeric(count: Int): String {
        return randomString(count)
    }

    @JvmStatic
    fun randomAlphabetic(count: Int): String {
        return randomString(count, numbers = false)
    }

    @JvmStatic
    fun randomNumeric(count: Int): String {
        return randomString(count, letters = false)
    }

    private fun randomString(
        count: Int,
        letters: Boolean = true,
        numbers: Boolean = true
    ): String {
        val alphabetic: List<Char> = ('A'..'Z') + ('a'..'z')
        val numeric = ('0'..'9').toList()
        val allowedChars: List<Char> = when {
            letters && numbers -> alphabetic + numeric
            letters -> alphabetic
            numbers -> numeric
            else -> throw UnsupportedOperationException("Neither letters nor numbers")
        }
        return (1..count)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
