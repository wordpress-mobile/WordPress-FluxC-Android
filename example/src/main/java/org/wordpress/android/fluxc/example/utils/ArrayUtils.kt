package org.wordpress.android.fluxc.example.utils

import org.apache.commons.lang3.ArrayUtils

object ArrayUtils {
    @JvmStatic
    fun contains(stringArray: Array<String>, string: String): Boolean {
        return ArrayUtils.contains(stringArray, string)
    }
}
