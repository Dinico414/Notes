package com.xenonware.notes.util

data class ModelInfo(
    val key: String,              // "en-small", "en-large", …
    val lang: String,             // "en" or "de"
    val name: String,             // user-visible name
    val zipName: String,          // filename used in download URL
    val folderName: String,       // folder name after extraction
    val approxSizeMB: Int         // approximate size shown to user
)

val AVAILABLE_MODELS = listOf(
    ModelInfo(
        key = "en-small",
        lang = "en",
        name = "Small English (fast, lower accuracy)",
        zipName = "vosk-model-small-en-us-0.15.zip",
        folderName = "vosk-model-small-en-us-0.15",
        approxSizeMB = 40
    ),
    ModelInfo(
        key = "en-large",
        lang = "en",
        name = "Large English (high accuracy)",
        zipName = "vosk-model-en-us-0.22-lgraph.zip",
        folderName = "vosk-model-en-us-0.22-lgraph",
        approxSizeMB = 1000
    ),
    ModelInfo(
        key = "de-small",
        lang = "de",
        name = "Small German (fast, lower accuracy)",
        zipName = "vosk-model-small-de-zamia-0.3.zip",
        folderName = "vosk-model-small-de-zamia-0.3",
        approxSizeMB = 50
    ),
    ModelInfo(
        key = "de-large",
        lang = "de",
        name = "Large German (high accuracy)",
        zipName = "vosk-model-de-0.21.zip",
        folderName = "vosk-model-de-0.21",
        approxSizeMB = 1900
    )
)