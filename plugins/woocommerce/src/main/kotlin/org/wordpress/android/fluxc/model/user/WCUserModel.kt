package org.wordpress.android.fluxc.model.user

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCUserModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteUserId: Long = 0L
    @Column var firstName: String = ""
    @Column var lastName: String = ""
    @Column var username: String = ""
    @Column var email: String = ""
    @Column var roles: String = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    fun getUserRoles(): ArrayList<WCUserRole> {
        val userRoles = ArrayList<WCUserRole>()
        if (roles.isNotEmpty()) {
            Gson().fromJson(roles, JsonElement::class.java).asJsonArray.forEach {
                userRoles.add(WCUserRole.valueOf(it.asString))
            }
        }
        return userRoles
    }
}
