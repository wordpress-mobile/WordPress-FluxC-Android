package org.wordpress.android.fluxc.utils

fun String.semverCompareTo(otherVersion: String): Int {
    try {
        val thisVersionTokens = substringBefore("-").split(".").map { Integer.parseInt(it) }
        val otherVersionTokens = otherVersion.substringBefore("-").split(".").map { Integer.parseInt(it) }

        thisVersionTokens.forEachIndexed { index, token ->
            if (token > otherVersionTokens[index]) {
                return 1
            } else if (token < otherVersionTokens[index]) {
                return -1
            }
        }
        return 0
    } catch (e: NumberFormatException) {
        // if the parsing fails, consider this version lower than the other one
        return -1
    }
}
