package com.xenonware.notes.viewmodel.classes

import kotlinx.serialization.Serializable

enum class NoteType {
    TEXT, AUDIO, LIST, SKETCH
}

@Serializable
data class NotesItems(
    val id: Int,
    val title: String,
    val description: String? = null,
    var listId: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    var displayOrder: Int = 0,
    val noteType: NoteType = NoteType.TEXT,
    val color: Long? = null
) {
    var currentHeader = ""
}