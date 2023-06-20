package org.wordpress.android.fluxc.utils

class JsonStringUtils {
    object EMPTY {
        const val STRING = "\"\""
        const val OBJECT = "{}"
        const val ARRAY = "[]"
    }
}
fun String.isJsonEmptyElement() = this in setOf(
    JsonStringUtils.EMPTY.STRING,
    JsonStringUtils.EMPTY.OBJECT,
    JsonStringUtils.EMPTY.ARRAY
)
