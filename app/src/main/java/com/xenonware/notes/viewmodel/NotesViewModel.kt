package com.xenonware.notes.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.notes.SharedPreferenceManager
import com.xenonware.notes.ui.theme.blueInversePrimaryLight
import com.xenonware.notes.ui.theme.greenInversePrimaryLight
import com.xenonware.notes.ui.theme.orangeInversePrimaryLight
import com.xenonware.notes.ui.theme.purpleInversePrimaryLight
import com.xenonware.notes.ui.theme.redInversePrimaryLight
import com.xenonware.notes.ui.theme.turquoiseInversePrimaryLight
import com.xenonware.notes.ui.theme.yellowInversePrimaryLight
import com.xenonware.notes.viewmodel.classes.Label
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
import java.util.UUID

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

enum class NotesLayoutType {
    LIST,
    GRID
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

    private val _notesLayoutType = MutableStateFlow(NotesLayoutType.LIST)
    val notesLayoutType: StateFlow<NotesLayoutType> = _notesLayoutType.asStateFlow()

    private val _gridColumnCount = MutableStateFlow(prefsManager.gridColumnCount)
    val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()

    private val _listItemLineCount = MutableStateFlow(prefsManager.listItemLineCount)
    val listItemLineCount: StateFlow<Int> = _listItemLineCount.asStateFlow()

    var currentSortOption: SortOption by mutableStateOf(SortOption.FREE_SORTING)
        private set
    var currentSortOrder: SortOrder by mutableStateOf(SortOrder.ASCENDING)
        private set

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _editingAudioNoteColor = MutableStateFlow<Long?>(null)
    val editingAudioNoteColor: StateFlow<Long?> = _editingAudioNoteColor.asStateFlow()

    private val _selectedColors = MutableStateFlow<Set<Long?>>(emptySet())
    val selectedColors: StateFlow<Set<Long?>> = _selectedColors.asStateFlow()

    private val _labels = MutableStateFlow<List<Label>>(emptyList())
    val labels: StateFlow<List<Label>> = _labels.asStateFlow()

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()


    init {
        loadAllNotes()
        loadLabels()
        applySortingAndFiltering()
    }

    fun setNotesLayoutType(layoutType: NotesLayoutType) {
        if (_notesLayoutType.value != layoutType) {
            _notesLayoutType.value = layoutType
            prefsManager.notesLayoutType = layoutType
        }
    }

    fun cycleGridColumnCount(screenWidthDp: Int) {
        val nextColumnCount = when (screenWidthDp) {
            in 0 until 600 -> // Compact/Small (assuming < 600dp)
                when (_gridColumnCount.value) {
                    2 -> 3
                    3 -> 2
                    else -> 2 // Default if somehow outside expected range
                }
            in 600 until 840 -> // Medium (assuming 600dp to 839dp)
                when (_gridColumnCount.value) {
                    3 -> 4
                    4 -> 5
                    5 -> 3
                    else -> 3
                }
            else -> // Expanded (assuming >= 840dp)
                when (_gridColumnCount.value) {
                    4 -> 5
                    5 -> 6
                    6 -> 4
                    else -> 4
                }
        }
        if (_gridColumnCount.value != nextColumnCount) {
            _gridColumnCount.value = nextColumnCount
            prefsManager.gridColumnCount = nextColumnCount
        }
    }

    fun cycleListItemLineCount() {
        val nextLineCount = when (_listItemLineCount.value) {
            3 -> 9
            9 -> MAX_LINES_FULL_NOTE // Representing "entire file"
            MAX_LINES_FULL_NOTE -> 3
            else -> 3
        }
        if (_listItemLineCount.value != nextLineCount) {
            _listItemLineCount.value = nextLineCount
            prefsManager.listItemLineCount = nextLineCount
        }
    }

    fun setNoteFilterType(filterType: NoteFilterType) {
        if (_noteFilterType.value != filterType) {
            _noteFilterType.value = filterType
            applySortingAndFiltering()
        }
    }

    fun toggleColorFilter(color: Long?) {
        val currentColors = _selectedColors.value.toMutableSet()
        if (currentColors.contains(color)) {
            currentColors.remove(color)
        } else {
            currentColors.add(color)
        }
        _selectedColors.value = currentColors
        applySortingAndFiltering()
    }

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            applySortingAndFiltering()
        }
    }

    private fun loadLabels() {
        _labels.value = prefsManager.labels
    }

    private fun saveLabels() {
        prefsManager.labels = _labels.value
    }

    fun addLabel(labelText: String) {
        if (labelText.isNotBlank()) {
            val newLabel = Label(id = UUID.randomUUID().toString(), text = labelText.trim())
            _labels.value = _labels.value + newLabel
            saveLabels()
        }
    }

    fun removeLabel(labelId: String) {
        _labels.value = _labels.value.filter { it.id != labelId }
        saveLabels()
    }

    fun setLabelFilter(labelId: String?) {
        _selectedLabel.value = if (_selectedLabel.value == labelId) null else labelId
        applySortingAndFiltering()
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

        val currentSelectedColors = _selectedColors.value
        if (currentSelectedColors.isNotEmpty()) {
            notesToDisplay = notesToDisplay.filter { note ->
                if (currentSelectedColors.contains(null)) {
                    // If 'null' is selected, include notes with no color AND notes with any of the other selected colors
                    note.color == null || currentSelectedColors.contains(note.color)
                } else {
                    // Only filter by actual colors if 'null' is not selected
                    currentSelectedColors.contains(note.color)
                }
            }
        }

        val currentSelectedLabel = _selectedLabel.value
        if (currentSelectedLabel != null) {
            notesToDisplay = notesToDisplay.filter { note ->
                note.labels.contains(currentSelectedLabel)
            }
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

//    private val database = Firebase.database
//
//    fun saveNote(note: ContactsContract.CommonDataKinds.Note) {
//        val myRef = database.getReference("notes")
//        myRef.child(note.id).setValue(note)
//    }

    fun addItem(
        title: String,
        description: String? = null,
        noteType: NoteType = NoteType.TEXT,
        color: Long? = null,
        labels: List<String> = emptyList()
    ) {
        if (title.isNotBlank()) {
            val newItem = NotesItems(
                id = currentNoteId++,
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                listId = "", // Not used anymore
                creationTimestamp = System.currentTimeMillis(),
                displayOrder = determineNextDisplayOrder(),
                noteType = noteType,
                color = color,
                labels = labels
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
                displayOrder = currentItem.displayOrder,
                color = updatedItem.color // Explicitly include color from updatedItem
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

    fun updateEditingAudioNoteColor(color: Long?) {
        _editingAudioNoteColor.value = color
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
        const val MAX_LINES_FULL_NOTE = -1

        // Define color constants here to ensure consistency
        val COLOR_RED = redInversePrimaryLight.toArgb().toLong()
        val COLOR_ORANGE = orangeInversePrimaryLight.toArgb().toLong()
        val COLOR_YELLOW = yellowInversePrimaryLight.toArgb().toLong()
        val COLOR_GREEN = greenInversePrimaryLight.toArgb().toLong()
        val COLOR_TURQUOISE = turquoiseInversePrimaryLight.toArgb().toLong()
        val COLOR_BLUE = blueInversePrimaryLight.toArgb().toLong()
        val COLOR_PURPLE = purpleInversePrimaryLight.toArgb().toLong()
    }
}
