package com.xenonware.notes.ui.res

import android.app.Application
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.NoteCanvas
import com.xenonware.notes.R
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.viewmodel.CanvasViewModel
import com.xenonware.notes.viewmodel.DrawingAction
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NoteSketchSheet(
    sketchTitle: String,
    onSketchTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    initialTheme: String = "Default",
    onThemeChange: (String) -> Unit,
    onSave: (String, String) -> Unit,
    saveTrigger: Boolean,
    onSaveTriggerConsumed: () -> Unit,
    isEraserMode: Boolean,
    usePressure: Boolean,
    strokeWidth: Float,
    strokeColor: Color,
    showColorPicker: Boolean,
    onColorPickerDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    showPenSizePicker: Boolean,
    onPenSizePickerDismiss: () -> Unit,
    onPenSizeSelected: (Float) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current
    val context = LocalContext.current
    val application = context.applicationContext as Application

    var selectedTheme by rememberSaveable { mutableStateOf(initialTheme) }
    var colorMenuItemText by remember { mutableStateOf("Color") }
    val scope = rememberCoroutineScope()
    var isFadingOut by remember { mutableStateOf<Boolean>(false) }
    var colorChangeJob by remember { mutableStateOf<Job?>(null) }

    val availableThemes = remember {
        listOf("Default", "Red", "Orange", "Yellow", "Green", "Turquoise", "Blue", "Purple")
    }

    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var isLabeled by remember { mutableStateOf(false) }
    var lastBackPressTime by rememberSaveable { mutableLongStateOf(0L) }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

    val viewModel =
        viewModel<CanvasViewModel>(factory = CanvasViewModelFactory(application = application))
    val currentPathState = viewModel.currentPathState.collectAsState()
    val pathState = viewModel.pathState.collectAsState()
    val isHandwritingMode by viewModel.isHandwritingMode.collectAsState()

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) { // 2 seconds
            onDismiss()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.navigate_back_description),
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
            }
            lastBackPressTime = currentTime
        }
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(sketchTitle, selectedTheme)
            onSaveTriggerConsumed()
        }
    }

    LaunchedEffect(isEraserMode, usePressure, strokeWidth, strokeColor) {
        viewModel.onAction(
            DrawingAction.UpdateTool(
                isEraserMode, usePressure, strokeWidth, strokeColor
            )
        )
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
                color = Color.Transparent, darkIcons = !isDarkTheme
            )
            onDispose {
                systemUiController.setStatusBarColor(
                    color = originalStatusBarColor
                )
            }
        }

        val hazeThinColor = colorScheme.surfaceDim
        val labelColor = extendedMaterialColorScheme.label

        val currentExtendedColorScheme = extendedMaterialColorScheme
        val themeDrawColors = remember(isDarkTheme, currentExtendedColorScheme) {
            listOf(
                currentExtendedColorScheme.drawDefault,
                currentExtendedColorScheme.drawRed,
                currentExtendedColorScheme.drawOrange,
                currentExtendedColorScheme.drawYellow,
                currentExtendedColorScheme.drawGreen,
                currentExtendedColorScheme.drawTurquoise,
                currentExtendedColorScheme.drawBlue,
                currentExtendedColorScheme.drawPurple
            )
        }

        LaunchedEffect(themeDrawColors) {
            viewModel.setDrawColors(themeDrawColors)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surfaceContainer)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal
                    )
                )
        ) {
            var isSideControlsCollapsed by rememberSaveable { mutableStateOf(false) }


            NoteCanvas(
                paths = pathState.value.paths,
                currentPath = currentPathState.value.path,
                currentToolState = currentPathState.value,
                onAction = viewModel::onAction,
                isHandwritingMode = isHandwritingMode,
                gridEnabled = pathState.value.gridEnabled,
                debugText = true,
                debugPoints = false,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            )

            if (showColorPicker || showPenSizePicker) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onColorPickerDismiss()
                            onPenSizePickerDismiss()
                        }
                )
            }


            // Top bar
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
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000L) { // 2 seconds
                            onDismiss()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.open_navigation_menu),
                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                )
                            }
                            lastBackPressTime = currentTime
                        }
                    }, modifier = Modifier.padding(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                val titleTextStyle = typography.titleLarge.merge(
                    TextStyle(
                        fontFamily = QuicksandTitleVariable,
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurface
                    )
                )
                BasicTextField(
                    value = sketchTitle,
                    onValueChange = onSketchTitleChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (sketchTitle.isEmpty()) {
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
                        onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownNoteMenu(
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
                                        Icon(
                                            Icons.Default.BookmarkBorder,
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
                                        delay(2500) // Keep current theme color for 2.5 seconds
                                        isFadingOut = true // Fade out animation for 0.5 seconds
                                        delay(500)
                                        colorMenuItemText = "Color"
                                        isFadingOut = false
                                    }
                                }, dismissOnClick = false, icon = {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = "Color",
                                        tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary
                                    )
                                }, textColor = animatedTextColor
                            ), MenuItem(
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
                                        Icon(
                                            Icons.Default.Cloud, contentDescription = "Online note"


                                        )
                                    }
                                })
                        ),
                        hazeState = hazeState
                    )
                }
            }




            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom
                            )
                        )
                        .padding(bottom = 80.dp)
                ) {
                    AnimatedVisibility(
                        visible = showColorPicker || showPenSizePicker,
                        enter = expandVertically(
                            expandFrom = Alignment.Bottom,
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 500))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(208.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colorScheme.surfaceDim)
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.ultraThin(hazeThinColor)
                                )
                        ) {
                            this@Row.AnimatedVisibility(
                                visible = showColorPicker,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 300))
                            ) {
                                ColorPicker(
                                    selectedColor = currentPathState.value.color,
                                    colors = themeDrawColors,
                                    onAction = { action ->
                                        if (action is DrawingAction.SelectColor) {
                                            onColorSelected(action.color)
                                        }
                                        viewModel.onAction(action)
                                    })
                            }
                            this@Row.AnimatedVisibility(
                                visible = showPenSizePicker,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                exit = fadeOut(animationSpec = tween(durationMillis = 300))
                            ) {
                                PenSizePicker(
                                    selectedSize = currentPathState.value.strokeWidth,
                                    sizes = listOf(2f, 5f, 10f, 20f, 40f, 60f, 80f, 100f),
                                    onAction = { action ->
                                        if (action is DrawingAction.SelectStrokeWidth) {
                                            onPenSizeSelected(action.strokeWidth)
                                        }
                                        viewModel.onAction(action)
                                    })
                            }
                        }
                    }
                    Spacer(Modifier.width(64.dp))
                }
            }


            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                    .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                VerticalFloatingToolbar(
                    onAction = viewModel::onAction,
                    isHandwritingMode = isHandwritingMode,
                    onToggleHandwritingMode = { enabled ->
                        viewModel.onAction(
                            DrawingAction.ToggleHandwritingMode(
                                enabled
                            )
                        )
                    },
                    onCollapseChange = { isSideControlsCollapsed = it },
                    isCollapsed = isSideControlsCollapsed,
                    hazeState = hazeState,
                    hazeThinColor = hazeThinColor,
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class CanvasViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CanvasViewModel::class.java)) {
            return CanvasViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(
    ExperimentalHazeMaterialsApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun VerticalFloatingToolbar(
    onAction: (DrawingAction) -> Unit,
    isHandwritingMode: Boolean,
    onToggleHandwritingMode: (Boolean) -> Unit,
    onCollapseChange: (Boolean) -> Unit,
    isCollapsed: Boolean,
    hazeState: HazeState,
    hazeThinColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(100f))
            .background(hazeThinColor)
            .hazeEffect(
                state = hazeState,
                style = HazeMaterials.ultraThin(hazeThinColor),
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.spacedBy(
            8.dp, Alignment.Top
        )
    ) {
        if (!isCollapsed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo (Long press to clear)",
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onAction(DrawingAction.Undo) },
                                onLongPress = { onAction(DrawingAction.ClearCanvas) })
                        })
                }
                IconButton(onClick = { onAction(DrawingAction.Redo) }) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }
            }
        }

        FilledIconButton(
            onClick = { onCollapseChange(!isCollapsed) },
            modifier = Modifier
                .height(56.dp)
                .width(48.dp)
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = if (isCollapsed) "Expand toolbar" else "Collapse toolbar"
            )
        }

        if (!isCollapsed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                FilledIconButton(
                    onClick = { onToggleHandwritingMode(true) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isHandwritingMode) colorScheme.tertiary else Color.Transparent,
                        contentColor = if (isHandwritingMode) colorScheme.onTertiary else colorScheme.onSurfaceVariant,
                    )
                ) {
                    Icon(
                        Icons.Default.Gesture, contentDescription = "Handwriting Mode"
                    )
                }
                FilledIconButton(
                    onClick = { onToggleHandwritingMode(false) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (!isHandwritingMode) colorScheme.tertiary else Color.Transparent,
                        contentColor = if (!isHandwritingMode) colorScheme.onTertiary else colorScheme.onSurfaceVariant,
                    )
                ) {
                    Icon(
                        Icons.Default.Edit, contentDescription = "Pen Mode"
                    )
                }
            }
        }
    }
}


@Composable
fun ColorPicker(
    selectedColor: Color,
    colors: List<Color>,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorButtonSize = 28.dp
    val itemsInRow = 4
    val spacing = 16.dp

    Column(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val numberOfRows = (colors.size + itemsInRow - 1) / itemsInRow

        repeat(numberOfRows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val startIndex = rowIndex * itemsInRow
                val endIndex = minOf(startIndex + itemsInRow, colors.size)

                for (i in startIndex until endIndex) {
                    val color = colors[i]
                    val isSelected = selectedColor == color

                    if (i > startIndex) {
                        Spacer(modifier = Modifier.width(spacing))
                    }

                    Box(
                        modifier = Modifier
                            .size(colorButtonSize)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 2.dp else 0.8.dp, Color.Gray, shape = CircleShape
                            )
                            .clickable {
                                onAction(DrawingAction.SelectColor(color))
                            })
                }
            }
        }
    }
}


@Composable
fun PenSizePicker(
    selectedSize: Float,
    sizes: List<Float>,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeButtonSize = 28.dp
    val itemsInRow = 4
    val spacing = 16.dp
    val onSurfaceColor = colorScheme.onSurface
    val maxPenSize = sizes.maxOrNull() ?: 1f

    Column(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        val numberOfRows = (sizes.size + itemsInRow - 1) / itemsInRow

        repeat(numberOfRows) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val startIndex = rowIndex * itemsInRow
                val endIndex = minOf(startIndex + itemsInRow, sizes.size)

                for (i in startIndex until endIndex) {
                    val size = sizes[i]
                    val isSelected = selectedSize == size

                    if (i > startIndex) {
                        Spacer(modifier = Modifier.width(spacing))
                    }

                    Box(
                        modifier = Modifier
                            .size(sizeButtonSize)
                            .clip(CircleShape)
                            .border(
                                if (isSelected) 2.dp else 0.8.dp, Color.Gray, shape = CircleShape
                            )
                            .clickable {
                                onAction(DrawingAction.SelectStrokeWidth(size))
                            }, contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = this.size.minDimension / 2
                            drawCircle(
                                color = onSurfaceColor, radius = (size / maxPenSize) * maxRadius
                            )
                        }
                    }
                }
            }
        }
    }
}