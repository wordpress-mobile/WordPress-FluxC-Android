package org.wordpress.android.fluxc.example.utils

object ArrayUtils {
    private const val INDEX_NOT_FOUND = -1

    @JvmStatic
    fun contains(stringArray: Array<String>, string: String): Boolean {
        return indexOf(stringArray, string) != INDEX_NOT_FOUND
    }

    @JvmStatic
    fun indexOf(stringArray: Array<String>, string: String): Int {
        return stringArray.indexOf(string)
    }
}
