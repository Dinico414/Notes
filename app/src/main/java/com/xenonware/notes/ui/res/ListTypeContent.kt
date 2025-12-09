package com.xenonware.notes.ui.res

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.Identity
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.res.GoogleProfilBorder
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.res.XenonTextField
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.LargerCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoPadding
import com.xenon.mylibrary.values.SmallerCornerRadius
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
    onFilterSelected: (NoteFilterType) -> Unit,
) {
    val currentFilter by notesViewModel.noteFilterType.collectAsState()
    val selectedColors by notesViewModel.selectedColors.collectAsState()
    val isDarkTheme = LocalIsDarkTheme.current

    val localLabel by notesViewModel.labels.collectAsState()
    val selectedLabel by notesViewModel.selectedLabel.collectAsState()
    var newLabelName by remember { mutableStateOf("") }

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


        val context = LocalContext.current
        val googleAuthUiClient = remember {
            GoogleAuthUiClient(
                context = context.applicationContext,
                oneTapClient = Identity.getSignInClient(context.applicationContext)
            )
        }
        val state by signInViewModel.state.collectAsStateWithLifecycle()
        val userData = googleAuthUiClient.getSignedInUser()

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
                // Add a focusable element at the top of the drawer to intercept the initial focus
                Box(modifier = Modifier.focusable())
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


                    if (state.isSignInSuccessful) {
                        Box(contentAlignment = Alignment.Center) {
                            GoogleProfilBorder(
                                isSignedIn = state.isSignInSuccessful,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )

                            GoogleProfilePicture(
                                profilePictureUrl = userData?.profilePictureUrl,
                                iconContentDescription = stringResource(R.string.profile_picture),
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
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = LargestPadding)
                    ) {
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

                        val showLocalOnly by notesViewModel.showLocalOnly.collectAsState()

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
                                    if (showLocalOnly) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Checked",
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            tint = colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Not Checked",
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                            tint = colorScheme.surfaceDim
                                        )
                                    }
                                })
                        }
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

                        // Red Color Filter Button
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
                            if (isRedSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Red Selected"
                                )
                            }
                        }

                        // Orange Color Filter Button
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
                            if (isOrangeSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Orange Selected"
                                )
                            }
                        }

                        // Yellow Color Filter Button
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
                            if (isYellowSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Yellow Selected"
                                )
                            }
                        }

                        // Green Color Filter Button
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
                            if (isGreenSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
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
                            border = BorderStroke(1.dp, outlineColor),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = if (isDarkTheme) turquoiseInversePrimaryDark else turquoiseInversePrimaryLight,
                                contentColor = if (isDarkTheme) turquoiseOnPrimaryLight else turquoiseOnPrimaryDark

                            )
                        ) {
                            if (isTurquoiseSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Turquoise Selected"
                                )
                            }
                        }

                        // Blue Color Filter Button
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
                            if (isBlueSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Blue Selected"
                                )
                            }
                        }

                        // Purple Color Filter Button
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
                            if (isPurpleSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Purple Selected"
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp, color = colorScheme.outlineVariant
                    )

                    if (localLabel.isNotEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = LargestPadding)
                        ) {
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            forceRequest = false
                        )
                        Spacer(modifier = Modifier.width(MediumPadding))

                        FilledIconButton(
                            onClick = {
                                if (newLabelName.isNotBlank()) {
                                    notesViewModel.addLabel(newLabelName)
                                    newLabelName = ""
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .width(40.dp),
                            enabled = newLabelName.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_new_label)
                            )
                        }
                    }
                }
            }
        }
    }
}