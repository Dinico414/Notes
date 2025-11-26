@file:Suppress("AssignedValueIsNeverRead")

package com.xenonware.notes.ui.layouts.notes

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalDensity
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
import com.xenon.mylibrary.QuicksandTitleVariable
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.notes.R
import com.xenonware.notes.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.notes.presentation.sign_in.SignInViewModel
import com.xenonware.notes.ui.res.GoogleProfilBorder
import com.xenonware.notes.ui.res.GoogleProfilePicture
import com.xenonware.notes.ui.res.ListContent
import com.xenonware.notes.ui.res.ListItem
import com.xenonware.notes.ui.res.NoteAudioSheet
import com.xenonware.notes.ui.res.NoteCard
import com.xenonware.notes.ui.res.NoteListSheet
import com.xenonware.notes.ui.res.NoteSketchSheet
import com.xenonware.notes.ui.res.NoteTextSheet
import com.xenonware.notes.ui.res.XenonSnackbar
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
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NotesLayoutType
import com.xenonware.notes.viewmodel.NotesViewModel
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CoverNotes(
    notesViewModel: NotesViewModel = viewModel(),
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
    var showTextNoteCard by rememberSaveable { mutableStateOf(false) }
    var saveTrigger by remember { mutableStateOf(false) }

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

    var showSketchNoteCard by rememberSaveable { mutableStateOf(false) }
    var showAudioNoteCard by rememberSaveable { mutableStateOf(false) }
    var showListNoteCard by rememberSaveable { mutableStateOf(false) }
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


    val noteItemsWithHeaders = notesViewModel.noteItems

    val density = LocalDensity.current
    val appWidthDp = with(density) { appSize.width.toDp() }
    val appHeightDp = with(density) { appSize.height.toDp() }

    val currentAspectRatio = if (isLandscape) {
        appWidthDp / appHeightDp
    } else {
        appHeightDp / appWidthDp
    }

    val aspectRatioConditionMet = if (isLandscape) {
        currentAspectRatio > 0.5625f
    } else {
        currentAspectRatio < 1.77f
    }

    val isAppBarCollapsible = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape || !aspectRatioConditionMet
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    val hazeState = rememberHazeState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSearchQuery by notesViewModel.searchQuery.collectAsState()
    val notesLayoutType by notesViewModel.notesLayoutType.collectAsState()

    val lazyListState = rememberLazyListState()

    var selectedNoteIds by remember { mutableStateOf(emptySet<Int>()) }
    val isSelectionModeActive = selectedNoteIds.isNotEmpty()
    var isAddModeActive by rememberSaveable { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

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
    val allLabels by notesViewModel.labels.collectAsState()
    var selectedLabelId by rememberSaveable { mutableStateOf<String?>(null) }

    fun onResizeClick() {
        if (notesLayoutType == NotesLayoutType.LIST) {
            listNoteLineLimitIndex = (listNoteLineLimitIndex + 1) % listLineLimits.size
        } else {
            gridNoteColumnCountIndex = (gridNoteColumnCountIndex + 1) % gridColumnCountOptions.size
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
                notesViewModel = notesViewModel,
                signInViewModel = signInViewModel,
                onFilterSelected = { filterType ->
                    notesViewModel.setNoteFilterType(filterType)
                },
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
                                        Icons.Default.FormatBold,
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
                                        Icons.Default.FormatItalic,
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
                                        Icons.Default.FormatUnderlined,
                                        contentDescription = stringResource(R.string.underline_text)
                                    )
                                }
                                IconButton(onClick = {
                                    currentSizeIndex = (currentSizeIndex + 1) % textSizes.size
                                }) {
                                    Icon(
                                        Icons.Default.FormatSize,
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
                                    listItemsState.add(
                                        ListItem(
                                            nextListItemId++, "", false
                                        )
                                    )
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
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_new_item_to_list)
                                )
                            }
                            IconButton(
                                onClick = ::onListTextResizeClick,
                            ) {
                                Icon(
                                    Icons.Default.FormatSize,
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
                                        Icons.Default.GraphicEq,
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
                                        Icons.AutoMirrored.Default.Article,
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
                                        val rectHeight = maxRadius
                                        drawRoundRect(
                                            color = onSurface,
                                            topLeft = Offset(
                                                x = (this.size.width - rectWidth) / 2f,
                                                y = (this.size.height - rectHeight) / 2f
                                            ),
                                            size = Size(width = rectWidth, height = rectHeight),
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
                            notesViewModel.setSearchQuery(newQuery)
                        },
                        lazyListState = lazyListState,
                        allowToolbarScrollBehavior = !isAppBarCollapsible && !isAnyNoteSheetOpen,
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
                                        notesViewModel.setNotesLayoutType(newLayout)
                                    },
                                    modifier = Modifier.alpha(listIconAlpha),
                                    enabled = !isSearchActive && showActionIconsExceptSearch
                                ) {
                                    Icon(
                                        imageVector = if (notesLayoutType == NotesLayoutType.LIST) Icons.Default.ViewStream else Icons.Default.ViewModule,
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
                                    Icon(
                                        Icons.Default.CenterFocusStrong,
                                        contentDescription = stringResource(R.string.resize_notes),
                                        tint = colorScheme.onSurface
                                    )
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
                                        Icons.Filled.Settings,
                                        contentDescription = stringResource(R.string.settings),
                                        tint = colorScheme.onSurface
                                    )
                                }
                            }

                        },
                        onAddModeToggle = onAddModeToggle,
                        selectionContentOverride = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        notesViewModel.deleteItems(selectedNoteIds.toList())
                                        selectedNoteIds = emptySet()
                                    },
                                    modifier = Modifier.width(192.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.delete),
                                        textAlign = TextAlign.Center,
                                        style = typography.bodyLarge.copy(
                                            fontFamily = QuicksandTitleVariable,
                                            color = extendedMaterialColorScheme.inverseErrorContainer

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
                                    isSearchActive =
                                        false // Disable search when opening a new text note
                                    notesViewModel.setSearchQuery("") // Clear search query
                                    showTextNoteCard = true
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Default.TextFields,
                                        contentDescription = stringResource(R.string.add_text_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    resetNoteState()
                                    isSearchActive =
                                        false // Disable search when opening a new list note
                                    notesViewModel.setSearchQuery("") // Clear search query
                                    showListNoteCard = true
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = stringResource(R.string.add_list_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    resetNoteState() // Reset state when opening audio note
                                    isSearchActive =
                                        false // Disable search when opening a new audio note
                                    notesViewModel.setSearchQuery("") // Clear search query
                                    showAudioNoteCard = true
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = stringResource(R.string.add_mic_note),
                                        tint = colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = {
                                    isSearchActive =
                                        false // Disable search when opening a new sketch note
                                    notesViewModel.setSearchQuery("") // Clear search query
                                    showSketchNoteCard = true
                                    onAddModeToggle()
                                }) {
                                    Icon(
                                        Icons.Filled.Create,
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
                                        imageVector = Icons.Default.Save,
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
                                        if (listTitleState.isNotBlank() || listItemsState.any { it.text.isNotBlank() }) saveTrigger =
                                            true
                                    }, containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = stringResource(R.string.save_list_note),
                                        tint = if (listTitleState.isNotBlank() || listItemsState.any { it.text.isNotBlank() }) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                                            alpha = 0.38f
                                        )
                                    )
                                }
                            }
                        } else if (showAudioNoteCard) { // FAB for Audio Note
                            {
                                FloatingActionButton(
                                    onClick = { if (titleState.isNotBlank()) saveTrigger = true },
                                    containerColor = colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = stringResource(R.string.save_audio_note),
                                        tint = if (titleState.isNotBlank()) colorScheme.onPrimary else colorScheme.onPrimary.copy(
                                            alpha = 0.38f
                                        )
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
                                        imageVector = Icons.Default.Save,
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

                expandable = isAppBarCollapsible,
                screenBackgroundColor = coverScreenBackgroundColor,
                contentBackgroundColor = coverScreenBackgroundColor,
                appBarNavigationIconContentColor = coverScreenContentColor,
                contentCornerRadius = NoCornerRadius,
                navigationIconStartPadding = MediumPadding,
                navigationIconPadding = if (state.isSignInSuccessful) SmallPadding else MediumPadding,
                navigationIconSpacing = MediumSpacing,

                navigationIcon = {
                    Icon(
                        Icons.Filled.Menu,
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
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            GoogleProfilBorder(
                                modifier = Modifier.size(32.dp),
                                state = state
                            )
                            GoogleProfilePicture(
                                state = state,
                                userData = userData,
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
                                                                    val parsedItems = desc.split("\n")
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
                                                                        notesViewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
                                                                        showTextNoteCard = true
                                                                    }

                                                                    NoteType.AUDIO -> {
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        notesViewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
                                                                        showAudioNoteCard = true
                                                                        selectedAudioViewType =
                                                                            AudioViewType.Waveform // Default for editing
                                                                        editingNoteColor =
                                                                            itemToEdit.color?.toULong()
                                                                    }

                                                                    NoteType.LIST -> {
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        notesViewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
                                                                        showListNoteCard = true
                                                                    }

                                                                    NoteType.SKETCH -> {
                                                                        isSearchActive =
                                                                            false // Disable search
                                                                        notesViewModel.setSearchQuery(
                                                                            ""
                                                                        ) // Clear search query
                                                                        showSketchNoteCard = true
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
                                                    isSelected = selectedNoteIds.contains(item.id),
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
                                                                desc.split("\n").mapNotNull { line ->
                                                                    if (line.isBlank()) null
                                                                    else {
                                                                        val isChecked =
                                                                            line.startsWith("[x]")
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
                                                            listItemsState.addAll(parsedItems)
                                                        }
                                                        when (itemToEdit.noteType) {
                                                            NoteType.TEXT -> {
                                                                isSearchActive =
                                                                    false // Disable search
                                                                notesViewModel.setSearchQuery("") // Clear search query
                                                                showTextNoteCard = true
                                                                editingNoteColor =
                                                                    itemToEdit.color?.toULong()
                                                            }

                                                            NoteType.AUDIO -> {
                                                                isSearchActive =
                                                                    false // Disable search
                                                                notesViewModel.setSearchQuery("") // Clear search query
                                                                showAudioNoteCard = true
                                                                selectedAudioViewType =
                                                                    AudioViewType.Waveform // Default for editing
                                                                editingNoteColor =
                                                                    itemToEdit.color?.toULong()
                                                            }

                                                            NoteType.LIST -> {
                                                                isSearchActive =
                                                                    false // Disable search
                                                                notesViewModel.setSearchQuery("") // Clear search query
                                                                showListNoteCard = true
                                                                editingNoteColor =
                                                                    itemToEdit.color?.toULong()
                                                            }

                                                            NoteType.SKETCH -> {
                                                                isSearchActive =
                                                                    false // Disable search
                                                                notesViewModel.setSearchQuery("") // Clear search query
                                                                showSketchNoteCard = true
                                                                editingNoteColor =
                                                                    itemToEdit.color?.toULong()
                                                            }
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
                    showTextNoteCard = false
                    isSearchActive = false // Disable search on dismiss
                    notesViewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }

                NoteTextSheet(
                    title = titleState,
                    onTitleChange = { titleState = it },
                    initialContent = descriptionState,
                    onDismiss = {
                        showTextNoteCard = false
                        isSearchActive = false // Disable search on dismiss
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                    onSave = { title, description, theme, labelId ->
                        if (title.isNotBlank() || description.isNotBlank()) {
                            val color = themeColorMap[theme]
                            if (editingNoteId != null) {
                                val updatedNote =
                                    notesViewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }?.copy(
                                            title = title,
                                            description = description.takeIf { it.isNotBlank() },
                                            color = color?.toLong(),
                                            labels = labelId?.let { listOf(it) } ?: emptyList()
                                        )
                                if (updatedNote != null) {
                                    notesViewModel.updateItem(updatedNote)
                                }
                            } else {
                                notesViewModel.addItem(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.TEXT,
                                    color = color?.toLong(),
                                    labels = labelId?.let { listOf(it) } ?: emptyList()
                                )
                            }
                        }
                        showTextNoteCard = false
                        isSearchActive = false
                        notesViewModel.setSearchQuery("")
                        resetNoteState()
                    },
                    saveTrigger = saveTrigger,
                    onSaveTriggerConsumed = { saveTrigger = false },
                    isBold = isBold,
                    isItalic = isItalic,
                    isUnderlined = isUnderlined,
                    editorFontSize = editorFontSize,
                    toolbarHeight = 72.dp,
                    onThemeChange = { newThemeName ->
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    allLabels = allLabels,
                    initialSelectedLabelId = selectedLabelId,
                    onLabelSelected = { selectedLabelId = it },
                    onAddNewLabel = { notesViewModel.addLabel(it) },
//                    isVisible = showTextNoteCard,

                )
            }

            AnimatedVisibility(
                visible = showSketchNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    showSketchNoteCard = false
                    isSearchActive = false // Disable search on dismiss
                    notesViewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }
                NoteSketchSheet(
                    sketchTitle = titleState,
                    onSketchTitleChange = { titleState = it },
                    onDismiss = {
                        showSketchNoteCard = false
                        isSearchActive = false // Disable search on dismiss
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                    onThemeChange = { newThemeName ->
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    onSave = { title, theme, labelId ->
                        if (title.isNotBlank()) {
                            val color = themeColorMap[theme]
                            if (editingNoteId != null) {
                                val updatedNote =
                                    notesViewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }?.copy(
                                            title = title,   color = color?.toLong(),
                                            labels = labelId?.let { listOf(it) } ?: emptyList()
                                        )
                                if (updatedNote != null) {
                                    notesViewModel.updateItem(updatedNote)
                                }
                            } else {
                                notesViewModel.addItem(
                                    title = title,
                                    description = null, // No text description for a sketch
                                    noteType = NoteType.SKETCH,
                                    color = color?.toLong(),
                                    labels = labelId?.let { listOf(it) } ?: emptyList()
                                )
                            }
                        }
                        showSketchNoteCard = false
                        isSearchActive = false // Disable search on save
                        notesViewModel.setSearchQuery("") // Clear search query
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
                    onAddNewLabel = { notesViewModel.addLabel(it) }
                )
            }


            AnimatedVisibility(
                visible = showAudioNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    showAudioNoteCard = false
                    isSearchActive = false // Disable search on dismiss
                    notesViewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }
                NoteAudioSheet(
                    audioTitle = titleState,
                    onAudioTitleChange = { titleState = it },
                    onDismiss = {
                        showAudioNoteCard = false
                        isSearchActive = false // Disable search on dismiss
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                    onSave = { title, uniqueAudioId, theme, labelId ->
                        if (title.isNotBlank() || uniqueAudioId.isNotBlank()) {
                            val color = themeColorMap[theme]
                            if (editingNoteId != null) {
                                val updatedNote =
                                    notesViewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }?.copy(
                                            title = title,
                                            description = uniqueAudioId,
                                            color = color?.toLong(),
                                            labels = labelId?.let { listOf(it) } ?: emptyList()
                                        )
                                if (updatedNote != null) {
                                    notesViewModel.updateItem(updatedNote)
                                }
                            } else {
                                notesViewModel.addItem(
                                    title = title,
                                    description = uniqueAudioId.takeIf { it.isNotBlank() },
                                    noteType = NoteType.AUDIO,
                                    color = color?.toLong(),
                                    labels = labelId?.let { listOf(it) } ?: emptyList()
                                )
                            }
                        }
                        showAudioNoteCard = false
                        isSearchActive = false // Disable search on save
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    toolbarHeight = 72.dp,
                    saveTrigger = saveTrigger,
                    onSaveTriggerConsumed = { saveTrigger = false },
                    selectedAudioViewType = selectedAudioViewType,
                    initialAudioFilePath = descriptionState.takeIf { it.isNotBlank() },
                    onThemeChange = { newThemeName ->
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    allLabels = allLabels,
                    initialSelectedLabelId = selectedLabelId,
                    onLabelSelected = { selectedLabelId = it },
                    onAddNewLabel = { notesViewModel.addLabel(it) }
                )

            }

            AnimatedVisibility(
                visible = showListNoteCard,
                enter = slideInVertically( initialOffsetY = { it }),
                exit = slideOutVertically( targetOffsetY = { it })
            ) {
                BackHandler {
                    showListNoteCard = false
                    isSearchActive = false // Disable search on dismiss
                    notesViewModel.setSearchQuery("") // Clear search query
                    resetNoteState()
                }
                NoteListSheet(
                    listTitle = listTitleState,
                    onListTitleChange = { listTitleState = it },
                    initialListItems = listItemsState,
                    onDismiss = {
                        showListNoteCard = false
                        isSearchActive = false // Disable search on dismiss
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    initialTheme = colorThemeMap[editingNoteColor] ?: "Default",
                    onSave = { title, items, theme, labelId ->
                        val description = items.joinToString("\n") {
                            "${if (it.isChecked) "[x]" else "[ ]"} ${it.text}"
                        }
                        if (title.isNotBlank() || description.isNotBlank()) {
                            val color = themeColorMap[theme]
                            if (editingNoteId != null) {
                                val updatedNote =
                                    notesViewModel.noteItems.filterIsInstance<NotesItems>()
                                        .find { it.id == editingNoteId }?.copy(
                                            title = title,
                                            description = description.takeIf { it.isNotBlank() },
                                            color = color?.toLong(),
                                            labels = labelId?.let { listOf(it) } ?: emptyList()
                                        )
                                if (updatedNote != null) {
                                    notesViewModel.updateItem(updatedNote)
                                }
                            } else {
                                notesViewModel.addItem(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.LIST,
                                    color = color?.toLong(),
                                    labels = labelId?.let { listOf(it) } ?: emptyList()
                                )
                            }
                        }
                        showListNoteCard = false
                        isSearchActive = false // Disable search on save
                        notesViewModel.setSearchQuery("") // Clear search query
                        resetNoteState()
                    },
                    toolbarHeight = 72.dp,
                    saveTrigger = saveTrigger,
                    onSaveTriggerConsumed = { saveTrigger = false },
                    onAddItem = {
                        listItemsState.add(ListItem(nextListItemId++, "", false))
                    },
                    onDeleteItem = { itemToDelete ->
                        listItemsState.remove(itemToDelete)
                    },
                    onToggleItemChecked = { item, isChecked ->
                        val index = listItemsState.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            listItemsState[index] =
                                listItemsState[index].copy(isChecked = isChecked)
                        }
                    },
                    onItemTextChange = { item, newText ->
                        val index = listItemsState.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            listItemsState[index] = listItemsState[index].copy(text = newText)
                        }
                    },

                    onAddItemClick = {
                        listItemsState.add(ListItem(nextListItemId++, "", false))
                    },
                    onTextResizeClick = ::onListTextResizeClick,
                    editorFontSize = listEditorFontSize,
                    addItemTrigger = saveTrigger,
                    onAddItemTriggerConsumed = { saveTrigger = false },
                    onThemeChange = { newThemeName -> // Pass the lambda here
                        editingNoteColor = themeColorMap[newThemeName]
                    },
                    allLabels = allLabels,
                    initialSelectedLabelId = selectedLabelId,
                    onLabelSelected = { selectedLabelId = it },
                    onAddNewLabel = { notesViewModel.addLabel(it) }
                )
            }
        }
    }
}
