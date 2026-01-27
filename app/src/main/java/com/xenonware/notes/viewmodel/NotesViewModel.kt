package com.xenonware.notes.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.ui.theme.noteBlueLight
import com.xenonware.notes.ui.theme.noteGreenLight
import com.xenonware.notes.ui.theme.noteOrangeLight
import com.xenonware.notes.ui.theme.notePurpleLight
import com.xenonware.notes.ui.theme.noteRedLight
import com.xenonware.notes.ui.theme.noteTurquoiseLight
import com.xenonware.notes.ui.theme.noteYellowLight
import com.xenonware.notes.viewmodel.classes.Label
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SortOption { FREE_SORTING, CREATION_DATE, NAME }
enum class SortOrder { ASCENDING, DESCENDING }
enum class NoteFilterType { ALL, TEXT, AUDIO, LIST, SKETCH }
enum class NotesLayoutType { LIST, GRID }

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = SharedPreferenceManager(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _allNotesItems = mutableStateListOf<NotesItems>()
    private val _displayedNotesItems = mutableStateListOf<Any>()
    val noteItems: List<Any> get() = _displayedNotesItems

    private var currentNoteId = 1

    private val syncingNoteIds = mutableStateSetOf<Int>()
    private val offlineNoteIds = mutableStateSetOf<Int>()

    fun isNoteBeingSynced(noteId: Int) = syncingNoteIds.contains(noteId)

    private var recentlyDeletedItem: NotesItems? = null
    private var recentlyDeletedItemOriginalIndex: Int = -1

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

    private val _showLocalOnly = MutableStateFlow(prefsManager.showLocalOnlyNotes)
    val showLocalOnly: StateFlow<Boolean> = _showLocalOnly.asStateFlow()


    private val _selectedColors = MutableStateFlow<Set<Long?>>(emptySet())
    val selectedColors: StateFlow<Set<Long?>> = _selectedColors.asStateFlow()

    private val _labels = MutableStateFlow<List<Label>>(emptyList())
    val labels: StateFlow<List<Label>> = _labels.asStateFlow()

    private val _selectedLabel = MutableStateFlow<String?>(null)
    val selectedLabel: StateFlow<String?> = _selectedLabel.asStateFlow()

    private val _showTextCard = MutableStateFlow(false)
    val showTextCard: StateFlow<Boolean> = _showTextCard.asStateFlow()

    private val _showAudioCard = MutableStateFlow(false)
    val showAudioCard: StateFlow<Boolean> = _showAudioCard.asStateFlow()

    private val _showListCard = MutableStateFlow(false)
    val showListCard: StateFlow<Boolean> = _showListCard.asStateFlow()

    private val _showSketchCard = MutableStateFlow(false)
    val showSketchCard: StateFlow<Boolean> = _showSketchCard.asStateFlow()


    init {
        loadAllNotes()
        loadLabels()
        loadLayoutSettings()
        applySortingAndFiltering()

        auth.currentUser?.uid?.let { uid ->
            startRealtimeListenerForFutureChanges(uid)
            startLabelsRealtimeListener(uid)
        }
    }

    private fun startRealtimeListenerForFutureChanges(userId: String) {
        firestore.collection("notes").document(userId).collection("user_notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges.forEach { change ->
                    val note = change.document.toObject(NotesItems::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            // Only add if it's not marked as local-only on this device
                            if (!offlineNoteIds.contains(note.id)) {
                                if (_allNotesItems.none { it.id == note.id }) {
                                    _allNotesItems.add(0, note.copy(isOffline = false))
                                    saveAllNotes()
                                    applySortingAndFiltering()
                                }
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            if (!offlineNoteIds.contains(note.id)) {
                                val index = _allNotesItems.indexOfFirst { it.id == note.id }
                                if (index != -1) {
                                    _allNotesItems[index] = note.copy(isOffline = false)
                                    saveAllNotes()
                                    applySortingAndFiltering()
                                }
                            }
                        }

                        DocumentChange.Type.REMOVED -> {
                            if (!offlineNoteIds.contains(note.id)) {
                                _allNotesItems.removeAll { it.id == note.id }
                                saveAllNotes()
                                applySortingAndFiltering()
                            }
                        }
                    }
                }
            }
    }

    private fun startLabelsRealtimeListener(userId: String) {
        firestore.collection("notes").document(userId).collection("labels")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val cloudLabels = snapshot.documents.mapNotNull { it.toObject(Label::class.java) }
                    .sortedBy { it.text.lowercase() }

                if (cloudLabels != _labels.value) {
                    _labels.value = cloudLabels
                    saveLabels()
                }
            }
    }

    fun showTextCard() {
        _showTextCard.value = true
    }
    fun showAudioCard() {
        _showAudioCard.value = true
    }
    fun showListCard() {
        _showListCard.value = true
    }
    fun showSketchCard() {
        _showSketchCard.value = true
    }

    fun hideTextCard() {
        _showTextCard.value = false
    }
    fun hideAudioCard() {
        _showAudioCard.value = false
    }
    fun hideSketchCard() {
        _showSketchCard.value = false
    }
    fun hideListCard() {
        _showListCard.value = false
    }

    fun syncLabelsToCloud() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _labels.value.forEach { label ->
                try {
                    firestore.collection("notes").document(uid).collection("labels")
                        .document(label.id).set(label).await()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun onSignedIn() {
        val uid = auth.currentUser?.uid ?: return

        startRealtimeListenerForFutureChanges(uid)
        startLabelsRealtimeListener(uid)
        syncLabelsToCloud()


        viewModelScope.launch {
            _allNotesItems.toList().forEach { note ->
                if (note.isOffline || note.id in syncingNoteIds) return@forEach

                syncingNoteIds.add(note.id)
                try {
                    firestore.collection("notes").document(uid).collection("user_notes")
                        .document(note.id.toString()).set(note).await()

                    offlineNoteIds.remove(note.id)
                    syncingNoteIds.remove(note.id)
                } catch (_: Exception) {
                    syncingNoteIds.remove(note.id)

                }
            }
        }

        uploadLocalLabelsIfNeeded(uid)
        viewModelScope.launch {
            try {
                val cloudSnapshot = firestore.collection("notes")
                    .document(uid)
                    .collection("labels")
                    .get()
                    .await()

                val cloudLabelIds = cloudSnapshot.documents.map { it.id }.toSet()
                val localLabelIds = _labels.value.map { it.id }.toSet()

                val orphaned = cloudLabelIds - localLabelIds
                orphaned.forEach { id ->
                    firestore.collection("notes").document(uid).collection("labels")
                        .document(id).delete().await()
                }
            } catch (_: Exception) { }
        }
    }

    private fun uploadLocalLabelsIfNeeded(userId: String) {
        if (_labels.value.isEmpty()) return

        viewModelScope.launch {
            try {
                val snapshot =
                    firestore.collection("notes").document(userId).collection("labels").get()
                        .await()

                val cloudLabelIds = snapshot.documents.mapNotNull { it.id }.toSet()
                val localLabelsToUpload = _labels.value.filter { it.id !in cloudLabelIds }

                if (localLabelsToUpload.isNotEmpty()) {
                    localLabelsToUpload.forEach { label ->
                        firestore.collection("notes").document(userId).collection("labels")
                            .document(label.id).set(label).await()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun addItem(
        title: String,
        description: String? = null,
        noteType: NoteType = NoteType.TEXT,
        color: Long? = null,
        labels: List<String> = emptyList(),
        forceLocal: Boolean = false,
    ) {
        if (title.isBlank()) return

        val newId = currentNoteId++
        val newItem = NotesItems(
            id = newId,
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            creationTimestamp = System.currentTimeMillis(),
            displayOrder = _allNotesItems.size,
            noteType = noteType,
            color = color,
            labels = labels,
            isOffline = forceLocal
        )

        _allNotesItems.add(0, newItem)
        saveAllNotes()
        applySortingAndFiltering()

        if (forceLocal) {
            offlineNoteIds.add(newId)
        } else if (auth.currentUser != null) {
            syncingNoteIds.add(newId)
            applySortingAndFiltering()

            val uid = auth.currentUser!!.uid
            firestore.collection("notes").document(uid).collection("user_notes")
                .document(newId.toString()).set(newItem).addOnSuccessListener {
                    syncingNoteIds.remove(newId)
                    applySortingAndFiltering()
                }
        }
    }


    fun updateItem(updatedItem: NotesItems, forceLocal: Boolean = false) {
        val index = _allNotesItems.indexOfFirst { it.id == updatedItem.id }
        if (index == -1) return

        val oldNote = _allNotesItems[index]
        val wasOffline = oldNote.isOffline
        val nowShouldBeOffline = forceLocal || updatedItem.isOffline

        val finalNote = updatedItem.copy(isOffline = nowShouldBeOffline)
        _allNotesItems[index] = finalNote

        if (nowShouldBeOffline) {
            offlineNoteIds.add(finalNote.id)
        } else {
            offlineNoteIds.remove(finalNote.id)
        }

        saveAllNotes()
        applySortingAndFiltering()

        if (!nowShouldBeOffline && auth.currentUser != null) {
            syncingNoteIds.add(finalNote.id)
            applySortingAndFiltering()

            viewModelScope.launch {
                try {
                    firestore.collection("notes").document(auth.currentUser!!.uid)
                        .collection("user_notes").document(finalNote.id.toString()).set(finalNote)
                        .await()

                    syncingNoteIds.remove(finalNote.id)
                    applySortingAndFiltering()
                } catch (_: Exception) {
                    syncingNoteIds.remove(finalNote.id)
                    applySortingAndFiltering()
                }
            }
        } else if (!wasOffline && nowShouldBeOffline && auth.currentUser != null) {
            viewModelScope.launch {
                try {
                    firestore.collection("notes").document(auth.currentUser!!.uid)
                        .collection("user_notes").document(finalNote.id.toString()).delete().await()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun deleteItems(itemIds: List<Int>) {
        _allNotesItems.removeAll { it.id in itemIds }
        offlineNoteIds.removeAll(itemIds)
        saveAllNotes()
        applySortingAndFiltering()

        if (auth.currentUser != null) {
            viewModelScope.launch {
                itemIds.forEach { id ->
                    try {
                        firestore.collection("notes").document(auth.currentUser!!.uid)
                            .collection("user_notes").document(id.toString()).delete().await()
                    } catch (_: Exception) {
                    }
                }
            }
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

    private fun loadLayoutSettings() {
        _notesLayoutType.value = prefsManager.notesLayoutType
        _gridColumnCount.value = prefsManager.gridColumnCount
        _listItemLineCount.value = prefsManager.listItemLineCount
    }

    fun setNotesLayoutType(layoutType: NotesLayoutType) {
        if (_notesLayoutType.value != layoutType) {
            _notesLayoutType.value = layoutType
            prefsManager.notesLayoutType = layoutType
        }
    }

    fun cycleListItemLineCount() {
        val nextLineCount = when (_listItemLineCount.value) {
            3 -> 9
            9 -> MAX_LINES_FULL_NOTE
            else -> 3
        }
        _listItemLineCount.value = nextLineCount
        prefsManager.listItemLineCount = nextLineCount
    }

    fun cycleGridColumnCount(screenWidthDp: Int) {
        val nextColumnCount = when (screenWidthDp) {
            in 0 until 600 -> if (_gridColumnCount.value == 2) 3 else 2
            in 600 until 840 -> when (_gridColumnCount.value) {
                3 -> 4; 4 -> 5; else -> 3
            }

            else -> when (_gridColumnCount.value) {
                4 -> 5; 5 -> 6; else -> 4
            }
        }
        _gridColumnCount.value = nextColumnCount
        prefsManager.gridColumnCount = nextColumnCount
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
        if (labelText.isBlank()) return

        val newLabel = Label(id = UUID.randomUUID().toString(), text = labelText.trim())
        _labels.value = (_labels.value + newLabel).sortedBy { it.text.lowercase() }
        saveLabels()

        auth.currentUser?.let {
            viewModelScope.launch {
                try {
                    firestore.collection("notes").document(it.uid).collection("labels")
                        .document(newLabel.id).set(newLabel).await()
                } catch (_: Exception) {

                }
            }
        }
    }

    fun removeLabel(labelId: String) {
        val updatedNotes = _allNotesItems.map { note ->
            if (note.labels.contains(labelId)) {
                note.copy(labels = note.labels.filter { it != labelId })
            } else note
        }
        _allNotesItems.clear()
        _allNotesItems.addAll(updatedNotes)
        saveAllNotes()

        _labels.value = _labels.value.filter { it.id != labelId }
        saveLabels()

        if (_selectedLabel.value == labelId) {
            _selectedLabel.value = null
        }

        applySortingAndFiltering()

        auth.currentUser?.let {
            viewModelScope.launch {
                try {
                    firestore.collection("notes")
                        .document(it.uid)
                        .collection("labels")
                        .document(labelId)
                        .delete()
                        .await()
                } catch (_: Exception) {
                }
            }
        }
    }
    fun toggleShowLocalOnly() {
        val newValue = !prefsManager.showLocalOnlyNotes
        prefsManager.showLocalOnlyNotes = newValue
        _showLocalOnly.value = newValue
        applySortingAndFiltering()
    }

    fun setLabelFilter(labelId: String?) {
        _selectedLabel.value = if (_selectedLabel.value == labelId) null else labelId
        applySortingAndFiltering()
    }

    fun moveItemInFreeSort(itemIdToMove: Int, newDisplayOrderCandidate: Int) {
        if (currentSortOption != SortOption.FREE_SORTING) return

        val itemsInList = _allNotesItems.sortedBy { it.displayOrder }.toMutableList()
        val itemToMoveIndex = itemsInList.indexOfFirst { it.id == itemIdToMove }
        if (itemToMoveIndex == -1) return

        val item = itemsInList.removeAt(itemToMoveIndex)
        val targetIndex = newDisplayOrderCandidate.coerceIn(0, itemsInList.size)
        itemsInList.add(targetIndex, item)

        itemsInList.forEachIndexed { newOrder, noteItem ->
            val originalIndex = _allNotesItems.indexOfFirst { it.id == noteItem.id }
            if (originalIndex != -1 && _allNotesItems[originalIndex].displayOrder != newOrder) {
                _allNotesItems[originalIndex] =
                    _allNotesItems[originalIndex].copy(displayOrder = newOrder)
            }
        }

        saveAllNotes()
        applySortingAndFiltering()
    }

    private fun applySortingAndFiltering(preserveRecentlyDeleted: Boolean = false) {
        val currentRecentlyDeleted = if (preserveRecentlyDeleted) recentlyDeletedItem else null

        val tempAllNoteItems = _allNotesItems.toMutableList()
        if (currentRecentlyDeleted != null && !tempAllNoteItems.contains(currentRecentlyDeleted)) {
            val insertIndex = recentlyDeletedItemOriginalIndex.coerceIn(0, tempAllNoteItems.size)
            tempAllNoteItems.add(insertIndex, currentRecentlyDeleted)
        }

        _displayedNotesItems.clear()
        var notesToDisplay = tempAllNoteItems.toList()

        if (searchQuery.value.isNotBlank()) {
            notesToDisplay = notesToDisplay.filter { note ->
                note.title.contains(
                    searchQuery.value, ignoreCase = true
                ) || (note.description?.contains(searchQuery.value, ignoreCase = true) == true)
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
                    note.color == null || currentSelectedColors.contains(note.color)
                } else {
                    currentSelectedColors.contains(note.color)
                }
            }
        }

        if (_showLocalOnly.value) {
            notesToDisplay = notesToDisplay.filter { it.isOffline }
        }

        if (_selectedLabel.value != null) {
            notesToDisplay = notesToDisplay.filter { note ->
                note.labels.contains(_selectedLabel.value)
            }
        }

        val sortedNotes = sortNotes(notesToDisplay, currentSortOption, currentSortOrder)

        if (currentSortOption != SortOption.FREE_SORTING && sortedNotes.isNotEmpty()) {
            val groupedItems = mutableListOf<Any>()
            var lastHeader: String? = null

            for (note in sortedNotes) {
                note.currentHeader = getHeaderForNote(note, currentSortOption)
                if (note.currentHeader != lastHeader) {
                    groupedItems.add(note.currentHeader)
                    lastHeader = note.currentHeader
                }
                groupedItems.add(note)
            }
            _displayedNotesItems.addAll(groupedItems)
        } else {
            sortedNotes.forEach { it.currentHeader = "" }
            _displayedNotesItems.addAll(sortedNotes)
        }
    }

    private fun getHeaderForNote(
        note: NotesItems,
        sortOption: SortOption,
    ): String {
        return when (sortOption) {
            SortOption.CREATION_DATE -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(note.creationTimestamp))
            }

            SortOption.NAME -> note.title.firstOrNull()?.uppercaseChar()?.toString() ?: "Unknown"
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
            SortOption.NAME -> compareBy<NotesItems, String>(String.CASE_INSENSITIVE_ORDER) { it.title }.thenBy { it.displayOrder }
        }
        return if (order == SortOrder.ASCENDING) notes.sortedWith(comparator)
        else notes.sortedWith(comparator.reversed())
    }

    companion object {
        const val MAX_LINES_FULL_NOTE = -1

        val COLOR_RED = noteRedLight.value.toLong()
        val COLOR_ORANGE = noteOrangeLight.value.toLong()
        val COLOR_YELLOW = noteYellowLight.value.toLong()
        val COLOR_GREEN = noteGreenLight.value.toLong()
        val COLOR_TURQUOISE = noteTurquoiseLight.value.toLong()
        val COLOR_BLUE = noteBlueLight.value.toLong()
        val COLOR_PURPLE = notePurpleLight.value.toLong()
    }
}