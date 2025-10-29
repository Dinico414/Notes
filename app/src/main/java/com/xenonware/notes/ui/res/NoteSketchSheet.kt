package com.xenonware.notes.ui.res

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.xenonware.notes.viewmodel.CanvasViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials


@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class, ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NoteSketchSheet(
    onDismiss: () -> Unit,
    initialColor: ULong? = null,
    onThemeChange: (String) -> Unit
) {
    val uLongSaver = Saver<ULong?, String>(
        save = { it?.toString() ?: "null" },
        restore = { if (it == "null") null else it.toULong() }
    )

    val hazeState = remember { HazeState() }
    val isDarkTheme = LocalIsDarkTheme.current
    val context = LocalContext.current
    val application = context.applicationContext as Application

    var selectedColor by rememberSaveable(stateSaver = uLongSaver) { mutableStateOf(initialColor) }

    val colorThemeMap = remember {
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

    val currentThemeName = colorThemeMap[selectedColor] ?: "Default"

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
    var showMenu by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var isLabeled by remember { mutableStateOf(false) }

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

    val viewModel = viewModel<CanvasViewModel>(factory = CanvasViewModelFactory(application = application))
    val currentPathState = viewModel.currentPathState.collectAsState()
    val pathState = viewModel.pathState.collectAsState()

    XenonTheme(
        darkTheme = isDarkTheme,
        useDefaultTheme = currentThemeName == "Default",
        useRedTheme = currentThemeName == "Red",
        useOrangeTheme = currentThemeName == "Orange",
        useYellowTheme = currentThemeName == "Yellow",
        useGreenTheme = currentThemeName == "Green",
        useTurquoiseTheme = currentThemeName == "Turquoise",
        useBlueTheme = currentThemeName == "Blue",
        usePurpleTheme = currentThemeName == "Purple",
        dynamicColor = currentThemeName == "Default"
    ) {
        val currentExtendedColorScheme = extendedMaterialColorScheme
        val (themeDrawColors, initialDrawColor) = remember(isDarkTheme, currentExtendedColorScheme) {
            val colors: List<Color> = listOf(
                currentExtendedColorScheme.drawDefault,
                currentExtendedColorScheme.drawRed,
                currentExtendedColorScheme.drawOrange,
                currentExtendedColorScheme.drawYellow,
                currentExtendedColorScheme.drawGreen,
                currentExtendedColorScheme.drawTurquoise,
                currentExtendedColorScheme.drawBlue,
                currentExtendedColorScheme.drawPurple
            )
            Pair(colors, colors.first())
        }
        val labelColor = currentExtendedColorScheme.label
        viewModel.setInitialDrawColor(initialDrawColor)

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceDim)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(100f))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(hazeThinColor),
                        ), verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, Modifier.padding(4.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "New Sketch Note",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = QuicksandTitleVariable),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurface
                    )
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
                                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Label")
                                        }
                                    }),
                                MenuItem(text = "Color", onClick = {
                                    val currentIndex = noteColors.indexOf(selectedColor)
                                    val nextIndex = (currentIndex + 1) % noteColors.size
                                    selectedColor = noteColors[nextIndex]
                                    onThemeChange(colorThemeMap[selectedColor] ?: "Default")
                                }, dismissOnClick = false, icon = {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = "Color",
                                        tint = selectedColor?.let { iconTintInvertMap[it] }
                                            ?: colorScheme.onSurfaceVariant
                                    )
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
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardColor) // Use cardColor for the background
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(innerPadding),
            ) {
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
        }
    }
}

// Added a ViewModelFactory for CanvasViewModel to pass initialDrawColor
@Suppress("UNCHECKED_CAST")
class CanvasViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CanvasViewModel::class.java)) {
            return CanvasViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
