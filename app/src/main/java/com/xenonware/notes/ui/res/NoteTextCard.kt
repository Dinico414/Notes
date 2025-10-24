package com.xenonware.notes.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.ui.theme.invertNoteBlueDark
import com.xenonware.notes.ui.theme.invertNoteBlueLight
import com.xenonware.notes.ui.theme.invertNoteGreenDark
import com.xenonware.notes.ui.theme.invertNoteGreenLight
import com.xenonware.notes.ui.theme.invertNoteOrangeDark
import com.xenonware.notes.ui.theme.invertNoteOrangeLight
import com.xenonware.notes.ui.theme.invertNotePurpleDark
import com.xenonware.notes.ui.theme.invertNotePurpleLight
import com.xenonware.notes.ui.theme.invertNoteRedDark
import com.xenonware.notes.ui.theme.invertNoteRedLight
import com.xenonware.notes.ui.theme.invertNoteTurquoiseDark
import com.xenonware.notes.ui.theme.invertNoteTurquoiseLight
import com.xenonware.notes.ui.theme.invertNoteYellowDark
import com.xenonware.notes.ui.theme.invertNoteYellowLight
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteTextCard(
    title: String,
    onTitleChange: (String) -> Unit,
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, ULong?) -> Unit,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderlined: Boolean,
    editorFontSize: TextUnit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    initialColor: ULong? = null
) {
    val ULongSaver = Saver<ULong?, String>(
        save = { it?.toString() ?: "null" },
        restore = { if (it == "null") null else it.toULong() }
    )

    val hazeState = remember { HazeState() }
    val isDarkTheme = isSystemInDarkTheme()

    var selectedColor by rememberSaveable(stateSaver = ULongSaver) { mutableStateOf(initialColor) }

    // Map for card background to provide the theme-aware color
    val cardColorMap = remember(isDarkTheme) {
        if (isDarkTheme) {
            mapOf(
                noteRedLight.value to Color(noteRedDark.value),
                noteOrangeLight.value to Color(noteOrangeDark.value),
                noteYellowLight.value to Color(noteYellowDark.value),
                noteGreenLight.value to Color(noteGreenDark.value),
                noteTurquoiseLight.value to Color(noteTurquoiseDark.value),
                noteBlueLight.value to Color(noteBlueDark.value),
                notePurpleLight.value to Color(notePurpleDark.value)
            )
        } else {
            mapOf(
                noteRedLight.value to Color(noteRedLight.value),
                noteOrangeLight.value to Color(noteOrangeLight.value),
                noteYellowLight.value to Color(noteYellowLight.value),
                noteGreenLight.value to Color(noteGreenLight.value),
                noteTurquoiseLight.value to Color(noteTurquoiseLight.value),
                noteBlueLight.value to Color(noteBlueLight.value),
                notePurpleLight.value to Color(notePurpleLight.value)
            )
        }
    }

    // Map for icon tinting to provide the visually opposite color
    val iconTintInvertMap = remember(isDarkTheme) {
        if (isDarkTheme) {
            mapOf(
                noteRedLight.value to Color(invertNoteRedDark.value),
                noteOrangeLight.value to Color(invertNoteOrangeDark.value),
                noteYellowLight.value to Color(invertNoteYellowDark.value),
                noteGreenLight.value to Color(invertNoteGreenDark.value),
                noteTurquoiseLight.value to Color(invertNoteTurquoiseDark.value),
                noteBlueLight.value to Color(invertNoteBlueDark.value),
                notePurpleLight.value to Color(invertNotePurpleDark.value)
            )
        } else {
            mapOf(
                noteRedLight.value to Color(invertNoteRedLight.value),
                noteOrangeLight.value to Color(invertNoteOrangeLight.value),
                noteYellowLight.value to Color(invertNoteYellowLight.value),
                noteGreenLight.value to Color(invertNoteGreenLight.value),
                noteTurquoiseLight.value to Color(invertNoteTurquoiseLight.value),
                noteBlueLight.value to Color(invertNoteBlueLight.value),
                notePurpleLight.value to Color(invertNotePurpleLight.value)
            )
        }
    }


    val cardColor = selectedColor?.let { cardColorMap[it] } ?: colorScheme.surfaceContainer

    val noteColors = remember {
        listOf(
            null, // Default
            noteRedLight.value,
            noteOrangeLight.value,
            noteYellowLight.value,
            noteGreenLight.value,
            noteTurquoiseLight.value,
            noteBlueLight.value,
            notePurpleLight.value
        )
    }

    val hazeThinColor = colorScheme.surfaceDim
    var content by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var isLabeled by remember { mutableStateOf(false) }
    val labelColor = extendedMaterialColorScheme.label

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (content.text.isEmpty()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(title, content.text, selectedColor)
            onSaveTriggerConsumed()
        }
    }


    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent
    DisposableEffect(systemUiController, cardColor) {
        systemUiController.setStatusBarColor(
            color = cardColor
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = originalStatusBarColor
            )
        }
    }
    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + toolbarHeight


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardColor)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .padding(top = 4.dp)
    ) {
        val topPadding = 68.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .verticalScroll(scrollState)
                .hazeSource(state = hazeState)

        ) {

            Spacer(modifier = Modifier.height(topPadding))
            val noteTextStyle = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(
                    color = colorScheme.onSurface,
                    fontSize = editorFontSize,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None
                )
            )
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .focusRequester(focusRequester),
                textStyle = noteTextStyle,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.text.isEmpty()) {
                            Text(
                                text = "Note",
                                style = noteTextStyle,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                })
            Spacer(modifier = Modifier.height(bottomPadding))

        }
        //topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(100f))
                .background(colorScheme.surfaceDim)
                .hazeEffect(
                    state = hazeState,
                    style = HazeMaterials.ultraThin(hazeThinColor),
                ), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss, Modifier.padding(4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                TextStyle(
                    fontFamily = QuicksandTitleVariable,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                )
            )
            BasicTextField(
                value = title,
                onValueChange = { onTitleChange(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = titleTextStyle,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = titleTextStyle,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        innerTextField()
                    }
                })
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                NoteDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = listOf(
                        MenuItem(
                            text = "Label",
                            onClick = { isLabeled = !isLabeled },
                            dismissOnClick = false,
                            icon = {
                                if (isLabeled) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = "Label",
                                        tint = labelColor
                                    )
                                } else {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Label")
                                }
                            }),
                        MenuItem(text = "Color", onClick = {
                            val currentIndex = noteColors.indexOf(selectedColor)
                            val nextIndex = (currentIndex + 1) % noteColors.size
                            selectedColor = noteColors[nextIndex]
                        }, dismissOnClick = false, icon = {
                            Icon(
                                Icons.Default.ColorLens,
                                contentDescription = "Color",
                                tint = selectedColor?.let { iconTintInvertMap[it] }
                                    ?: colorScheme.onSurfaceVariant)
                        }),
                        MenuItem(
                            text = if (isOffline) "Online note" else "Offline note",
                            onClick = { isOffline = !isOffline },
                            dismissOnClick = false,
                            textColor = if (isOffline) colorScheme.error else null,
                            icon = {
                                if (isOffline) {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = "Offline note",
                                        tint = colorScheme.error
                                    )
                                } else {
                                    Icon(Icons.Default.Cloud, contentDescription = "Online note")
                                }
                            })
                    ),
                    hazeState = hazeState
                )
            }
        }
    }
}
