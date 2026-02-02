package com.xenonware.notes.ui.layouts

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenonware.notes.ui.layouts.notes.CompactNotes
import com.xenonware.notes.ui.layouts.notes.CoverNotes
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainLayout(
    viewModel: NotesViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {

    when (layoutType) {
        LayoutType.COVER -> {
            if (isLandscape) {
                CoverNotes(
                    viewModel = viewModel,

                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CoverNotes(
                    viewModel = viewModel,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }

        LayoutType.SMALL -> {
            if (isLandscape) {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }

        LayoutType.COMPACT -> {
            if (isLandscape) {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }

        LayoutType.MEDIUM -> {
            if (isLandscape) {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }

        LayoutType.EXPANDED -> {
            if (isLandscape) {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactNotes(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }
    }
}
