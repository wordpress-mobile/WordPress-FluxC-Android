package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse
import javax.inject.Inject

class WCUserMapper
@Inject constructor() {
    fun map(user: UserApiResponse): UserRole {
        return UserRole.fromValue(user.roles[0]) ?: UserRole.OTHER
    }
}
