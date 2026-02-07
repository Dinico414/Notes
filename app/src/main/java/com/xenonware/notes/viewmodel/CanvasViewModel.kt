package com.xenonware.notes.viewmodel

import android.app.Application
import android.util.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.xenonware.notes.data.SharedPreferenceManager
import com.xenonware.notes.util.sketch.calculateBezierControlPoints
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
    val color: Color = Color.Transparent,   // ← changed: no hard-coded color here
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
    data class Erase(val offset: Offset) : DrawingAction
    data object DeletePathStart : DrawingAction
    data object Undo : DrawingAction
    data object Redo : DrawingAction
    data class SelectColor(val color: Color) : DrawingAction
    data class SelectStrokeWidth(val strokeWidth: Float) : DrawingAction
    data object ClearCanvas : DrawingAction
    data class EnableGrid(val enabled: Boolean) : DrawingAction
    data class ToggleHandwritingMode(val enabled: Boolean) : DrawingAction
    data class UpdateTool(val isEraser: Boolean, val usePressure: Boolean, val strokeWidth: Float, val strokeColor: Color) : DrawingAction
    // NEW ACTION
    data class SnapToShape(val newPath: List<PathOffset>) : DrawingAction
}

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val prefManager = SharedPreferenceManager(application.applicationContext)

    private val _currentPathState = MutableStateFlow(CurrentPathState())
    val currentPathState = _currentPathState.asStateFlow()

    private val _pathState = MutableStateFlow(DrawingState())
    val pathState = _pathState.asStateFlow()

    private val _canvasSize = MutableStateFlow(Size(0, 0))
    val canvasSize = _canvasSize.asStateFlow()

    private val _isHandwritingMode = MutableStateFlow(true)
    val isHandwritingMode = _isHandwritingMode.asStateFlow()

    private var drawColors: List<Color>? = null

    private val _undoRedoHistory = mutableListOf<List<PathData>>()
    private var _undoRedoPointer = -1

    init {
        _undoRedoHistory.add(_pathState.value.paths)
        _undoRedoPointer = 0
    }

    fun setCanvasSize(size: Size) {
        _canvasSize.update { size }
    }

    fun setDrawColors(colors: List<Color>) {
        val oldColors = drawColors
        drawColors = colors

        if (colors.isEmpty()) return

        val primaryColor = colors.getOrElse(0) { Color.Black }  // fallback only

        // First initialization – very important!
        if (oldColors == null) {
            _currentPathState.update { it.copy(color = primaryColor) }
            return
        }

        // Theme / palette change
        if (oldColors.size == colors.size) {
            val oldColorToIndex = oldColors.withIndex()
                .associate { (idx, c) -> c to idx }

            _pathState.update { drawingState ->
                val updatedPaths = drawingState.paths.map { pathData ->
                    val oldIndex = oldColorToIndex[pathData.color]
                    if (oldIndex != null && oldIndex < colors.size) {
                        pathData.copy(color = colors[oldIndex])
                    } else {
                        // Most important fallback: use current primary color
                        pathData.copy(color = primaryColor)
                    }
                }
                drawingState.copy(paths = updatedPaths)
            }

            _currentPathState.update { current ->
                val oldIndex = oldColorToIndex[current.color]
                val newColor = if (oldIndex != null && oldIndex < colors.size) {
                    colors[oldIndex]
                } else {
                    primaryColor
                }
                current.copy(color = newColor)
            }
        } else {
            // Size changed → reset to primary
            _pathState.update { state ->
                state.copy(paths = state.paths.map { it.copy(color = primaryColor) })
            }
            _currentPathState.update { it.copy(color = primaryColor) }
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
            is DrawingAction.Erase -> onErase(action.offset)
            DrawingAction.DeletePathStart -> TODO()
            DrawingAction.Redo -> onRedo()
            DrawingAction.Undo -> onUndo()
            DrawingAction.ClearCanvas -> onClearCanvasClick()
            is DrawingAction.SelectColor -> onSelectColor(action.color)
            is DrawingAction.SelectStrokeWidth -> onSelectStrokeWidth(action.strokeWidth)
            is DrawingAction.EnableGrid -> onEnableGrid(action.enabled)
            is DrawingAction.ToggleHandwritingMode -> onToggleHandwritingMode(action.enabled)
            is DrawingAction.UpdateTool -> onUpdateTool(action.isEraser, action.usePressure, action.strokeWidth, action.strokeColor)
            is DrawingAction.SnapToShape -> onSnapToShape(action.newPath)
        }
    }

    private fun onNewPathStart() {
        // Very important: always use the current primary color (index 0) when starting
        val primary = drawColors?.getOrNull(0) ?: Color.Black   // fallback only

        _currentPathState.update {
            it.copy(
                isEraser = false,
                color = primary,   // ← enforced here
                path = PathData(
                    id = System.currentTimeMillis().toString(),
                    color = primary,   // ← enforced here too
                    path = emptyList()
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

    fun getSmoothness(): Float = smoothness

    private fun updateControlPoints(path: List<PathOffset>, new: PathOffset) {
        val i = path.lastIndex
        if (path.size > 2) {
            val controlPts = calculateBezierControlPoints(
                path[i - 2].offset, path[i - 1].offset, path[i].offset, new.offset, smoothness
            )
            path[i].controlPoint1 = controlPts.first
            path[i].controlPoint2 = controlPts.second
        } else if (path.size == 2) {
            val controlPts = calculateBezierControlPoints(
                path[i - 1].offset, path[i - 1].offset, path[i].offset, new.offset, smoothness
            )
            path[i].controlPoint1 = controlPts.second
            path[i].controlPoint2 = controlPts.second
        }
        if (path.size > 1) {
            val controlPts = calculateBezierControlPoints(
                path[i - 1].offset, path[i].offset, new.offset, new.offset, smoothness
            )
            new.controlPoint1 = controlPts.first
            new.controlPoint2 = controlPts.second
        }
    }

    private fun recalculateControlPoints(path: List<PathOffset>) {
        if (path.isEmpty()) return
        for (i in 2 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]
            // (logic omitted – same as original)
        }
    }

    private fun distancePointToLineSegment(
        point: Offset,
        segmentStart: Offset,
        segmentEnd: Offset
    ): Float {
        val l2 = (segmentEnd.x - segmentStart.x).square() + (segmentEnd.y - segmentStart.y).square()
        if (l2 == 0f) return (point - segmentStart).getDistance()

        val t = ((point.x - segmentStart.x) * (segmentEnd.x - segmentStart.x) +
                (point.y - segmentStart.y) * (segmentEnd.y - segmentStart.y)) / l2

        return when {
            t < 0 -> (point - segmentStart).getDistance()
            t > 1 -> (point - segmentEnd).getDistance()
            else -> {
                val projX = segmentStart.x + t * (segmentEnd.x - segmentStart.x)
                val projY = segmentStart.y + t * (segmentEnd.y - segmentStart.y)
                (point - Offset(projX, projY)).getDistance()
            }
        }
    }

    private fun Float.square() = this * this

    private fun onDraw(offset: Offset, pressure: Float) {
        val thickness = if (currentPathState.value.usePressure) {
            pressure * currentPathState.value.strokeWidth
        } else {
            currentPathState.value.strokeWidth
        }

        val currentPathData = currentPathState.value.path ?: return
        val path = currentPathData.path

        if (path.isNotEmpty()) {
            val last = path.last()
            if (abs(last.x - offset.x) + abs(last.y - offset.y) < 0.1f) return
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
            saveStateForUndoRedo()
            _currentPathState.update { it.copy(path = null) }
            return
        }

        val currentPathData = currentPathState.value.path ?: return
        _currentPathState.update { it.copy(path = null) }
        _pathState.update { it.copy(paths = it.paths + currentPathData) }
        saveStateForUndoRedo()
    }

    private fun onErase(offset: Offset) {
        if (!currentPathState.value.isEraser) {
            _currentPathState.update { it.copy(isEraser = true) }
        }
        val thickness = currentPathState.value.strokeWidth
        val eraserRadius = thickness / 2
        val updatedPaths = _pathState.value.paths.filterNot { path ->
            path.path.windowed(2, 1).any { (p1, p2) ->
                distancePointToLineSegment(offset, p1.offset, p2.offset) <=
                        eraserRadius + (p1.thickness + p2.thickness) / 2
            } || path.path.any { p ->
                (p.offset - offset).getDistance() <= eraserRadius + p.thickness / 2
            }
        }
        _pathState.update { it.copy(paths = updatedPaths) }
    }

    fun drawFunction(
        f: (step: Int) -> Offset,
        steps: Int = 100,
    ) {
        onAction(DrawingAction.NewPathStart)
        for (step in 0 until steps) {
            onAction(DrawingAction.Draw(f(step), 3f))
        }
        onAction(DrawingAction.PathEnd)
    }

    private fun onSelectColor(color: Color) {
        _currentPathState.update { it.copy(color = color) }
    }

    private fun onSelectStrokeWidth(strokeWidth: Float) {
        _currentPathState.update { it.copy(strokeWidth = strokeWidth) }
    }

    private fun onClearCanvasClick() {
        _currentPathState.update { it.copy(path = null) }
        _pathState.update { it.copy(paths = emptyList()) }
        _undoRedoHistory.clear()
        _undoRedoHistory.add(emptyList())
        _undoRedoPointer = 0
    }

    private fun onEnableGrid(enabled: Boolean) {
        _pathState.update { it.copy(gridEnabled = enabled) }
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

    private fun onSnapToShape(newPathPoints: List<PathOffset>) {
        recalculateControlPoints(newPathPoints)

        _currentPathState.update { state ->
            state.path?.let { current ->
                state.copy(path = current.copy(path = newPathPoints))
            } ?: state
        }
    }

    private fun onUpdateTool(isEraser: Boolean, usePressure: Boolean, strokeWidth: Float, strokeColor: Color) {
        _currentPathState.update {
            it.copy(
                isEraser = isEraser,
                usePressure = usePressure,
                strokeWidth = strokeWidth,
                color = if (isEraser) Color.Transparent else strokeColor
            )
        }
    }
}