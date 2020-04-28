package dev.anli.entityocean

import com.beust.klaxon.TypeFor
import dev.anli.entityocean.type.LiveItemState

data class Item(
    val id: String,
    val content: Doc
)

@TypeFor(field = "type", adapter = ContentTypeAdapter::class)
open class Content(val type: String)

data class Doc(val content: List<Content>): Content("doc")

data class Paragraph(val content: List<Content>): Content("paragraph")

data class Text(val text: String): Content("text")

data class Math(val attrs: Attrs): Content("math") {
    data class Attrs(val latex: String?)
}

data class MultipleChoice(val attrs: Attrs): Content("multiple-choice") {
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
}

/* data class FreeResponse(val attrs: Attrs): Content("expression") {
    data class Attrs(
        val id: String,
        val weight: Double,
        val content: AttrContent,
    )

    data class AttrContent(val answerForms: List<AnswerForm>)

    data class AnswerForm(

    )
} */

data class LiveChallenge(
    val state: LiveItemState,
    val itemRevision: Item
)