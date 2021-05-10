package org.wordpress.android.fluxc.model.user

import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse
import javax.inject.Inject

class WCUserMapper
@Inject constructor() {
    fun map(user: UserApiResponse): List<WCUserRole> {
        val userRoles = mutableListOf<WCUserRole>()
        user.roles.map { userRoles.add(WCUserRole.fromValue(it)) }
        return userRoles
    }
}
