package org.wordpress.android.fluxc.model.user

data class WCUserModel(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    var roles: List<WCUserRole>
)
