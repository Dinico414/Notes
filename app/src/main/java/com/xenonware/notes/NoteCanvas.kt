package com.xenonware.notes

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.abs

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
    var s by remember { mutableStateOf("") }
    var wasEraserMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var cursorPos: Offset? by remember { mutableStateOf(null) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter {
                    if (!interactable) return@pointerInteropFilter true

                    if (debugText) {
                        s = "${it.action}\n${it.getToolType(0)}\n${it.buttonState}\n" +
                                "${it.x} ${it.y}\n${it.pressure}\n${it.orientation}\n" +
                                "${it.getAxisValue(MotionEvent.AXIS_TILT)}\n" +
                                "${it.flags == MotionEvent.FLAG_CANCELED}"
                    }

                    val isDetectedToolEraser = it.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER

                    if (isDetectedToolEraser != wasEraserMode) {
                        wasEraserMode = isDetectedToolEraser
                        onAction(
                            UpdateTool(
                                isEraser = isDetectedToolEraser,
                                usePressure = currentToolState.usePressure,
                                strokeWidth = currentToolState.strokeWidth,
                                strokeColor = currentToolState.color
                            )
                        )
                    }

                    val isStylus = it.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                    val isFinger = it.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                    val isEraser = currentToolState.isEraser

                    if (isEraser) {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> {
                                onAction(Erase(Offset(it.x, it.y)))
                            }
                            MotionEvent.ACTION_UP -> {
                                onAction(PathEnd)
                            }
                        }
                        return@pointerInteropFilter true
                    }


                    val canDraw = isStylus || (isHandwritingMode && isFinger)

                    if (canDraw) {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_MOVE -> {
                                cursorPos = null
                                if (it.action == MotionEvent.ACTION_DOWN)
                                    onAction(NewPathStart)
                                onAction(Draw(Offset(it.x, it.y), it.pressure))
                                if (it.action == MotionEvent.ACTION_UP)
                                    onAction(PathEnd)
                            }
                            MotionEvent.ACTION_HOVER_ENTER,
                            MotionEvent.ACTION_HOVER_EXIT,
                            MotionEvent.ACTION_HOVER_MOVE -> {
                                if (it.action == MotionEvent.ACTION_HOVER_EXIT)
                                    cursorPos = null
                                else
                                    cursorPos = Offset(it.x, it.y)
                            }
                        }
                        return@pointerInteropFilter true
                    }
                    return@pointerInteropFilter true
                }
                .drawWithCache {
                    val cellSizePx = 25.sp.roundToPx()
                    val gridColor = Color.LightGray
                    val gridStrokeWidth = 1f
                    val xLines = (size.width / cellSizePx).toInt()
                    val xOffset = (size.width - xLines * cellSizePx) / 2f
                    val yLines = (size.height / cellSizePx).toInt()
                    val yOffset = (size.height - yLines * cellSizePx) / 2f

                    onDrawBehind {
                        if (gridEnabled) {
                            for (i in 0..xLines) {
                                val x = i * cellSizePx.toFloat()// + xOffset
                                drawLine(
                                    gridColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = gridStrokeWidth
                                )
                            }
                            for (i in 0..yLines) {
                                val y = i * cellSizePx.toFloat()// + yOffset
                                drawLine(
                                    gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(0f, size.height),
                                    strokeWidth = gridStrokeWidth
                                )
                            }
                        }
                    }
                }
        ) {
            paths.forEach {
                drawPath(it.path, it.color, debugPoints)
            }
            currentPath?.let {
                drawPath(it.path, it.color, debugPoints)
            }
            cursorPos?.let { drawCursor(it) }
        }
        Text(s, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawPath(
    path: List<PathOffset>,
    color: Color,
    drawDebugPoints: Boolean = false
) {
    if(path.isNotEmpty()) {
        val smoothedPath = Path()
        smoothedPath.moveTo(path.first().x, path.first().y)

        val thicknessMinChange = 0.2
        var thicknessSum = path.first().thickness
        var n = 1
        for(i in 1..path.lastIndex) {
            val from = path[i - 1]
            val to = path[i]

            if (to.controlPoint1 != Offset.Unspecified && to.controlPoint2 != Offset.Unspecified)
                smoothedPath.cubicTo(
                    to.controlPoint1.x,
                    to.controlPoint1.y,
                    to.controlPoint2.x,
                    to.controlPoint2.y,
                    to.x,
                    to.y
                )
            else if (to.controlPoint1 != Offset.Unspecified)
                smoothedPath.quadraticBezierTo(
                    to.controlPoint1.x,
                    to.controlPoint1.y,
                    to.x,
                    to.y
                )
            else
                smoothedPath.lineTo(to.x, to.y)

            thicknessSum += to.thickness
            n++

            if (i == path.lastIndex ||
                (i < path.lastIndex && (abs(to.thickness - thicknessSum / n) > thicknessMinChange))) {
                drawPath(
                    path = smoothedPath,
                    color = color,
                    style = Stroke(
                        width = from.thickness,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                smoothedPath.reset()
                smoothedPath.moveTo(to.x, to.y)
                thicknessSum = to.thickness
                n = 1
            }
        }
        if (drawDebugPoints) {
            drawPoints(
                path
                    .filter { p -> p.controlPoint1 != Offset.Unspecified }
                    .map { p -> p.controlPoint1 },
                pointMode = PointMode.Points,
                color = Color.Cyan,
                strokeWidth = 4f,
            )
            drawPoints(
                path
                    .filter { p -> p.controlPoint2 != Offset.Unspecified }
                    .map { p -> p.controlPoint2 },
                pointMode = PointMode.Points,
                color = Color.Cyan,
                strokeWidth = 4f,
            )
            drawPoints(
                path.map { p -> p.offset },
                pointMode = PointMode.Points,
                color = Color.Red,
                strokeWidth = 2.5f,
            )
        }
    }
}

private fun DrawScope.drawCursor(cursor: Offset) {
    drawCircle(
        Color.LightGray,
        radius = 5f,
        center = cursor,
        style = Stroke(),
    )
}