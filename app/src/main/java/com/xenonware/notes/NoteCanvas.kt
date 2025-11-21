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
import androidx.compose.ui.graphics.nativeCanvas
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
    // Holds the bitmap of completed paths (and the baked tail of the current path)
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    // Tracks how many completed paths from the list are already on the bitmap
    var drawnPathsCount by remember { mutableIntStateOf(0) }
    // Tracks how many points of the CURRENT active path are already on the bitmap
    var currentPathBakedSize by remember { mutableIntStateOf(0) }

    // Helper to ensure bitmap exists and is the right size
    fun ensureBitmap(size: IntSize): ImageBitmap {
        val current = cachedBitmap
        return if (current != null && current.width == size.width && current.height == size.height) {
            current
        } else {
            val newBitmap = ImageBitmap(size.width, size.height)
            cachedBitmap = newBitmap
            drawnPathsCount = 0
            currentPathBakedSize = 0
            newBitmap
        }
    }

    // 1. Handle "Old" Finished Paths
    // This updates the bitmap when the user lifts their finger and a path is added to 'paths'
    LaunchedEffect(paths, cachedBitmap) {
        val bitmap = cachedBitmap ?: return@LaunchedEffect
        val canvas = ComposeCanvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // If paths list shrunk (Undo/Clear), we must reset everything
        if (paths.size < drawnPathsCount) {
            canvas.nativeCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            drawnPathsCount = 0
            currentPathBakedSize = 0
        }

        // Draw new finished paths
        if (paths.size > drawnPathsCount) {
            for (i in drawnPathsCount until paths.size) {
                val pathData = paths[i]
                drawPathToCanvas(canvas, paint, pathData.path, pathData.color)
            }
            drawnPathsCount = paths.size
        }
    }

    // 2. Handle "Current" Active Path (Incremental Baking)
    // This updates the bitmap WHILE the user is drawing if the line gets too long
    LaunchedEffect(currentPath) {
        val pathData = currentPath
        val bitmap = cachedBitmap

        // If user lifted finger (currentPath is null), reset our baked counter
        if (pathData == null || bitmap == null) {
            currentPathBakedSize = 0
            return@LaunchedEffect
        }

        // We keep a buffer of ~4 points at the end to allow the Bezier curve to update smoothly
        val bufferSize = 4
        val safeIndex = pathData.path.size - bufferSize

        if (safeIndex > currentPathBakedSize && safeIndex > 1) {
            val canvas = ComposeCanvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true }

            // Extract the segment we want to bake
            // We start from (currentPathBakedSize - 1) to ensure connection with previous segment
            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
            val segmentToBake = pathData.path.subList(startIdx, safeIndex + 1)

            drawPathToCanvas(canvas, paint, segmentToBake, pathData.color)

            // Update the tracker so we don't draw these points again
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

                    if (isEraser) {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> onAction(Erase(Offset(it.x, it.y)))
                            MotionEvent.ACTION_UP -> onAction(PathEnd)
                        }
                        return@pointerInteropFilter true
                    }

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
                    // Initialize or resize bitmap
                    val bitmap = ensureBitmap(IntSize(size.width.toInt(), size.height.toInt()))

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

                        // 2. Draw the Cached Bitmap (Background Layer)
                        drawImage(image = bitmap, topLeft = Offset.Zero)

                        // 3. Draw the Live Segment (The "Head" of the current path)
                        currentPath?.let {
                            // Only draw from the baked point onwards
                            val startIdx = (currentPathBakedSize - 1).coerceAtLeast(0)
                            val liveSegment = if (startIdx < it.path.size) {
                                it.path.subList(startIdx, it.path.size)
                            } else {
                                emptyList()
                            }

                            drawPathScope(liveSegment, it.color, debugPoints)
                        }

                        // 4. Draw Cursor
                        cursorPos?.let { drawCursor(it) }
                    }
                }
        )
        if (debugText) {
            Text(debugString, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

/**
 * Draws a path onto a native Android Canvas (used for the Bitmap buffer).
 */
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

        // Skip tiny movements to save CPU
        if (distance < 1f) continue

        // Optimization: Step size 2.5f (Reduced from 1.5f for performance)
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

/**
 * Draws a path within the Compose DrawScope (used for the live "Head" of the path).
 */
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
        drawPoints(
            path.filter { p -> p.controlPoint1 != Offset.Unspecified }.map { p -> p.controlPoint1 },
            pointMode = PointMode.Points, color = Color.Cyan, strokeWidth = 4f
        )
        drawPoints(
            path.filter { p -> p.controlPoint2 != Offset.Unspecified }.map { p -> p.controlPoint2 },
            pointMode = PointMode.Points, color = Color.Cyan, strokeWidth = 4f
        )
        drawPoints(
            path.map { p -> p.offset },
            pointMode = PointMode.Points, color = Color.Red, strokeWidth = 2.5f
        )
    }
}


private fun DrawScope.drawCursor(cursor: Offset) {
    drawCircle(Color.LightGray, radius = 5f, center = cursor, style = Stroke())
}

// --- Math Helpers ---

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

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