package com.xenonware.notes.ui.layouts.notes

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenonware.notes.R
import com.xenonware.notes.ui.layouts.ActivityScreen
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import com.xenonware.notes.ui.res.CellAudioNote
import com.xenonware.notes.ui.res.CellListNote
import com.xenonware.notes.ui.res.CellSketchNote
import com.xenonware.notes.ui.res.CellTextNote
import com.xenonware.notes.ui.res.FloatingToolbarContent
import com.xenonware.notes.ui.res.GoogleProfilBorder
import com.xenonware.notes.ui.res.ListContent
import com.xenonware.notes.ui.res.NoteAudioCard
import com.xenonware.notes.ui.res.NoteListCard
import com.xenonware.notes.ui.res.NoteSketchCard
import com.xenonware.notes.ui.res.NoteTextCard
import com.xenonware.notes.ui.res.XenonSnackbar
import com.xenonware.notes.ui.values.ExtraLargePadding
import com.xenonware.notes.ui.values.ExtraLargeSpacing
import com.xenonware.notes.ui.values.LargestPadding
import com.xenonware.notes.ui.values.MediumPadding
import com.xenonware.notes.ui.values.MediumSpacing
import com.xenonware.notes.ui.values.SmallPadding
import com.xenonware.notes.viewmodel.DevSettingsViewModel
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.NotesViewModel
import com.xenonware.notes.viewmodel.classes.NoteType
import com.xenonware.notes.viewmodel.classes.NotesItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactNotes(
    notesViewModel: NotesViewModel = viewModel(),
    devSettingsViewModel: DevSettingsViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    appSize: IntSize,

    ) {

    var editingNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    var titleState by rememberSaveable { mutableStateOf("") }
    var descriptionState by rememberSaveable { mutableStateOf("") }

    var showTextNoteCard by rememberSaveable { mutableStateOf(false) }
    var showSketchNoteCard by rememberSaveable { mutableStateOf(false) }
    var showAudioNoteCard by rememberSaveable { mutableStateOf(false) }
    var showListNoteCard by rememberSaveable { mutableStateOf(false) }

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

    val lazyListState = rememberLazyListState()

    var selectedNoteIds by remember { mutableStateOf(emptySet<Int>()) }
    val isSelectionModeActive = selectedNoteIds.isNotEmpty()
    var isAddModeActive by rememberSaveable { mutableStateOf(false) }


    fun resetNoteState() {
        editingNoteId = null
        titleState = ""
        descriptionState = ""
    }

    val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
    val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

    ModalNavigationDrawer(
        drawerContent = {
            ListContent(
                notesViewModel = notesViewModel,
                onFilterSelected = { filterType ->
                    notesViewModel.setNoteFilterType(filterType)
                    scope.launch { drawerState.close() }
                },
            )
        },
        drawerState = drawerState
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
                FloatingToolbarContent(
                    hazeState = hazeState,
                    onOpenSettings = onOpenSettings,
                    currentSearchQuery = currentSearchQuery,
                    onSearchQueryChanged = { newQuery ->
                        notesViewModel.setSearchQuery(newQuery)
                    },
                    lazyListState = lazyListState,
                    allowToolbarScrollBehavior = !isAppBarCollapsible,
                    selectedNoteIds = selectedNoteIds.toList(),
                    onClearSelection = { selectedNoteIds = emptySet() },
                    onDeleteConfirm = {
                        notesViewModel.deleteItems(selectedNoteIds.toList())
                        selectedNoteIds = emptySet()
                    },
                    isAddModeActive = isAddModeActive,
                    onAddModeToggle = { isAddModeActive = !isAddModeActive },
                    onTextNoteClick = {
                        resetNoteState()
                        showTextNoteCard = true
                    },
                    onPenNoteClick = { showSketchNoteCard = true },
                    onMicNoteClick = { showAudioNoteCard = true },
                    onListNoteClick = { showListNoteCard = true }
                )
            },
        ) { scaffoldPadding ->
            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding()
                    .hazeSource(hazeState)
                    .onSizeChanged { newSize ->
                    },
                titleText = stringResource(id = R.string.app_name),

                expandable = isAppBarCollapsible,

                navigationIconStartPadding = MediumPadding,
                navigationIconPadding = if (isDeveloperModeEnabled && showDummyProfile) SmallPadding else MediumPadding,
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
                hasNavigationIconExtraContent = isDeveloperModeEnabled && showDummyProfile,

                navigationIconExtraContent = {
                    if (isDeveloperModeEnabled && showDummyProfile) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            GoogleProfilBorder(
                                modifier = Modifier.size(32.dp),
                            )
                            Image(
                                painter = painterResource(id = R.mipmap.default_icon),
                                contentDescription = stringResource(R.string.open_navigation_menu),
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
                                        text = stringResource(R.string.no_tasks_message),
                                        style = MaterialTheme.typography.bodyLarge,
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
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(
                                        top = ExtraLargePadding,
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
                                                    style = MaterialTheme.typography.titleMedium.copy(
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
                                                val isSelected = selectedNoteIds.contains(item.id)
                                                when (item.noteType) {
                                                    NoteType.TEXT -> CellTextNote(
                                                        item = item,
                                                        isSelected = isSelected,
                                                        isSelectionModeActive = isSelectionModeActive,
                                                        onSelectItem = {
                                                            if (isSelected) {
                                                                selectedNoteIds -= item.id
                                                            } else {
                                                                selectedNoteIds += item.id
                                                            }
                                                        },
                                                        onEditItem = { itemToEdit ->
                                                            editingNoteId = itemToEdit.id
                                                            titleState = itemToEdit.title
                                                            descriptionState = itemToEdit.description ?: ""
                                                            showTextNoteCard = true
                                                        }
                                                    )
                                                    NoteType.AUDIO -> CellAudioNote(
                                                        item = item,
                                                        isSelected = isSelected,
                                                        isSelectionModeActive = isSelectionModeActive,
                                                        onSelectItem = {
                                                            if (isSelected) {
                                                                selectedNoteIds -= item.id
                                                            } else {
                                                                selectedNoteIds += item.id
                                                            }
                                                        },
                                                        onEditItem = { itemToEdit ->
                                                            editingNoteId = itemToEdit.id
                                                            titleState = itemToEdit.title
                                                            descriptionState = itemToEdit.description ?: ""
                                                            showAudioNoteCard = true
                                                        }
                                                    )
                                                    NoteType.LIST -> CellListNote(
                                                        item = item,
                                                        isSelected = isSelected,
                                                        isSelectionModeActive = isSelectionModeActive,
                                                        onSelectItem = {
                                                            if (isSelected) {
                                                                selectedNoteIds -= item.id
                                                            } else {
                                                                selectedNoteIds += item.id
                                                            }
                                                        },
                                                        onEditItem = { itemToEdit ->
                                                            editingNoteId = itemToEdit.id
                                                            titleState = itemToEdit.title
                                                            descriptionState = itemToEdit.description ?: ""
                                                            showListNoteCard = true
                                                        }
                                                    )
                                                    NoteType.SKETCH -> CellSketchNote(
                                                        item = item,
                                                        isSelected = isSelected,
                                                        isSelectionModeActive = isSelectionModeActive,
                                                        onSelectItem = {
                                                            if (isSelected) {
                                                                selectedNoteIds -= item.id
                                                            } else {
                                                                selectedNoteIds += item.id
                                                            }
                                                        },
                                                        onEditItem = { itemToEdit ->
                                                            editingNoteId = itemToEdit.id
                                                            titleState = itemToEdit.title
                                                            descriptionState = itemToEdit.description ?: ""
                                                            showSketchNoteCard = true
                                                        }
                                                    )
                                                }
                                                val isLastItemInListOrNextIsHeader =
                                                    index == noteItemsWithHeaders.lastIndex || (index + 1 < noteItemsWithHeaders.size && noteItemsWithHeaders[index + 1] is String)

                                                if (!isLastItemInListOrNextIsHeader) {
                                                    Spacer(
                                                        modifier = Modifier.Companion.height(
                                                            MediumPadding
                                                        )
                                                    )
                                                }
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
                                        onClick = { isAddModeActive = false }
                                    )
                            )
                        }
                    }
                })

            AnimatedVisibility(
                visible = showTextNoteCard,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { showTextNoteCard = false }
                NoteTextCard(
                    initialTitle = titleState,
                    initialContent = descriptionState,
                    onDismiss = { showTextNoteCard = false },
                    onSave = { title, description ->
                        if (title.isNotBlank() || description.isNotBlank()) {
                            if (editingNoteId != null) {
                                val updatedNote = notesViewModel.noteItems.filterIsInstance<NotesItems>().find { it.id == editingNoteId }?.copy(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                )
                                if (updatedNote != null) {
                                    notesViewModel.updateItem(updatedNote)
                                }
                            } else {
                                notesViewModel.addItem(
                                    title = title,
                                    description = description.takeIf { it.isNotBlank() },
                                    noteType = NoteType.TEXT
                                )
                            }
                        }
                        showTextNoteCard = false
                        resetNoteState()
                    }
                )
            }

            AnimatedVisibility(
                visible = showSketchNoteCard,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { showSketchNoteCard = false }
                NoteSketchCard(onDismiss = { showSketchNoteCard = false })
            }

            AnimatedVisibility(
                visible = showAudioNoteCard,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { showAudioNoteCard = false }
                NoteAudioCard(onDismiss = { showAudioNoteCard = false })
            }

            AnimatedVisibility(
                visible = showListNoteCard,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { showListNoteCard = false }
                NoteListCard(onDismiss = { showListNoteCard = false })
            }
        }
    }
}
