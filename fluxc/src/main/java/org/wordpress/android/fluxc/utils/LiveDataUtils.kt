package org.wordpress.android.fluxc.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations

fun <T, O> LiveData<T>.map(mapper: (T) -> O): LiveData<O> {
    return Transformations.map(this) { item ->
        if (item != null) {
            mapper(item)
        } else {
            null
        }
    }
}
