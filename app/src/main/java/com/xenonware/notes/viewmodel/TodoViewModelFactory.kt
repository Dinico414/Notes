package com.xenonware.notes.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TodoViewModelFactory(
    private val application: Application,
    private val notesViewModel: NotesViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(application, notesViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ${modelClass.name}")
    }
}