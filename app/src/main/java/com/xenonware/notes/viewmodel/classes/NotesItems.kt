@file:Suppress("DEPRECATION")

package com.xenonware.notes.viewmodel.classes

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

enum class NoteType {
    TEXT, AUDIO, LIST, SKETCH
}
@Serializable
data class NotesItems(
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val transcript: String? = null,
    val notificationCount: Int = 0,
    val attachmentCount: Int = 0,
    var listId: String = "",
    val creationTimestamp: Long = System.currentTimeMillis(),
    var displayOrder: Int = 0,
    val noteType: NoteType = NoteType.TEXT,
    val color: Long? = null,
    val themeName: String? = null,
    val labels: List<String> = emptyList(),
    val labelId: String? = null,
    val audioUrl: String? = null,
    val localAudioId: String? = null,
    // CRITICAL FIX: Use @PropertyName + var
    @PropertyName("isOffline")
    var isOffline: Boolean = false
) {
    // Keep constructor for backward compatibility
    constructor() : this(
        id = 0,
        title = "",
        description = null,
        listId = "",
        creationTimestamp = System.currentTimeMillis(),
        displayOrder = 0,
        noteType = NoteType.TEXT,
        color = null,
        labels = emptyList(),
        isOffline = false
    )

    var currentHeader = ""
}