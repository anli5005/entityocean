package dev.anli.entityocean.util

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ItempoolCookieJar(var refreshToken: String?): CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val token = refreshToken
        return if (token != null) listOf(Cookie.Builder()
            .name(TOKEN_COOKIE)
            .value(token)
            .domain(url.host)
            .build()) else listOf()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        refreshToken = cookies.find { it.name == TOKEN_COOKIE }?.value
    }

    companion object {
        const val TOKEN_COOKIE = "jid"
    }
}