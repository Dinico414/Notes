@file:Suppress("VariableNeverRead", "AssignedValueIsNeverRead")

package com.xenonware.notes.util.sketch

import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.xenonware.notes.viewmodel.CurrentPathState
import com.xenonware.notes.viewmodel.DrawingAction
import com.xenonware.notes.viewmodel.DrawingAction.Draw
import com.xenonware.notes.viewmodel.DrawingAction.Erase
import com.xenonware.notes.viewmodel.DrawingAction.NewPathStart
import com.xenonware.notes.viewmodel.DrawingAction.PathEnd
import com.xenonware.notes.viewmodel.PathData
import com.xenonware.notes.viewmodel.PathOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NoteCanvas(
    paths: List<PathData>,
    currentPath: PathData?,
    currentToolState: CurrentPathState,
    isEraserMode: Boolean,
    onAction: (DrawingAction) -> Unit,
    isHandwritingMode: Boolean,
    modifier: Modifier = Modifier,
    gridEnabled: Boolean = false,
    drawColors: List<Color> = emptyList(),
    debugText: Boolean = false,
    debugPoints: Boolean = false,
    interactable: Boolean = true
) {
    var debugString by remember { mutableStateOf("") }

    // --- DOUBLE TAP AND HOLD STATE ---
    var lastUpTime by remember { mutableLongStateOf(0L) }
    var isDoubleTapSequence by remember { mutableStateOf(false) }
    var fillJob by remember { mutableStateOf<Job?>(null) }

    // --- BUFFERING STATE ---
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var drawnPathsCount by remember { mutableIntStateOf(0) }
    var currentPathBakedSize by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    
    val textMeasurer = rememberTextMeasurer()

    // --- SHAPE SNAP STATE ---
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var isShapeSnapped by remember { mutableStateOf(false) } // Blocks drawing after snap


    // 1. Handle Paths Updates (Drawing, Erasing, Undo, Load)

    LaunchedEffect(drawColors) {
        currentPathBakedSize = 0
    }

    LaunchedEffect(paths, canvasSize, drawColors) {
        if (canvasSize == IntSize.Zero) return@LaunchedEffect

        val currentBitmap = cachedBitmap
        val isAppend = paths.size == drawnPathsCount + 1 && 
                       currentBitmap != null && 
                       currentBitmap.width == canvasSize.width && 
                       currentBitmap.height == canvasSize.height
                       
        if (isAppend && currentBitmap != null) {
            val canvas = ComposeCanvas(currentBitmap)
            val paint = Paint().apply { isAntiAlias = true }
            val newPath = paths.last()
            drawPathToCanvas(canvas, paint, newPath.path, newPath.color, newPath.colorIndex, drawColors, newPath.isShape, newPath.fillColor, newPath.fillColorIndex)
            drawnPathsCount = paths.size
            currentPathBakedSize = 0
        } else {
            coroutineScope.launch(Dispatchers.Default) {
                val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
                val canvas = ComposeCanvas(newBitmap)
                val paint = Paint().apply { isAntiAlias = true }
                paths.forEach { pathData ->
                    drawPathToCanvas(canvas, paint, pathData.path, pathData.color, pathData.colorIndex, drawColors, pathData.isShape, pathData.fillColor, pathData.fillColorIndex)
                }
                withContext(Dispatchers.Main) {
                    cachedBitmap = newBitmap
                    drawnPathsCount = paths.size
                    currentPathBakedSize = 0
                }
            }
        }
    }

    // 2. Handle "Current" Active Path
    LaunchedEffect(currentPath) {
        val bitmap = cachedBitmap ?: return@LaunchedEffect

        if (currentPath == null) {
            currentPathBakedSize = 0
            return@LaunchedEffect
        }

        // If the path shrunk (replaced by shape), reset baking to redraw the clean shape
        if (currentPath.path.size < currentPathBakedSize) {
            currentPathBakedSize = 0
        }

        val bufferSize = 4
        val safeIndex = currentPath.path.size - bufferSize

        if (safeIndex > currentPathBakedSize && safeIndex > 1) {
            val canvas = ComposeCanvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true }

            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
            val segmentToBake = currentPath.path.subList(startIdx, safeIndex + 1)

            drawPathToCanvas(canvas, paint, segmentToBake,
                currentPath.color, currentPath.colorIndex, drawColors)
            currentPathBakedSize = safeIndex
        }
    }

    LaunchedEffect(isShapeSnapped) {
        if (isShapeSnapped) {
            coroutineScope.launch(Dispatchers.Default) {
                // 1. Create a fresh bitmap
                val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
                val canvas = ComposeCanvas(newBitmap)
                val paint = Paint().apply { isAntiAlias = true }

                // 2. Draw ONLY the completed paths (the viewmodel paths).
                // The messy stroke currently in 'currentPath' is NOT drawn here.
                paths.forEach { pathData ->
                    drawPathToCanvas(canvas, paint, pathData.path, pathData.color, pathData.colorIndex, drawColors, pathData.isShape, pathData.fillColor, pathData.fillColorIndex)
                }

                // 3. Swap the bitmap
                withContext(Dispatchers.Main) {
                    cachedBitmap = newBitmap
                    drawnPathsCount = paths.size
                    currentPathBakedSize = 0
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var cursorPos: Offset? by remember { mutableStateOf(null) }
        val drawColorsState = remember { mutableStateOf(drawColors) }
        SideEffect { drawColorsState.value = drawColors }
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    if (!interactable) return@pointerInteropFilter true

                    val toolInt = event.getToolType(0)
                    val toolStr = when (toolInt) {
                        MotionEvent.TOOL_TYPE_UNKNOWN -> "UNKNOWN"
                        MotionEvent.TOOL_TYPE_FINGER -> "FINGER"
                        MotionEvent.TOOL_TYPE_STYLUS -> "STYLUS"
                        MotionEvent.TOOL_TYPE_MOUSE -> "MOUSE"
                        MotionEvent.TOOL_TYPE_ERASER -> "ERASER"
                        else -> "OTHER"
                    }

                    if (debugText) {
                        val actionStr = MotionEvent.actionToString(event.action)
                        val btnState = event.buttonState
                        val btnStr = buildString {
                            if (btnState == 0) append("NONE ")
                            if ((btnState and MotionEvent.BUTTON_PRIMARY) != 0) append("PRIMARY ")
                            if ((btnState and MotionEvent.BUTTON_SECONDARY) != 0) append("SECONDARY ")
                            if ((btnState and MotionEvent.BUTTON_TERTIARY) != 0) append("TERTIARY ")
                            if ((btnState and MotionEvent.BUTTON_BACK) != 0) append("BACK ")
                            if ((btnState and MotionEvent.BUTTON_FORWARD) != 0) append("FORWARD ")
                            if ((btnState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0) append("STYLUS_PRIMARY ")
                            if ((btnState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0) append("STYLUS_SECONDARY ")
                        }.trimEnd()

                        debugString = "" +
                            "Action: $actionStr (${event.action})\n" +
                            "X: ${String.format(Locale.US, "%.1f", event.x)}, Y: ${String.format(Locale.US, "%.1f", event.y)}\n" +
                            "Pressure: ${String.format(Locale.US, "%.2f", event.pressure)}\n" +
                            "Tool: $toolStr ($toolInt)\n" +
                            "Button: $btnStr ($btnState)\n" +
                            "Pointer Count: ${event.pointerCount}\n" +
                            "Size: ${String.format(Locale.US, "%.2f", event.size)}\n" +
                            "Orientation: ${String.format(Locale.US, "%.2f", event.orientation)}\n" +
                            "Path Points: ${currentPath?.path?.size ?: 0}\n" +
                            "Snapped: $isShapeSnapped"
                    }

                    val isDetectedToolEraser = event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
                    val isStylus = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                    val isFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                    val isEraser = isDetectedToolEraser || isEraserMode

                    // --- ERASER INPUT ---
                    if (isEraser) {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    Log.d("NoteCanvasInput", "Input Tool: $toolStr")
                                }
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
                                Log.d("NoteCanvasInput", "Input Tool: $toolStr")
                                val now = System.currentTimeMillis()
                                if (now - lastUpTime < 300L) {
                                    isDoubleTapSequence = true
                                    fillJob = coroutineScope.launch {
                                        delay(500L)
                                        // Double tap and hold triggered
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        withContext(Dispatchers.Main) {
                                            onAction(NewPathStart) // Cancel current drawing
                                            
                                            // Check if the previous path was just a dot/short tap
                                            val lastPath = paths.lastOrNull()
                                            if (lastPath != null) {
                                                var pathLength = 0f
                                                for (i in 1 until lastPath.path.size) {
                                                    pathLength += (lastPath.path[i].offset - lastPath.path[i-1].offset).getDistance()
                                                }
                                                // If the entire first tap was less than 50 pixels long, undo it
                                                if (pathLength < 50f) {
                                                    onAction(DrawingAction.Undo)
                                                }
                                            }

                                            onAction(DrawingAction.FillShape(currentPos, currentToolState.color))
                                        }
                                    }
                                } else {
                                    isDoubleTapSequence = false
                                }

                                cursorPos = null
                                // Reset state
                                isShapeSnapped = false
                                holdJob?.cancel()
                                lastTouchPosition = currentPos

                                onAction(NewPathStart)
                                onAction(Draw(currentPos, event.pressure))
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val dist = (currentPos - lastTouchPosition).getDistance()
                                if (dist > 20f) {
                                    fillJob?.cancel()
                                }

                                // If snapped, we stop drawing to protect the perfect shape
                                if (!isShapeSnapped) {
                                    onAction(Draw(currentPos, event.pressure))
                                }

                                // --- SNAP LOGIC ---
                                // If moved > 20px, reset timer
                                if (dist > 20f) {
                                    lastTouchPosition = currentPos
                                    holdJob?.cancel()

                                    // Only restart timer if we haven't already snapped
                                    if (!isShapeSnapped) {
                                        holdJob = coroutineScope.launch {
                                            delay(1000L) // Reduced to 1 Second

                                            // Timer finished, analyze shape
                                            currentPath?.let { activePath ->
                                                val shapePath = ShapeRecognizer.recognizeAndCreatePath(
                                                    points = activePath.path,
                                                    originalThickness = currentToolState.strokeWidth
                                                )

                                                if (shapePath != null) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                                                    // Stop further drawing events
                                                    isShapeSnapped = true

                                                    // Replace the path
                                                    withContext(Dispatchers.Main) {
                                                        onAction(DrawingAction.SnapToShape(shapePath))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            MotionEvent.ACTION_UP -> {
                                lastUpTime = System.currentTimeMillis()
                                fillJob?.cancel()
                                holdJob?.cancel()
                                isShapeSnapped = false
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
                            val liveSegment = if (startIdx < it.path.size) it.path.subList(startIdx, it.path.size) else emptyList()
                            drawPathScope(liveSegment, it.color, it.colorIndex, drawColorsState.value, debugPoints, it.isShape, it.fillColor, it.fillColorIndex)
                        }
                        cursorPos?.let { drawCursor(it) }

                        if (debugText && debugString.isNotEmpty()) {
                            val textLayoutResult = textMeasurer.measure(
                                text = debugString,
                                style = TextStyle(fontSize = 14.sp)
                            )
                            val textWidth = textLayoutResult.size.width
                            val textHeight = textLayoutResult.size.height
                            val textOffset = Offset(
                                x = (size.width - textWidth) / 2f,
                                y = (size.height - textHeight) / 2f
                            )

                            // Draw inverted white text that forces maximum pixel contrast against whatever is beneath it.
                            drawText(
                                textMeasurer = textMeasurer,
                                text = debugString,
                                topLeft = textOffset,
                                style = TextStyle(fontSize = 14.sp, color = Color.White),
                                blendMode = BlendMode.Difference
                            )
                        }
                    }
                }
        )
    }
}

// --- HELPER FUNCTIONS ---

private fun drawPathToCanvas(
    canvas: ComposeCanvas,
    paint: Paint,
    path: List<PathOffset>,
    color: Color,
    colorIndex: Int = -1,
    drawColors: List<Color> = emptyList(),
    isShape: Boolean = false,
    fillColor: Color = Color.Transparent,
    fillColorIndex: Int = -1
) {
    if (path.isEmpty()) return

    if (isShape && fillColor != Color.Transparent) {
        val resolvedFillColor = if (fillColorIndex in drawColors.indices) drawColors[fillColorIndex] else fillColor
        val fillPaint = Paint().apply {
            this.color = resolvedFillColor
            this.style = PaintingStyle.Fill
            this.isAntiAlias = true
        }
        val composePath = androidx.compose.ui.graphics.Path()
        composePath.moveTo(path[0].offset.x, path[0].offset.y)
        for (i in 1 until path.size) {
            composePath.lineTo(path[i].offset.x, path[i].offset.y)
        }
        composePath.close()
        canvas.drawPath(composePath, fillPaint)
    }

    val resolvedColor = if (colorIndex in drawColors.indices) {
        drawColors[colorIndex]
    } else {
        color
    }
    paint.color = resolvedColor
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
    colorIndex: Int = -1,
    drawColors: List<Color> = emptyList(),
    drawDebugPoints: Boolean = false,
    isShape: Boolean = false,
    fillColor: Color = Color.Transparent,
    fillColorIndex: Int = -1
) {
    if (path.isEmpty()) return

    if (isShape && fillColor != Color.Transparent) {
        val resolvedFillColor = if (fillColorIndex in drawColors.indices) drawColors[fillColorIndex] else fillColor
        val composePath = androidx.compose.ui.graphics.Path()
        composePath.moveTo(path[0].offset.x, path[0].offset.y)
        for (i in 1 until path.size) {
            composePath.lineTo(path[i].offset.x, path[i].offset.y)
        }
        composePath.close()
        drawPath(
            path = composePath,
            color = resolvedFillColor,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }

    val resolvedColor = if (colorIndex in drawColors.indices) {
        drawColors[colorIndex]
    } else {
        color
    }

    if (path.size < 2) {
        val p = path.first()
        drawCircle(resolvedColor, radius = p.thickness / 2, center = p.offset)
        return
    }

    for (i in 1..path.lastIndex) {
        val start = path[i - 1]
        val end = path[i]
        val distance = (end.offset - start.offset).getDistance()

        if (distance < 1f) {
            drawLine(
                color = resolvedColor,
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
                color = resolvedColor,
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