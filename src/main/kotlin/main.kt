package dev.anli.entityocean

import kotlinx.coroutines.runBlocking

internal fun main() {
    val client = ItempoolClient("https://api.itempool.com/graphql")

    runBlocking {
        val challenge = client.liveChallenge("777f4119-ba9d-4ab8-a2c4-f233304c231d")
        challenge?.itemRevision?.multipleChoice?.options?.let { options ->
            println("Options: ${options.map { it.second }.joinToString(", ")}")
        }
    }
}