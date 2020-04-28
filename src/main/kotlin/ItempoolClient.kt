package dev.anli.entityocean

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred
import com.beust.klaxon.*
import dev.anli.entityocean.type.Doc
import dev.anli.entityocean.type.Item
import dev.anli.entityocean.type.LiveChallenge

class ItempoolClient(val url: String) {
    val client: ApolloClient = ApolloClient.builder()
        .useHttpGetMethodForQueries(false)
        .serverUrl(url)
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
}