package dev.anli.entityocean

val MultipleChoice.options: List<Pair<String, String?>> get() = attrs.content.orderedOptions.map {
    it to attrs.content.options[it]
}

val Item.answerField: AnswerField? get() = doc.content.find {
    it is AnswerField
}?.let { it as AnswerField }