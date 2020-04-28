package dev.anli.entityocean

import com.beust.klaxon.TypeAdapter
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
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }
}