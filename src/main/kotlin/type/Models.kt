package dev.anli.entityocean.type

import com.beust.klaxon.JsonObject
import com.beust.klaxon.TypeFor
import dev.anli.entityocean.util.ContentTypeAdapter
import kotlin.reflect.KClass

data class Item(
    val id: String,
    val doc: Doc
)

@TypeFor(field = "type", adapter = ContentTypeAdapter::class)
open class Content(val type: String)

data class Doc(val content: List<Content>): Content("doc")

data class Paragraph(val content: List<Content> = listOf()): Content("paragraph")

data class Text(val text: String): Content("text")

data class Math(val attrs: Attrs): Content("math") {
    data class Attrs(val latex: String?)
}

interface AnswerField {
    val id: String
    val answerType: KClass<out Answer>
}

data class MultipleChoice(val attrs: Attrs): Content("multiple-choice"),
    AnswerField {
    data class Attrs(
        val id: String,
        val weight: Double,
        val content: Options,
        val scorableVersion: String
    )

    data class Options(
        val options: Map<String, String>,
        val orderedOptions: List<String>,
        val correctOptions: List<String>
    )

    override val id get() = attrs.id
    override val answerType get() = MultipleChoiceAnswer::class
}

data class ExpressionResponse(val attrs: Attrs): Content("expression"),
    AnswerField {
    data class Attrs(
        val id: String,
        val weight: Double,
        val content: AttrContent,
        val scorableVersion: String
    )

    data class AttrContent(
        val answerForms: List<AnswerForm>,
        val functionLetters: List<String>
    )

    data class AnswerForm(
        val considered: String,
        val latex: String,
        val sameForm: Boolean,
        val simplified: Boolean
    )

    override val id get() = attrs.id
    override val answerType = MultipleChoiceAnswer::class
}

data class LiveChallenge(
    val state: LiveItemState,
    val itemRevision: Item
)

interface Answer {
    val scorableState: JsonObject
    val normalizedState: JsonObject
}

data class MultipleChoiceAnswer(val optionsChosen: List<String>): Answer {
    override val scorableState: JsonObject
        get() = JsonObject(mapOf(
            "response" to mapOf(
                "optionsChosen" to optionsChosen
            ),
            "hintsTaken" to emptyList<Any?>(),
            "scorableVersion" to "0.1"
        ))
    override val normalizedState: JsonObject
        get() = JsonObject(mapOf(
            "type" to "multiple-choice",
            "response" to optionsChosen.firstOrNull()
        ))
}

class ExpressionAnswer: Answer {
    override val scorableState: JsonObject
        get() = TODO("Not yet implemented")
    override val normalizedState: JsonObject
        get() = TODO("Not yet implemented")
}