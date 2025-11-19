package com.xenonware.notes.ui.res

import androidx.compose.runtime.Composable
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems

@Composable
fun NoteCard(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
    isNoteSheetOpen: Boolean,
) {
    when (item.noteType) {
        NoteType.TEXT -> NoteTextCard(
            item = item,
            isSelected = isSelected,
            isSelectionModeActive = isSelectionModeActive,
            onSelectItem = onSelectItem,
            onEditItem = onEditItem,
            maxLines = maxLines,
            isNoteSheetOpen = isNoteSheetOpen
        )

        NoteType.AUDIO -> NoteAudioCard(
            item = item,
            isSelected = isSelected,
            isSelectionModeActive = isSelectionModeActive,
            onSelectItem = onSelectItem,
            onEditItem = onEditItem,
            isNoteSheetOpen = isNoteSheetOpen
        )

        NoteType.LIST -> NoteListCard(
            item = item,
            isSelected = isSelected,
            isSelectionModeActive = isSelectionModeActive,
            onSelectItem = onSelectItem,
            onEditItem = onEditItem,
            maxLines = maxLines,
            isNoteSheetOpen = isNoteSheetOpen
        )

        NoteType.SKETCH -> NoteSketchCard(
            item = item,
            isSelected = isSelected,
            isSelectionModeActive = isSelectionModeActive,
            onSelectItem = onSelectItem,
            onEditItem = onEditItem
        )
    }
}