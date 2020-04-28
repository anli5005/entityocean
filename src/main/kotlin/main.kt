package dev.anli.entityocean

import dev.anli.entityocean.type.*
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

internal fun main() {
    val client = ItempoolClient("api.itempool.com")

    runBlocking<Nothing> {
        client.obtainTokens()
        println("Obtained tokens")

        val challenge: LiveChallenge?
        try {
            challenge = client.liveChallenge("777f4119-ba9d-4ab8-a2c4-f233304c231d")
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }

        if (challenge == null) {
            println("Challenge not found or unobtainable")
        } else {
            val item = challenge.itemRevision
            println("ID: ${item.id}")
            println("State: ${challenge.state.rawValue}")

            val doc = item.doc
            val text = doc.content.flatMap { content ->
                if (content is Paragraph) content.content.map {
                    when (it) {
                        is Text -> it.text
                        is Math -> "$${it.attrs.latex}$"
                        else -> "<${it.type}>"
                    }
                } else listOf()
            }.joinToString("")
            println("Text: $text")

            item.answerField?.let { field ->
                when (field) {
                    is MultipleChoice -> {
                        println("Type: Multiple Choice")
                        println("Options: ${field.options.joinToString(", ") { it.second ?: "<${it.first}>" }}")
                    }
                    is ExpressionResponse -> {
                        println("Type: Expression Response")
                    }
                }
            }
        }

        exitProcess(0)
    }
}