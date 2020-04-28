package dev.anli.entityocean.type

import com.beust.klaxon.TypeFor
import dev.anli.entityocean.util.ContentTypeAdapter

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
}

data class LiveChallenge(
    val state: LiveItemState,
    val itemRevision: Item
)