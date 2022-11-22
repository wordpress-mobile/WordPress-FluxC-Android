package org.wordpress.android.fluxc.example.utils

import org.apache.commons.lang3.RandomStringUtils

object RandomStringUtils {
    @JvmStatic
    fun randomAlphanumeric(count: Int): String {
        return RandomStringUtils.randomAlphanumeric(count)
    }

    @JvmStatic
    fun randomAlphabetic(count: Int): String {
        return RandomStringUtils.randomAlphabetic(count)
    }

    @JvmStatic
    fun randomNumeric(count: Int): String {
        return RandomStringUtils.randomNumeric(count)
    }
}
