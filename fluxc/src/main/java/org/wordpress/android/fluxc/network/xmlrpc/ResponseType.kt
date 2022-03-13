package org.wordpress.android.fluxc.network.xmlrpc

import org.wordpress.android.fluxc.network.xmlrpc.ResponseType.HashMapType
import org.wordpress.android.fluxc.network.xmlrpc.ResponseType.ObjectArrayType
import org.wordpress.android.fluxc.network.xmlrpc.ResponseType.ObjectType

sealed class ResponseType {
    data class ObjectType(val data: Any?): ResponseType()
    // TODO: for ObjectArrayType, evaluate AS warning that says
    // "Array property in data class: it's recommended to override equals() / hashCode()"
    data class ObjectArrayType(val data: Array<Any?>?): ResponseType()
    data class HashMapType(val data: HashMap<*,*>?): ResponseType()

    companion object {
        fun wrapRawDataOrNull(data: Any?): ResponseType? {
            return data?.let {
                @Suppress("UNCHECKED_CAST")
                when(it) {
                    // Not great to have, order (from more specific to Object) is important to
                    // get the correct wrapping!
                    is HashMap<*,*> -> HashMapType(it)
                    is Array<*> -> ObjectArrayType(it as Array<Any?>)
                    else -> ObjectType(it)
                }
            }
        }
    }
}

fun ResponseType?.dataOrNull(): Any? {
    if (this == null) return null
    return when(this) {
        is HashMapType -> this.data
        is ObjectArrayType -> this.data
        is ObjectType -> this.data
    }
}
