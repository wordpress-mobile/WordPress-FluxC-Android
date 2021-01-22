package org.wordpress.android.fluxc.utils

fun <T> MutableMap<T, String>.putIfNotEmpty(pair: Pair<T, String?>) = apply {
    pair.second?.takeIf { it.isNotEmpty() }
            ?.let { put(pair.first, it) }
}
