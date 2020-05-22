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

class Image: Content("image") // TODO

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

interface AnswerFactory<TAnswer: Answer> {
    fun from(scorableState: JsonObject): TAnswer?
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

    companion object Factory: AnswerFactory<MultipleChoiceAnswer> {
        @JvmStatic override fun from(scorableState: JsonObject): MultipleChoiceAnswer? {
            return MultipleChoiceAnswer(scorableState.obj("response")?.array("optionsChosen") ?: return null)
        }
    }
}

data class ExpressionAnswer(val latex: String): Answer {
    override val scorableState: JsonObject
        get() = JsonObject(mapOf(
            "response" to mapOf(
                "latex" to latex
            ),
            "hintsTaken" to emptyList<Any?>(),
            "scorableVersion" to "0.1"
        ))
    override val normalizedState: JsonObject
        get() = JsonObject(mapOf(
            "type" to "expression",
            "response" to latex
        ))

    companion object Factory: AnswerFactory<ExpressionAnswer> {
        @JvmStatic override fun from(scorableState: JsonObject): ExpressionAnswer? {
            return ExpressionAnswer(scorableState.obj("response")?.string("latex") ?: return null)
        }
    }
}

data class Attempt(val id: String, val answers: Map<String, Answer?>, val normalizedScore: Double, val complete: Boolean, val evaluated: Boolean)