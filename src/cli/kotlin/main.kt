@file:JvmName("CLIMain")

package dev.anli.entityocean.cli

import dev.anli.entityocean.ItempoolClient
import dev.anli.entityocean.options
import dev.anli.entityocean.refreshToken
import dev.anli.entityocean.type.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

fun printContents(contents: List<Content>, attempts: List<Attempt>) {
    contents.forEach { content ->
        when (content) {
            is Text -> print(content.text)
            is Math -> print("\u001b[1m${content.attrs.latex ?: "\u001b[31m[math]\u001b[0m"}\u001b[0m")
            is Paragraph -> {
                printContents(content.content, attempts)
                println("")
            }
            is MultipleChoice -> {
                println("\u001b[36;1mMultiple Choice:\u001b[0m")
                val selected = (attempts.firstOrNull()?.answers?.get(content.id) as? MultipleChoiceAnswer)?.optionsChosen
                content.options.forEachIndexed { index, pair ->
                    val (id, name) = pair
                    if (selected?.contains(id) == true) {
                        println("\u001b[34;1m${index + 1}. ${name ?: "???"} \u001B[0m\u001b[30;1m($id)\u001b[0m")
                    } else {
                        println("\u001b[36;1m${index + 1}.\u001b[0m ${name ?: "\u001b[31;1m???\u001b[0m"} \u001b[30;1m($id)\u001b[0m")
                    }
                }
            }
            is ExpressionResponse -> println("Free Response")
            else -> print("\u001b[33;1m[${content.type}]\u001b[0m")
        }
    }
}

fun printChallenge(challenge: LiveChallenge?, attempts: List<Attempt>) {
    if (challenge == null) {
        println("Challenge not found.")
        return
    }

    println("\u001b[" + when (challenge.state) {
        LiveItemState.UNSTARTED -> "30;1mUnstarted"
        LiveItemState.IN_PROGRESS -> "34;1mIn Progress"
        LiveItemState.EVALUATED -> "32;1mComplete"
        LiveItemState.UNKNOWN__ -> "31mUnknown"
    } + "\u001b[0m")

    printContents(challenge.itemRevision.doc.content, attempts)
}

fun main() {
    val host = "itempool.com"
    val liveId = "777f4119-ba9d-4ab8-a2c4-f233304c231d"
    val authFile = Paths.get(System.getProperty("user.home"), ".entityoceanconfig").toString()

    println("\u001b[1mEntityocean CLI\u001b[0m")
    println("\u001b[36m\u001b[1mDomain:\u001b[0m $host")
    println("\u001b[36m\u001b[1mLive ID:\u001b[0m $liveId")
    println("\u001b[36m\u001b[1mRefresh Token:\u001b[0m Stored at \u001B[4m$authFile\u001B[0m")
    println("")

    val fileToken = try {
        val reader = File(authFile).bufferedReader()
        val line = reader.readLine()
        reader.close()
        line
    } catch (_: Exception) {
        println("Unable to read refresh token.")
        null
    }

    val client = ItempoolClient("itempool.com", fileToken)

    @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
    runBlocking {
        try {
            if (client.refreshToken == null) {
                println("Fetching new refresh token...")
                client.obtainRefreshToken()
            }

            println("Fetching access token...")
            client.obtainAccessToken()

            if (client.refreshToken != fileToken && client.refreshToken != null) {
                println("Saving new refresh token...")
                try {
                    File(authFile).writeText(client.refreshToken!!)
                } catch (e: Exception) {
                    println("\u001b[31;1mUnable to save refresh token.\u001b[0m")
                }
            }

            println("Authentication complete.\n")

            var challenge = client.liveChallenge(liveId)
            var itemAttempts = challenge?.let { client.myAttempts(it.itemRevision.id) }
            printChallenge(challenge, itemAttempts ?: emptyList())
            println("\nYou are now in the Entityocean shell. Type \u001b[1mhelp\u001b[0m for help.")
            loop@ while (true) {
                print("\u001b[36mentityocean> \u001b[0m")
                val input = readLine() ?: break

                when (input.substringBefore(" ")) {
                    "choose" -> {
                        val mc = challenge?.itemRevision?.doc?.content?.firstOrNull { it is MultipleChoice } as? MultipleChoice

                        if (mc == null) {
                            println("\u001b[31;1mThis command can only be used in a multiple choice question\u001b[0m")
                        } else {
                            val text = input.substringAfter(" ")
                            val choice = text.toIntOrNull()
                            if (choice == null) {
                                println("\u001b[31;1m$text is not a number\u001b[0m")
                            } else {
                                if (mc.options.indices.contains(choice - 1)) {
                                    val (id, name) = mc.options[choice - 1]
                                    println("Submitting ${name ?: ""}")
                                    client.submitAnswer(challenge!!.itemRevision.id, mapOf(mc.id to MultipleChoiceAnswer(listOf(id))))
                                } else {
                                    println("\u001b[31;1m$choice is not an option\u001b[0m")
                                }
                            }
                        }
                    }
                    "refresh" -> {
                        println("Refreshing...\n")
                        challenge = client.liveChallenge(liveId)
                        itemAttempts = challenge?.let { client.myAttempts(it.itemRevision.id) }
                        printChallenge(challenge, itemAttempts ?: emptyList())
                        println("")
                    }
                    "help" -> println("""
                        === Entityocean CLI Help ===
                        choose: Answers a multiple choice question.
                        refresh: Refreshes the question.
                        help: Prints this message.
                        quit: Exits.
                    """.trimIndent())
                    "quit" -> break@loop
                    else -> println("Unrecognized command. Type \u001b[1mhelp\u001b[0m for a list of available commands.")
                }
            }
        } catch (e: Exception) {
            println("\n\u001b[1m\u001b[31;1mAn error occurred.\u001b[0m")
            e.printStackTrace()
            exitProcess(1)
        }

        exitProcess(0)
    }
}