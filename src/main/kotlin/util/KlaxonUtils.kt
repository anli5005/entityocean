package dev.anli.entityocean.util

import com.beust.klaxon.TypeAdapter
import dev.anli.entityocean.type.*
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

internal class ContentTypeAdapter: TypeAdapter<Content> {
    override fun classFor(type: Any): KClass<out Content> {
        return when (type as String) {
            "doc" -> Doc::class
            "paragraph" -> Paragraph::class
            "text" -> Text::class
            "math" -> Math::class
            "multiple-choice" -> MultipleChoice::class
            "expression" -> ExpressionResponse::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }
}