// File: com/xenonware/notes/ui/res/ListContent.kt

package com.xenonware.notes.ui.res

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.res.XenonDrawer
import com.xenon.mylibrary.res.XenonTextField
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenonware.notes.R
import com.xenonware.notes.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.notes.presentation.sign_in.SignInViewModel
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
import com.xenonware.notes.viewmodel.NoteFilterType
import com.xenonware.notes.viewmodel.NotesViewModel

@Composable
fun ListContent(
    notesViewModel: NotesViewModel,
    signInViewModel: SignInViewModel,
    googleAuthUiClient: GoogleAuthUiClient,
    onFilterSelected: (NoteFilterType) -> Unit,
) {
    val currentFilter by notesViewModel.noteFilterType.collectAsState()
    val selectedColors by notesViewModel.selectedColors.collectAsState()
    val isDarkTheme = LocalIsDarkTheme.current

    val localLabel by notesViewModel.labels.collectAsState()
    val selectedLabel by notesViewModel.selectedLabel.collectAsState()
    var newLabelName by remember { mutableStateOf("") }

    val state by signInViewModel.state.collectAsStateWithLifecycle()
    val userData = googleAuthUiClient.getSignedInUser()
    val showLocalOnly by notesViewModel.showLocalOnly.collectAsState()

    XenonDrawer(
        title = stringResource(R.string.app_name),
        profilePictureUrl = userData?.profilePictureUrl,
        hasBottomContent = false,
        isSignedIn = state.isSignInSuccessful,
        noAccIcon = painterResource(R.drawable.default_icon),
        profilePicDesc = stringResource(R.string.profile_picture)
    ) {
        Column {
            // === Type Filters ===
            Column(modifier = Modifier.padding(vertical = LargestPadding)) {
                FilterItem(
                    icon = Icons.Rounded.ViewComfy,
                    label = stringResource(R.string.all_notes),
                    isSelected = currentFilter == NoteFilterType.ALL,
                    onClick = { onFilterSelected(NoteFilterType.ALL) })
                FilterItem(
                    icon = Icons.Rounded.TextFields,
                    label = stringResource(R.string.text_notes),
                    isSelected = currentFilter == NoteFilterType.TEXT,
                    onClick = { onFilterSelected(NoteFilterType.TEXT) })
                FilterItem(
                    icon = Icons.Rounded.Checklist,
                    label = stringResource(R.string.list_notes),
                    isSelected = currentFilter == NoteFilterType.LIST,
                    onClick = { onFilterSelected(NoteFilterType.LIST) })
                FilterItem(
                    icon = Icons.Rounded.Mic,
                    label = stringResource(R.string.audio_notes),
                    isSelected = currentFilter == NoteFilterType.AUDIO,
                    onClick = { onFilterSelected(NoteFilterType.AUDIO) })
                FilterItem(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(R.string.sketch_notes),
                    isSelected = currentFilter == NoteFilterType.SKETCH,
                    onClick = { onFilterSelected(NoteFilterType.SKETCH) })

                Spacer(modifier = Modifier.height(MediumPadding))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100f))
                        .padding(horizontal = LargestPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = LargestPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LargestPadding)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorScheme.onSurface
                        )
                        Text(
                            text = "Show local only",
                            fontFamily = QuicksandTitleVariable,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Switch(
                        checked = showLocalOnly,
                        onCheckedChange = { notesViewModel.toggleShowLocalOnly() },
                        colors = SwitchDefaults.colors(),
                        thumbContent = {
                            if (showLocalOnly) Icon(
                                Icons.Rounded.Check,
                                "Checked",
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = colorScheme.onPrimaryContainer
                            )
                            else Icon(
                                Icons.Rounded.Close,
                                "Not Checked",
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = colorScheme.surfaceDim
                            )
                        })
                }
            }
            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant)

            // === Color Filters ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LargestPadding),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val outlineColor = colorScheme.onSurface

                val isFilteringByDefaultColor = selectedColors.contains(null)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(null) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = colorScheme.surfaceBright,
                        contentColor = colorScheme.onSurface
                    )
                ) {
                    if (isFilteringByDefaultColor) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Default Color Selected"
                        )
                    }
                }

                // Red
                val isRedSelected = selectedColors.contains(NotesViewModel.COLOR_RED)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_RED) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) redInversePrimaryDark else redInversePrimaryLight,
                        contentColor = if (isDarkTheme) redOnPrimaryLight else redOnPrimaryDark,
                    )
                ) {
                    if (isRedSelected) Icon(Icons.Rounded.Check, "Red Selected")
                }

                // Orange
                val isOrangeSelected = selectedColors.contains(NotesViewModel.COLOR_ORANGE)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_ORANGE) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) orangeInversePrimaryDark else orangeInversePrimaryLight,
                        contentColor = if (isDarkTheme) orangeOnPrimaryLight else orangeOnPrimaryDark
                    )
                ) {
                    if (isOrangeSelected) Icon(Icons.Rounded.Check, "Orange Selected")
                }

                // Yellow
                val isYellowSelected = selectedColors.contains(NotesViewModel.COLOR_YELLOW)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_YELLOW) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) yellowInversePrimaryDark else yellowInversePrimaryLight,
                        contentColor = if (isDarkTheme) yellowOnPrimaryLight else yellowOnPrimaryDark
                    )
                ) {
                    if (isYellowSelected) Icon(Icons.Rounded.Check, "Yellow Selected")
                }

                // Green
                val isGreenSelected = selectedColors.contains(NotesViewModel.COLOR_GREEN)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_GREEN) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) greenInversePrimaryDark else greenInversePrimaryLight,
                        contentColor = if (isDarkTheme) greenOnPrimaryLight else greenOnPrimaryDark
                    )
                ) {
                    if (isGreenSelected) Icon(Icons.Rounded.Check, "Green Selected")
                }

                // Turquoise
                val isTurquoiseSelected = selectedColors.contains(NotesViewModel.COLOR_TURQUOISE)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_TURQUOISE) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) turquoiseInversePrimaryDark else turquoiseInversePrimaryLight,
                        contentColor = if (isDarkTheme) turquoiseOnPrimaryLight else turquoiseOnPrimaryDark
                    )
                ) {
                    if (isTurquoiseSelected) Icon(Icons.Rounded.Check, "Turquoise Selected")
                }

                // Blue
                val isBlueSelected = selectedColors.contains(NotesViewModel.COLOR_BLUE)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_BLUE) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) blueInversePrimaryDark else blueInversePrimaryLight,
                        contentColor = if (isDarkTheme) blueOnPrimaryLight else blueOnPrimaryDark
                    )
                ) {
                    if (isBlueSelected) Icon(Icons.Rounded.Check, "Blue Selected")
                }

                // Purple
                val isPurpleSelected = selectedColors.contains(NotesViewModel.COLOR_PURPLE)
                OutlinedIconButton(
                    onClick = { notesViewModel.toggleColorFilter(NotesViewModel.COLOR_PURPLE) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, outlineColor),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = if (isDarkTheme) purpleInversePrimaryDark else purpleInversePrimaryLight,
                        contentColor = if (isDarkTheme) purpleOnPrimaryLight else purpleOnPrimaryDark
                    )
                ) {
                    if (isPurpleSelected) Icon(Icons.Rounded.Check, "Purple Selected")
                }
            }
            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant)

            // === Labels ===
            if (localLabel.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = LargestPadding)) {
                    localLabel.forEach { label ->
                        FilterItem(
                            icon = Icons.AutoMirrored.Rounded.Label,
                            label = label.text,
                            isSelected = selectedLabel == label.id,
                            onClick = { notesViewModel.setLabelFilter(label.id) },
                            onDeleteClick = { notesViewModel.removeLabel(label.id) })
                    }
                }
            }

            // === Add New Label ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LargestPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                XenonTextField(
                    value = newLabelName,
                    onValueChange = { newLabelName = it },
                    placeholder = { Text(stringResource(R.string.add_new_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    forceRequest = false
                )
                Spacer(modifier = Modifier.width(MediumPadding))
                FilledIconButton(
                    onClick = {
                        if (newLabelName.isNotBlank()) {
                            notesViewModel.addLabel(newLabelName.trim()); newLabelName = ""
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .width(40.dp),
                    enabled = newLabelName.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorScheme.primary)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_new_label)
                    )
                }
            }
        }
    }
}