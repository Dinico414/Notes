package com.xenonware.notes

import android.os.Build
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
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
    // cachedBitmap: The active image shown on screen
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    // drawnPathsCount: How many paths are currently baked into cachedBitmap
    var drawnPathsCount by remember { mutableIntStateOf(0) }
    // currentPathBakedSize: How many points of the ACTIVE stroke are baked
    var currentPathBakedSize by remember { mutableIntStateOf(0) }

    // Canvas Size tracker to ensure we create bitmaps of correct size
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val coroutineScope = rememberCoroutineScope()

    // 1. Handle Paths Updates (Drawing, Erasing, Undo)
    LaunchedEffect(paths) {
        // Wait until layout gives us a size
        if (canvasSize == IntSize.Zero) return@LaunchedEffect

        val currentBitmap = cachedBitmap

        // CASE A: LIST SHRUNK or CHANGED (Erasing / Undo)
        // If the list is smaller, OR the count implies a modification/replacement, we must redraw fully.
        // We also check if drawnPathsCount > paths.size to detect deletion.
        val needsFullRedraw = paths.size < drawnPathsCount || (paths.size == drawnPathsCount && paths.isNotEmpty()) || currentBitmap == null

        // CASE B: APPEND (Normal Drawing)
        // If we just added one new path to the end, we can draw it incrementally on the Main thread.
        val isAppend = paths.size == drawnPathsCount + 1 && currentBitmap != null

        if (isAppend && currentBitmap != null) {
            // -- FAST PATH --
            // Just draw the one new stroke onto the existing bitmap.
            // This is safe to do on Main thread because it's just one stroke.
            val canvas = ComposeCanvas(currentBitmap)
            val paint = Paint().apply { isAntiAlias = true }
            val newPath = paths.last()
            drawPathToCanvas(canvas, paint, newPath.path, newPath.color)
            drawnPathsCount = paths.size

            // Reset the baked size for the *next* potential active stroke
            currentPathBakedSize = 0
        }
        else if (needsFullRedraw) {
            // -- SLOW PATH (Background Thread) --
            // The list changed in a complex way (Eraser removed/split lines).
            // We offload the reconstruction of the bitmap to a background thread.
            // This prevents the UI (cursor) from freezing while we loop through 1000 paths.

            coroutineScope.launch(Dispatchers.Default) {
                // Create a NEW bitmap in background
                val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
                val canvas = ComposeCanvas(newBitmap)
                val paint = Paint().apply { isAntiAlias = true }

                // Draw ALL paths
                paths.forEach { pathData ->
                    drawPathToCanvas(canvas, paint, pathData.path, pathData.color)
                }

                // Swap in the new bitmap on Main thread
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
                .pointerInteropFilter {
                    if (!interactable) return@pointerInteropFilter true

                    if (debugText) {
                        debugString = "${it.action}\nPoints: ${currentPath?.path?.size ?: 0}\nBaked: $currentPathBakedSize"
                    }

                    // --- TOOL SWITCHING LOGIC ---
                    val isDetectedToolEraser = it.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
                    if (isDetectedToolEraser != wasEraserMode) {
                        wasEraserMode = isDetectedToolEraser
                        onAction(UpdateTool(
                            isEraser = isDetectedToolEraser,
                            usePressure = currentToolState.usePressure,
                            strokeWidth = currentToolState.strokeWidth,
                            strokeColor = currentToolState.color
                        ))
                    }

                    val isStylus = it.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                    val isFinger = it.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                    val isEraser = currentToolState.isEraser

                    // --- ERASER INPUT ---
                    if (isEraser) {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                onAction(Erase(Offset(it.x, it.y)))
                                cursorPos = Offset(it.x, it.y) // Update cursor even while erasing
                            }
                            MotionEvent.ACTION_UP -> {
                                onAction(PathEnd)
                                cursorPos = null
                            }
                            MotionEvent.ACTION_HOVER_MOVE -> {
                                cursorPos = Offset(it.x, it.y)
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
                        when (it.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE -> {
                                cursorPos = null
                                if (it.action == MotionEvent.ACTION_DOWN) onAction(NewPathStart)
                                onAction(Draw(Offset(it.x, it.y), it.pressure))
                                if (it.action == MotionEvent.ACTION_UP) onAction(PathEnd)
                            }
                            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_HOVER_MOVE -> {
                                cursorPos = if (it.action == MotionEvent.ACTION_HOVER_EXIT) null else Offset(it.x, it.y)
                            }
                        }
                        return@pointerInteropFilter true
                    }
                    return@pointerInteropFilter true
                }
                .drawWithCache {
                    // Update size tracker so LaunchedEffect knows how big the bitmap should be
                    val currentSize = IntSize(size.width.toInt(), size.height.toInt())
                    if (canvasSize != currentSize) {
                        canvasSize = currentSize
                        // If size changed, we force a bitmap reset logic implicitly by null check in helper or effect
                        // But practically, LaunchedEffect will catch the size change if we restart it or check inside
                        // Since LaunchedEffect(paths) doesn't watch canvasSize, we trigger a manual update if needed
                        // For simplicity, we assume size is stable after layout.
                    }

                    // Ensure we have at least one bitmap to start
                    if (cachedBitmap == null && size.width > 0 && size.height > 0) {
                        cachedBitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
                    }

                    val cellSizePx = 25.sp.roundToPx()
                    val gridColor = Color.LightGray
                    val xLines = (size.width / cellSizePx).toInt()
                    val yLines = (size.height / cellSizePx).toInt()

                    onDrawBehind {
                        // 1. Draw Grid
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

                        // 2. Draw the Cached Bitmap
                        // If we are erasing, this might be 1 frame "old", but the cursor will be live.
                        cachedBitmap?.let {
                            drawImage(image = it, topLeft = Offset.Zero)
                        }

                        // 3. Draw the Live Head of current stroke
                        currentPath?.let {
                            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
                            val liveSegment = if (startIdx < it.path.size) {
                                it.path.subList(startIdx, it.path.size)
                            } else {
                                emptyList()
                            }
                            drawPathScope(liveSegment, it.color, debugPoints)
                        }

                        // 4. Draw Cursor (Always on top, always fast)
                        cursorPos?.let { drawCursor(it) }
                    }
                }
        )
        if (debugText) {
            Text(debugString, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

// --- HELPER FUNCTIONS (Same as before) ---

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

        // Optimization: Step size 2.5f for speed
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