@file:Suppress("unused")

package com.xenonware.notes.ui.res

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.QuicksandTitleVariable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

data class ListItem(
    val id: Long,
    var text: String,
    var isChecked: Boolean,
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteListCard(
    listTitle: String,
    onListTitleChange: (String) -> Unit,
    initialListItems: List<ListItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, List<ListItem>) -> Unit,
    cardBackgroundColor: Color = colorScheme.surfaceContainer,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    addItemTrigger: Boolean,
    onAddItemTriggerConsumed: () -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (ListItem) -> Unit,
    onToggleItemChecked: (ListItem, Boolean) -> Unit,
    onItemTextChange: (ListItem, String) -> Unit,
    onAddItemClick: () -> Unit,
    onTextResizeClick: () -> Unit,
    editorFontSize: TextUnit
) {
    val hazeState = remember { HazeState() }
    var currentListItems by remember { mutableStateOf(initialListItems) }

    val hazeThinColor = colorScheme.surfaceDim
    var showMenu by remember { mutableStateOf(false) }

    val listTitleFocusRequester = remember { FocusRequester() }

    var focusOnNewItemId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (initialListItems.isEmpty()) {
            val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
            currentListItems = listOf(newItem)
            focusOnNewItemId = newItem.id
        } else if (listTitle.isEmpty()) {
            listTitleFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialListItems) {
        currentListItems = initialListItems
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(listTitle, currentListItems)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(addItemTrigger) {
        if (addItemTrigger) {
            val newItem = ListItem(id = System.nanoTime(), text = "", isChecked = false)
            currentListItems = currentListItems + newItem
            focusOnNewItemId = newItem.id
            onAddItem()
            onAddItemTriggerConsumed()
        }
    }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent
    DisposableEffect(systemUiController, cardBackgroundColor) {
        systemUiController.setStatusBarColor(
            color = cardBackgroundColor
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = originalStatusBarColor
            )
        }
    }

    val contentBottomPadding = toolbarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cardBackgroundColor)
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

            currentListItems.forEachIndexed { index, listItem ->
                val itemFocusRequester = remember(listItem.id) { FocusRequester() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = listItem.isChecked,
                        onCheckedChange = { isChecked -> onToggleItemChecked(listItem, isChecked) })

                    val itemTextStyle = MaterialTheme.typography.bodyLarge.merge(
                        TextStyle(
                            color = colorScheme.onSurface,
                            textDecoration = if (listItem.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            fontSize = editorFontSize
                        )
                    )

                    BasicTextField(
                        value = listItem.text,
                        onValueChange = { newText -> onItemTextChange(listItem, newText) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(itemFocusRequester),
                        textStyle = itemTextStyle,
                        cursorBrush = SolidColor(colorScheme.primary),
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
                        })

                    IconButton(
                        onClick = { onDeleteItem(listItem) }) {
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
            Spacer(modifier = Modifier.height(contentBottomPadding))

        }
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
                value = listTitle,
                onValueChange = { onListTitleChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(listTitleFocusRequester),
                singleLine = true,
                textStyle = titleTextStyle,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (listTitle.isEmpty()) {
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
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            // Handle delete
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}