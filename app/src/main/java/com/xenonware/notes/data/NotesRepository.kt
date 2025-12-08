// repository/NotesRepository.kt
package com.xenonware.notes.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.xenonware.notes.viewmodel.classes.NotesItems
import kotlinx.coroutines.tasks.await

class NotesRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notesCollection = db.collection("notes")

    suspend fun saveNote(userId: String, note: NotesItems) {
        notesCollection
            .document(userId)
            .collection("user_notes")
            .document(note.id.toString())
            .set(note)
            .await()
    }

    suspend fun getAllNotes(userId: String): List<NotesItems> {
        return try {
            notesCollection
                .document(userId)
                .collection("user_notes")
                .orderBy("creationTimestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(NotesItems::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteNote(userId: String, noteId: Int) {
        notesCollection
            .document(userId)
            .collection("user_notes")
            .document(noteId.toString())
            .delete()
            .await()
    }

    suspend fun updateNote(userId: String, note: NotesItems) {
        saveNote(userId, note) // Firestore upserts
    }
}