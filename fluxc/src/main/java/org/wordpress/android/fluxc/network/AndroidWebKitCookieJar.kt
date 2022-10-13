package org.wordpress.android.fluxc.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.internal.cookieToString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfig

class AndroidWebKitCookieJar(private val cookieManager: CookieManager) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            cookieManager.setCookie(url.toString(), cookieToString(cookie, true))
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = cookieManager.getCookie(url.toString())?.split(";") ?: return emptyList()

        return cookies.mapNotNull { cookie ->
            Cookie.parse(url, cookie.trim()).also {
                if (it == null) {
                    AppLog.w(
                        AppLog.T.UTILS, "Parsing Cookie failed" +
                                if (BuildConfig.DEBUG) ", Cookie: $cookie" else ""
                    )
                }
            }
        }
    }
}