package com.xenonware.notes.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenonware.notes.R
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import com.xenonware.notes.ui.values.ExtraLargePadding
import com.xenonware.notes.ui.values.LargerCornerRadius
import com.xenonware.notes.ui.values.MediumPadding
import com.xenonware.notes.ui.values.NoPadding
import com.xenonware.notes.ui.values.SmallerCornerRadius
import com.xenonware.notes.viewmodel.DevSettingsViewModel
import com.xenonware.notes.viewmodel.NoteFilterType
import com.xenonware.notes.viewmodel.NotesViewModel

@Composable
fun ListContent(
    notesViewModel: NotesViewModel = viewModel(),
    devSettingsViewModel: DevSettingsViewModel = viewModel(),
    onFilterSelected: (NoteFilterType) -> Unit,
) {
    val currentFilter by notesViewModel.noteFilterType.collectAsState()

    ModalDrawerSheet(
        drawerContainerColor = Color.Transparent,
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()

        val startPadding =
            if (safeDrawingInsets.calculateStartPadding(layoutDirection) > 0.dp) NoPadding else MediumPadding
        val topPadding =
            if (safeDrawingInsets.calculateTopPadding() > 0.dp) NoPadding else MediumPadding
        val bottomPadding =
            if (safeDrawingInsets.calculateBottomPadding() > 0.dp) NoPadding else MediumPadding

        val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
        val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    start = startPadding, top = topPadding, bottom = bottomPadding
                )
                .clip(
                    RoundedCornerShape(
                        topStart = SmallerCornerRadius,
                        bottomStart = SmallerCornerRadius,
                        topEnd = LargerCornerRadius,
                        bottomEnd = LargerCornerRadius
                    )
                )
                .background(
                    lerp(colorScheme.background, colorScheme.surfaceBright, 0.2f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ExtraLargePadding)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = stringResource(id = R.string.filter_tasks_description),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = QuicksandTitleVariable, color = colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = ExtraLargePadding)
                    )


                    if (isDeveloperModeEnabled && showDummyProfile) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            GoogleProfilBorder(
                                modifier = Modifier.size(32.dp),
                            )
                            Image(
                                painter = painterResource(id = R.mipmap.default_icon),
                                contentDescription = stringResource(R.string.open_navigation_menu),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp, color = colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier.padding(vertical = MediumPadding)
                ) {
                    FilterItem(
                        icon = Icons.Default.Notes,
                        label = "All Notes",
                        isSelected = currentFilter == NoteFilterType.ALL,
                        onClick = { onFilterSelected(NoteFilterType.ALL) }
                    )
                    FilterItem(
                        icon = Icons.Default.Abc,
                        label = "Text Notes",
                        isSelected = currentFilter == NoteFilterType.TEXT,
                        onClick = { onFilterSelected(NoteFilterType.TEXT) }
                    )
                    FilterItem(
                        icon = Icons.Default.Audiotrack,
                        label = "Audio Notes",
                        isSelected = currentFilter == NoteFilterType.AUDIO,
                        onClick = { onFilterSelected(NoteFilterType.AUDIO) }
                    )
                    FilterItem(
                        icon = Icons.Default.FormatListBulleted,
                        label = "List Notes",
                        isSelected = currentFilter == NoteFilterType.LIST,
                        onClick = { onFilterSelected(NoteFilterType.LIST) }
                    )
                    FilterItem(
                        icon = Icons.Default.Draw,
                        label = "Sketch Notes",
                        isSelected = currentFilter == NoteFilterType.SKETCH,
                        onClick = { onFilterSelected(NoteFilterType.SKETCH) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100f))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(MediumPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = MediumPadding),
            color = colorScheme.onSurface
        )
    }
}
