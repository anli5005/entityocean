package dev.anli.entityocean

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.coroutines.toDeferred

class ItempoolClient(val url: String) {
    val client: ApolloClient = ApolloClient.builder()
        .useHttpGetMethodForQueries(false)
        .serverUrl(url)
        .build()

    suspend fun getCurrentChallenge(liveId: String) {
        client.query(LiveChallengeQuery(liveId)).toDeferred().await()
    }
}