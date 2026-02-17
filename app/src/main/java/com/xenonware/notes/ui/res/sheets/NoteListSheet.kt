@file:Suppress("DEPRECATION", "AssignedValueIsNeverRead")

package com.xenonware.notes.ui.res.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.notes.ui.res.LabelSelectionDialog
import com.xenonware.notes.ui.res.MenuItem
import com.xenonware.notes.ui.res.XenonDropDown
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.viewmodel.NoteEditingViewModel
import com.xenonware.notes.viewmodel.classes.Label
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ListItem(
    val id: Long,
    var text: String,
    var isChecked: Boolean,
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteListSheet(
    onDismiss: () -> Unit,
    onSave: (String, List<ListItem>, String, String?, Boolean) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    addItemTrigger: Boolean,
    onAddItemTriggerConsumed: () -> Unit,
    editorFontSize: TextUnit,
    allLabels: List<Label>,
    onAddNewLabel: (String) -> Unit,
    noteEditingViewModel: NoteEditingViewModel,
    isBlackThemeActive: Boolean = false,
    isCoverModeActive: Boolean = false,
    backProgress: Float = 0f,
    ) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current

    val title by noteEditingViewModel.listTitle.collectAsStateWithLifecycle()
    val items by noteEditingViewModel.listItems.collectAsStateWithLifecycle()
    val theme by noteEditingViewModel.listTheme.collectAsStateWithLifecycle()
    val labelId by noteEditingViewModel.listLabelId.collectAsStateWithLifecycle()
    val isOffline by noteEditingViewModel.listIsOffline.collectAsStateWithLifecycle()

    val selectedTheme = theme.ifEmpty { "Default" }
    val isLabeled = labelId != null

    var showMenu by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    var isFadingOut by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }

    val availableThemes = remember {
        listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple")
    }

    var focusOnNewItemId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(title, items, selectedTheme, labelId, isOffline)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(addItemTrigger) {
        if (addItemTrigger) {
            val lastItem = items.lastOrNull()
            if (lastItem == null || lastItem.text.isNotBlank()) {
                val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
                noteEditingViewModel.addListItem(newItem)
                focusOnNewItemId = newItem.id
            } else {
                focusOnNewItemId = lastItem.id
            }
            onAddItemTriggerConsumed()
        }
    }

    // Auto-create first item if empty
    LaunchedEffect(items) {
        if (items.isEmpty()) {
            val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
            noteEditingViewModel.addListItem(newItem)
            focusOnNewItemId = newItem.id
        }
    }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent
    DisposableEffect(systemUiController, isDarkTheme) {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = !isDarkTheme)
        onDispose { systemUiController.setStatusBarColor(originalStatusBarColor) }
    }

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
        val animatedTextColor by animateColorAsState(
            targetValue = if (isFadingOut) colorScheme.onSurface.copy(alpha = 0f) else colorScheme.onSurface,
            animationSpec = tween(durationMillis = 500),
            label = "animatedTextColor"
        )

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = labelId,
                onLabelSelected = noteEditingViewModel::setListLabelId,
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label

        val layoutDirection = LocalLayoutDirection.current

        val safeDrawingPaddingTop = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

        val topPadding = 4.dp + safeDrawingPaddingTop - safeDrawingPaddingTop * backProgress
        val animatedTopPadding = if (topPadding < 16.dp) 16.dp else topPadding

        val safeDrawingPaddingStart = WindowInsets.safeDrawing.only(WindowInsetsSides.Start).asPaddingValues().calculateStartPadding(layoutDirection)
        val safeDrawingPaddingEnd = WindowInsets.safeDrawing.only(WindowInsetsSides.End).asPaddingValues().calculateEndPadding(layoutDirection)

        val adaptivePaddingStart = if (safeDrawingPaddingStart <= 16.dp) 16.dp-safeDrawingPaddingStart else safeDrawingPaddingStart
        val adaptivePaddingEnd = if (safeDrawingPaddingEnd <= 16.dp) 16.dp-safeDrawingPaddingEnd else safeDrawingPaddingEnd

        val safeDrawingPaddingBottom = if (WindowInsets.ime.asPaddingValues().calculateBottomPadding() > WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()) {
            WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        } else {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        }

        val bottomPadding = safeDrawingPaddingBottom + toolbarHeight + 16.dp
        val backgroundColor = if (isCoverModeActive || isBlackThemeActive) Color.Black else colorScheme.surfaceContainer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(start = adaptivePaddingStart, end = adaptivePaddingEnd)
        ) {
            val topPadding = 68.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(top = animatedTopPadding)
                    .padding(horizontal = 1.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .verticalScroll(scrollState)
                    .hazeSource(hazeState)
            ) {
                Spacer(modifier = Modifier.height(topPadding))

                items.forEach { listItem ->
                    val itemFocusRequester = remember(listItem.id) { FocusRequester() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = listItem.isChecked,
                            onCheckedChange = { isChecked ->
                                noteEditingViewModel.updateListItem(
                                    listItem.id,
                                    listItem.copy(isChecked = isChecked)
                                )
                            }
                        )

                        val itemTextStyle = typography.bodyLarge.merge(
                            TextStyle(
                                color = colorScheme.onSurface,
                                textDecoration = if (listItem.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = editorFontSize
                            )
                        )

                        BasicTextField(
                            value = listItem.text,
                            onValueChange = { newText ->
                                noteEditingViewModel.updateListItem(
                                    listItem.id,
                                    listItem.copy(text = newText)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(itemFocusRequester),
                            textStyle = itemTextStyle,
                            cursorBrush = SolidColor(colorScheme.primary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (listItem.text.isEmpty()) {
                                        Text(
                                            text = "New item",
                                            style = itemTextStyle,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                noteEditingViewModel.removeListItem(listItem.id)
                                if (items.size == 1) {
                                    val newItem = ListItem(
                                        id = System.nanoTime(),
                                        text = "",
                                        isChecked = false
                                    )
                                    noteEditingViewModel.addListItem(newItem)
                                    focusOnNewItemId = newItem.id
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete item")
                        }
                    }

                    LaunchedEffect(focusOnNewItemId) {
                        if (focusOnNewItemId == listItem.id) {
                            itemFocusRequester.requestFocus()
                            focusOnNewItemId = null
                        }
                    }
                }

                Spacer(modifier = Modifier.height(bottomPadding))
            }

            // Toolbar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = animatedTopPadding)
                    .clip(RoundedCornerShape(100f))
                    .background(colorScheme.surfaceDim)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = typography.titleLarge.merge(
                    TextStyle(fontFamily = QuicksandTitleVariable, textAlign = TextAlign.Center, color = colorScheme.onSurface)
                )

                BasicTextField(
                    value = title,
                    onValueChange = noteEditingViewModel::setListTitle,
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
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box {
                    IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    XenonDropDown(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOfNotNull(
                            MenuItem(text = "Label", onClick = {
                                showLabelDialog = true
                                showMenu = false
                            }, dismissOnClick = true, icon = {
                                if (isLabeled) {
                                    Icon(
                                        Icons.Rounded.Bookmark,
                                        contentDescription = "Label",
                                        tint = labelColor
                                    )
                                } else {
                                    Icon(Icons.Rounded.BookmarkBorder, contentDescription = "Label")
                                }
                            }),
                            MenuItem(text = colorMenuItemText, onClick = {
                                val currentIndex = availableThemes.indexOf(selectedTheme)
                                val nextIndex = (currentIndex + 1) % availableThemes.size
                                val newTheme = availableThemes[nextIndex]
                                noteEditingViewModel.setListTheme(newTheme)
                                colorChangeJob?.cancel()
                                colorChangeJob = scope.launch {
                                    colorMenuItemText = newTheme
                                    isFadingOut = false
                                    delay(2500)
                                    isFadingOut = true
                                    delay(500)
                                    colorMenuItemText = "Color"
                                    isFadingOut = false
                                }
                            }, dismissOnClick = false, icon = {
                                Icon(
                                    Icons.Rounded.ColorLens,
                                    contentDescription = "Color",
                                    tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary
                                )
                            }, textColor = animatedTextColor),
                            MenuItem(
                                text = if (isOffline) "Offline note" else "Online note",
                                onClick = {
                                    noteEditingViewModel.setListIsOffline(!isOffline)
                                },
                                dismissOnClick = false,
                                textColor = if (isOffline) colorScheme.error else null,
                                icon = {
                                    if (isOffline) {
                                        Icon(
                                            Icons.Rounded.CloudOff,
                                            "Local only",
                                            tint = colorScheme.error
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Cloud, "Synced")
                                    }
                                }
                            )
                        ),
                        hazeState = hazeState
                    )
                }
            }
        }
    }
}