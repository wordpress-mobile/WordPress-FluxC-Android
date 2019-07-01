package org.wordpress.android.fluxc.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations

/**
 * Simple wrapper of the map utility method that is null safe
 */
fun <T, U> LiveData<T>.mapNullable(mapper: (T?) -> U?): LiveData<U> {
    return Transformations.map(this) { mapper(it) }
}

/**
 * Simple wrapper of the map utility method that is null safe
 */
fun <T, U> LiveData<T>.map(mapper: (T) -> U?): MediatorLiveData<U> {
    val result = MediatorLiveData<U>()
    result.addSource(this) { x -> result.value = x?.let { mapper(x) } }
    return result
}
