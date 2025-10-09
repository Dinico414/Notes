package com.xenonware.notes.ui.layouts

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenonware.notes.ui.layouts.notes.CompactNotes
import com.xenonware.notes.ui.layouts.notes.CoverNotes
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NotesListLayout(
    viewModel: NotesViewModel,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {

    when (layoutType) {
        LayoutType.COVER -> {
            if (isLandscape) {
                CoverNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                )
            } else {
                CoverNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = false,
                    appSize = appSize,
                )
            }
        }

        LayoutType.SMALL -> {
            if (isLandscape) {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                )
            } else {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = false,
                    appSize = appSize,
                )
            }
        }

        LayoutType.COMPACT -> {
            if (isLandscape) {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                )
            } else {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = false,
                    appSize = appSize,
                )
            }
        }

        LayoutType.MEDIUM -> {
            if (isLandscape) {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                )
            } else {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = false,
                    appSize = appSize,
                )
            }
        }

        LayoutType.EXPANDED -> {
            if (isLandscape) {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                )
            } else {
                CompactNotes(
                    onOpenSettings = onOpenSettings,
                    notesViewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = false,
                    appSize = appSize,
                )
            }
        }
    }
}
