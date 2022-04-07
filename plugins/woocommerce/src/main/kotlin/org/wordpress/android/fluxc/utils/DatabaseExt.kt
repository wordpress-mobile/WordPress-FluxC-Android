package org.wordpress.android.fluxc.utils

import androidx.room.RoomDatabase

suspend fun RoomDatabase.runInTransactionAsync(block: suspend () -> Unit) {
    block()
}
