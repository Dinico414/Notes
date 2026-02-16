@file:Suppress("DEPRECATION", "AssignedValueIsNeverRead")

package com.xenonware.notes.ui.res.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
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
    } catch (_: JSONException) {
        AnnotatedString(this)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun NoteTextSheet(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, Boolean) -> Unit,
    toolbarHeight: Dp,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
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

    val title by noteEditingViewModel.textTitle.collectAsStateWithLifecycle()
    val content by noteEditingViewModel.textContent.collectAsStateWithLifecycle()
    val theme by noteEditingViewModel.textTheme.collectAsStateWithLifecycle()
    val labelId by noteEditingViewModel.textLabelId.collectAsStateWithLifecycle()
    val isOffline by noteEditingViewModel.textIsOffline.collectAsStateWithLifecycle()
    val isBold by noteEditingViewModel.textIsBold.collectAsStateWithLifecycle()
    val isItalic by noteEditingViewModel.textIsItalic.collectAsStateWithLifecycle()
    val isUnderlined by noteEditingViewModel.textIsUnderlined.collectAsStateWithLifecycle()

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

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(AnnotatedString("")))
    }

    LaunchedEffect(content) {
        val newAnnotated = if (content.isNotEmpty()) {
            content.fromSerialized()
        } else {
            AnnotatedString("")
        }
        if (textFieldValue.annotatedString.text != newAnnotated.text) {
            textFieldValue = TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(newAnnotated.length)
            )
        }
    }

    LaunchedEffect(textFieldValue.annotatedString) {
        val serialized = textFieldValue.annotatedString.toSerialized()
        if (serialized != content && textFieldValue.annotatedString.text.isNotEmpty()) {
            noteEditingViewModel.setTextContent(serialized)
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(title, content, selectedTheme, labelId, isOffline)
            onSaveTriggerConsumed()
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

    DisposableEffect(systemUiController, isDarkTheme) {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = !isDarkTheme)
        onDispose { systemUiController.setStatusBarColor(originalStatusBarColor) }
    }

    LaunchedEffect(Unit) {
        if (textFieldValue.text.isEmpty()) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(textFieldValue.selection) {
        if (textFieldValue.selection.collapsed && textFieldValue.text.isNotEmpty()) {
            val cursorPos = textFieldValue.selection.start
            if (cursorPos > 0) {
                val checkPos = cursorPos - 1
                val spansAtPos = textFieldValue.annotatedString.spanStyles.filter {
                    it.start <= checkPos && it.end > checkPos
                }
                var effectiveStyle = SpanStyle()
                spansAtPos.forEach { range ->
                    effectiveStyle = effectiveStyle.merge(range.item)
                }
                val shouldBeBold = effectiveStyle.fontWeight == FontWeight.Bold
                val shouldBeItalic = effectiveStyle.fontStyle == FontStyle.Italic
                val shouldBeUnderlined = effectiveStyle.textDecoration?.contains(TextDecoration.Underline) ?: false
                if (isBold != shouldBeBold) noteEditingViewModel.setTextIsBold(shouldBeBold)
                if (isItalic != shouldBeItalic) noteEditingViewModel.setTextIsItalic(shouldBeItalic)
                if (isUnderlined != shouldBeUnderlined) noteEditingViewModel.setTextIsUnderlined(shouldBeUnderlined)
            } else if (cursorPos == 0 && textFieldValue.text.isNotEmpty()) {
                val spansAtPos = textFieldValue.annotatedString.spanStyles.filter {
                    it.start == 0 && it.end > 0
                }
                var effectiveStyle = SpanStyle()
                spansAtPos.forEach { range ->
                    effectiveStyle = effectiveStyle.merge(range.item)
                }
                val shouldBeBold = effectiveStyle.fontWeight == FontWeight.Bold
                val shouldBeItalic = effectiveStyle.fontStyle == FontStyle.Italic
                val shouldBeUnderlined = effectiveStyle.textDecoration?.contains(TextDecoration.Underline) ?: false
                if (isBold != shouldBeBold) noteEditingViewModel.setTextIsBold(shouldBeBold)
                if (isItalic != shouldBeItalic) noteEditingViewModel.setTextIsItalic(shouldBeItalic)
                if (isUnderlined != shouldBeUnderlined) noteEditingViewModel.setTextIsUnderlined(shouldBeUnderlined)
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

        if (showLabelDialog) {
            LabelSelectionDialog(
                allLabels = allLabels,
                selectedLabelId = labelId,
                onLabelSelected = noteEditingViewModel::setTextLabelId,
                onAddNewLabel = onAddNewLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label

        val safeDrawingPaddingBottom = if (WindowInsets.ime.asPaddingValues().calculateBottomPadding() > WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()) {
            WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        } else {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        }

        val safeDrawingPaddingTop = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

        val topPadding = 4.dp + safeDrawingPaddingTop - safeDrawingPaddingTop * backProgress
        val animatedTopPadding = if (topPadding < 16.dp) 16.dp else topPadding

        val bottomPadding = safeDrawingPaddingBottom + toolbarHeight + 16.dp
        val backgroundColor = if (isCoverModeActive || isBlackThemeActive) Color.Black else colorScheme.surfaceContainer

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            val topPadding = 68.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(top = animatedTopPadding)
                    .padding(horizontal = 20.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .verticalScroll(scrollState)
                    .hazeSource(hazeState)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                                textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
                            }
                        )
                    }
            ) {
                Spacer(modifier = Modifier.height(topPadding))
                val lineHeightFactor = 1.25
                val noteTextStyle = typography.bodyLarge.merge(
                    TextStyle(
                        color = colorScheme.onSurface,
                        fontSize = editorFontSize,
                        lineHeight = editorFontSize * lineHeightFactor
                    )
                )

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        if (newValue.text == textFieldValue.text &&
                            newValue.annotatedString.spanStyles.isEmpty() &&
                            textFieldValue.annotatedString.spanStyles.isNotEmpty()) {
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

                        var changeStart = 0
                        while (changeStart < oldText.length &&
                            changeStart < newText.length &&
                            oldText[changeStart] == newText[changeStart]) {
                            changeStart++
                        }

                        var changeEndOld = oldText.length
                        var changeEndNew = newText.length
                        while (changeEndOld > changeStart &&
                            changeEndNew > changeStart &&
                            oldText[changeEndOld - 1] == newText[changeEndNew - 1]) {
                            changeEndOld--
                            changeEndNew--
                        }

                        val builder = AnnotatedString.Builder()

                        if (changeStart > 0) {
                            builder.append(textFieldValue.annotatedString.subSequence(0, changeStart))
                        }

                        val insertedText = newText.substring(changeStart, changeEndNew)
                        if (insertedText.isNotEmpty()) {
                            val insertStartIndex = builder.length
                            builder.append(insertedText)

                            if (isBold || isItalic || isUnderlined) {
                                val currentStyle = SpanStyle(
                                    fontWeight = if (isBold) FontWeight.Bold else null,
                                    fontStyle = if (isItalic) FontStyle.Italic else null,
                                    textDecoration = if (isUnderlined) TextDecoration.Underline else null
                                )
                                builder.addStyle(currentStyle, insertStartIndex, insertStartIndex + insertedText.length)
                            }
                        }

                        if (changeEndOld < oldText.length) {
                            val afterText = textFieldValue.annotatedString.subSequence(changeEndOld, oldText.length)
                            val afterStartIndex = builder.length
                            builder.append(afterText.text)

                            afterText.spanStyles.forEach { range ->
                                builder.addStyle(
                                    range.item,
                                    afterStartIndex + range.start,
                                    afterStartIndex + range.end
                                )
                            }
                        }

                        textFieldValue = newValue.copy(annotatedString = builder.toAnnotatedString())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = noteTextStyle.copy(lineHeight = editorFontSize * lineHeightFactor),
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
                    .padding(horizontal = 16.dp)
                    .padding(top = animatedTopPadding)
                    .clip(RoundedCornerShape(100f))
                    .background(colorScheme.surfaceDim)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin(hazeThinColor)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = typography.titleLarge.merge(
                    TextStyle(fontFamily = QuicksandTitleVariable, textAlign = TextAlign.Center, color = colorScheme.onSurface)
                )

                BasicTextField(
                    value = title,
                    onValueChange = noteEditingViewModel::setTextTitle,
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
                                noteEditingViewModel.setTextTheme(newTheme)
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
                                    noteEditingViewModel.setTextIsOffline(!isOffline)
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