@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.notes.ui.layouts.notes

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.res.GoogleProfilBorder
import com.xenon.mylibrary.res.GoogleProfilePicture
import com.xenon.mylibrary.res.XenonSnackbar
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.NoSpacing
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
fun CoverNotes(
    viewModel: NotesViewModel = viewModel(),
    noteEditingViewModel: NoteEditingViewModel = viewModel(),
    signInViewModel: SignInViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    appSize: IntSize,

    ) {
    val uLongSaver = Saver<ULong?, String>(
        save = { it?.toString() ?: "null" },
        restore = { if (it == "null") null else it.toULong() })

    var editingNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var titleState by rememberSaveable { mutableStateOf("") }
    var descriptionState by rememberSaveable { mutableStateOf("") }
    var editingNoteColor by rememberSaveable(stateSaver = uLongSaver) { mutableStateOf(null) }
    var saveTrigger by remember { mutableStateOf(false) }
    var addListItemTrigger by remember { mutableStateOf(false) }

    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderlined by remember { mutableStateOf(false) }
    val textSizes = listOf(16.sp, 20.sp, 24.sp, 28.sp)
    var currentSizeIndex by remember { mutableIntStateOf(1) }
    val editorFontSize = textSizes[currentSizeIndex]

    val listTextSizes = remember { listOf(16.sp, 20.sp, 24.sp, 28.sp) }
    var currentListSizeIndex by rememberSaveable { mutableIntStateOf(1) }
    val listEditorFontSize = listTextSizes[currentListSizeIndex]

    var selectedAudioViewType by rememberSaveable { mutableStateOf(AudioViewType.Waveform) } // State for audio view

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
    var showColorPicker by remember { mutableStateOf(false) } // This will now trigger the picker in NoteSketchSheet
    var isEraserMode by remember { mutableStateOf(false) }
    var usePressure by remember { mutableStateOf(true) }
    var currentSketchSize by remember { mutableFloatStateOf(10f) }
    val initialSketchColor = colorScheme.onSurface
    var currentSketchColor by remember { mutableStateOf(initialSketchColor) }


    val noteItemsWithHeaders = viewModel.noteItems

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

    var listNoteLineLimitIndex by rememberSaveable { mutableIntStateOf(0) } // 0: 3 lines, 1: 9 lines, 2: Unlimited
    var gridNoteColumnCountIndex by rememberSaveable { mutableIntStateOf(0) } // Cycles through column options

    val listLineLimits = remember { listOf(3, 9, Int.MAX_VALUE) }

    val gridColumnCountOptions = remember(layoutType) {
        when (layoutType) {
            LayoutType.COVER, LayoutType.SMALL, LayoutType.COMPACT -> listOf(2, 3)
            LayoutType.MEDIUM -> listOf(2, 3, 4)
            else -> listOf(4, 5, 6)
        }
    }

    val currentListMaxLines = listLineLimits[listNoteLineLimitIndex]
    val currentGridColumns = gridColumnCountOptions[gridNoteColumnCountIndex]
    val gridMaxLines = 20
    val allLabels by viewModel.labels.collectAsState()
    var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }

    var hasAudioContent by rememberSaveable { mutableStateOf(false) }

    fun onResizeClick() {
        if (notesLayoutType == NotesLayoutType.LIST) {
            listNoteLineLimitIndex = (listNoteLineLimitIndex + 1) % listLineLimits.size
        } else {
            gridNoteColumnCountIndex = (gridNoteColumnCountIndex + 1) % gridColumnCountOptions.size
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
    val selectedTextNoteTheme = colorThemeMap[editingNoteColor] ?: "Default"
    val context = LocalContext.current
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
        sharedPreferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitDispose {
            sharedPreferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
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

    ModalNavigationDrawer(
        drawerContent = {
            ListContent(
                notesViewModel = viewModel,
                signInViewModel = signInViewModel,
                googleAuthUiClient = googleAuthUiClient,   // ← THIS IS REQUIRED NOW
                onFilterSelected = { filterType ->
                    viewModel.setNoteFilterType(filterType)
                }
            )
        }, drawerState = drawerState, gesturesEnabled = !isAnyNoteSheetOpen
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                    XenonSnackbar(
                        snackbarData = snackbarData,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            },
            bottomBar = {
                val textEditorContent: @Composable (RowScope.() -> Unit)? = if (showTextNoteCard) {
                    @Composable {
                        XenonTheme(
                            darkTheme = isDarkTheme,
                            useDefaultTheme = selectedTextNoteTheme == "Default",
                            useRedTheme = selectedTextNoteTheme == "Red",
                            useOrangeTheme = selectedTextNoteTheme == "Orange",
                            useYellowTheme = selectedTextNoteTheme == "Yellow",
                            useGreenTheme = selectedTextNoteTheme == "Green",
                            useTurquoiseTheme = selectedTextNoteTheme == "Turquoise",
                            useBlueTheme = selectedTextNoteTheme == "Blue",
                            usePurpleTheme = selectedTextNoteTheme == "Purple",
                            dynamicColor = selectedTextNoteTheme == "Default"
                        ) {
                            Row {
                                val toggledColor = colorScheme.primary
                                val defaultColor = Color.Transparent
                                val toggledIconColor = colorScheme.onPrimary
                                val defaultIconColor = colorScheme.onSurface
                                FilledIconButton(
                                    onClick = { isBold = !isBold },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (isBold) toggledColor else defaultColor,
                                        contentColor = if (isBold) toggledIconColor else defaultIconColor
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.FormatBold,
                                        contentDescription = stringResource(R.string.bold_text)
                                    )
                                }
                                FilledIconButton(
                                    onClick = { isItalic = !isItalic },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (isItalic) toggledColor else defaultColor,
                                        contentColor = if (isItalic) toggledIconColor else defaultIconColor
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.FormatItalic,
                                        contentDescription = stringResource(R.string.italic_text)
                                    )
                                }
                                FilledIconButton(
                                    onClick = { isUnderlined = !isUnderlined },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (isUnderlined) toggledColor else defaultColor,
                                        contentColor = if (isUnderlined) toggledIconColor else defaultIconColor
                                    )
                                ) {
                                    Icon(
                                        Icons.Rounded.FormatUnderlined,
                                        contentDescription = stringResource(R.string.underline_text)
                                    )
                                }
                                IconButton(onClick = {
                                    currentSizeIndex = (currentSizeIndex + 1) % textSizes.size
                                }) {
                                    Icon(
                                        Icons.Rounded.FormatSize,
                                        contentDescription = stringResource(R.string.change_text_size)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    null
                }

                val listEditorContent: @Composable (RowScope.() -> Unit)? = if (showListNoteCard) {
                    @Composable {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    addListItemTrigger = true
                                },
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
                            IconButton(
                                onClick = ::onListTextResizeClick,
                            ) {
                                Icon(
                                    Icons.Rounded.FormatSize,
                                    contentDescription = stringResource(R.string.change_text_size)
                                )
                            }
                        }
                    }
                } else {
                    null
                }

                val audioEditorContent: @Composable (RowScope.() -> Unit)? =
                    if (showAudioNoteCard) {
                        @Composable {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val waveformCornerTopStart by animateDpAsState(
                                    targetValue = 28.dp, label = "waveformTopStart"
                                )
                                val waveformCornerBottomStart by animateDpAsState(
                                    targetValue = 28.dp, label = "waveformBottomStart"
                                )
                                val waveformCornerTopEnd by animateDpAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Waveform) 28.dp else 8.dp,
                                    label = "waveformTopEnd"
                                )
                                val waveformCornerBottomEnd by animateDpAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Waveform) 28.dp else 8.dp,
                                    label = "waveformBottomEnd"
                                )
                                val animatedWaveformShape = RoundedCornerShape(
                                    topStart = waveformCornerTopStart,
                                    bottomStart = waveformCornerBottomStart,
                                    topEnd = waveformCornerTopEnd,
                                    bottomEnd = waveformCornerBottomEnd
                                )

                                val transcriptCornerTopStart by animateDpAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Transcript) 28.dp else 8.dp,
                                    label = "transcriptTopStart"
                                )
                                val transcriptCornerBottomStart by animateDpAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Transcript) 28.dp else 8.dp,
                                    label = "transcriptBottomStart"
                                )
                                val transcriptCornerTopEnd by animateDpAsState(
                                    targetValue = 28.dp, label = "transcriptTopEnd"
                                )
                                val transcriptCornerBottomEnd by animateDpAsState(
                                    targetValue = 28.dp, label = "transcriptBottomEnd"
                                )
                                val animatedTranscriptShape = RoundedCornerShape(
                                    topStart = transcriptCornerTopStart,
                                    bottomStart = transcriptCornerBottomStart,
                                    topEnd = transcriptCornerTopEnd,
                                    bottomEnd = transcriptCornerBottomEnd
                                )

                                val waveformContainerColor by animateColorAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Waveform) colorScheme.tertiary else colorScheme.surfaceBright,
                                    label = "waveformContainerColor"
                                )
                                val waveformContentColor by animateColorAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Waveform) colorScheme.onTertiary else colorScheme.onSurface,
                                    label = "waveformContentColor"
                                )

                                val transcriptContainerColor by animateColorAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Transcript) colorScheme.tertiary else colorScheme.surfaceBright,
                                    label = "transcriptContainerColor"
                                )
                                val transcriptContentColor by animateColorAsState(
                                    targetValue = if (selectedAudioViewType == AudioViewType.Transcript) colorScheme.onTertiary else colorScheme.onSurface,
                                    label = "transcriptContentColor"
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
                    } else {
                        null
                    }

                val sketchEditorContent: @Composable (RowScope.() -> Unit)? =
                    if (showSketchNoteCard) {
                        @Composable {
                            val sketchSizes =
                                remember { listOf(2f, 5f, 10f, 20f, 40f, 60f, 80f, 100f) }
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
                                        .background(
                                            colorScheme.primary.copy(alpha = 0.4f), CircleShape
                                        )
                                        .clickable {
                                            showSketchSizePopup = true
                                            showColorPicker = false
                                        }, contentAlignment = Alignment.Center
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
                                        showColorPicker = true
                                        showSketchSizePopup = false
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
                                        painter = painterResource(
                                            id = if (usePressure) R.drawable.dynamic else R.drawable.constant
                                        ), contentDescription = "Toggle Pressure/Speed"
                                    )
                                }

                            }
                        }
                    } else {
                        null
                    }

                val onAddModeToggle = { isAddModeActive = !isAddModeActive }

                XenonTheme(
                    darkTheme = isDarkTheme,
                    useDefaultTheme = selectedTextNoteTheme == "Default",
                    useRedTheme = selectedTextNoteTheme == "Red",
                    useOrangeTheme = selectedTextNoteTheme == "Orange",
                    useYellowTheme = selectedTextNoteTheme == "Yellow",
                    useGreenTheme = selectedTextNoteTheme == "Green",
                    useTurquoiseTheme = selectedTextNoteTheme == "Turquoise",
                    useBlueTheme = selectedTextNoteTheme == "Blue",
                    usePurpleTheme = selectedTextNoteTheme == "Purple",
                    dynamicColor = selectedTextNoteTheme == "Default"
                ) {
                    FloatingToolbarContent(
                        hazeState = hazeState,
                        currentSearchQuery = currentSearchQuery,
                        onSearchQueryChanged = { newQuery ->
                            viewModel.setSearchQuery(newQuery)
                        },
                        lazyListState = lazyListState,
                        allowToolbarScrollBehavior = !isAnyNoteSheetOpen,
                        selectedNoteIds = selectedNoteIds.toList(),
                        onClearSelection = { selectedNoteIds = emptySet() },
                        isAddModeActive = isAddModeActive,
                        isSearchActive = isSearchActive,
                        onIsSearchActiveChange = { isSearchActive = it },
                        defaultContent = { iconsAlphaDuration, showActionIconsExceptSearch ->
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
                                    onClick = ::onResizeClick, // Use the new callback
                                    modifier = Modifier.alpha(resizeIconAlpha),
                                    enabled = !isSearchActive && showActionIconsExceptSearch
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        this@Row.AnimatedVisibility(
                                            visible = showResizeValue,
                                            enter = fadeIn(animationSpec = tween(0)),
                                            exit = fadeOut(animationSpec = tween(500))
                                        ) {
                                            val text = when (notesLayoutType) {
                                                NotesLayoutType.LIST -> if (listNoteLineLimitIndex == 0 || listNoteLineLimitIndex == 1) listLineLimits[listNoteLineLimitIndex].toString() else "Max"
                                                NotesLayoutType.GRID -> gridColumnCountOptions[gridNoteColumnCountIndex].toString()
                                            }
                                            Text(
                                                text = text,
                                                style = typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                        this@Row.AnimatedVisibility(
                                            visible = !showResizeValue,
                                            enter = fadeIn(animationSpec = tween(500)),
                                            exit = fadeOut(animationSpec = tween(0))
                                        ) {
                                            Icon(
                                                Icons.Rounded.CenterFocusStrong,
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

                        },
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
                                    },
                                    modifier = Modifier.width(192.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.delete),
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
                        fabOverride = if (showTextNoteCard) {
                            {
                                FloatingActionButton(
                                    onClick = { if (titleState.isNotBlank()) saveTrigger = true },
                                    containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.save_note),
                                        tint = if (titleState.isNotBlank()) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                                            alpha = 0.38f
                                        )
                                    )
                                }
                            }
                        } else if (showListNoteCard) {
                            {
                                FloatingActionButton(
                                    onClick = {
                                        if (listTitleState.isNotBlank() && listItemsState.any { it.text.isNotBlank() }) {
                                            saveTrigger = true
                                        }
                                    },
                                    containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.save_list_note),
                                        tint = if (listTitleState.isNotBlank() && listItemsState.any { it.text.isNotBlank() })
                                            colorScheme.onPrimary else colorScheme.onPrimary.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        } else if (showAudioNoteCard) {
                            {
                                val canSave = titleState.isNotBlank() && hasAudioContent

                                FloatingActionButton(
                                    onClick = {
                                        if (canSave) saveTrigger = true
                                    },
                                    containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.save_audio_note),
                                        tint = if (canSave) colorScheme.onPrimary
                                        else colorScheme.onPrimary.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        } else if (showSketchNoteCard) {
                            {
                                FloatingActionButton(
                                    onClick = { /* Implement save logic for sketch note */ },
                                    containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Save,
                                        contentDescription = stringResource(R.string.save_sketch_note),
                                        tint = colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            },
        ) { scaffoldPadding ->
            // This Box will intercept all touch events when any note sheet is open
            if (isAnyNoteSheetOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Intercept touches */ })
                )
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

            val coverScreenBackgroundColor = Color.Black
            val coverScreenContentColor = Color.White

            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding()
                    .hazeSource(hazeState)
                    .onSizeChanged { },
                titleText = stringResource(id = R.string.app_name),

                expandable = false,
                screenBackgroundColor = coverScreenBackgroundColor,
                contentBackgroundColor = coverScreenBackgroundColor,
                appBarNavigationIconContentColor = coverScreenContentColor,
                contentCornerRadius = NoCornerRadius,
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
                                .padding(horizontal = NoSpacing)
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
                                        style = typography.bodyLarge,
                                        color = coverScreenContentColor
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
                                        style = typography.bodyLarge,
                                        color = coverScreenContentColor
                                    )
                                }
                            } else {
                                when (notesLayoutType) {
                                    NotesLayoutType.LIST -> {
                                        LazyColumn(
                                            state = lazyListState,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(
                                                top = NoSpacing,
                                                bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding,
                                            )
                                        ) {
                                            itemsIndexed(
                                                items = noteItemsWithHeaders,
                                                key = { _, item -> if (item is NotesItems) item.id else item.hashCode() }) { index, item ->
                                                when (item) {
                                                    is String -> {
                                                        Text(
                                                            text = item,
                                                            style = typography.titleMedium.copy(
                                                                fontStyle = FontStyle.Italic,
                                                                color = coverScreenContentColor
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
                                                                if (selectedNoteIds.contains(item.id)) {
                                                                    selectedNoteIds -= item.id
                                                                } else {
                                                                    selectedNoteIds += item.id
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
                                                                                        )
                                                                                            .trim() else line.substringAfter(
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
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        viewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
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
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        viewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
                                                                        viewModel.showListCard()
                                                                    }

                                                                    NoteType.SKETCH -> {
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        viewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
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
                                            columns = StaggeredGridCells.Fixed(currentGridColumns),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(
                                                top = NoSpacing,
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
                                                    notesViewModel = viewModel, // ← THIS WAS MISSING!
                                                    isSelected = selectedNoteIds.contains(item.id),
                                                    isSelectionModeActive = isSelectionModeActive,
                                                    onSelectItem = {
                                                        selectedNoteIds = if (selectedNoteIds.contains(item.id)) {
                                                            selectedNoteIds - item.id
                                                        } else {
                                                            selectedNoteIds + item.id
                                                        }
                                                    },
                                                    onEditItem = { itemToEdit ->
                                                        editingNoteId = itemToEdit.id
                                                        titleState = itemToEdit.title
                                                        descriptionState = itemToEdit.description ?: ""
                                                        listTitleState = itemToEdit.title
                                                        editingNoteColor = itemToEdit.color?.toULong()
                                                        selectedLabelId = itemToEdit.labels.firstOrNull()
                                                        listItemsState.clear()
                                                        nextListItemId = 0L
                                                        currentListSizeIndex = 1

                                                        itemToEdit.description?.let { desc ->
                                                            val parsedItems = desc.split("\n").mapNotNull { line ->
                                                                if (line.isBlank()) return@mapNotNull null
                                                                val isChecked = line.startsWith("[x]")
                                                                val text = if (isChecked) {
                                                                    line.substringAfter("[x] ").trim()
                                                                } else {
                                                                    line.substringAfter("[ ] ").trim()
                                                                }
                                                                ListItem(nextListItemId++, text, isChecked)
                                                            }
                                                            listItemsState.addAll(parsedItems)
                                                        }

                                                        isSearchActive = false
                                                        viewModel.setSearchQuery("")

                                                        when (itemToEdit.noteType) {
                                                            NoteType.TEXT -> viewModel.showTextCard()
                                                            NoteType.AUDIO -> {
                                                                viewModel.showAudioCard()
                                                                selectedAudioViewType = AudioViewType.Waveform
                                                            }
                                                            NoteType.LIST ->  viewModel.showListCard()
                                                            NoteType.SKETCH ->  viewModel.showSketchCard()
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
                                        onClick = { isAddModeActive = false }))
                        }
                    }
                })

            AnimatedVisibility(
                visible = showTextNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    viewModel.hideTextCard()
                    isSearchActive = false // Disable search on dismiss
                    viewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }

                NoteTextSheet(
                    textTitle = titleState,
                    onTextTitleChange = { noteEditingViewModel.setTextTitle(it) },
                    initialContent = descriptionState,
                    onDismiss = {
                        viewModel.hideTextCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                    onSave = { title, description, theme, labelId, isOffline ->
                        if (title.isBlank() && description.isBlank()) {
                            viewModel.hideTextCard()
                            resetNoteState()
                            return@NoteTextSheet
                        }

                        val colorLong = themeColorMap[theme]?.toLong()

                        if (editingNoteId != null) {
                            val existingNote = viewModel.noteItems
                                .filterIsInstance<NotesItems>()
                                .find { it.id == editingNoteId }

                            if (existingNote != null) {
                                val updatedNote = existingNote.copy(
                                    title = title.trim(),
                                    description = description.takeIf { it.isNotBlank() },
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    isOffline = isOffline
                                )
                                viewModel.updateItem(updatedNote, forceLocal = isOffline)
                            } else {
                                viewModel.addItem(
                                    title = title.trim(),
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.TEXT,
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    forceLocal = isOffline
                                )
                            }
                        } else {
                            viewModel.addItem(
                                title = title.trim(),
                                description = description.takeIf { it.isNotBlank() },
                                noteType = NoteType.TEXT,
                                color = colorLong,
                                labels = labelId?.let { listOf(it) } ?: emptyList(),
                                forceLocal = isOffline
                            )
                        }

                        viewModel.hideTextCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    },
                    saveTrigger = saveTrigger,
                    onSaveTriggerConsumed = { saveTrigger = false },
                    isBold = isBold,
                    isItalic = isItalic,
                    isUnderlined = isUnderlined,
                    onIsBoldChange = { isBold = it },
                    onIsItalicChange = { isItalic = it },
                    onIsUnderlinedChange = { isUnderlined = it },
                    editorFontSize = editorFontSize,
                    toolbarHeight = 72.dp,
                    onThemeChange = { newThemeName ->
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    allLabels = allLabels,
                    initialSelectedLabelId = selectedLabelId,
                    onLabelSelected = { selectedLabelId = it },
                    onAddNewLabel = { viewModel.addLabel(it) },
                    isBlackThemeActive = isBlackedOut,
                    isCoverModeActive = true,
                    noteEditingViewModel = noteEditingViewModel,
                )
            }

            AnimatedVisibility(
                visible = showSketchNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    viewModel.hideSketchCard()
                    isSearchActive = false
                    viewModel.setSearchQuery("")
                    resetNoteState()
                }
                NoteSketchSheet(
                    sketchTitle = titleState,
                    onSketchTitleChange = { titleState = it },
                    onDismiss = {
                        viewModel.hideSketchCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
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
                            val existingNote = viewModel.noteItems
                                .filterIsInstance<NotesItems>()
                                .find { it.id == editingNoteId }

                            existingNote?.let { it ->
                                val updatedNote = it.copy(
                                    title = title.trim(),
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    isOffline = isOffline
                                )
                                viewModel.updateItem(updatedNote, forceLocal = isOffline)
                            }
                        } else {
                            viewModel.addItem(
                                title = title.trim(),
                                description = null,
                                noteType = NoteType.SKETCH,
                                color = colorLong,
                                labels = labelId?.let { listOf(it) } ?: emptyList(),
                                forceLocal = isOffline
                            )
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
                    isCoverModeActive = true,
                    editingNoteId = editingNoteId,
                    notesViewModel = viewModel,
                )
            }


            AnimatedVisibility(
                visible = showAudioNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                LaunchedEffect(showAudioNoteCard) {
                    if (!showAudioNoteCard) {
                        GlobalAudioPlayer.getInstance().stopAudio()
                    }
                }
                BackHandler {
                    viewModel.hideAudioCard()
                    isSearchActive = false // Disable search on dismiss
                    viewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }
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
                            val existingNote = viewModel.noteItems
                                .filterIsInstance<NotesItems>()
                                .find { it.id == editingNoteId }

                            existingNote?.let { it ->
                                val updatedNote = it.copy(
                                    title = title.trim(),
                                    description = uniqueAudioId.takeIf { it.isNotBlank() },
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
                                    isOffline = isOffline
                                )
                                viewModel.updateItem(updatedNote, forceLocal = isOffline)
                            }
                        } else {
                            viewModel.addItem(
                                title = title.trim(),
                                description = uniqueAudioId.takeIf { it.isNotBlank() },
                                noteType = NoteType.AUDIO,
                                color = colorLong,
                                labels = labelId?.let { listOf(it) } ?: emptyList(),
                                forceLocal = isOffline
                            )
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
                        File(context.filesDir, "$uniqueId.mp3").takeIf { it.exists() }?.absolutePath
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
                    isCoverModeActive = true,
                    editingNoteId = editingNoteId,
                    notesViewModel = viewModel,
                )

            }

            AnimatedVisibility(
                visible = showListNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    viewModel.hideListCard()
                    isSearchActive = false
                    viewModel.setSearchQuery("")
                    resetNoteState()
                }

                NoteListSheet(
                    listTitle = listTitleState,
                    onListTitleChange = { listTitleState = it },
                    listItems = listItemsState,
                    onDismiss = {
                        viewModel.hideListCard()
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
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
                            val existingNote = viewModel.noteItems
                                .filterIsInstance<NotesItems>()
                                .find { it.id == editingNoteId }

                            existingNote?.let { it ->
                                val updatedNote = it.copy(
                                    title = title.trim(),
                                    description = description.takeIf { it.isNotBlank() },
                                    color = colorLong,
                                    labels = labelId?.let { listOf(it) } ?: emptyList(),
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
                    onThemeChange = { newThemeName ->
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    allLabels = allLabels,
                    initialSelectedLabelId = selectedLabelId,
                    onLabelSelected = { selectedLabelId = it },
                    onAddNewLabel = { viewModel.addLabel(it) },
                    isBlackThemeActive = isBlackedOut,
                    isCoverModeActive = true,
                    editingNoteId = editingNoteId,
                    notesViewModel = viewModel,
                )
            }
        }
    }
}
