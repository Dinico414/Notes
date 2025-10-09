package com.xenonware.notes.ui.layouts.notes

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenonware.notes.R
import com.xenonware.notes.ui.layouts.ActivityScreen
import com.xenonware.notes.ui.layouts.QuicksandTitleVariable
import com.xenonware.notes.ui.res.DialogTaskItemFiltering
import com.xenonware.notes.ui.res.DialogTaskItemSorting
import com.xenonware.notes.ui.res.FloatingToolbarContent
import com.xenonware.notes.ui.res.GoogleProfilBorder
import com.xenonware.notes.ui.res.TextNoteCell
import com.xenonware.notes.ui.res.TextNoteEditor
import com.xenonware.notes.ui.res.TodoListContent
import com.xenonware.notes.ui.res.XenonSnackbar
import com.xenonware.notes.ui.values.DialogPadding
import com.xenonware.notes.ui.values.LargestPadding
import com.xenonware.notes.ui.values.MediumPadding
import com.xenonware.notes.ui.values.MediumSpacing
import com.xenonware.notes.ui.values.NoCornerRadius
import com.xenonware.notes.ui.values.NoSpacing
import com.xenonware.notes.ui.values.SmallPadding
import com.xenonware.notes.viewmodel.DevSettingsViewModel
import com.xenonware.notes.viewmodel.LayoutType
import com.xenonware.notes.viewmodel.TaskViewModel
import com.xenonware.notes.viewmodel.TodoViewModel
import com.xenonware.notes.viewmodel.TodoViewModelFactory
import com.xenonware.notes.viewmodel.classes.TaskItem
import com.xenonware.notes.viewmodel.classes.TaskStep
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
fun CoverNotes(
    taskViewModel: TaskViewModel = viewModel(),
    devSettingsViewModel: DevSettingsViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    appSize: IntSize,

    ) {
    val application = LocalContext.current.applicationContext as Application
    val todoViewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(application, taskViewModel)
    )

    var editingTaskId by rememberSaveable { mutableStateOf<Int?>(null) }
    var titleState by rememberSaveable { mutableStateOf("") }
    var descriptionState by rememberSaveable { mutableStateOf("") }
    val currentSteps = remember { mutableStateListOf<TaskStep>() }

    val selectedListId by todoViewModel.selectedDrawerItemId

    LaunchedEffect(selectedListId) {
        taskViewModel.currentSelectedListId = selectedListId
    }

    val todoItemsWithHeaders = taskViewModel.taskItems

    val density = LocalDensity.current
    val appWidthDp = with(density) { appSize.width.toDp() }
    val appHeightDp = with(density) { appSize.height.toDp() }

    val currentAspectRatio = if (isLandscape) {
        appWidthDp / appHeightDp
    } else {
        appHeightDp / appWidthDp
    }

    val isAppBarCollapsible = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    val hazeState = rememberHazeState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }


    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSearchQuery by taskViewModel.searchQuery.collectAsState()

    var selectedTaskIds by remember { mutableStateOf(emptySet<Int>()) }
    val isSelectionModeActive = selectedTaskIds.isNotEmpty()

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            todoViewModel.clearAllSelections()
        }
    }

    val undoActionLabel = stringResource(R.string.undo)
    val taskTextSnackbar = stringResource(R.string.task_text)
    val deletedTextSnackbar = stringResource(R.string.deleted_text)


    fun resetBottomSheetState() {
        editingTaskId = null
        titleState = ""
        descriptionState = ""
        currentSteps.clear()
    }

    val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
    val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(drawerState.isOpen) {
        todoViewModel.drawerOpenFlow.emit(drawerState.isOpen)
    }

    ModalNavigationDrawer(
        drawerContent = {
            TodoListContent(
                viewModel = todoViewModel,
                onDrawerItemClicked = { _ ->
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
                    onShowBottomSheet = {
                        resetBottomSheetState()
                        showBottomSheet = true
                    },
                    onOpenSettings = onOpenSettings,
                    onOpenSortDialog = { showSortDialog = true },
                    onOpenFilterDialog = { showFilterDialog = true },
                    currentSearchQuery = currentSearchQuery,
                    onSearchQueryChanged = { newQuery ->
                        taskViewModel.setSearchQuery(newQuery)
                    },
                    lazyListState = lazyListState,
                    allowToolbarScrollBehavior = true
                )
            },
        ) { scaffoldPadding ->
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

                content = { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = NoSpacing)
                    ) {
                        if (todoItemsWithHeaders.isEmpty() && currentSearchQuery.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_tasks_message),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = coverScreenContentColor
                                )
                            }
                        } else if (todoItemsWithHeaders.isEmpty() && currentSearchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_search_results),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = coverScreenContentColor
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(
                                    top = NoSpacing,
                                    bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                                )
                            ) {
                                itemsIndexed(
                                    items = todoItemsWithHeaders,
                                    key = { _, item -> if (item is TaskItem) item.id else item.hashCode() }
                                ) { index, item ->
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
                                                color = coverScreenContentColor,
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

                                        is TaskItem -> {
                                            val isSelected = selectedTaskIds.contains(item.id)
                                            TextNoteCell(
                                                item = item,
                                                isSelected = isSelected,
                                                isSelectionModeActive = isSelectionModeActive,
                                                onSelectItem = {
                                                    if (isSelected) {
                                                        selectedTaskIds -= item.id
                                                    } else {
                                                        selectedTaskIds += item.id
                                                    }
                                                },
                                                onEditItem = { itemToEdit ->
                                                    editingTaskId = itemToEdit.id
                                                    titleState = itemToEdit.title
                                                    descriptionState = itemToEdit.description ?: ""
                                                    showBottomSheet = true
                                                }
                                            )
                                            val isLastItemInListOrNextIsHeader =
                                                index == todoItemsWithHeaders.lastIndex || (index + 1 < todoItemsWithHeaders.size && todoItemsWithHeaders[index + 1] is String)

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
                }
            )

            if (showBottomSheet) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                        }, sheetState = sheetState, modifier = Modifier.imePadding()
                    ) {
                        TextNoteEditor(
                            textState = titleState,
                            onTextChange = { titleState = it },
                            descriptionState = descriptionState,
                            onDescriptionChange = { descriptionState = it },
                            onSaveTask = {
                                if (titleState.isNotBlank()) {
                                    if (editingTaskId != null) {
                                        val updatedTask = taskViewModel.taskItems.filterIsInstance<TaskItem>().find { it.id == editingTaskId }?.copy(
                                            title = titleState,
                                            description = descriptionState.takeIf { it.isNotBlank() },
                                        )
                                        if (updatedTask != null) {
                                            taskViewModel.updateItem(updatedTask)
                                        }
                                    } else {
                                        taskViewModel.addItem(
                                            title = titleState,
                                            description = descriptionState.takeIf { it.isNotBlank() },
                                        )
                                    }
                                    resetBottomSheetState()
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }
                                }
                            },
                            isSaveEnabled = titleState.isNotBlank(),
                            horizontalContentPadding = DialogPadding,
                            bottomContentPadding = DialogPadding
                        )
                    }
                }
            }
            if (showSortDialog) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DialogTaskItemSorting(
                        currentSortOption = taskViewModel.currentSortOption,
                        currentSortOrder = taskViewModel.currentSortOrder,
                        onDismissRequest = { showSortDialog = false },
                        onApplySort = { newOption, newOrder ->
                            taskViewModel.setSortCriteria(newOption, newOrder)
                        })
                }
            }

            if (showFilterDialog) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DialogTaskItemFiltering(
                        initialFilterStates = taskViewModel.filterStates.toMap(),
                        onDismissRequest = { showFilterDialog = false },
                        onApplyFilters = { newStates ->
                            taskViewModel.updateMultipleFilterStates(newStates)
                        },
                        onResetFilters = {
                            taskViewModel.resetAllFilters()
                        })
                }
            }
        }
    }
}
