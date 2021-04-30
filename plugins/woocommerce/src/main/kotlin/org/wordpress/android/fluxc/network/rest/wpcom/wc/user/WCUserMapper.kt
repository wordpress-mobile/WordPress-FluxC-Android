package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse
import javax.inject.Inject

class WCUserMapper
@Inject constructor() {
    fun map(user: UserApiResponse): List<UserRole> {
        val userRoles = mutableListOf<UserRole>()
        user.roles.map { userRoles.add(UserRole.fromValue(it)) }
        return userRoles
    }
}
