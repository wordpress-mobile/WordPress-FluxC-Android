package org.wordpress.android.fluxc.network.utils

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData

/**
 * Simple wrapper of the map utility method that is null safe
 */
fun <T, U> LiveData<T>.map(mapper: (T) -> U?): MediatorLiveData<U> {
    val result = MediatorLiveData<U>()
    result.addSource(this) { x -> result.value = x?.let { mapper(x) } }
    return result
}
