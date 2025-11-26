package com.xenonware.notes.ui.res

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
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
    listTitle: String,
    onListTitleChange: (String) -> Unit,
    listItems: SnapshotStateList<ListItem>,               // ← NOW mutable & shared
    onDismiss: () -> Unit,
    onSave: (String, List<ListItem>, String, String?) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    addItemTrigger: Boolean,
    onAddItemTriggerConsumed: () -> Unit,
    editorFontSize: TextUnit,
    initialTheme: String = "Default",
    onThemeChange: (String) -> Unit,
    allLabels: List<Label>,
    initialSelectedLabelId: String?,
    onLabelSelected: (String?) -> Unit,
    onAddNewLabel: (String) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current

    var selectedTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    val scope = rememberCoroutineScope()
    var isFadingOut by remember { mutableStateOf(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }

    val availableThemes = remember {
        listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple")
    }

    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }

    var showLabelDialog by remember { mutableStateOf(false) }
    var selectedLabelId by rememberSaveable { mutableStateOf(initialSelectedLabelId) }
    val isLabeled = selectedLabelId != null

    val listTitleFocusRequester = remember { FocusRequester() }
    var focusOnNewItemId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (listItems.isEmpty()) {
            val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
            listItems.add(newItem)
            focusOnNewItemId = newItem.id
        } else if (listTitle.isEmpty()) {
            listTitleFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(listTitle, listItems.toList(), selectedTheme, selectedLabelId)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(addItemTrigger) {
        if (addItemTrigger) {
            val lastItem = listItems.lastOrNull()
            if (lastItem == null || lastItem.text.isNotEmpty()) {
                val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
                listItems.add(newItem)
                focusOnNewItemId = newItem.id
            } else {
                focusOnNewItemId = lastItem.id
            }
            onAddItemTriggerConsumed()
        }
    }

    val systemUiController = rememberSystemUiController()

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
            targetValue = if (isFadingOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0f) else MaterialTheme.colorScheme.onSurface,
            animationSpec = tween(durationMillis = 500),
            label = "animatedTextColor"
        )

        DisposableEffect(systemUiController, isDarkTheme) {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !isDarkTheme
            )
            onDispose {
                systemUiController.setStatusBarColor(color = Color.Transparent)
            }
        }

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = selectedLabelId,
                onLabelSelected = {
                    selectedLabelId = it
                    onLabelSelected(it)
                },
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        val hazeThinColor = MaterialTheme.colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + toolbarHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            val topPadding = 68.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .verticalScroll(scrollState)
                    .hazeSource(state = hazeState)
            ) {
                Spacer(modifier = Modifier.height(topPadding))

                listItems.forEachIndexed { index, listItem ->
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
                                val idx = listItems.indexOfFirst { it.id == listItem.id }
                                if (idx != -1) {
                                    listItems[idx] = listItems[idx].copy(isChecked = isChecked)
                                }
                            }
                        )

                        val itemTextStyle = MaterialTheme.typography.bodyLarge.merge(
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (listItem.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = editorFontSize
                            )
                        )

                        BasicTextField(
                            value = listItem.text,
                            singleLine = true,
                            onValueChange = { newText ->
                                val idx = listItems.indexOfFirst { it.id == listItem.id }
                                if (idx != -1) {
                                    listItems[idx] = listItems[idx].copy(text = newText)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(itemFocusRequester),
                            textStyle = itemTextStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (listItem.text.isEmpty()) {
                                        Text(
                                            text = "New item",
                                            style = itemTextStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        IconButton(
                            onClick = {
                                val idx = listItems.indexOfFirst { it.id == listItem.id }
                                if (idx != -1) {
                                    listItems.removeAt(idx)
                                    if (listItems.isEmpty()) {
                                        val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
                                        listItems.add(newItem)
                                        focusOnNewItemId = newItem.id
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete item")
                        }
                    }

                    LaunchedEffect(focusOnNewItemId, listItem.id) {
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
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(100f))
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(
                        fontFamily = QuicksandTitleVariable,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                BasicTextField(
                    value = listTitle,
                    onValueChange = onListTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (listTitle.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = titleTextStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownNoteMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOf(
                            MenuItem(
                                text = "Label",
                                onClick = { showLabelDialog = true; showMenu = false },
                                dismissOnClick = true,
                                icon = {
                                    if (isLabeled) {
                                        Icon(Icons.Default.Bookmark, contentDescription = "Label", tint = labelColor)
                                    } else {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Label")
                                    }
                                }
                            ),
                            MenuItem(
                                text = colorMenuItemText,
                                onClick = {
                                    val currentIndex = availableThemes.indexOf(selectedTheme)
                                    val nextIndex = (currentIndex + 1) % availableThemes.size
                                    selectedTheme = availableThemes[nextIndex]
                                    onThemeChange(selectedTheme)
                                    colorChangeJob?.cancel()
                                    colorChangeJob = scope.launch {
                                        colorMenuItemText = availableThemes[nextIndex]
                                        isFadingOut = false
                                        delay(2500)
                                        isFadingOut = true
                                        delay(500)
                                        colorMenuItemText = "Color"
                                        isFadingOut = false
                                    }
                                },
                                dismissOnClick = false,
                                icon = {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = "Color",
                                        tint = if (selectedTheme == "Default") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                },
                                textColor = animatedTextColor
                            ),
                            MenuItem(
                                text = if (isOffline) "Offline note" else "Online note",
                                onClick = { isOffline = !isOffline },
                                dismissOnClick = false,
                                textColor = if (isOffline) MaterialTheme.colorScheme.error else null,
                                icon = {
                                    if (isOffline) Icon(Icons.Default.CloudOff, contentDescription = "Offline", tint = MaterialTheme.colorScheme.error)
                                    else Icon(Icons.Default.Cloud, contentDescription = "Online")
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