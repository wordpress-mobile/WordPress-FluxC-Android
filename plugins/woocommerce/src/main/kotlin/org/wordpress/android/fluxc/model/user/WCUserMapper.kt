package org.wordpress.android.fluxc.model.user

import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse
import javax.inject.Inject

class WCUserMapper
@Inject constructor() {
    fun map(user: UserApiResponse): WCUserModel {
        return WCUserModel(
                id = user.id,
                username = user.username ?: "",
                firstName = user.firstName ?: "",
                lastName = user.lastName ?: "",
                email = user.email ?: "",
                roles = user.roles.map { WCUserRole.fromValue(it) }
        )
    }
}
