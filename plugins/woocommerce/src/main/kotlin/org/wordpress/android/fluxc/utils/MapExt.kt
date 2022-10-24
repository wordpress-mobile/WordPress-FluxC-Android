package org.wordpress.android.fluxc.utils

fun <T> MutableMap<T, String>.putIfNotEmpty(vararg pairs: Pair<T, String?>) = apply {
    pairs.forEach { pair ->
        pair.second?.takeIf { it.isNotEmpty() }
                ?.let { put(pair.first, it) }
    }
}

fun <K,V> MutableMap<K, List<V>>.putIfNotEmpty(key: K, value: List<V>) {
    if (value.isNotEmpty()) this[key] = value
}
