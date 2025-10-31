package com.xenonware.notes.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewComfy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.LargerCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.NoPadding
import com.xenon.mylibrary.values.SmallerCornerRadius
import com.xenonware.notes.R
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.blueInversePrimaryDark
import com.xenonware.notes.ui.theme.blueInversePrimaryLight
import com.xenonware.notes.ui.theme.blueOnPrimaryDark
import com.xenonware.notes.ui.theme.blueOnPrimaryLight
import com.xenonware.notes.ui.theme.greenInversePrimaryDark
import com.xenonware.notes.ui.theme.greenInversePrimaryLight
import com.xenonware.notes.ui.theme.greenOnPrimaryDark
import com.xenonware.notes.ui.theme.greenOnPrimaryLight
import com.xenonware.notes.ui.theme.orangeInversePrimaryDark
import com.xenonware.notes.ui.theme.orangeInversePrimaryLight
import com.xenonware.notes.ui.theme.orangeOnPrimaryDark
import com.xenonware.notes.ui.theme.orangeOnPrimaryLight
import com.xenonware.notes.ui.theme.purpleInversePrimaryDark
import com.xenonware.notes.ui.theme.purpleInversePrimaryLight
import com.xenonware.notes.ui.theme.purpleOnPrimaryDark
import com.xenonware.notes.ui.theme.purpleOnPrimaryLight
import com.xenonware.notes.ui.theme.redInversePrimaryDark
import com.xenonware.notes.ui.theme.redInversePrimaryLight
import com.xenonware.notes.ui.theme.redOnPrimaryDark
import com.xenonware.notes.ui.theme.redOnPrimaryLight
import com.xenonware.notes.ui.theme.turquoiseInversePrimaryDark
import com.xenonware.notes.ui.theme.turquoiseInversePrimaryLight
import com.xenonware.notes.ui.theme.turquoiseOnPrimaryDark
import com.xenonware.notes.ui.theme.turquoiseOnPrimaryLight
import com.xenonware.notes.ui.theme.yellowInversePrimaryDark
import com.xenonware.notes.ui.theme.yellowInversePrimaryLight
import com.xenonware.notes.ui.theme.yellowOnPrimaryDark
import com.xenonware.notes.ui.theme.yellowOnPrimaryLight
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
    val selectedColors by notesViewModel.selectedColors.collectAsState()
    val isDarkTheme = LocalIsDarkTheme.current

    ModalDrawerSheet(
        drawerContainerColor = Color.Transparent,
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val safeDrawingInsets = WindowInsets.safeDrawing.asPaddingValues()

        val startPadding =
            if (safeDrawingInsets.calculateStartPadding(layoutDirection) > 0.dp) NoPadding else 12.dp
        val topPadding = if (safeDrawingInsets.calculateTopPadding() > 0.dp) NoPadding else 12.dp
        val bottomPadding =
            if (safeDrawingInsets.calculateBottomPadding() > 0.dp) NoPadding else 12.dp

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
                .background(colorScheme.surfaceContainerHigh)

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ExtraLargePadding)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = stringResource(id = R.string.app_name),
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
                            // This part of the code was not provided in the original context snippet
                            // I am adding a placeholder to prevent compilation errors.
                            // Assuming GoogleProfilBorder is defined elsewhere.
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
                //Add Scroll container from below here
                Column(
                    modifier = Modifier
                        .padding(vertical = LargestPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    FilterItem(
                        icon = Icons.Default.ViewComfy,
                        label = stringResource(R.string.all_notes),
                        isSelected = currentFilter == NoteFilterType.ALL,
                        onClick = { onFilterSelected(NoteFilterType.ALL) })
                    FilterItem(
                        icon = Icons.Default.TextFields,
                        label = stringResource(R.string.text_notes),
                        isSelected = currentFilter == NoteFilterType.TEXT,
                        onClick = { onFilterSelected(NoteFilterType.TEXT) })

                    FilterItem(
                        icon = Icons.Default.Checklist,
                        label = stringResource(R.string.list_notes),
                        isSelected = currentFilter == NoteFilterType.LIST,
                        onClick = { onFilterSelected(NoteFilterType.LIST) })
                    FilterItem(
                        icon = Icons.Default.Mic,
                        label = stringResource(R.string.audio_notes),
                        isSelected = currentFilter == NoteFilterType.AUDIO,
                        onClick = { onFilterSelected(NoteFilterType.AUDIO) })
                    FilterItem(
                        icon = Icons.Default.Edit,
                        label = stringResource(R.string.sketch_notes),
                        isSelected = currentFilter == NoteFilterType.SKETCH,
                        onClick = { onFilterSelected(NoteFilterType.SKETCH) })
                }
                HorizontalDivider(
                    thickness = 1.dp, color = colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LargestPadding),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isFilteringByDefaultColor = selectedColors.contains(null)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(null) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colorScheme.surfaceBright,
                            contentColor = colorScheme.onSurface
                        )
                    ) {
                        if (isFilteringByDefaultColor) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Default Color Selected"
                            )
                        }
                    }

                    // Red Color Filter Button
                    val isRedSelected = selectedColors.contains(NotesViewModel.COLOR_RED)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_RED) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) redInversePrimaryDark else redInversePrimaryLight,
                            contentColor = if (isDarkTheme) redOnPrimaryLight else redOnPrimaryDark

                        )
                    ) {
                        if (isRedSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Red Selected"
                            )
                        }
                    }

                    // Orange Color Filter Button
                    val isOrangeSelected = selectedColors.contains(NotesViewModel.COLOR_ORANGE)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_ORANGE) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) orangeInversePrimaryDark else orangeInversePrimaryLight,
                            contentColor = if (isDarkTheme) orangeOnPrimaryLight else orangeOnPrimaryDark

                        )
                    ) {
                        if (isOrangeSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Orange Selected"
                            )
                        }
                    }

                    // Yellow Color Filter Button
                    val isYellowSelected = selectedColors.contains(NotesViewModel.COLOR_YELLOW)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_YELLOW) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) yellowInversePrimaryDark else yellowInversePrimaryLight,
                            contentColor = if (isDarkTheme) yellowOnPrimaryLight else yellowOnPrimaryDark

                        )
                    ) {
                        if (isYellowSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Yellow Selected"
                            )
                        }
                    }

                    // Green Color Filter Button
                    val isGreenSelected = selectedColors.contains(NotesViewModel.COLOR_GREEN)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_GREEN) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) greenInversePrimaryDark else greenInversePrimaryLight,
                            contentColor = if (isDarkTheme) greenOnPrimaryLight else greenOnPrimaryDark

                        )
                    ) {
                        if (isGreenSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Green Selected"
                            )
                        }
                    }

                    // Turquoise Color Filter Button
                    val isTurquoiseSelected =
                        selectedColors.contains(NotesViewModel.COLOR_TURQUOISE)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_TURQUOISE) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) turquoiseInversePrimaryDark else turquoiseInversePrimaryLight,
                            contentColor = if (isDarkTheme) turquoiseOnPrimaryLight else turquoiseOnPrimaryDark

                        )
                    ) {
                        if (isTurquoiseSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Turquoise Selected"
                            )
                        }
                    }

                    // Blue Color Filter Button
                    val isBlueSelected = selectedColors.contains(NotesViewModel.COLOR_BLUE)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_BLUE) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) blueInversePrimaryDark else blueInversePrimaryLight,
                            contentColor = if (isDarkTheme) blueOnPrimaryLight else blueOnPrimaryDark
                        )
                    ) {
                        if (isBlueSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Blue Selected"
                            )
                        }
                    }

                    // Purple Color Filter Button
                    val isPurpleSelected = selectedColors.contains(NotesViewModel.COLOR_PURPLE)
                    OutlinedIconButton(
                        onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_PURPLE) },
                        modifier = Modifier.weight(1f),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDarkTheme) purpleInversePrimaryDark else purpleInversePrimaryLight,
                            contentColor = if (isDarkTheme) purpleOnPrimaryLight else purpleOnPrimaryDark
                        )
                    ) {
                        if (isPurpleSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Purple Selected"
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp, color = colorScheme.outlineVariant
                )
//                the scrolling should stop here
            }
        }
    }
}

@Composable
private fun FilterItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        colorScheme.inversePrimary
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100f))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(LargestPadding),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = colorScheme.onSurface
        )
        Text(
            text = label,
            fontFamily = QuicksandTitleVariable,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = LargestPadding),
            color = colorScheme.onSurface
        )
    }
}
