package com.xenonware.notes

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.xenonware.notes.util.ShapeRecognizer
import com.xenonware.notes.viewmodel.CurrentPathState
import com.xenonware.notes.viewmodel.DrawingAction
import com.xenonware.notes.viewmodel.DrawingAction.Draw
import com.xenonware.notes.viewmodel.DrawingAction.Erase
import com.xenonware.notes.viewmodel.DrawingAction.NewPathStart
import com.xenonware.notes.viewmodel.DrawingAction.PathEnd
import com.xenonware.notes.viewmodel.DrawingAction.UpdateTool
import com.xenonware.notes.viewmodel.PathData
import com.xenonware.notes.viewmodel.PathOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoteCanvas(
    paths: List<PathData>,
    currentPath: PathData?,
    currentToolState: CurrentPathState,
    onAction: (DrawingAction) -> Unit,
    isHandwritingMode: Boolean,
    modifier: Modifier = Modifier,
    gridEnabled: Boolean = false,
    debugText: Boolean = false,
    debugPoints: Boolean = false,
    interactable: Boolean = true
) {
    var debugString by remember { mutableStateOf("") }
    var wasEraserMode by remember { mutableStateOf(false) }

    // --- BUFFERING STATE ---
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var drawnPathsCount by remember { mutableIntStateOf(0) }
    var currentPathBakedSize by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current // Need this for Haptics

    // --- SHAPE SNAP STATE ---
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }

    // 1. Handle Paths Updates (Drawing, Erasing, Undo)
    LaunchedEffect(paths) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect

        val currentBitmap = cachedBitmap
        val needsFullRedraw = paths.size < drawnPathsCount || (paths.size == drawnPathsCount && paths.isNotEmpty()) || currentBitmap == null
        val isAppend = paths.size == drawnPathsCount + 1 && currentBitmap != null

        if (isAppend && currentBitmap != null) {
            val canvas = ComposeCanvas(currentBitmap)
            val paint = Paint().apply { isAntiAlias = true }
            val newPath = paths.last()
            drawPathToCanvas(canvas, paint, newPath.path, newPath.color)
            drawnPathsCount = paths.size
            currentPathBakedSize = 0
        }
        else if (needsFullRedraw) {
            coroutineScope.launch(Dispatchers.Default) {
                val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
                val canvas = ComposeCanvas(newBitmap)
                val paint = Paint().apply { isAntiAlias = true }
                paths.forEach { pathData ->
                    drawPathToCanvas(canvas, paint, pathData.path, pathData.color)
                }
                withContext(Dispatchers.Main) {
                    cachedBitmap = newBitmap
                    drawnPathsCount = paths.size
                    currentPathBakedSize = 0
                }
            }
        }
    }

    // 2. Handle "Current" Active Path (Incremental Baking)
    LaunchedEffect(currentPath) {
        val pathData = currentPath
        val bitmap = cachedBitmap ?: return@LaunchedEffect

        if (pathData == null) {
            currentPathBakedSize = 0
            return@LaunchedEffect
        }

        // Logic to handle Shape Snap Redraw:
        // If currentPath suddenly changes content significantly (snap happened),
        // we might want to force the 'live segment' to redraw completely from start
        // checking if size decreased or changed in a non-append way:
        if (pathData.path.size < currentPathBakedSize) {
            currentPathBakedSize = 0 // Reset baking for this stroke
        }

        val bufferSize = 4
        val safeIndex = pathData.path.size - bufferSize

        if (safeIndex > currentPathBakedSize && safeIndex > 1) {
            val canvas = ComposeCanvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true }

            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
            val segmentToBake = pathData.path.subList(startIdx, safeIndex + 1)

            drawPathToCanvas(canvas, paint, segmentToBake, pathData.color)
            currentPathBakedSize = safeIndex
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var cursorPos: Offset? by remember { mutableStateOf(null) }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (!interactable) return@pointerInteropFilter true

                    if (debugText) {
                        debugString = "${event.action}\nPoints: ${currentPath?.path?.size ?: 0}\nBaked: $currentPathBakedSize"
                    }

                    val isDetectedToolEraser = event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
                    if (isDetectedToolEraser != wasEraserMode) {
                        wasEraserMode = isDetectedToolEraser
                        onAction(UpdateTool(
                            isEraser = isDetectedToolEraser,
                            usePressure = currentToolState.usePressure,
                            strokeWidth = currentToolState.strokeWidth,
                            strokeColor = currentToolState.color
                        ))
                    }

                    val isStylus = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                    val isFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                    val isEraser = currentToolState.isEraser

                    // --- ERASER INPUT ---
                    if (isEraser) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                onAction(Erase(Offset(event.x, event.y)))
                                cursorPos = Offset(event.x, event.y)
                            }
                            MotionEvent.ACTION_UP -> {
                                onAction(PathEnd)
                                cursorPos = null
                            }
                            MotionEvent.ACTION_HOVER_MOVE -> {
                                cursorPos = Offset(event.x, event.y)
                            }
                            MotionEvent.ACTION_HOVER_EXIT -> {
                                cursorPos = null
                            }
                        }
                        return@pointerInteropFilter true
                    }

                    // --- DRAWING INPUT ---
                    val canDraw = isStylus || (isHandwritingMode && isFinger)
                    if (canDraw) {
                        val currentPos = Offset(event.x, event.y)

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                cursorPos = null
                                onAction(NewPathStart)
                                onAction(Draw(currentPos, event.pressure))

                                // Reset Snap Logic
                                holdJob?.cancel()
                                lastTouchPosition = currentPos
                            }

                            MotionEvent.ACTION_MOVE -> {
                                onAction(Draw(currentPos, event.pressure))

                                // --- SHAPE SNAP LOGIC ---
                                val dist = (currentPos - lastTouchPosition).getDistance()

                                // If moved more than 20px radius, reset the timer
                                if (dist > 20f) {
                                    lastTouchPosition = currentPos
                                    holdJob?.cancel()

                                    // Restart the timer
                                    holdJob = coroutineScope.launch {
                                        delay(2000) // 2 Seconds Hold

                                        // Timer finished! Check logic
                                        currentPath?.let { activePath ->
                                            val shapePath = ShapeRecognizer.recognizeAndCreatePath(
                                                points = activePath.path,
                                                originalThickness = currentToolState.strokeWidth
                                            )

                                            if (shapePath != null) {
                                                // Haptic Feedback
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                                                // Replace Path
                                                withContext(Dispatchers.Main) {
                                                    onAction(DrawingAction.SnapToShape(shapePath))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            MotionEvent.ACTION_UP -> {
                                holdJob?.cancel() // Cancel snap if finger lifted
                                onAction(PathEnd)
                            }

                            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_HOVER_MOVE -> {
                                cursorPos = if (event.action == MotionEvent.ACTION_HOVER_EXIT) null else currentPos
                            }
                        }
                        return@pointerInteropFilter true
                    }
                    return@pointerInteropFilter true
                }
                .drawWithCache {
                    val currentSize = IntSize(size.width.toInt(), size.height.toInt())
                    if (canvasSize != currentSize) {
                        canvasSize = currentSize
                    }

                    if (cachedBitmap == null && size.width > 0 && size.height > 0) {
                        cachedBitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
                    }

                    val cellSizePx = 25.sp.roundToPx()
                    val gridColor = Color.LightGray
                    val xLines = (size.width / cellSizePx).toInt()
                    val yLines = (size.height / cellSizePx).toInt()

                    onDrawBehind {
                        if (gridEnabled) {
                            for (i in 0..xLines) {
                                val x = i * cellSizePx.toFloat()
                                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height))
                            }
                            for (i in 0..yLines) {
                                val y = i * cellSizePx.toFloat()
                                drawLine(gridColor, Offset(0f, y), Offset(0f, size.height))
                            }
                        }

                        cachedBitmap?.let {
                            drawImage(image = it, topLeft = Offset.Zero)
                        }

                        currentPath?.let {
                            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
                            val liveSegment = if (startIdx < it.path.size) {
                                it.path.subList(startIdx, it.path.size)
                            } else {
                                emptyList()
                            }
                            drawPathScope(liveSegment, it.color, debugPoints)
                        }

                        cursorPos?.let { drawCursor(it) }
                    }
                }
        )
        if (debugText) {
            Text(debugString, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

// --- HELPER FUNCTIONS ---

private fun drawPathToCanvas(
    canvas: ComposeCanvas,
    paint: Paint,
    path: List<PathOffset>,
    color: Color
) {
    if (path.isEmpty()) return
    paint.color = color
    paint.style = PaintingStyle.Fill

    if (path.size < 2) {
        val p = path.first()
        canvas.drawCircle(p.offset, p.thickness / 2, paint)
        return
    }

    for (i in 1..path.lastIndex) {
        val start = path[i - 1]
        val end = path[i]
        val distance = (end.offset - start.offset).getDistance()

        if (distance < 1f) continue

        val steps = ceil(distance / 2.5f).toInt().coerceAtLeast(2)

        var previousPoint = start.offset
        var previousThickness = start.thickness

        for (step in 1..steps) {
            val t = step.toFloat() / steps

            val currentPoint = when {
                end.controlPoint1 != Offset.Unspecified && end.controlPoint2 != Offset.Unspecified ->
                    cubicBezier(t, start.offset, end.controlPoint1, end.controlPoint2, end.offset)
                end.controlPoint1 != Offset.Unspecified ->
                    quadraticBezier(t, start.offset, end.controlPoint1, end.offset)
                else -> Offset(
                    lerp(start.offset.x, end.offset.x, t),
                    lerp(start.offset.y, end.offset.y, t)
                )
            }

            val currentThickness = lerp(start.thickness, end.thickness, t)

            paint.strokeWidth = (previousThickness + currentThickness) / 2f
            paint.strokeCap = StrokeCap.Round

            canvas.drawLine(previousPoint, currentPoint, paint)

            previousPoint = currentPoint
            previousThickness = currentThickness
        }
    }
}

private fun DrawScope.drawPathScope(
    path: List<PathOffset>,
    color: Color,
    drawDebugPoints: Boolean = false
) {
    if (path.isEmpty()) return

    if (path.size < 2) {
        val p = path.first()
        drawCircle(color, radius = p.thickness / 2, center = p.offset)
        return
    }

    for (i in 1..path.lastIndex) {
        val start = path[i - 1]
        val end = path[i]
        val distance = (end.offset - start.offset).getDistance()

        if (distance < 1f) {
            drawLine(
                color = color,
                start = start.offset,
                end = end.offset,
                strokeWidth = (start.thickness + end.thickness) / 2f,
                cap = StrokeCap.Round
            )
            continue
        }

        val steps = ceil(distance / 2.5f).toInt().coerceAtLeast(2)

        var previousPoint = start.offset
        var previousThickness = start.thickness

        for (step in 1..steps) {
            val t = step.toFloat() / steps

            val currentPoint = when {
                end.controlPoint1 != Offset.Unspecified && end.controlPoint2 != Offset.Unspecified ->
                    cubicBezier(t, start.offset, end.controlPoint1, end.controlPoint2, end.offset)
                end.controlPoint1 != Offset.Unspecified ->
                    quadraticBezier(t, start.offset, end.controlPoint1, end.offset)
                else -> Offset(
                    lerp(start.offset.x, end.offset.x, t),
                    lerp(start.offset.y, end.offset.y, t)
                )
            }

            val currentThickness = lerp(start.thickness, end.thickness, t)

            drawLine(
                color = color,
                start = previousPoint,
                end = currentPoint,
                strokeWidth = (previousThickness + currentThickness) / 2f,
                cap = StrokeCap.Round
            )

            previousPoint = currentPoint
            previousThickness = currentThickness
        }
    }

    if (drawDebugPoints) {
        drawPoints(path.filter { p -> p.controlPoint1 != Offset.Unspecified }.map { p -> p.controlPoint1 }, PointMode.Points, Color.Cyan, 4f)
        drawPoints(path.filter { p -> p.controlPoint2 != Offset.Unspecified }.map { p -> p.controlPoint2 }, PointMode.Points, Color.Cyan, 4f)
        drawPoints(path.map { p -> p.offset }, PointMode.Points, Color.Red, 2.5f)
    }
}

private fun DrawScope.drawCursor(cursor: Offset) {
    drawCircle(Color.LightGray, radius = 5f, center = cursor, style = Stroke())
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = (1 - fraction) * start + fraction * stop
private fun cubicBezier(t: Float, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Offset {
    val u = 1 - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t
    val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
    return Offset(x, y)
}
private fun quadraticBezier(t: Float, p0: Offset, p1: Offset, p2: Offset): Offset {
    val u = 1 - t
    val tt = t * t
    val uu = u * u
    val x = uu * p0.x + 2 * u * t * p1.x + tt * p2.x
    val y = uu * p0.y + 2 * u * t * p1.y + tt * p2.y
    return Offset(x, y)
}
