package dev.anli.entityocean

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import com.beust.klaxon.*
import dev.anli.entityocean.type.Doc
import dev.anli.entityocean.type.Item
import dev.anli.entityocean.type.LiveChallenge
import dev.anli.entityocean.util.ItempoolCookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.internal.EMPTY_REQUEST
import okhttp3.internal.EMPTY_RESPONSE
import ru.gildor.coroutines.okhttp.await

class ItempoolClient(val host: String, refreshToken: String? = null) {
    var accessToken: String? = null

    val jar = ItempoolCookieJar(refreshToken)

    val http = OkHttpClient.Builder()
        .cookieJar(jar)
        .addInterceptor { chain ->
            chain.proceed(if (accessToken == null) chain.request() else chain.request().newBuilder()
                .header("Authorization", "Bearer $accessToken").build())
        }
        .build()

    val client: ApolloClient = ApolloClient.builder()
        .okHttpClient(http)
        .useHttpGetMethodForQueries(false)
        .serverUrl(HttpUrl.Builder()
            .scheme("https")
            .host(host)
            .addPathSegment("graphql")
            .build())
        .build()

    val klaxon = Klaxon()

    suspend fun liveChallenge(liveId: String): LiveChallenge? {
        val query = LiveChallengeQuery(liveId)
        val data = client.query(query).toDeferred().await().data

        return data?.let {
            val content = klaxon.parse<Doc>(it.liveChallenge.itemRevision.itemDoc) ?: return null

            LiveChallenge(
                it.liveChallenge.liveItemState, Item(
                    it.liveChallenge.itemRevision.id,
                    content
                )
            )
        }
    }

    suspend fun obtainRefreshToken() {
        val query = GetRefreshTokenQuery()
        client.query(query).toDeferred().await()
    }

    data class AccessTokenResponse(val ok: Boolean, val accessToken: String)

    suspend fun obtainAccessToken(): AccessTokenResponse? {
        val body = EMPTY_REQUEST
        val response = http.newCall(Request.Builder().url(HttpUrl.Builder()
            .scheme("https")
            .host(host)
            .addPathSegment("refresh_token")
            .build()).method("POST", body).build()).await()
        val res: AccessTokenResponse? = response.body?.let {
            klaxon.parse(it.charStream())
        }
        accessToken = res?.run { if (ok) accessToken else null }
        return res
    }
}

var ItempoolClient.refreshToken
    get() = jar.refreshToken
    set(value) {
        jar.refreshToken = value
    }

suspend fun ItempoolClient.obtainTokens() {
    obtainRefreshToken()
    obtainAccessToken()
}