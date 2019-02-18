package org.wordpress.android.fluxc.example.utils

import android.content.Context
import android.content.SharedPreferences

fun getFluxPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences("${context.packageName}_fluxc-example-preferences", Context.MODE_PRIVATE)
}

fun getStringFromPreferences(preferences: SharedPreferences, key: String): String? {
    return preferences.getString(key, null)
}

fun addStringToPreferences(preferences: SharedPreferences, key: String, value: String) {
    preferences
            .edit()
            .putString(key, value)
            .apply()
}
