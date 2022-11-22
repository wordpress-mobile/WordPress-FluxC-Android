package org.wordpress.android.fluxc.example.utils

object RandomStringUtils {
    @JvmStatic
    fun randomAlphanumeric(count: Int): String {
        return randomString(
            count = count,
            letters = true,
            numbers = true
        )
    }

    @JvmStatic
    fun randomAlphabetic(count: Int): String {
        return randomString(
            count = count,
            letters = true,
            numbers = false
        )
    }

    @JvmStatic
    fun randomNumeric(count: Int): String {
        return randomString(
            count = count,
            letters = false,
            numbers = true
        )
    }

    private fun randomString(count: Int, letters: Boolean, numbers: Boolean): String {
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
