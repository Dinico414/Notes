package com.xenonware.notes.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.notes.SharedPreferenceManager
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class SortOption {
    FREE_SORTING,
    CREATION_DATE,
    NAME
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class NoteFilterType {
    ALL,
    TEXT,
    AUDIO,
    LIST,
    SKETCH
}

sealed class SnackbarEvent {
    data class ShowUndoDeleteSnackbar(val notesItems: NotesItems) : SnackbarEvent()
}


class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = SharedPreferenceManager(application.applicationContext)
    private val _allNotesItems = mutableStateListOf<NotesItems>()
    private val _displayedNotesItems = mutableStateListOf<Any>()
    val noteItems: List<Any> get() = _displayedNotesItems
    private var currentNoteId = 1

    private var recentlyDeletedItem: NotesItems? = null
    private var recentlyDeletedItemOriginalIndex: Int = -1


    private val _snackbarEvent = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent: SharedFlow<SnackbarEvent> = _snackbarEvent.asSharedFlow()


    private val _noteFilterType = MutableStateFlow(NoteFilterType.ALL)
    val noteFilterType: StateFlow<NoteFilterType> = _noteFilterType.asStateFlow()

    var currentSortOption: SortOption by mutableStateOf(SortOption.FREE_SORTING)
        private set
    var currentSortOrder: SortOrder by mutableStateOf(SortOrder.ASCENDING)
        private set

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    init {
        loadAllNotes()
        applySortingAndFiltering()
    }

    fun setNoteFilterType(filterType: NoteFilterType) {
        if (_noteFilterType.value != filterType) {
            _noteFilterType.value = filterType
            applySortingAndFiltering()
        }
    }

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            applySortingAndFiltering()
        }
    }

    private fun loadAllNotes() {
        currentSortOption = prefsManager.sortOption
        currentSortOrder = prefsManager.sortOrder
        val loadedNotes = prefsManager.notesItems
        _allNotesItems.clear()
        _allNotesItems.addAll(loadedNotes)
        currentNoteId = if (loadedNotes.isNotEmpty()) {
            (loadedNotes.maxOfOrNull { it.id } ?: 0) + 1
        } else {
            1
        }
    }

    fun saveAllNotes() {
        prefsManager.notesItems = _allNotesItems.toList()
    }

    private fun applySortingAndFiltering(preserveRecentlyDeleted: Boolean = false) {
        val currentRecentlyDeleted = if (preserveRecentlyDeleted) recentlyDeletedItem else null
        val tempAllNoteItems = _allNotesItems.toMutableList()
        if (currentRecentlyDeleted != null && !tempAllNoteItems.contains(currentRecentlyDeleted)) {
        }


        _displayedNotesItems.clear()
        var notesToDisplay = _allNotesItems.toList()

        val currentQuery = searchQuery.value
        if (currentQuery.isNotBlank()) {
            notesToDisplay = notesToDisplay.filter { note ->
                note.title.contains(currentQuery, ignoreCase = true) ||
                        (note.description?.contains(currentQuery, ignoreCase = true) == true)
            }
        }

        notesToDisplay = when (noteFilterType.value) {
            NoteFilterType.ALL -> notesToDisplay
            NoteFilterType.TEXT -> notesToDisplay.filter { it.noteType == NoteType.TEXT }
            NoteFilterType.AUDIO -> notesToDisplay.filter { it.noteType == NoteType.AUDIO }
            NoteFilterType.LIST -> notesToDisplay.filter { it.noteType == NoteType.LIST }
            NoteFilterType.SKETCH -> notesToDisplay.filter { it.noteType == NoteType.SKETCH }
        }

        val sortedNotes = sortNotes(notesToDisplay, currentSortOption, currentSortOrder)

        if (currentSortOption != SortOption.FREE_SORTING && sortedNotes.isNotEmpty()) {
            val groupedItems = mutableListOf<Any>()
            var lastHeader: String? = null

            for (note in sortedNotes) {
                note.currentHeader = getHeaderForNote(note, currentSortOption, currentSortOrder)
                if (note.currentHeader != lastHeader) {
                    groupedItems.add(note.currentHeader)
                    lastHeader = note.currentHeader
                }
                groupedItems.add(note)
            }
            _displayedNotesItems.addAll(groupedItems)
        } else {
            for (note in sortedNotes) {
                note.currentHeader = ""
            }
            _displayedNotesItems.addAll(sortedNotes)
        }
    }

    private fun getHeaderForNote(note: NotesItems, sortOption: SortOption, sortOrder: SortOrder): String {
        return when (sortOption) {
            SortOption.CREATION_DATE -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(note.creationTimestamp))
            }

            SortOption.NAME -> {
                note.title.firstOrNull()?.uppercaseChar()?.toString() ?: "Unknown"
            }

            SortOption.FREE_SORTING -> ""
        }
    }

    private fun sortNotes(
        notes: List<NotesItems>,
        option: SortOption,
        order: SortOrder,
    ): List<NotesItems> {
        val comparator: Comparator<NotesItems> = when (option) {
            SortOption.FREE_SORTING -> compareBy { it.displayOrder }
            SortOption.CREATION_DATE -> compareBy<NotesItems> { it.creationTimestamp }.thenBy { it.displayOrder }
            SortOption.NAME -> compareBy<NotesItems, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
                .thenBy { it.displayOrder }
        }

        return if (order == SortOrder.ASCENDING) {
            notes.sortedWith(comparator)
        } else {
            notes.sortedWith(comparator.reversed())
        }
    }

    fun setSortCriteria(option: SortOption, order: SortOrder) {
        if (currentSortOption != option || currentSortOrder != order) {
            currentSortOption = option
            currentSortOrder = order
            prefsManager.sortOption = option
            prefsManager.sortOrder = order
            applySortingAndFiltering()
        }
    }

    fun swapDisplayOrder(from: Int, to: Int) {
        val item1 = noteItems[from] as? NotesItems
        val item2 = noteItems[to] as? NotesItems
        if (item1 != null && item2 != null) {
            val tmp = item1.displayOrder
            item1.displayOrder = item2.displayOrder
            item2.displayOrder = tmp
        }
        _displayedNotesItems.add(to, _displayedNotesItems.removeAt(from))
    }

    private fun determineNextDisplayOrder(): Int {
        return _allNotesItems.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0
    }

    fun addItem(
        title: String,
        description: String? = null,
        noteType: NoteType = NoteType.TEXT
    ) {
        if (title.isNotBlank()) {
            val newItem = NotesItems(
                id = currentNoteId++,
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                listId = "", // Not used anymore
                creationTimestamp = System.currentTimeMillis(),
                displayOrder = determineNextDisplayOrder(),
                noteType = noteType
            )
            _allNotesItems.add(newItem)
            saveAllNotes()
            applySortingAndFiltering()
        }
    }

    fun prepareRemoveItem(itemId: Int) {
        val itemIndex = _allNotesItems.indexOfFirst { it.id == itemId }
        if (itemIndex != -1) {
            recentlyDeletedItem = _allNotesItems[itemIndex]
            recentlyDeletedItemOriginalIndex = itemIndex

            _allNotesItems.removeAt(itemIndex)
            applySortingAndFiltering(preserveRecentlyDeleted = true)

            viewModelScope.launch {
                recentlyDeletedItem?.let {
                    _snackbarEvent.emit(SnackbarEvent.ShowUndoDeleteSnackbar(it))
                }
            }
        }
    }

    fun undoRemoveItem() {
        recentlyDeletedItem?.let { itemToRestore ->
            if (recentlyDeletedItemOriginalIndex != -1 && recentlyDeletedItemOriginalIndex <= _allNotesItems.size) {
                _allNotesItems.add(recentlyDeletedItemOriginalIndex, itemToRestore)
            } else {
                _allNotesItems.add(itemToRestore)
            }
            recentlyDeletedItem = null
            recentlyDeletedItemOriginalIndex = -1
            applySortingAndFiltering()
        }
    }

    fun confirmRemoveItem() {
        if (recentlyDeletedItem != null) {
            saveAllNotes()
            recentlyDeletedItem = null
            recentlyDeletedItemOriginalIndex = -1
        }
    }

    fun updateItem(
        updatedItem: NotesItems,
    ) {
        val indexInAll = _allNotesItems.indexOfFirst { it.id == updatedItem.id }
        if (indexInAll != -1) {
            val currentItem = _allNotesItems[indexInAll]
            _allNotesItems[indexInAll] = updatedItem.copy(
                listId = currentItem.listId,
                creationTimestamp = currentItem.creationTimestamp,
                displayOrder = currentItem.displayOrder
            )
            saveAllNotes()
            applySortingAndFiltering()
        }
    }

    fun deleteItems(itemIds: List<Int>) {
        val itemsWereRemoved = _allNotesItems.removeAll { it.id in itemIds }
        if (itemsWereRemoved) {
            if (recentlyDeletedItem?.id in itemIds) {
                recentlyDeletedItem = null
                recentlyDeletedItemOriginalIndex = -1
            }
            saveAllNotes()
            applySortingAndFiltering()
        }
    }

    fun clearNotesForList(listIdToClear: String) {
        if (recentlyDeletedItem?.listId == listIdToClear) {
            recentlyDeletedItem = null
            recentlyDeletedItemOriginalIndex = -1
        }
        val notesWereRemoved = _allNotesItems.removeAll { it.listId == listIdToClear }
        if (notesWereRemoved) {
            saveAllNotes()
            applySortingAndFiltering()
        }
    }


    fun moveItemInFreeSort(itemIdToMove: Int, newDisplayOrderCandidate: Int) {
        if (currentSortOption != SortOption.FREE_SORTING) {
            System.err.println("Manual reordering only available in FREE_SORTING mode.")
            return
        }

        val itemsInList = _allNotesItems.sortedBy { it.displayOrder }.toMutableList()

        val itemToMoveIndex = itemsInList.indexOfFirst { it.id == itemIdToMove }
        if (itemToMoveIndex == -1) {
            System.err.println("Item to move not found in the current list.")
            return
        }

        val item = itemsInList.removeAt(itemToMoveIndex)
        val targetIndex = newDisplayOrderCandidate.coerceIn(0, itemsInList.size)
        itemsInList.add(targetIndex, item)

        itemsInList.forEachIndexed { newOrder, noteItem ->
            val originalNoteIndexInAll = _allNotesItems.indexOfFirst { it.id == noteItem.id }
            if (originalNoteIndexInAll != -1) {
                if (_allNotesItems[originalNoteIndexInAll].displayOrder != newOrder) {
                    _allNotesItems[originalNoteIndexInAll] = _allNotesItems[originalNoteIndexInAll].copy(displayOrder = newOrder)
                }
            }
        }

        saveAllNotes()
        applySortingAndFiltering()
    }

    companion object {
        const val DEFAULT_LIST_ID = "default_list"
    }
}
