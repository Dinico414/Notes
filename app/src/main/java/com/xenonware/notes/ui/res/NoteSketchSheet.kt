package com.xenonware.notes.ui.res

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenonware.notes.NoteCanvas
import com.xenonware.notes.NoteControls
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
import com.xenonware.notes.viewmodel.CanvasViewModel
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
) {
    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current
    val context = LocalContext.current
    val application = context.applicationContext as Application

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
    var isLabeled by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    val originalStatusBarColor = Color.Transparent

    val viewModel = viewModel<CanvasViewModel>(factory = CanvasViewModelFactory(application = application))
    val currentPathState = viewModel.currentPathState.collectAsState()
    val pathState = viewModel.pathState.collectAsState()

    LaunchedEffect(saveTrigger) {
        if (saveTrigger) {
            onSave(sketchTitle, selectedTheme)
            onSaveTriggerConsumed()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {
                Spacer(modifier = Modifier.height(68.dp)) // Padding for the top bar
                NoteControls(
                    currentPathState.value.color,
                    themeDrawColors,
                    viewModel::onAction
                )
                NoteCanvas(
                    pathState.value.paths,
                    currentPathState.value.path,
                    viewModel::onAction,
                    gridEnabled = pathState.value.gridEnabled,
                    debugText = true,
                    debugPoints = false,
                )
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
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
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(4.dp)
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
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.padding(4.dp)
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
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Label")
                                    }
                                }),
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
                                        tint = if (selectedTheme == "Default") colorScheme.onSurfaceVariant else colorScheme.primary
                                    )
                                },
                                textColor = animatedTextColor
                            ),
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
