package dev.anli.entityocean

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.internal.firstNotNullResult
import dev.anli.entityocean.type.*
import dev.anli.entityocean.util.ItempoolCookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.EMPTY_REQUEST
import ru.gildor.coroutines.okhttp.await
import java.util.*

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
            .addPathSegment("api")
            .addPathSegment("graphql")
            .build())
        .build()

    val klaxon = Klaxon()

    var factories = listOf(MultipleChoiceAnswer.Factory, ExpressionAnswer.Factory)

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
            .addPathSegment("api")
            .addPathSegment("refresh_token")
            .build()).method("POST", body).build()).await()
        val res: AccessTokenResponse? = response.body?.let {
            klaxon.parse(it.charStream())
        }
        accessToken = res?.run { if (ok) accessToken else null }
        return res
    }

    suspend fun myAttempts(itemId: String): List<Attempt>? {
        val query = GetAttemptsQuery(itemId)
        val data = client.query(query).toDeferred().await().data

        return data?.let { d ->
            d.myItemAttempts.map { myItemAttempt ->
                val scorableStates: Map<String, JsonObject>? = klaxon.parse(myItemAttempt.scorableStates)

                Attempt(
                    id = myItemAttempt.id,
                    answers = scorableStates?.mapValues { entry -> factories.firstNotNullResult { it.from(entry.value) } } ?: emptyMap(),
                    normalizedScore = myItemAttempt.normalizedScore,
                    complete = myItemAttempt.complete,
                    evaluated = myItemAttempt.evaluated
                )
            }
        }
    }

    suspend fun submitAnswer(itemId: String, answers: Map<String, Answer>): String? {
        val scorableStates = JsonObject(answers.mapValues { it.value.scorableState })
        val normalizedAttempt = JsonArray(answers.map { it.value.normalizedState })

        val mutation = AnswerMutation(ItemAttemptInput(
            id = UUID.randomUUID().toString(),
            itemRevisionId = itemId,
            widgetStates = "{}",
            scorableStates = scorableStates.toJsonString(),
            complete = true,
            evaluated = false,
            normalizedScore = 0.0,
            normalizedAttempt = normalizedAttempt.toJsonString(),
            apiVersion = "0.1"
        ))

        val res = client.mutate(mutation).toDeferred().await()
        val errors = res.errors
        if (errors?.isNotEmpty() == true) {
            throw Exception(errors.first().message)
        }

        return res.data?.updateItemAttempt?.id
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