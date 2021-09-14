package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SSREntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localSiteId: Long? = null,
    val environment: String? = null,
    val database: String? = null,
    val active_plugins: String? = null,
    val theme: String? = null,
    val settings: String? = null,
    val security: String? = null,
    val pages: String? = null
)
