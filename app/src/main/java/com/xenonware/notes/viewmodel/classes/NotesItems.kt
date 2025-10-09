package com.xenonware.notes.viewmodel.classes

import kotlinx.serialization.Serializable

@Serializable
data class NotesItems(
    val id: Int,
    val title: String,
    val description: String? = null,
    val notificationCount: Int = 0,
    val attachmentCount: Int = 0,
    var listId: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    var displayOrder: Int = 0
) {
    var currentHeader = ""
}
