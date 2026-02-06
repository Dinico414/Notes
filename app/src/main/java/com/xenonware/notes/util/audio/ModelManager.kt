package com.xenonware.notes.util.audio

data class ModelInfo(
    val key: String,
    val lang: String,
    val name: String,
    val zipName: String,
    val folderName: String,
)

val AVAILABLE_MODELS = listOf(
    ModelInfo(
        key = "en",
        lang = "en",
        name = "English",
        zipName = "vosk-model-small-en-us-0.15.zip",
        folderName = "vosk-model-small-en-us-0.15",
    ), ModelInfo(
        key = "de",
        lang = "de",
        name = "German",
        zipName = "vosk-model-small-de-0.15.zip",
        folderName = "vosk-model-small-de-0.15",
    )
)