package com.xenonware.notes.ui.res.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargerSpacing
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.noteBlueDark
import com.xenonware.notes.ui.theme.noteBlueLight
import com.xenonware.notes.ui.theme.noteGreenDark
import com.xenonware.notes.ui.theme.noteGreenLight
import com.xenonware.notes.ui.theme.noteOrangeDark
import com.xenonware.notes.ui.theme.noteOrangeLight
import com.xenonware.notes.ui.theme.notePurpleDark
import com.xenonware.notes.ui.theme.notePurpleLight
import com.xenonware.notes.ui.theme.noteRedDark
import com.xenonware.notes.ui.theme.noteRedLight
import com.xenonware.notes.ui.theme.noteTurquoiseDark
import com.xenonware.notes.ui.theme.noteTurquoiseLight
import com.xenonware.notes.ui.theme.noteYellowDark
import com.xenonware.notes.ui.theme.noteYellowLight
import com.xenonware.notes.viewmodel.NotesViewModel
import com.xenonware.notes.viewmodel.classes.NotesItems

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListCard(
    item: NotesItems,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onSelectItem: () -> Unit,
    onEditItem: (NotesItems) -> Unit,
    notesViewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    isNoteSheetOpen: Boolean,
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val colorToThemeName = remember {
        mapOf(
            noteRedLight.value to "Red",
            noteRedDark.value to "Red",
            noteOrangeLight.value to "Orange",
            noteOrangeDark.value to "Orange",
            noteYellowLight.value to "Yellow",
            noteYellowDark.value to "Yellow",
            noteGreenLight.value to "Green",
            noteGreenDark.value to "Green",
            noteTurquoiseLight.value to "Turquoise",
            noteTurquoiseDark.value to "Turquoise",
            noteBlueLight.value to "Blue",
            noteBlueDark.value to "Blue",
            notePurpleLight.value to "Purple",
            notePurpleDark.value to "Purple"
        )
    }

    val selectedTheme = item.color?.let { colorToThemeName[it.toULong()] } ?: "Default"

    XenonTheme(
        darkTheme = isDarkTheme,
        useDefaultTheme = selectedTheme == "Default",
        useRedTheme = selectedTheme == "Red",
        useOrangeTheme = selectedTheme == "Orange",
        useYellowTheme = selectedTheme == "Yellow",
        useGreenTheme = selectedTheme == "Green",
        useTurquoiseTheme = selectedTheme == "Turquoise",
        useBlueTheme = selectedTheme == "Blue",
        usePurpleTheme = selectedTheme == "Purple",
        dynamicColor = selectedTheme == "Default"
    ) {
        val borderColor by animateColorAsState(
            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "Border Color Animation"
        )

        val backgroundColor =
            if (selectedTheme == "Default") MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.inversePrimary

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MediumCornerRadius))
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(MediumCornerRadius)
                )
                .then(
                    Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.075f),
                        shape = RoundedCornerShape(MediumCornerRadius)
                    )
                )
                .combinedClickable(
                    enabled = !isNoteSheetOpen,
                    onClick = {
                        if (isSelectionModeActive) {
                            onSelectItem()
                        } else {
                            onEditItem(item)
                        }
                    }, onLongClick = onSelectItem
                )
        ) {
            Column(
                modifier = Modifier.padding(LargestPadding)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = QuicksandTitleVariable,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(LargerSpacing))
                    val listItems = try {
                        item.description.split("\n").filter { it.isNotBlank() }
                    } catch (_: Exception) {
                        listOf(item.description)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        listItems.take(maxLines).forEach { listItemText ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (listItemText.startsWith("[x]"))
                                        Icons.Rounded.CheckBox
                                    else
                                        Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = "List",
                                    tint = if (listItemText.startsWith("[x]"))
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.7f
                                    ),
                                )
                                Spacer(modifier = Modifier.size(8.dp))

                                val cleanText = listItemText
                                    .replace(Regex("""\[[x ]]""", RegexOption.IGNORE_CASE), "")
                                    .trim()

                                Text(
                                    text = cleanText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (listItemText.startsWith("[x]", true))
                                            TextDecoration.LineThrough
                                        else
                                            TextDecoration.None
                                    ),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = if (listItemText.startsWith("[x]"))
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSelectionModeActive,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                        .background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(isSelected) { selected ->
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Box(
                                Modifier.padding(2.dp).size(20.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(0.6f), CircleShape)
                            )
                        }
                    }
                }
            }

            //Sync Icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
            ) {
                val isLocalOnly = item.isOffline
                val isSyncing = notesViewModel.isNoteBeingSynced(item.id)

                when {
                    isSyncing -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "spin")

                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "spinAngle"
                        )

                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Syncing",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(angle)
                        )
                    }
                    isLocalOnly -> {
                        Icon(Icons.Rounded.CloudOff, "Local only", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    else -> {
                        Icon(Icons.Rounded.CloudDone, "Synced", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }


            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 6.dp)
                    .size(26.dp), contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = "List",
                        tint = backgroundColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}