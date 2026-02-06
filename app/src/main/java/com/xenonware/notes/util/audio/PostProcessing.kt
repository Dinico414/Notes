package com.xenonware.notes.util.audio

fun postProcessText(raw: String): String {
    if (raw.isBlank()) return raw.trim()

    var text = raw.trim()

    text = heuristischNomenGross(text)

    if (text.isNotEmpty() && text[0].isLowerCase()) {
        text = text.replaceFirstChar { it.uppercaseChar() }
    }

    return text.trim()
}

private fun heuristischNomenGross(text: String): String {
    val stopWords = setOf(
        "der", "die", "das", "den", "dem", "ein", "eine", "ich", "du", "er", "sie", "es",
        "wir", "und", "oder", "in", "für", "mit", "auf", "zu", "aus", "bei", "nach",
        "ist", "sind", "hat", "haben", "wird", "kann", "muss", "als", "wenn", "dass"
    ).map { it.lowercase() }

    return text.split(" ").joinToString(" ") { word ->
        if (word.isEmpty()) word
        else if (word[0].isLowerCase() &&
            word.length >= 4 &&
            word.lowercase() !in stopWords) {
            word.replaceFirstChar { it.uppercaseChar() }
        } else word
    }
}
