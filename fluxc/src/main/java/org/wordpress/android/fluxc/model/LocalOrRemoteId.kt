package org.wordpress.android.fluxc.model

sealed class LocalOrRemoteId {
    data class LocalId(val value: Int) : LocalOrRemoteId()
    data class RemoteId(val value: Long) : LocalOrRemoteId()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this is LocalId && other is LocalId) {
            return this.value == other.value
        }
        if (this is RemoteId && other is RemoteId) {
            return this.value == other.value
        }
        return false
    }

    override fun hashCode(): Int {
        return when(this) {
            is LocalId -> this.hashCode()
            is RemoteId -> this.hashCode()
        }
    }
}
