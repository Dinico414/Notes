package com.xenonware.notes.viewmodel

import android.app.Application
import android.util.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.util.calculateBezierControlPoints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class DrawingState(
    val paths: List<PathData> = emptyList(),
    val gridEnabled: Boolean = false
)

data class CurrentPathState(
    val path: PathData? = null,
    val color: Color,
    val isEraser: Boolean = false,
    val usePressure: Boolean = true,
    val strokeWidth: Float = 10f
)

data class PathData(
    val id: String,
    val color: Color,
    val path: List<PathOffset>,
)

data class PathOffset(
    val offset: Offset,
    val thickness: Float,
) {
    val x = offset.x
    val y = offset.y
    var controlPoint1 = Offset.Unspecified
    var controlPoint2 = Offset.Unspecified
}

sealed interface DrawingAction {
    data object NewPathStart : DrawingAction
    data class Draw(val offset: Offset, val pressure: Float) : DrawingAction
    data object PathEnd : DrawingAction
    data object DeletePathStart : DrawingAction
    data object Undo : DrawingAction
    data object Redo : DrawingAction
    data class SelectColor(val color: Color) : DrawingAction
    data class SelectStrokeWidth(val strokeWidth: Float) : DrawingAction // New action
    data object ClearCanvas : DrawingAction
    data class EnableGrid(val enabled: Boolean) : DrawingAction
    data class ToggleHandwritingMode(val enabled: Boolean) : DrawingAction
    data class UpdateTool(val isEraser: Boolean, val usePressure: Boolean, val strokeWidth: Float, val strokeColor: Color) : DrawingAction
}

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val prefManager = SharedPreferenceManager(application.applicationContext)

    private val _currentPathState = MutableStateFlow(CurrentPathState(color = Color.Black)) // Default color
    val currentPathState = _currentPathState.asStateFlow()

    private val _pathState = MutableStateFlow(DrawingState())
    val pathState = _pathState.asStateFlow()

    private val _canvasSize = MutableStateFlow(Size(0, 0))
    val canvasSize = _canvasSize.asStateFlow()

    private val _isHandwritingMode = MutableStateFlow(true) // Default to handwriting mode
    val isHandwritingMode = _isHandwritingMode.asStateFlow()

    private var drawColors: List<Color>? = null

    // For Undo/Redo
    private val _undoRedoHistory = mutableListOf<List<PathData>>()
    private var _undoRedoPointer = -1

    init {
        // Save initial state
        _undoRedoHistory.add(_pathState.value.paths)
        _undoRedoPointer = 0
    }

    fun setCanvasSize(size: Size) {
        _canvasSize.update { size }
    }

    fun setDrawColors(colors: List<Color>) {
        val oldColors = drawColors
        drawColors = colors

        if (oldColors != null && oldColors != colors) {
            val colorMap = oldColors.zip(colors).toMap()
            _pathState.update { drawingState ->
                val updatedPaths = drawingState.paths.map { pathData ->
                    colorMap[pathData.color]?.let { newColor ->
                        pathData.copy(color = newColor)
                    } ?: pathData
                }
                drawingState.copy(paths = updatedPaths)
            }

            _currentPathState.update { currentPathState ->
                colorMap[currentPathState.color]?.let {
                    currentPathState.copy(color = it)
                } ?: currentPathState
            }
        } else if (oldColors == null && colors.isNotEmpty()) {
            _currentPathState.update { it.copy(color = colors.first()) }
        }
    }

    private fun saveStateForUndoRedo() {
        if (_undoRedoPointer < _undoRedoHistory.lastIndex) {
            _undoRedoHistory.subList(_undoRedoPointer + 1, _undoRedoHistory.size).clear()
        }
        _undoRedoHistory.add(_pathState.value.paths)
        _undoRedoPointer++
    }

    fun onAction(action: DrawingAction) {
        when (action) {
            DrawingAction.NewPathStart -> onNewPathStart()
            is DrawingAction.Draw -> onDraw(action.offset, action.pressure)
            DrawingAction.PathEnd -> onPathEnd()
            DrawingAction.DeletePathStart -> TODO()
            DrawingAction.Redo -> onRedo()
            DrawingAction.Undo -> onUndo()
            DrawingAction.ClearCanvas -> onClearCanvasClick()
            is DrawingAction.SelectColor -> onSelectColor(action.color)
            is DrawingAction.SelectStrokeWidth -> onSelectStrokeWidth(action.strokeWidth) // New case
            is DrawingAction.EnableGrid -> onEnableGrid(action.enabled)
            is DrawingAction.ToggleHandwritingMode -> onToggleHandwritingMode(action.enabled)
            is DrawingAction.UpdateTool -> onUpdateTool(action.isEraser, action.usePressure, action.strokeWidth, action.strokeColor)
        }
    }

    private fun onUpdateTool(isEraser: Boolean, usePressure: Boolean, strokeWidth: Float, strokeColor: Color) {
        _currentPathState.update {
            it.copy(
                isEraser = isEraser,
                usePressure = usePressure,
                strokeWidth = strokeWidth,
                color = if(isEraser) Color.Transparent else strokeColor
            )
        }
    }

    private fun onNewPathStart() {
        // If it's an eraser, we don't need to create a new PathData for the eraser itself.
        // Ensure the path in currentPathState is null for eraser mode.
        if (currentPathState.value.isEraser) {
            _currentPathState.update { it.copy(path = null) }
            return
        }

        _currentPathState.update {
            it.copy(
                path = PathData(
                    id = System.currentTimeMillis().toString(),
                    color = it.color,
                    path = emptyList(),
                )
            )
        }
    }

    private var smoothness = prefManager.smoothness
    fun setSmoothness(value: Float) {
        if (smoothness != value) {
            smoothness = value
            prefManager.smoothness = value

            for (pd in _pathState.value.paths) {
                recalculateControlPoints(pd.path)
            }
            _pathState.update { it }
        }
    }

    fun getSmoothness(): Float {
        return smoothness
    }

    private fun updateControlPoints(path: List<PathOffset>, new: PathOffset) {
        // Calculate bezier control points of previous PathOffset
        val i = path.lastIndex
        if (path.size > 2) {
            val controlPts = calculateBezierControlPoints(
                path[i - 2].offset, path[i - 1].offset, path[i].offset, new.offset, smoothness
            )
            path[i].controlPoint1 = controlPts.first
            path[i].controlPoint2 = controlPts.second
        }
        // Add control point for first point
        else if (path.size == 2) {
            val controlPts = calculateBezierControlPoints(
                path[i - 1].offset, path[i - 1].offset, path[i].offset, new.offset, smoothness
            )
            path[i].controlPoint1 = controlPts.second
            path[i].controlPoint2 = controlPts.second
        }
        // Add control point for last (new) point, to make the future readjustment smoother looking
        if (path.size > 1) {
            val controlPts = calculateBezierControlPoints(
                path[i - 1].offset, path[i].offset, new.offset, new.offset, smoothness
            )
            new.controlPoint1 = controlPts.first
            new.controlPoint2 = controlPts.second
        }
    }

    private fun recalculateControlPoints(path: List<PathOffset>) {
        for (i in 3..path.lastIndex) {
            updateControlPoints(path.subList(0, i), path[i])
        }
    }

    var tmp = 0
    private fun onDraw(offset: Offset, pressure: Float) {
        val thickness = if (currentPathState.value.usePressure) {
            pressure * currentPathState.value.strokeWidth
        } else {
            currentPathState.value.strokeWidth
        }

        if (currentPathState.value.isEraser) {
            val eraserRadius = thickness / 2
            val updatedPaths = _pathState.value.paths.filterNot { existingPath ->
                existingPath.path.any { pathOffset ->
                    val distance = (pathOffset.offset - offset).getDistance()
                    // Consider the stroke width of the existing path when checking for collision
                    distance <= eraserRadius + (pathOffset.thickness / 2)
                }
            }
            _pathState.update { it.copy(paths = updatedPaths) }
            return
        }

        // --- Existing drawing logic for pen below ---
        val currentPathData = currentPathState.value.path ?: return
        val path = currentPathData.path

        if (path.isNotEmpty()) {
            val last = path.last()
            if (abs(last.offset.x - offset.x) + abs(last.offset.y - offset.y) < 0.1)
                return
        }

        val po = PathOffset(offset, thickness)
        updateControlPoints(path, po)

        _currentPathState.update {
            it.copy(
                path = currentPathData.copy(
                    path = path + po
                )
            )
        }
    }

    private fun onPathEnd() {
        if (currentPathState.value.isEraser) {
            // Eraser operation has completed, save the current state for undo/redo
            saveStateForUndoRedo()
            // Reset current path state to null for consistency, as no path was actively built for the eraser
            _currentPathState.update { it.copy(path = null) }
            return
        }

        // Regular drawing path end logic
        val currentPathData = currentPathState.value.path ?: return
        _currentPathState.update {
            it.copy(
                path = null
            )
        }
        _pathState.update {
            it.copy(
                paths = it.paths + currentPathData
            )
        }
        // Save state for undo/redo for regular drawing
        saveStateForUndoRedo()
    }

    fun drawFunction(
        f: (step: Int) -> Offset,
        steps: Int = 100,
    ) {
        onAction(DrawingAction.NewPathStart)
        for (step in 0..steps - 1) {
            onAction(DrawingAction.Draw(f(step), 3f))
        }
        onAction(DrawingAction.PathEnd)
    }

    private fun onSelectColor(color: Color) {
        _currentPathState.update {
            it.copy(
                color = color
            )
        }
    }

    private fun onSelectStrokeWidth(strokeWidth: Float) { // New function
        _currentPathState.update {
            it.copy(
                strokeWidth = strokeWidth
            )
        }
    }

    private fun onClearCanvasClick() {
        _currentPathState.update {
            it.copy(
                path = null,
            )
        }
        _pathState.update {
            it.copy(
                paths = emptyList()
            )
        }
        // Clear history and add current state
        _undoRedoHistory.clear()
        _undoRedoHistory.add(emptyList())
        _undoRedoPointer = 0
    }

    private fun onEnableGrid(enabled: Boolean) {
        _pathState.update {
            it.copy(
                gridEnabled = enabled
            )
        }
    }

    private fun onToggleHandwritingMode(enabled: Boolean) {
        _isHandwritingMode.update { enabled }
    }

    private fun onUndo() {
        if (_undoRedoPointer > 0) {
            _undoRedoPointer--
            _pathState.update { it.copy(paths = _undoRedoHistory[_undoRedoPointer]) }
        }
    }

    private fun onRedo() {
        if (_undoRedoPointer < _undoRedoHistory.lastIndex) {
            _undoRedoPointer++
            _pathState.update { it.copy(paths = _undoRedoHistory[_undoRedoPointer]) }
        }
    }
}
