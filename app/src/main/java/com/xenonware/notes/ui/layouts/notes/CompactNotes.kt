@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.notes.ui.layouts.notes

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.res.GoogleProfilBorder
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.res.SpannedModeFAB
import com.xenon.mylibrary.res.XenonSnackbar
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.notes.R
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.notes.presentation.sign_in.SignInViewModel
import com.xenonware.notes.ui.layouts.NoteCard
import com.xenonware.notes.ui.res.ListContent
import com.xenonware.notes.ui.res.sheets.AudioViewType
import com.xenonware.notes.ui.res.sheets.ListItem
import com.xenonware.notes.ui.res.sheets.NoteAudioSheet
import com.xenonware.notes.ui.res.sheets.NoteListSheet
import com.xenonware.notes.ui.res.sheets.NoteSketchSheet
import com.xenonware.notes.ui.res.sheets.NoteTextSheet
import com.xenonware.notes.ui.theme.LocalIsDarkTheme
import com.xenonware.notes.ui.theme.XenonTheme
import com.xenonware.notes.ui.theme.extendedMaterialColorScheme
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
import com.xenonware.notes.util.GlobalAudioPlayer
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NoteEditingViewModel
import com.xenonware.notes.viewmodel.NotesLayoutType
import com.xenonware.notes.viewmodel.NotesViewModel
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Suppress("unused")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactNotes(
    viewModel: NotesViewModel = viewModel(),
    noteEditingViewModel: NoteEditingViewModel = viewModel(),
    signInViewModel: SignInViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {
        val deviceConfig = LocalDeviceConfig.current

        val uLongSaver = Saver<ULong?, String>(
            save = { it?.toString() ?: "null" },
            restore = { if (it == "null") null else it.toULong() })

        var editingNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
        var titleState by rememberSaveable { mutableStateOf("") }
        var descriptionState by rememberSaveable { mutableStateOf("") }
        var editingNoteColor by rememberSaveable(stateSaver = uLongSaver) { mutableStateOf(null) }
        var saveTrigger by remember { mutableStateOf(false) }
        var addListItemTrigger by remember { mutableStateOf(false) }

        // Formatting states come from ViewModel, not local state
        val textSizes = listOf(16.sp, 20.sp, 24.sp, 28.sp)
        val vmFontSizeIndex by noteEditingViewModel.textFontSizeIndex.collectAsState()
        var currentSizeIndex by remember { mutableIntStateOf(vmFontSizeIndex) }
        val editorFontSize = textSizes[currentSizeIndex]


        val listTextSizes = remember { listOf(16.sp, 20.sp, 24.sp, 28.sp) }
        var currentListSizeIndex by rememberSaveable { mutableIntStateOf(1) }
        val listEditorFontSize = listTextSizes[currentListSizeIndex]

        var selectedAudioViewType by rememberSaveable { mutableStateOf(AudioViewType.Waveform) }

        val showTextNoteCard by viewModel.showTextCard.collectAsStateWithLifecycle()
        val showSketchNoteCard by viewModel.showSketchCard.collectAsStateWithLifecycle()
        val showAudioNoteCard by viewModel.showAudioCard.collectAsStateWithLifecycle()
        val showListNoteCard by viewModel.showListCard.collectAsStateWithLifecycle()
        var listTitleState by rememberSaveable { mutableStateOf("") }
        val listItemsState = rememberSaveable(saver = listSaver(save = { list: List<ListItem> ->
            list.map { it.id.toString() + "," + it.text + "," + it.isChecked.toString() }
        }, restore = { list: List<String> ->
            list.map { itemString ->
                val parts = itemString.split(",")
                ListItem(parts[0].toLong(), parts[1], parts[2].toBoolean())
            }.toMutableStateList()
        })) { mutableStateListOf() }
        var nextListItemId by rememberSaveable { mutableLongStateOf(0L) }

        var showSketchSizePopup by remember { mutableStateOf(false) }
        var showColorPicker by remember { mutableStateOf(false) }
        var isEraserMode by remember { mutableStateOf(false) }
        var usePressure by remember { mutableStateOf(true) }
        var currentSketchSize by remember { mutableFloatStateOf(10f) }
        val initialSketchColor = colorScheme.onSurface
        var currentSketchColor by remember { mutableStateOf(initialSketchColor) }

        val noteItemsWithHeaders = viewModel.noteItems

        val density = LocalDensity.current

        val configuration = LocalConfiguration.current
        val appHeight = configuration.screenHeightDp.dp
        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        val hazeState = rememberHazeState()

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        val currentSearchQuery by viewModel.searchQuery.collectAsState()
        val notesLayoutType by viewModel.notesLayoutType.collectAsState()

        val lazyListState = rememberLazyListState()

        var selectedNoteIds by remember { mutableStateOf(emptySet<Int>()) }
        val isSelectionModeActive = selectedNoteIds.isNotEmpty()
        var isAddModeActive by rememberSaveable { mutableStateOf(false) }
        var isSearchActive by rememberSaveable { mutableStateOf(false) }

        var showResizeValue by remember { mutableStateOf(false) }
        var resizeTimerKey by remember { mutableIntStateOf(0) }

        val listItemLineCount by viewModel.listItemLineCount.collectAsState()
        val gridColumnCount by viewModel.gridColumnCount.collectAsState()

        val currentListMaxLines = when (listItemLineCount) {
            3 -> 3
            9 -> 9
            else -> Int.MAX_VALUE
        }
        val currentGridColumns = gridColumnCount

        val gridMaxLines = 20
        val allLabels by viewModel.labels.collectAsState()
        var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }

        var hasAudioContent by rememberSaveable { mutableStateOf(false) }

        val screenWidthDp = with(density) { appSize.width.toDp() }.value.toInt()

        val context = LocalContext.current

        fun onResizeClick() {
            when (notesLayoutType) {
                NotesLayoutType.LIST -> viewModel.cycleListItemLineCount()
                NotesLayoutType.GRID -> viewModel.cycleGridColumnCount(screenWidthDp)
            }
            showResizeValue = true
            resizeTimerKey++
        }

        LaunchedEffect(resizeTimerKey) {
            if (showResizeValue) {
                delay(2000)
                showResizeValue = false
            }
        }
        LaunchedEffect(currentSizeIndex) {
            if (currentSizeIndex != vmFontSizeIndex) {
                noteEditingViewModel.setTextFontSizeIndex(currentSizeIndex)
            }
        }


        fun onListTextResizeClick() {
            currentListSizeIndex = (currentListSizeIndex + 1) % listTextSizes.size
        }

        fun resetNoteState() {
            editingNoteId = null
            titleState = ""
            descriptionState = ""
            editingNoteColor = null
            listTitleState = ""
            listItemsState.clear()
            nextListItemId = 0L
            currentListSizeIndex = 1
            selectedAudioViewType = AudioViewType.Waveform
            selectedLabelId = null
            // Clear ViewModel state immediately when closing
            noteEditingViewModel.clearAllStates()
        }

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

        val isDarkTheme = LocalIsDarkTheme.current


        val vmTextTheme by noteEditingViewModel.textTheme.collectAsState()
        val vmListTheme by noteEditingViewModel.listTheme.collectAsState()
        val vmAudioTheme by noteEditingViewModel.audioTheme.collectAsState()
        val vmSketchTheme by noteEditingViewModel.sketchTheme.collectAsState()

        val selectedNoteTheme = when {
            showTextNoteCard -> vmTextTheme
            showListNoteCard -> vmListTheme
            showAudioNoteCard -> vmAudioTheme
            showSketchNoteCard -> vmSketchTheme
            else -> colorThemeMap[editingNoteColor] ?: "Default"
        }

        val googleAuthUiClient = remember {
            GoogleAuthUiClient(
                context = context.applicationContext,
                oneTapClient = Identity.getSignInClient(context.applicationContext)
            )
        }
        val signInViewModel: SignInViewModel = viewModel()
        val sharedPreferenceManager = remember { SharedPreferenceManager(context) }
        val isBlackedOut by produceState(
            initialValue = sharedPreferenceManager.blackedOutModeEnabled && isDarkTheme
        ) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "blacked_out_mode_enabled") {
                    value = sharedPreferenceManager.blackedOutModeEnabled
                }
            }
            sharedPreferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(
                listener
            )
            awaitDispose {
                sharedPreferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                    listener
                )
            }
        }

        val themeColorMap = remember {
            mapOf(
                "Red" to noteRedLight.value,
                "Orange" to noteOrangeLight.value,
                "Yellow" to noteYellowLight.value,
                "Green" to noteGreenLight.value,
                "Turquoise" to noteTurquoiseLight.value,
                "Blue" to noteBlueLight.value,
                "Purple" to notePurpleLight.value
            )
        }

        val isAnyNoteSheetOpen =
            showTextNoteCard || showSketchNoteCard || showAudioNoteCard || showListNoteCard

        val textEditorContent: @Composable (RowScope.() -> Unit)? = if (showTextNoteCard) {
            @Composable {
                XenonTheme(
                    darkTheme = isDarkTheme,
                    useDefaultTheme = selectedNoteTheme == "Default",
                    useRedTheme = selectedNoteTheme == "Red",
                    useOrangeTheme = selectedNoteTheme == "Orange",
                    useYellowTheme = selectedNoteTheme == "Yellow",
                    useGreenTheme = selectedNoteTheme == "Green",
                    useTurquoiseTheme = selectedNoteTheme == "Turquoise",
                    useBlueTheme = selectedNoteTheme == "Blue",
                    usePurpleTheme = selectedNoteTheme == "Purple",
                    dynamicColor = selectedNoteTheme == "Default"

                ) {
                    val vmIsBold by noteEditingViewModel.textIsBold.collectAsState()
                    val vmIsItalic by noteEditingViewModel.textIsItalic.collectAsState()
                    val vmIsUnderlined by noteEditingViewModel.textIsUnderlined.collectAsState()
                    val vmFontSizeIndex by noteEditingViewModel.textFontSizeIndex.collectAsState()

                    Row {
                        val toggledColor = colorScheme.primary
                        val defaultColor = Color.Transparent
                        val toggledIconColor = colorScheme.onPrimary
                        val defaultIconColor = colorScheme.onSurface
                        FilledIconButton(
                            onClick = {
                                noteEditingViewModel.setTextIsBold(!vmIsBold)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (vmIsBold) toggledColor else defaultColor,
                                contentColor = if (vmIsBold) toggledIconColor else defaultIconColor
                            )
                        ) {
                            Icon(Icons.Rounded.FormatBold, contentDescription = stringResource(R.string.bold_text))
                        }
                        FilledIconButton(
                            onClick = {
                                noteEditingViewModel.setTextIsItalic(!vmIsItalic)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (vmIsItalic) toggledColor else defaultColor,
                                contentColor = if (vmIsItalic) toggledIconColor else defaultIconColor
                            )
                        ) {
                            Icon(Icons.Rounded.FormatItalic, contentDescription = stringResource(R.string.italic_text))
                        }
                        FilledIconButton(
                            onClick = {
                                noteEditingViewModel.setTextIsUnderlined(!vmIsUnderlined)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (vmIsUnderlined) toggledColor else defaultColor,
                                contentColor = if (vmIsUnderlined) toggledIconColor else defaultIconColor
                            )
                        ) {
                            Icon(
                                Icons.Rounded.FormatUnderlined,
                                contentDescription = stringResource(R.string.underline_text)
                            )
                        }
                        IconButton(onClick = {
                            val newIndex = (vmFontSizeIndex + 1) % textSizes.size
                            currentSizeIndex = newIndex
                            noteEditingViewModel.setTextFontSizeIndex(newIndex)
                        }) {
                            Icon(
                                Icons.Rounded.FormatSize,
                                contentDescription = stringResource(R.string.change_text_size)
                            )
                        }
                    }
                }
            }
        } else null

        val listEditorContent: @Composable (RowScope.() -> Unit)? = if (showListNoteCard) {
            @Composable {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { addListItemTrigger = true },
                        modifier = Modifier
                            .width(140.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.tertiary,
                            contentColor = colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_new_item_to_list)
                        )
                    }
                    IconButton(onClick = ::onListTextResizeClick) {
                        Icon(
                            Icons.Rounded.FormatSize,
                            contentDescription = stringResource(R.string.change_text_size)
                        )
                    }
                }
            }
        } else null

        val audioEditorContent: @Composable (RowScope.() -> Unit)? = if (showAudioNoteCard) {
            @Composable {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val waveformCornerTopStart by animateDpAsState(targetValue = 28.dp, label = "")
                    val waveformCornerBottomStart by animateDpAsState(
                        targetValue = 28.dp, label = ""
                    )
                    val waveformCornerTopEnd by animateDpAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Waveform) 28.dp else 8.dp,
                        label = ""
                    )
                    val waveformCornerBottomEnd by animateDpAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Waveform) 28.dp else 8.dp,
                        label = ""
                    )
                    val animatedWaveformShape = RoundedCornerShape(
                        topStart = waveformCornerTopStart,
                        bottomStart = waveformCornerBottomStart,
                        topEnd = waveformCornerTopEnd,
                        bottomEnd = waveformCornerBottomEnd
                    )

                    val transcriptCornerTopStart by animateDpAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Transcript) 28.dp else 8.dp,
                        label = ""
                    )
                    val transcriptCornerBottomStart by animateDpAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Transcript) 28.dp else 8.dp,
                        label = ""
                    )
                    val transcriptCornerTopEnd by animateDpAsState(targetValue = 28.dp, label = "")
                    val transcriptCornerBottomEnd by animateDpAsState(
                        targetValue = 28.dp, label = ""
                    )
                    val animatedTranscriptShape = RoundedCornerShape(
                        topStart = transcriptCornerTopStart,
                        bottomStart = transcriptCornerBottomStart,
                        topEnd = transcriptCornerTopEnd,
                        bottomEnd = transcriptCornerBottomEnd
                    )

                    val waveformContainerColor by animateColorAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Waveform) colorScheme.tertiary else colorScheme.surfaceBright,
                        label = ""
                    )
                    val waveformContentColor by animateColorAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Waveform) colorScheme.onTertiary else colorScheme.onSurface,
                        label = ""
                    )

                    val transcriptContainerColor by animateColorAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Transcript) colorScheme.tertiary else colorScheme.surfaceBright,
                        label = ""
                    )
                    val transcriptContentColor by animateColorAsState(
                        targetValue = if (selectedAudioViewType == AudioViewType.Transcript) colorScheme.onTertiary else colorScheme.onSurface,
                        label = ""
                    )

                    FilledIconButton(
                        onClick = { selectedAudioViewType = AudioViewType.Waveform },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = waveformContainerColor,
                            contentColor = waveformContentColor
                        ),
                        shape = animatedWaveformShape,
                        modifier = Modifier
                            .width(95.dp)
                            .height(56.dp)
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = stringResource(R.string.waveform_view)
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    FilledIconButton(
                        onClick = { selectedAudioViewType = AudioViewType.Transcript },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = transcriptContainerColor,
                            contentColor = transcriptContentColor
                        ),
                        shape = animatedTranscriptShape,
                        modifier = Modifier
                            .width(95.dp)
                            .height(56.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Article,
                            contentDescription = stringResource(R.string.transcript_view)
                        )
                    }
                }
            }
        } else null

        val sketchEditorContent: @Composable (RowScope.() -> Unit)? = if (showSketchNoteCard) {
            @Composable {
                val sketchSizes = remember { listOf(2f, 5f, 10f, 20f, 40f, 60f, 80f, 100f) }
                val maxPenSize = sketchSizes.maxOrNull() ?: 1f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (showSketchSizePopup) colorScheme.primary else colorScheme.onSurface.copy(
                                    alpha = 0.6f
                                ),
                                CircleShape
                            )
                            .background(colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                            .clickable { showSketchSizePopup = true; showColorPicker = false },
                        contentAlignment = Alignment.Center
                    ) {
                        val onSurface = colorScheme.onSurface
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxRadius = this.size.minDimension * 8f / 10f
                            val rectWidth = (currentSketchSize / maxPenSize) * maxRadius
                            drawRoundRect(
                                color = onSurface,
                                topLeft = Offset(
                                    x = (this.size.width - rectWidth) / 2f,
                                    y = (this.size.height - maxRadius) / 2f
                                ),
                                size = Size(width = rectWidth, height = maxRadius),
                                cornerRadius = CornerRadius(100f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(38.dp)
                            .border(
                                2.dp,
                                if (showColorPicker) colorScheme.primary else colorScheme.onSurface.copy(
                                    alpha = 0.6f
                                ),
                                CircleShape
                            )
                            .border(4.dp, colorScheme.surfaceDim, CircleShape)
                            .background(currentSketchColor, CircleShape)
                    ) {
                        IconButton(onClick = {
                            showColorPicker = true; showSketchSizePopup = false
                        }) {}
                    }

                    FilledIconButton(
                        onClick = { isEraserMode = !isEraserMode },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isEraserMode) colorScheme.tertiary else Color.Transparent,
                            contentColor = if (isEraserMode) colorScheme.onTertiary else colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.eraser),
                            contentDescription = "Eraser"
                        )
                    }

                    IconButton(onClick = { usePressure = !usePressure }) {
                        Icon(
                            painter = painterResource(id = if (usePressure) R.drawable.dynamic else R.drawable.constant),
                            contentDescription = "Toggle Pressure/Speed"
                        )
                    }
                }
            }
        } else null

        val onAddModeToggle = { isAddModeActive = !isAddModeActive }

        val commonToolbarProps =
            @Composable { iconsAlphaDuration: Int, showActionIconsExceptSearch: Boolean ->
                Row {
                    val iconAlphaTarget = if (isSearchActive) 0f else 1f

                    val listIconAlpha by animateFloatAsState(
                        targetValue = iconAlphaTarget, animationSpec = tween(
                            durationMillis = iconsAlphaDuration,
                            delayMillis = if (isSearchActive) 0 else 0
                        ), label = "ListIconAlpha"
                    )
                    IconButton(
                        onClick = {
                            val newLayout =
                                if (notesLayoutType == NotesLayoutType.LIST) NotesLayoutType.GRID else NotesLayoutType.LIST
                            viewModel.setNotesLayoutType(newLayout)
                        },
                        modifier = Modifier.alpha(listIconAlpha),
                        enabled = !isSearchActive && showActionIconsExceptSearch
                    ) {
                        Icon(
                            imageVector = if (notesLayoutType == NotesLayoutType.LIST) Icons.Rounded.ViewStream else Icons.Rounded.ViewModule,
                            contentDescription = stringResource(R.string.change_layout),
                            tint = colorScheme.onSurface
                        )
                    }

                    val resizeIconAlpha by animateFloatAsState(
                        targetValue = iconAlphaTarget, animationSpec = tween(
                            durationMillis = iconsAlphaDuration,
                            delayMillis = if (isSearchActive) 100 else 0
                        ), label = "ResizeIconAlpha"
                    )
                    IconButton(
                        onClick = ::onResizeClick,
                        modifier = Modifier.alpha(resizeIconAlpha),
                        enabled = !isSearchActive && showActionIconsExceptSearch
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            this@Row.AnimatedVisibility(
                                visible = showResizeValue,
                                enter = fadeIn(tween(0)),
                                exit = fadeOut(tween(500))
                            ) {
                                val text = when (notesLayoutType) {
                                    NotesLayoutType.LIST -> if (listItemLineCount == 3 || listItemLineCount == 9) listItemLineCount.toString() else "Max"
                                    NotesLayoutType.GRID -> gridColumnCount.toString()
                                }
                                Text(
                                    text,
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            this@Row.AnimatedVisibility(
                                visible = !showResizeValue,
                                enter = fadeIn(tween(500)),
                                exit = fadeOut(tween(0))
                            ) {
                                Icon(
                                    Icons.Rounded.OpenWith,
                                    contentDescription = stringResource(R.string.resize_notes),
                                    tint = colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val settingsIconAlpha by animateFloatAsState(
                        targetValue = iconAlphaTarget, animationSpec = tween(
                            durationMillis = iconsAlphaDuration,
                            delayMillis = if (isSearchActive) 200 else 0
                        ), label = "SettingsIconAlpha"
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.alpha(settingsIconAlpha),
                        enabled = !isSearchActive && showActionIconsExceptSearch
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = colorScheme.onSurface
                        )
                    }
                }
            }
        val fabOverride = if (showTextNoteCard) {
            @Composable {
                //Text Note
                val canSave = titleState.isNotBlank()
                FloatingActionButton(
                    onClick = { if (canSave) saveTrigger = true },
                    containerColor = colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.save_note),
                        tint = if (canSave) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        } else if (showListNoteCard) {
            {
                //List Note
                val canSave = listTitleState.isNotBlank() && listItemsState.any { it.text.isNotBlank() }
                FloatingActionButton(
                    onClick = { if (canSave) saveTrigger = true },
                    containerColor = colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.save_list_note),
                        tint = if (canSave) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        } else if (showAudioNoteCard) {
            {
                //Audio Note
                val canSave = titleState.isNotBlank() && hasAudioContent
                FloatingActionButton(
                    onClick = { if (canSave) saveTrigger = true },
                    containerColor = colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.save_audio_note),
                        tint = if (canSave) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        } else if (showSketchNoteCard) {
            {
                //Sketch Note
                val canSave = titleState.isNotBlank() /*&& hasSketchContent*/
                FloatingActionButton(
                    onClick = { if (canSave) saveTrigger = true }, containerColor = colorScheme.primary
                ) {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = stringResource(R.string.save_sketch_note),
                        tint = if (canSave) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                            alpha = 0.38f
                        )
                    )
                }
            }
        } else null

        ModalNavigationDrawer(
            drawerContent = {
                ListContent(
                    notesViewModel = viewModel,
                    signInViewModel = signInViewModel,
                    googleAuthUiClient = googleAuthUiClient,
                    onFilterSelected = { viewModel.setNoteFilterType(it) })
            }, drawerState = drawerState, gesturesEnabled = !isAnyNoteSheetOpen
        ) {
            Scaffold(snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    XenonSnackbar(
                        snackbarData = data,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }, bottomBar = {
                XenonTheme(
                    darkTheme = isDarkTheme,
                    useDefaultTheme = selectedNoteTheme == "Default",
                    useRedTheme = selectedNoteTheme == "Red",
                    useOrangeTheme = selectedNoteTheme == "Orange",
                    useYellowTheme = selectedNoteTheme == "Yellow",
                    useGreenTheme = selectedNoteTheme == "Green",
                    useTurquoiseTheme = selectedNoteTheme == "Turquoise",
                    useBlueTheme = selectedNoteTheme == "Blue",
                    usePurpleTheme = selectedNoteTheme == "Purple",
                    dynamicColor = selectedNoteTheme == "Default"
                ) {
                    val bottomPaddingNavigationBar =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val imePaddingValues = WindowInsets.ime.asPaddingValues()
                    val imeHeight = imePaddingValues.calculateBottomPadding()

                    val targetBottomPadding =
                        remember(imeHeight, bottomPaddingNavigationBar, imePaddingValues) {
                            val calculatedPadding = if (imeHeight > bottomPaddingNavigationBar) {
                                imeHeight + LargePadding
                            } else {
                                max(
                                    bottomPaddingNavigationBar,
                                    imePaddingValues.calculateTopPadding()
                                ) + LargePadding
                            }
                            max(calculatedPadding, 0.dp)
                        }

                    val animatedBottomPadding by animateDpAsState(
                        targetValue = targetBottomPadding, animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ), label = "bottomPaddingAnimation"
                    )

                    FloatingToolbarContent(
                        hazeState = hazeState,
                        currentSearchQuery = currentSearchQuery,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        lazyListState = lazyListState,
                        allowToolbarScrollBehavior = !isAppBarExpandable && !isAnyNoteSheetOpen,
                        selectedNoteIds = selectedNoteIds.toList(),
                        onClearSelection = { selectedNoteIds = emptySet() },
                        isAddModeActive = isAddModeActive,
                        isSearchActive = isSearchActive,
                        onIsSearchActiveChange = { isSearchActive = it },
                        defaultContent = commonToolbarProps,
                        onAddModeToggle = onAddModeToggle,
                        isSelectedColor = extendedMaterialColorScheme.inverseErrorContainer,
                        selectionContentOverride = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteItems(selectedNoteIds.toList())
                                        selectedNoteIds = emptySet()
                                    }, modifier = Modifier.width(192.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.delete),
                                        textAlign = TextAlign.Center,
                                        style = typography.bodyLarge.copy(
                                            fontFamily = QuicksandTitleVariable,
                                            color = extendedMaterialColorScheme.inverseOnErrorContainer
                                        )
                                    )
                                }
                            }
                        },
                        addModeContentOverride = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    resetNoteState()
                                    // Ensure ViewModel is cleared for new note
                                    noteEditingViewModel.clearTextState()
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                    viewModel.showTextCard()
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Rounded.TextFields,
                                        contentDescription = stringResource(R.string.add_text_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    resetNoteState()
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                    viewModel.showListCard()
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Rounded.Checklist,
                                        contentDescription = stringResource(R.string.add_list_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    GlobalAudioPlayer.getInstance().stopAudio()
                                    if (showAudioNoteCard) {
                                        viewModel.hideAudioCard()
                                        resetNoteState()
                                    } else {
                                        resetNoteState()
                                        isSearchActive = false
                                        viewModel.setSearchQuery("")
                                        viewModel.showAudioCard()
                                    }
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Rounded.Mic,
                                        contentDescription = stringResource(R.string.add_mic_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                    viewModel.showSketchCard()
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Rounded.Create,
                                        contentDescription = stringResource(R.string.add_pen_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        },
                        contentOverride = when {
                            showTextNoteCard -> textEditorContent
                            showListNoteCard -> listEditorContent
                            showAudioNoteCard -> audioEditorContent
                            showSketchNoteCard -> sketchEditorContent
                            else -> null
                        },
                        fabOverride = fabOverride,
                        isSpannedMode = deviceConfig.isSpannedMode,
                        fabOnLeftInSpannedMode = deviceConfig.fabOnLeft,
                        spannedModeHingeGap = deviceConfig.hingeGapDp,
                        spannedModeFab = {
                            SpannedModeFAB(
                                hazeState = hazeState,
                                onClick = deviceConfig.toggleFabSide,
                                modifier = Modifier.padding(bottom = animatedBottomPadding),
                                isSheetOpen = isAnyNoteSheetOpen
                            )
                        }
                    )
                }
            }) { scaffoldPadding ->
                if (isAnyNoteSheetOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {})
                }

                val context = LocalContext.current
                val googleAuthUiClient = remember {
                    GoogleAuthUiClient(
                        context = context.applicationContext,
                        oneTapClient = Identity.getSignInClient(context.applicationContext)
                    )
                }
                val signInViewModel: SignInViewModel = viewModel()
                val state by signInViewModel.state.collectAsStateWithLifecycle()
                val userData = googleAuthUiClient.getSignedInUser()

                ActivityScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding()
                        .hazeSource(hazeState)
                        .onSizeChanged {},
                    titleText = stringResource(id = R.string.app_name),
                    expandable = isAppBarExpandable,
                    navigationIconStartPadding = MediumPadding,
                    navigationIconPadding = if (state.isSignInSuccessful) SmallPadding else MediumPadding,
                    navigationIconSpacing = MediumSpacing,
                    navigationIcon = {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.open_navigation_menu),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onNavigationIconClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    hasNavigationIconExtraContent = state.isSignInSuccessful,
                    navigationIconExtraContent = {
                        if (state.isSignInSuccessful) {
                            Box(contentAlignment = Alignment.Center) {
                                @Suppress("KotlinConstantConditions")
                                GoogleProfilBorder(
                                    isSignedIn = state.isSignInSuccessful,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.5.dp
                                )
                                GoogleProfilePicture(
                                    noAccIcon = painterResource(id = R.drawable.default_icon),
                                    profilePictureUrl = userData?.profilePictureUrl,
                                    contentDescription = stringResource(R.string.profile_picture),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    },
                    actions = {},
                    content = {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = ExtraLargeSpacing)
                            ) {
                                if (noteItemsWithHeaders.isEmpty() && currentSearchQuery.isBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_notes_message),
                                            style = typography.bodyLarge
                                        )
                                    }
                                } else if (noteItemsWithHeaders.isEmpty() && currentSearchQuery.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_search_results),
                                            style = typography.bodyLarge
                                        )
                                    }
                                } else {
                                    when (notesLayoutType) {
                                        NotesLayoutType.LIST -> {
                                            LazyColumn(
                                                state = lazyListState,
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(
                                                    top = ExtraLargePadding,
                                                    bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                                                )
                                            ) {
                                                itemsIndexed(
                                                    items = noteItemsWithHeaders, key = { _, item ->
                                                        if (item is NotesItems) item.id else item.hashCode()
                                                    }) { index, item ->
                                                    when (item) {
                                                        is String -> {
                                                            Text(
                                                                text = item,
                                                                style = typography.titleMedium.copy(
                                                                    fontStyle = FontStyle.Italic
                                                                ),
                                                                fontWeight = FontWeight.Thin,
                                                                textAlign = TextAlign.Start,
                                                                fontFamily = QuicksandTitleVariable,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(
                                                                        top = if (index == 0) 0.dp else LargestPadding,
                                                                        bottom = SmallPadding,
                                                                        start = SmallPadding,
                                                                        end = LargestPadding
                                                                    )
                                                            )
                                                        }

                                                        is NotesItems -> {
                                                            NoteCard(
                                                                item = item,
                                                                notesViewModel = viewModel,
                                                                isSelected = selectedNoteIds.contains(
                                                                    item.id
                                                                ),
                                                                isSelectionModeActive = isSelectionModeActive,
                                                                onSelectItem = {
                                                                    selectedNoteIds =
                                                                        if (selectedNoteIds.contains(
                                                                                item.id
                                                                            )
                                                                        ) {
                                                                            selectedNoteIds - item.id
                                                                        } else {
                                                                            selectedNoteIds + item.id
                                                                        }
                                                                },
                                                                onEditItem = { itemToEdit ->
                                                                    editingNoteId = itemToEdit.id
                                                                    titleState = itemToEdit.title
                                                                    descriptionState =
                                                                        itemToEdit.description ?: ""
                                                                    listTitleState =
                                                                        itemToEdit.title
                                                                    editingNoteColor =
                                                                        itemToEdit.color?.toULong()
                                                                    selectedLabelId =
                                                                        itemToEdit.labels.firstOrNull()
                                                                    listItemsState.clear()
                                                                    nextListItemId = 0L
                                                                    currentListSizeIndex = 1
                                                                    itemToEdit.description?.let { desc ->
                                                                        val parsedItems =
                                                                            desc.split("\n")
                                                                                .mapNotNull { line ->
                                                                                    if (line.isBlank()) null
                                                                                    else {
                                                                                        val isChecked =
                                                                                            line.startsWith(
                                                                                                "[x]"
                                                                                            )
                                                                                        val text =
                                                                                            if (isChecked) line.substringAfter(
                                                                                                "[x] "
                                                                                            ).trim()
                                                                                            else line.substringAfter(
                                                                                                "[ ] "
                                                                                            ).trim()
                                                                                        ListItem(
                                                                                            nextListItemId++,
                                                                                            text,
                                                                                            isChecked
                                                                                        )
                                                                                    }
                                                                                }
                                                                        listItemsState.addAll(
                                                                            parsedItems
                                                                        )
                                                                    }
                                                                    when (itemToEdit.noteType) {
                                                                        NoteType.TEXT -> {
                                                                            // Initialize ALL ViewModel states with new note data
                                                                            noteEditingViewModel.setTextTitle(itemToEdit.title)
                                                                            noteEditingViewModel.setTextContent(descriptionState)
                                                                            noteEditingViewModel.setTextTheme(
                                                                                colorThemeMap[editingNoteColor] ?: "Default"
                                                                            )
                                                                            noteEditingViewModel.setTextLabelId(selectedLabelId)
                                                                            noteEditingViewModel.setTextIsOffline(itemToEdit.isOffline)
                                                                            // Reset formatting states to default
                                                                            noteEditingViewModel.setTextIsBold(false)
                                                                            noteEditingViewModel.setTextIsItalic(false)
                                                                            noteEditingViewModel.setTextIsUnderlined(false)
                                                                            noteEditingViewModel.setTextFontSizeIndex(1)

                                                                            isSearchActive = false
                                                                            viewModel.setSearchQuery("")
                                                                            viewModel.showTextCard()
                                                                        }

                                                                        NoteType.AUDIO -> {
                                                                            isSearchActive = false
                                                                            viewModel.setSearchQuery(
                                                                                ""
                                                                            )
                                                                            viewModel.showAudioCard()
                                                                            selectedAudioViewType =
                                                                                AudioViewType.Waveform
                                                                            editingNoteColor =
                                                                                itemToEdit.color?.toULong()
                                                                        }

                                                                        NoteType.LIST -> {
                                                                            // Initialize ViewModel BEFORE showListCard
                                                                            noteEditingViewModel.setListTitle(itemToEdit.title)
                                                                            noteEditingViewModel.setListTheme(
                                                                                colorThemeMap[editingNoteColor] ?: "Default"
                                                                            )
                                                                            noteEditingViewModel.setListLabelId(selectedLabelId)
                                                                            noteEditingViewModel.setListIsOffline(itemToEdit.isOffline)
                                                                            noteEditingViewModel.setListItems(listItemsState.toList())

                                                                            isSearchActive = false
                                                                            viewModel.setSearchQuery("")
                                                                            viewModel.showListCard()
                                                                        }

                                                                        NoteType.SKETCH -> {
                                                                            isSearchActive = false
                                                                            viewModel.setSearchQuery(
                                                                                ""
                                                                            )
                                                                            viewModel.showSketchCard()
                                                                        }
                                                                    }
                                                                },
                                                                maxLines = currentListMaxLines,
                                                                isNoteSheetOpen = isAnyNoteSheetOpen
                                                            )
                                                            val isLastItemInListOrNextIsHeader =
                                                                index == noteItemsWithHeaders.lastIndex || (index + 1 < noteItemsWithHeaders.size && noteItemsWithHeaders[index + 1] is String)

                                                            if (!isLastItemInListOrNextIsHeader) {
                                                                Spacer(
                                                                    modifier = Modifier.height(
                                                                        MediumPadding
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        NotesLayoutType.GRID -> {
                                            LazyVerticalStaggeredGrid(
                                                columns = StaggeredGridCells.Fixed(
                                                    currentGridColumns
                                                ),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(
                                                    top = ExtraLargePadding,
                                                    bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                                                ),
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    MediumPadding
                                                ),
                                                verticalItemSpacing = MediumPadding
                                            ) {
                                                items(noteItemsWithHeaders.filterIsInstance<NotesItems>()) { item ->
                                                    NoteCard(
                                                        item = item,
                                                        notesViewModel = viewModel,
                                                        isSelected = selectedNoteIds.contains(item.id),
                                                        isSelectionModeActive = isSelectionModeActive,
                                                        onSelectItem = {
                                                            selectedNoteIds =
                                                                if (selectedNoteIds.contains(item.id)) {
                                                                    selectedNoteIds - item.id
                                                                } else {
                                                                    selectedNoteIds + item.id
                                                                }
                                                        },
                                                        onEditItem = { itemToEdit ->
                                                            editingNoteId = itemToEdit.id
                                                            titleState = itemToEdit.title
                                                            descriptionState =
                                                                itemToEdit.description ?: ""
                                                            listTitleState = itemToEdit.title
                                                            editingNoteColor =
                                                                itemToEdit.color?.toULong()
                                                            selectedLabelId =
                                                                itemToEdit.labels.firstOrNull()
                                                            listItemsState.clear()
                                                            nextListItemId = 0L
                                                            currentListSizeIndex = 1

                                                            itemToEdit.description?.let { desc ->
                                                                val parsedItems = desc.split("\n")
                                                                    .mapNotNull { line ->
                                                                        if (line.isBlank()) return@mapNotNull null
                                                                        val isChecked =
                                                                            line.startsWith("[x]")
                                                                        val text =
                                                                            if (isChecked) line.substringAfter(
                                                                                "[x] "
                                                                            ).trim()
                                                                            else line.substringAfter(
                                                                                "[ ] "
                                                                            ).trim()
                                                                        ListItem(
                                                                            nextListItemId++,
                                                                            text,
                                                                            isChecked
                                                                        )
                                                                    }
                                                                listItemsState.addAll(parsedItems)
                                                            }

                                                            isSearchActive = false
                                                            viewModel.setSearchQuery("")

                                                            when (itemToEdit.noteType) {
                                                                NoteType.TEXT -> {
                                                                    // Initialize ALL ViewModel states with new note data
                                                                    noteEditingViewModel.setTextTitle(itemToEdit.title)
                                                                    noteEditingViewModel.setTextContent(descriptionState)
                                                                    noteEditingViewModel.setTextTheme(
                                                                        colorThemeMap[editingNoteColor] ?: "Default"
                                                                    )
                                                                    noteEditingViewModel.setTextLabelId(selectedLabelId)
                                                                    noteEditingViewModel.setTextIsOffline(itemToEdit.isOffline)
                                                                    // Reset formatting states to default
                                                                    noteEditingViewModel.setTextIsBold(false)
                                                                    noteEditingViewModel.setTextIsItalic(false)
                                                                    noteEditingViewModel.setTextIsUnderlined(false)
                                                                    noteEditingViewModel.setTextFontSizeIndex(1)

                                                                    isSearchActive = false
                                                                    viewModel.setSearchQuery("")
                                                                    viewModel.showTextCard()
                                                                }

                                                                NoteType.AUDIO -> {
                                                                    viewModel.showAudioCard()
                                                                    selectedAudioViewType =
                                                                        AudioViewType.Waveform
                                                                }

                                                                NoteType.LIST -> {
                                                                    // Initialize ViewModel BEFORE showListCard
                                                                    noteEditingViewModel.setListTitle(itemToEdit.title)
                                                                    noteEditingViewModel.setListTheme(
                                                                        colorThemeMap[editingNoteColor] ?: "Default"
                                                                    )
                                                                    noteEditingViewModel.setListLabelId(selectedLabelId)
                                                                    noteEditingViewModel.setListIsOffline(itemToEdit.isOffline)
                                                                    noteEditingViewModel.setListItems(listItemsState.toList())

                                                                    isSearchActive = false
                                                                    viewModel.setSearchQuery("")
                                                                    viewModel.showListCard()
                                                                }
                                                                NoteType.SKETCH -> viewModel.showSketchCard()
                                                            }
                                                        },
                                                        maxLines = gridMaxLines,
                                                        isNoteSheetOpen = isAnyNoteSheetOpen
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (isAddModeActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { isAddModeActive = false })
                                )
                            }
                        }
                    })

                AnimatedVisibility(
                    visible = showTextNoteCard,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BackHandler {
                        viewModel.hideTextCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    }

                    val vmTitle by noteEditingViewModel.textTitle.collectAsState()

                    NoteTextSheet(
                        textTitle = vmTitle,
                        onTextTitleChange = { newTitle ->
                            titleState = newTitle
                            noteEditingViewModel.setTextTitle(newTitle)
                        },
                        onDismiss = {
                            viewModel.hideTextCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },

                        onSave = { title, description, theme, labelId, isOffline ->
                            if (title.isBlank() && description.isBlank()) {
                                viewModel.hideTextCard()
                                resetNoteState()
                                return@NoteTextSheet
                            }

                            val colorLong = themeColorMap[theme]?.toLong()

                            if (editingNoteId != null) {
                                val existingNote =
                                    viewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }

                                if (existingNote != null) {
                                    val updatedNote = existingNote.copy(
                                        title = title.trim(),
                                        description = description.takeIf { it.isNotBlank() },
                                        color = colorLong,
                                        labels = labelId?.let { listOf(it) } ?: emptyList(),
                                        isOffline = isOffline)
                                    viewModel.updateItem(updatedNote, forceLocal = isOffline)
                                } else {
                                    viewModel.addItem(
                                        title = title.trim(),
                                        description = description.takeIf { it.isNotBlank() },
                                        noteType = NoteType.TEXT,
                                        color = colorLong,
                                        labels = labelId?.let { listOf(it) } ?: emptyList(),
                                        forceLocal = isOffline)
                                }
                            } else {
                                viewModel.addItem(
                                    title = title.trim(),
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.TEXT,
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    forceLocal = isOffline)
                            }

                            viewModel.hideTextCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        saveTrigger = saveTrigger,
                        onSaveTriggerConsumed = { saveTrigger = false },
                        onIsBoldChange = { },
                        onIsItalicChange = { },
                        onIsUnderlinedChange = { },
                        editorFontSize = editorFontSize,
                        toolbarHeight = 72.dp,
                        onThemeChange = { newThemeName ->
                            editingNoteColor = themeColorMap[newThemeName]
                            noteEditingViewModel.setTextTheme(newThemeName)
                        },
                        allLabels = allLabels,
                        onLabelSelected = {
                            selectedLabelId = it
                            noteEditingViewModel.setTextLabelId(it)
                        },
                        onAddNewLabel = { viewModel.addLabel(it) },
                        isBlackThemeActive = isBlackedOut,
                        isCoverModeActive = false,
                        noteEditingViewModel = noteEditingViewModel
                    )
                }

                AnimatedVisibility(
                    visible = showSketchNoteCard,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BackHandler {
                        viewModel.hideSketchCard()
                        isSearchActive = false // Disable search on dismiss
                        viewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    }
                    NoteSketchSheet(
                        sketchTitle = titleState,
                        onSketchTitleChange = { titleState = it },
                        onDismiss = {
                            viewModel.hideSketchCard()
                            isSearchActive = false // Disable search on dismiss
                            viewModel.setSearchQuery("") // Clear search query
                            resetNoteState()
                        },
                        initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                        onThemeChange = { newThemeName ->
                            editingNoteColor = themeColorMap[newThemeName]
                        },
                        onSave = { title, theme, labelId, isOffline ->
                            if (title.isBlank()) {
                                viewModel.hideSketchCard()
                                resetNoteState()
                                return@NoteSketchSheet
                            }

                            val colorLong = themeColorMap[theme]?.toLong()

                            if (editingNoteId != null) {
                                val existingNote =
                                    viewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }

                                existingNote?.let { it ->
                                    val updatedNote = it.copy(
                                        title = title.trim(),
                                        color = colorLong,
                                        labels = labelId?.let { it -> listOf(it) } ?: emptyList(),
                                        isOffline = isOffline)
                                    viewModel.updateItem(updatedNote, forceLocal = isOffline)
                                }
                            } else {
                                viewModel.addItem(
                                    title = title.trim(),
                                    description = null,
                                    noteType = NoteType.SKETCH,
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    forceLocal = isOffline)
                            }

                            viewModel.hideSketchCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        saveTrigger = saveTrigger,
                        onSaveTriggerConsumed = { saveTrigger = false },
                        isEraserMode = isEraserMode,
                        usePressure = usePressure,
                        strokeWidth = currentSketchSize,
                        strokeColor = currentSketchColor,
                        showColorPicker = showColorPicker,
                        onColorPickerDismiss = { showColorPicker = false }, // Callback to dismiss
                        onColorSelected = { color -> // Callback for selected color
                            currentSketchColor = color
                            showColorPicker = false
                        },
                        showPenSizePicker = showSketchSizePopup,
                        onPenSizePickerDismiss = { showSketchSizePopup = false },
                        onPenSizeSelected = { size ->
                            currentSketchSize = size
                            showSketchSizePopup = false
                        },
                        snackbarHostState = snackbarHostState, // Pass the snackbarHostState here
                        allLabels = allLabels,
                        initialSelectedLabelId = selectedLabelId,
                        onLabelSelected = { selectedLabelId = it },
                        onAddNewLabel = { viewModel.addLabel(it) },
                        isBlackThemeActive = isBlackedOut,
                        isCoverModeActive = false,
                        editingNoteId = editingNoteId,
                        notesViewModel = viewModel,
                    )
                }


                AnimatedVisibility(
                    visible = showAudioNoteCard,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    LaunchedEffect(showAudioNoteCard) {
                        if (!showAudioNoteCard) {
                            GlobalAudioPlayer.getInstance().stopAudio()
                        }
                    }
                    BackHandler {
                        viewModel.hideAudioCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    }
                    val context = LocalContext.current
                    NoteAudioSheet(
                        audioTitle = titleState,
                        onAudioTitleChange = { titleState = it },
                        onDismiss = {
                            viewModel.hideAudioCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                        onSave = { title, uniqueAudioId, theme, labelId, isOffline ->
                            if (title.isBlank() && uniqueAudioId.isBlank()) {
                                viewModel.hideAudioCard()
                                resetNoteState()
                                return@NoteAudioSheet
                            }

                            val colorLong = themeColorMap[theme]?.toLong()

                            if (editingNoteId != null) {
                                val existingNote =
                                    viewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }

                                existingNote?.let {
                                    val updatedNote = it.copy(
                                        title = title.trim(),
                                        description = uniqueAudioId.takeIf { it -> it.isNotBlank() },
                                        color = colorLong,
                                        labels = labelId?.let { it -> listOf(it) } ?: emptyList(),
                                        isOffline = isOffline)
                                    viewModel.updateItem(updatedNote, forceLocal = isOffline)
                                }
                            } else {
                                viewModel.addItem(
                                    title = title.trim(),
                                    description = uniqueAudioId.takeIf { it.isNotBlank() },
                                    noteType = NoteType.AUDIO,
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    forceLocal = isOffline)
                            }

                            viewModel.hideAudioCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        toolbarHeight = 72.dp,
                        saveTrigger = saveTrigger,
                        onSaveTriggerConsumed = { saveTrigger = false },
                        selectedAudioViewType = selectedAudioViewType,
                        initialAudioFilePath = descriptionState.let { uniqueId ->
                            File(
                                context.filesDir, "$uniqueId.mp3"
                            ).takeIf { it.exists() }?.absolutePath
                        },
                        onThemeChange = { newThemeName ->
                            editingNoteColor = themeColorMap[newThemeName]
                        },
                        allLabels = allLabels,
                        initialSelectedLabelId = selectedLabelId,
                        onLabelSelected = { selectedLabelId = it },
                        onAddNewLabel = { viewModel.addLabel(it) },
                        onHasUnsavedAudioChange = { hasAudioContent = it },
                        isBlackThemeActive = isBlackedOut,
                        isCoverModeActive = false,
                        editingNoteId = editingNoteId,
                        notesViewModel = viewModel,
                    )

                }

                AnimatedVisibility(
                    visible = showListNoteCard,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    BackHandler {
                        viewModel.hideListCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    }

                    NoteListSheet(
                        onDismiss = {
                            viewModel.hideListCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        onSave = { title, items, theme, labelId, isOffline ->
                            val nonEmptyItems = items.filter { it.text.isNotBlank() }
                            val description = nonEmptyItems.joinToString("\n") {
                                "${if (it.isChecked) "[x]" else "[ ]"} ${it.text}"
                            }

                            if (title.isBlank() && description.isBlank()) {
                                viewModel.hideListCard()
                                resetNoteState()
                                return@NoteListSheet
                            }

                            val colorLong = themeColorMap[theme]?.toLong()

                            if (editingNoteId != null) {
                                val existingNote = viewModel.noteItems.filterIsInstance<NotesItems>()
                                    .find { it.id == editingNoteId }

                                existingNote?.let {
                                    val updatedNote = it.copy(
                                        title = title.trim(),
                                        description = description.takeIf { it -> it.isNotBlank() },
                                        color = colorLong,
                                        labels = labelId?.let { it -> listOf(it) } ?: emptyList(),
                                        isOffline = isOffline
                                    )
                                    viewModel.updateItem(updatedNote, forceLocal = isOffline)
                                }
                            } else {
                                viewModel.addItem(
                                    title = title.trim(),
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.LIST,
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    forceLocal = isOffline
                                )
                            }

                            viewModel.hideListCard()
                            isSearchActive = false
                            viewModel.setSearchQuery("")
                            resetNoteState()
                        },
                        toolbarHeight = 72.dp,
                        saveTrigger = saveTrigger,
                        onSaveTriggerConsumed = { saveTrigger = false },
                        addItemTrigger = addListItemTrigger,
                        onAddItemTriggerConsumed = { addListItemTrigger = false },
                        editorFontSize = listEditorFontSize,
                        allLabels = allLabels,
                        onAddNewLabel = { viewModel.addLabel(it) },
                        noteEditingViewModel = noteEditingViewModel,
                        isBlackThemeActive = isBlackedOut,
                        isCoverModeActive = false
                    )
                }
            }
        }
    }
}