@file:Suppress("DEPRECATION", "AssignedValueIsNeverRead")

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun AnnotatedString.toSerialized(): String {
    val json = JSONObject()
    json.put("text", text)
    val spansArray = JSONArray()
    spanStyles.forEach { range ->
        val spanJson = JSONObject()
        spanJson.put("start", range.start)
        spanJson.put("end", range.end)
        val item = range.item
        if (item.fontWeight == FontWeight.Bold) spanJson.put("bold", true)
        if (item.fontStyle == FontStyle.Italic) spanJson.put("italic", true)
        if (item.textDecoration?.contains(TextDecoration.Underline) == true) spanJson.put("underline", true)
        if (spanJson.length() > 2) {
            spansArray.put(spanJson)
        }
    }
    json.put("spans", spansArray)
    return json.toString()
}

fun String.fromSerialized(): AnnotatedString {
    return try {
        val json = JSONObject(this)
        val text = json.getString("text")
        val spansArray = json.getJSONArray("spans")
        buildAnnotatedString {
            append(text)
            for (i in 0 until spansArray.length()) {
                val spanJson = spansArray.getJSONObject(i)
                val start = spanJson.getInt("start")
                val end = spanJson.getInt("end")
                val style = SpanStyle(
                    fontWeight = if (spanJson.optBoolean("bold")) FontWeight.Bold else null,
                    fontStyle = if (spanJson.optBoolean("italic")) FontStyle.Italic else null,
                    textDecoration = if (spanJson.optBoolean("underline")) TextDecoration.Underline else null
                )
                addStyle(style, start, end)
            }
        }
    } catch (e: JSONException) {
        AnnotatedString(this)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteTextSheet(
    textTitle: String,
    onTextTitleChange: (String) -> Unit,
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?) -> Unit,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderlined: Boolean,
    onIsBoldChange: (Boolean) -> Unit,
    onIsItalicChange: (Boolean) -> Unit,
    onIsUnderlinedChange: (Boolean) -> Unit,
    editorFontSize: TextUnit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    initialTheme: String = "Default",
    onThemeChange: (String) -> Unit,
    allLabels: List<Label>,
    initialSelectedLabelId: String?,
    onLabelSelected: (String?) -> Unit,
    onAddNewLabel: (String) -> Unit,
    isCoverModeActive: Boolean = false
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

    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialContent.fromSerialized())) }
    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }

    var showLabelDialog by remember { mutableStateOf(false) }
    var selectedLabelId by rememberSaveable { mutableStateOf(initialSelectedLabelId) }
    val isLabeled = selectedLabelId != null

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentSpanStyle = remember(isBold, isItalic, isUnderlined) {
        SpanStyle(
            fontWeight = if (isBold) FontWeight.Bold else null,
            fontStyle = if (isItalic) FontStyle.Italic else null,
            textDecoration = if (isUnderlined) TextDecoration.Underline else null
        )
    }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

    val lineHeightFactor = 1.25


    LaunchedEffect(Unit) {
        if (textFieldValue.text.isEmpty()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(textTitle, textFieldValue.annotatedString.toSerialized(), selectedTheme, selectedLabelId)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(isBold, isItalic, isUnderlined) {
        if (!textFieldValue.selection.collapsed) {
            val builder = AnnotatedString.Builder(textFieldValue.annotatedString)
            builder.addStyle(
                SpanStyle(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal),
                textFieldValue.selection.min,
                textFieldValue.selection.max
            )
            builder.addStyle(
                SpanStyle(fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal),
                textFieldValue.selection.min,
                textFieldValue.selection.max
            )
            builder.addStyle(
                SpanStyle(textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None),
                textFieldValue.selection.min,
                textFieldValue.selection.max
            )
            textFieldValue = textFieldValue.copy(annotatedString = builder.toAnnotatedString())
        }
    }

    LaunchedEffect(textFieldValue.selection) {
        if (textFieldValue.selection.collapsed) {
            val pos = textFieldValue.selection.start - 1
            if (pos >= 0) {
                val spansAtPos = textFieldValue.annotatedString.spanStyles.filter {
                    it.start <= pos && it.end > pos
                }
                var effectiveStyle = SpanStyle()
                spansAtPos.forEach { range ->
                    effectiveStyle = effectiveStyle.merge(range.item)
                }
                onIsBoldChange(effectiveStyle.fontWeight == FontWeight.Bold)
                onIsItalicChange(effectiveStyle.fontStyle == FontStyle.Italic)
                onIsUnderlinedChange(effectiveStyle.textDecoration?.contains(TextDecoration.Underline) ?: false)
            } else {
                onIsBoldChange(false)
                onIsItalicChange(false)
                onIsUnderlinedChange(false)
            }
        }
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

        DisposableEffect(systemUiController, isDarkTheme) {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !isDarkTheme
            )
            onDispose {
                systemUiController.setStatusBarColor(
                    color = originalStatusBarColor
                )
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

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label
        val bottomPadding =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + toolbarHeight + 16.dp
        val backgroundColor = if (isCoverModeActive) Color.Black else colorScheme.surfaceContainer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal
                    )
                )
        ) {
            val topPadding = 68.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top
                        )
                    )
                    .padding(top = 4.dp)
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
                        lineHeight = editorFontSize * lineHeightFactor,
                        platformStyle = null
                    )
                )
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        if (newValue.text == textFieldValue.text &&
                            newValue.annotatedString.spanStyles.isEmpty() &&
                            textFieldValue.annotatedString.spanStyles.isNotEmpty()
                        ) {
                            textFieldValue = textFieldValue.copy(
                                selection = newValue.selection,
                                composition = newValue.composition
                            )
                            return@BasicTextField
                        }

                        if (newValue.annotatedString.text == textFieldValue.annotatedString.text) {
                            textFieldValue = newValue
                            return@BasicTextField
                        }

                        val oldText = textFieldValue.text
                        val newText = newValue.text
                        val commonPrefixLength = oldText.commonPrefixWith(newText).length
                        val commonSuffixLength = oldText.commonSuffixWith(newText).length
                        val oldChangeEnd = oldText.length - commonSuffixLength
                        val newChangeEnd = newText.length - commonSuffixLength
                        val delta = (newChangeEnd - commonPrefixLength) - (oldChangeEnd - commonPrefixLength)

                        val builder = AnnotatedString.Builder(newText)
                        textFieldValue.annotatedString.spanStyles.forEach { range ->
                            val rangeStart = range.start
                            val rangeEnd = range.end
                            if (rangeEnd <= commonPrefixLength) {
                                builder.addStyle(range.item, rangeStart, rangeEnd)
                            } else if (rangeStart >= oldChangeEnd) {
                                builder.addStyle(range.item, rangeStart + delta, rangeEnd + delta)
                            } else {
                                if (rangeStart < commonPrefixLength) {
                                    builder.addStyle(range.item, rangeStart, commonPrefixLength)
                                }
                                if (rangeEnd > oldChangeEnd) {
                                    val newAfterStart = commonPrefixLength + (newChangeEnd - commonPrefixLength)
                                    builder.addStyle(range.item, newAfterStart, newAfterStart + (rangeEnd - oldChangeEnd))
                                }
                            }
                        }

                        // Apply current formatting to newly typed text
                        if (newChangeEnd > commonPrefixLength && currentSpanStyle != SpanStyle()) {
                            builder.addStyle(currentSpanStyle, commonPrefixLength, newChangeEnd)
                        }

                        textFieldValue = newValue.copy(annotatedString = builder.toAnnotatedString())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = noteTextStyle.copy(
                        lineHeight = editorFontSize * lineHeightFactor
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Note",
                                    style = noteTextStyle.copy(
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        lineHeight = editorFontSize * lineHeightFactor
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(bottomPadding))
            }
            // Toolbar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
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
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = MaterialTheme.typography.titleLarge.merge(
                    TextStyle(
                        fontFamily = QuicksandTitleVariable,
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurface
                    )
                )

                BasicTextField(
                    value = textTitle,
                    onValueChange = onTextTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (textTitle.isEmpty()) {
                                Text(
                                    text = "Title",
                                    style = titleTextStyle,
                                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            innerTextField()
                        }
                    })
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                    DropdownNoteMenu(
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
                                    Icon(
                                        Icons.Rounded.BookmarkBorder,
                                        contentDescription = "Label"
                                    )
                                }
                            }), MenuItem(
                                text = colorMenuItemText, onClick = {
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
                                }, dismissOnClick = false, icon = {
                                    Icon(
                                        Icons.Rounded.ColorLens,
                                        contentDescription = "Color",
                                        tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary
                                    )
                                }, textColor = animatedTextColor
                            ), MenuItem(
                                text = if (isOffline) "Offline note" else "Online note",
                                onClick = { isOffline = !isOffline },
                                dismissOnClick = false,
                                textColor = if (isOffline) colorScheme.error else null,
                                icon = {
                                    if (isOffline) {
                                        Icon(
                                            Icons.Rounded.CloudOff,
                                            contentDescription = "Offline note",
                                            tint = colorScheme.error
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Cloud, contentDescription = "Online note"
                                        )
                                    }
                                })
                        ),
                        hazeState = hazeState
                    )
                }
            }
        }
    }
}