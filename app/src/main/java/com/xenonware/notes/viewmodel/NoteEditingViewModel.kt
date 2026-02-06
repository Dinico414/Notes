package com.xenonware.notes.viewmodel

import androidx.lifecycle.ViewModel
import com.xenonware.notes.ui.res.sheets.ListItem
import com.xenonware.notes.util.TranscriptSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("unused")
class NoteEditingViewModel : ViewModel() {

    // ========== TEXT NOTE STATES ==========
    private val _textTitle = MutableStateFlow("")
    val textTitle: StateFlow<String> = _textTitle.asStateFlow()

    private val _textContent = MutableStateFlow("")
    val textContent: StateFlow<String> = _textContent.asStateFlow()

    private val _textTheme = MutableStateFlow("Default")
    val textTheme: StateFlow<String> = _textTheme.asStateFlow()

    private val _textLabelId = MutableStateFlow<String?>(null)
    val textLabelId: StateFlow<String?> = _textLabelId.asStateFlow()

    private val _textIsOffline = MutableStateFlow(false)
    val textIsOffline: StateFlow<Boolean> = _textIsOffline.asStateFlow()

    private val _textIsBold = MutableStateFlow(false)
    val textIsBold: StateFlow<Boolean> = _textIsBold.asStateFlow()

    private val _textIsItalic = MutableStateFlow(false)
    val textIsItalic: StateFlow<Boolean> = _textIsItalic.asStateFlow()

    private val _textIsUnderlined = MutableStateFlow(false)
    val textIsUnderlined: StateFlow<Boolean> = _textIsUnderlined.asStateFlow()

    private val _textFontSizeIndex = MutableStateFlow(1)
    val textFontSizeIndex: StateFlow<Int> = _textFontSizeIndex.asStateFlow()

    // ========== LIST NOTE STATES ==========
    private val _listTitle = MutableStateFlow("")
    val listTitle: StateFlow<String> = _listTitle.asStateFlow()

    private val _listItems = MutableStateFlow<List<ListItem>>(emptyList())
    val listItems: StateFlow<List<ListItem>> = _listItems.asStateFlow()

    private val _listTheme = MutableStateFlow("Default")
    val listTheme: StateFlow<String> = _listTheme.asStateFlow()

    private val _listLabelId = MutableStateFlow<String?>(null)
    val listLabelId: StateFlow<String?> = _listLabelId.asStateFlow()

    private val _listIsOffline = MutableStateFlow(false)
    val listIsOffline: StateFlow<Boolean> = _listIsOffline.asStateFlow()

    private val _listFontSizeIndex = MutableStateFlow(1)
    val listFontSizeIndex: StateFlow<Int> = _listFontSizeIndex.asStateFlow()

    // ========== AUDIO NOTE STATES ==========
    private val _audioTitle = MutableStateFlow("")
    val audioTitle: StateFlow<String> = _audioTitle.asStateFlow()

    private val _audioFilePath = MutableStateFlow<String?>(null)
    val audioFilePath: StateFlow<String?> = _audioFilePath.asStateFlow()

    private val _audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val audioAmplitudes: StateFlow<List<Float>> = _audioAmplitudes.asStateFlow()

    private val _audioTheme = MutableStateFlow("Default")
    val audioTheme: StateFlow<String> = _audioTheme.asStateFlow()

    private val _audioLabelId = MutableStateFlow<String?>(null)
    val audioLabelId: StateFlow<String?> = _audioLabelId.asStateFlow()

    private val _audioIsOffline = MutableStateFlow(false)
    val audioIsOffline: StateFlow<Boolean> = _audioIsOffline.asStateFlow()

    private val _audioViewType = MutableStateFlow("Waveform")
    val audioViewType: StateFlow<String> = _audioViewType.asStateFlow()

    private val _audioUniqueId = MutableStateFlow<String?>(null)
    val audioUniqueId: StateFlow<String?> = _audioUniqueId.asStateFlow()

    private val _audioRecordingDuration = MutableStateFlow(0L)
    val audioRecordingDuration: StateFlow<Long> = _audioRecordingDuration.asStateFlow()

    private val _audioIsPersistent = MutableStateFlow(false)
    val audioIsPersistent: StateFlow<Boolean> = _audioIsPersistent.asStateFlow()

    private val _audioTranscriptSegments = MutableStateFlow<List<TranscriptSegment>>(emptyList())
    val audioTranscriptSegments: StateFlow<List<TranscriptSegment>> = _audioTranscriptSegments.asStateFlow()

    // NEW: transcript setter for saving to NotesItems
    fun setAudioTranscriptForSearch(transcriptText: String) {
        // This can be called from sheet on save
        // But actually we set it in NotesViewModel
    }

    // ========== SKETCH NOTE STATES ==========
    private val _sketchTitle = MutableStateFlow("")
    val sketchTitle: StateFlow<String> = _sketchTitle.asStateFlow()

    private val _sketchTheme = MutableStateFlow("Default")
    val sketchTheme: StateFlow<String> = _sketchTheme.asStateFlow()

    private val _sketchLabelId = MutableStateFlow<String?>(null)
    val sketchLabelId: StateFlow<String?> = _sketchLabelId.asStateFlow()

    private val _sketchIsOffline = MutableStateFlow(false)
    val sketchIsOffline: StateFlow<Boolean> = _sketchIsOffline.asStateFlow()

    // ========== TEXT NOTE SETTERS ==========
    fun setTextTitle(title: String) {
        _textTitle.value = title
    }

    fun setTextContent(content: String) {
        _textContent.value = content
    }

    fun setTextTheme(theme: String) {
        _textTheme.value = theme
    }

    fun setTextLabelId(labelId: String?) {
        _textLabelId.value = labelId
    }

    fun setTextIsOffline(isOffline: Boolean) {
        _textIsOffline.value = isOffline
    }

    fun setTextIsBold(isBold: Boolean) {
        _textIsBold.value = isBold
    }

    fun setTextIsItalic(isItalic: Boolean) {
        _textIsItalic.value = isItalic
    }

    fun setTextIsUnderlined(isUnderlined: Boolean) {
        _textIsUnderlined.value = isUnderlined
    }

    fun setTextFontSizeIndex(index: Int) {
        _textFontSizeIndex.value = index
    }

    fun clearTextState() {
        _textContent.value = ""
        _textTitle.value = ""
        _textTheme.value = "Default"
        _textLabelId.value = null
        _textIsOffline.value = false
        _textIsBold.value = false
        _textIsItalic.value = false
        _textIsUnderlined.value = false
        _textFontSizeIndex.value = 1
    }

    // ========== LIST NOTE SETTERS ==========
    fun setListTitle(title: String) {
        _listTitle.value = title
    }

    fun setListItems(items: List<ListItem>) {
        _listItems.value = items
    }

    fun setListTheme(theme: String) {
        _listTheme.value = theme
    }

    fun setListLabelId(labelId: String?) {
        _listLabelId.value = labelId
    }

    fun setListIsOffline(isOffline: Boolean) {
        _listIsOffline.value = isOffline
    }

    fun setListFontSizeIndex(index: Int) {
        _listFontSizeIndex.value = index
    }

    // ========== LIST ITEM MANAGEMENT ==========
    fun addListItem(item: ListItem) {
        _listItems.value += item
    }

    fun updateListItem(itemId: Long, updatedItem: ListItem) {
        _listItems.value = _listItems.value.map { item ->
            if (item.id == itemId) updatedItem else item
        }
    }

    fun removeListItem(itemId: Long) {
        _listItems.value = _listItems.value.filter { it.id != itemId }
    }

    fun clearListState() {
        _listTitle.value = ""
        _listItems.value = emptyList()
        _listTheme.value = "Default"
        _listLabelId.value = null
        _listIsOffline.value = false
        _listFontSizeIndex.value = 1
    }

    // ========== AUDIO NOTE SETTERS ==========
    fun setAudioTitle(title: String) {
        _audioTitle.value = title
    }

    fun setAudioFilePath(path: String?) {
        _audioFilePath.value = path
    }

    fun setAudioAmplitudes(amplitudes: List<Float>) {
        _audioAmplitudes.value = amplitudes
    }

    fun setAudioTheme(theme: String) {
        _audioTheme.value = theme
    }

    fun setAudioLabelId(labelId: String?) {
        _audioLabelId.value = labelId
    }

    fun setAudioIsOffline(isOffline: Boolean) {
        _audioIsOffline.value = isOffline
    }

    fun setAudioViewType(viewType: String) {
        _audioViewType.value = viewType
    }

    fun setAudioUniqueId(uniqueId: String?) {
        _audioUniqueId.value = uniqueId
    }

    fun setAudioRecordingDuration(duration: Long) {
        _audioRecordingDuration.value = duration
    }

    fun setAudioIsPersistent(isPersistent: Boolean) {
        _audioIsPersistent.value = isPersistent
    }

    fun setAudioTranscriptSegments(segments: List<TranscriptSegment>) {
        _audioTranscriptSegments.value = segments
    }

    fun clearAudioState() {
        _audioTitle.value = ""
        _audioFilePath.value = null
        _audioAmplitudes.value = emptyList()
        _audioTheme.value = "Default"
        _audioLabelId.value = null
        _audioIsOffline.value = false
        _audioViewType.value = "Waveform"
        _audioUniqueId.value = null
        _audioRecordingDuration.value = 0L
        _audioIsPersistent.value = false
        _audioTranscriptSegments.value = emptyList()
    }

    // ========== SKETCH NOTE SETTERS ==========
    fun setSketchTitle(title: String) {
        _sketchTitle.value = title
    }

    fun setSketchTheme(theme: String) {
        _sketchTheme.value = theme
    }

    fun setSketchLabelId(labelId: String?) {
        _sketchLabelId.value = labelId
    }

    fun setSketchIsOffline(isOffline: Boolean) {
        _sketchIsOffline.value = isOffline
    }

    fun clearSketchState() {
        _sketchTitle.value = ""
        _sketchTheme.value = "Default"
        _sketchLabelId.value = null
        _sketchIsOffline.value = false
    }

    // ========== CLEAR ALL ==========
    fun clearAllStates() {
        clearTextState()
        clearListState()
        clearAudioState()
        clearSketchState()
    }
}