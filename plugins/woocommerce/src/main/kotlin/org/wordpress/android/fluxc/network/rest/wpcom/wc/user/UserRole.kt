package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

enum class UserRole(val value: String = "") {
    ADMINISTRATOR("administrator"),
    EDITOR("editor"),
    AUTHOR("author"),
    CONTRIBUTOR("contributor"),
    SUBSCRIBER("subscriber"),
    OTHER;

    companion object {
        private val valueMap = values().associateBy(UserRole::value)

        /**
         * Convert the base value into the associated UserRole object.
         * There are plugins available that can add custom roles.
         * So if we are unable to find a matching [UserRole] object, [OTHER] is returned.
         * This is not currently supported by our app.
         */
        fun fromValue(value: String) = valueMap[value] ?: OTHER
    }
}
