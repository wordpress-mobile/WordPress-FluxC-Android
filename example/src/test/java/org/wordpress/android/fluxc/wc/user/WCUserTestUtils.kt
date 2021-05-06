package org.wordpress.android.fluxc.wc.user

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient.UserApiResponse

object WCUserTestUtils {
    fun generateSampleUApiResponse(): UserApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/store-user-role.json")
        val responseType = object : TypeToken<UserApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? UserApiResponse
    }
}
