package dev.anli.entityocean

val MultipleChoice.options: List<Pair<String, String?>> get() = attrs.content.orderedOptions.map {
    it to attrs.content.options[it]
}

val Item.multipleChoice: MultipleChoice? get() = content.content.find {
    it is MultipleChoice
}?.let { it as MultipleChoice }