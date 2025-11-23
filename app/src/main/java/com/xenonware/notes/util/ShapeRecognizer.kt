package com.xenonware.notes.util

import androidx.compose.ui.geometry.Offset
import com.xenonware.notes.viewmodel.PathOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object ShapeRecognizer {

    // Configuration
    private const val CLOSED_THRESHOLD = 0.20f // Gap allowed to consider a shape "closed"
    private const val LINE_LINEARITY_THRESHOLD = 0.96f // How straight a line must be (0.0 - 1.0)
    private const val MIN_CORNER_ANGLE = 35.0 // Degrees to be considered a corner (Stricter)

    enum class ShapeType { CIRCLE, OVAL, SQUARE, RECTANGLE, TRIANGLE, LINE }

    fun recognizeAndCreatePath(
        points: List<PathOffset>,
        originalThickness: Float,
    ): List<PathOffset>? {
        if (points.size < 5) return null

        val rawPoints = points.map { it.offset }

        // 1. Bounding Box & Basic Metrics
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        rawPoints.forEach {
            minX = min(minX, it.x)
            minY = min(minY, it.y)
            maxX = max(maxX, it.x)
            maxY = max(maxY, it.y)
        }

        val width = maxX - minX
        val height = maxY - minY
        val center = Offset(minX + width / 2, minY + height / 2)

        val start = rawPoints.first()
        val end = rawPoints.last()
        val distStartEnd = (start - end).getDistance()
        val totalPathLength = calculatePathLength(rawPoints)

        // Safety check for dots/taps
        if (totalPathLength < 30f) return null

        // 2. Check for LINE (Open Shape)
        val linearity = distStartEnd / totalPathLength
        if (linearity > LINE_LINEARITY_THRESHOLD) {
            return generatePerfectShape(ShapeType.LINE, minX, minY, maxX, maxY, originalThickness, start, end)
        }

        // 3. Check for CLOSED Shape
        val perimeterApprox = (width + height) * 2
        val isClosed = distStartEnd < perimeterApprox * CLOSED_THRESHOLD

        if (!isClosed) return null

        // 4. Geometric Analysis
        val resampled = resamplePoints(rawPoints)
        val corners = detectCorners(resampled)

        // Calculate Radial Statistics (Crucial for differentiating Circle vs Rounded Square)
        val radii = rawPoints.map { (it - center).getDistance() }
        val minRadius = radii.minOrNull() ?: 0f
        val maxRadius = radii.maxOrNull() ?: 0f
        // A perfect circle has a ratio of 1.0. A square is ~1.41. A triangle is ~2.0.
        val radiusRatio = if (minRadius > 0) maxRadius / minRadius else 2f

        val aspectRatio = if (width > height) width / height else height / width
        val isEquilateral = aspectRatio < 1.2f

        val detectedShape = when {
            // TRIANGLE: 3 Corners OR high radius ratio with low corner count (soft corners)
            corners == 3 -> ShapeType.TRIANGLE

            // BOXES: 4 Corners OR high radius ratio (indicates corners exist even if not detected)
            corners == 4 -> if (isEquilateral) ShapeType.SQUARE else ShapeType.RECTANGLE

            // ROUND SHAPES (0, 1, 2, or >4 messy corners)
            else -> {
                // Strict check: If the radius varies too much (ratio > 1.15), it is NOT a circle.
                // It's likely a messy Square or Triangle.
                if (radiusRatio > 1.25f) {
                    // Fallback heuristic: If it looks like a box, make it a box.
                    if (isEquilateral) ShapeType.SQUARE else ShapeType.RECTANGLE
                } else {
                    // It's round. Check if it's a perfect circle or an oval.
                    if (isEquilateral && radiusRatio < 1.15f) ShapeType.CIRCLE else ShapeType.OVAL
                }
            }
        }

        return generatePerfectShape(detectedShape, minX, minY, maxX, maxY, originalThickness, start, end)
    }

    private fun detectCorners(points: List<Offset>): Int {
        if (points.size < 3) return 0

        var corners = 0
        val angles = mutableListOf<Double>()

        for (i in 0 until points.size) {
            val pPrev = points[(i - 1 + points.size) % points.size]
            val pCurr = points[i]
            val pNext = points[(i + 1) % points.size]

            val angle1 = atan2((pCurr.y - pPrev.y).toDouble(), (pCurr.x - pPrev.x).toDouble())
            val angle2 = atan2((pNext.y - pCurr.y).toDouble(), (pNext.x - pCurr.x).toDouble())

            var diff = angle2 - angle1
            while (diff <= -PI) diff += 2 * PI
            while (diff > PI) diff -= 2 * PI

            angles.add(Math.toDegrees(abs(diff)))
        }

        var i = 0
        while (i < angles.size) {
            // Angle check
            if (angles[i] > MIN_CORNER_ANGLE) {
                corners++
                i += 4 // Skip more points to avoid double counting the same corner
            } else {
                i++
            }
        }
        return corners
    }

    private fun resamplePoints(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return emptyList()

        val targetCount = 40 // Increased resolution for better corner handling
        val result = mutableListOf<Offset>()
        result.add(points.first())

        val totalLength = calculatePathLength(points)
        val interval = totalLength / (targetCount - 1)

        val srcPoints = points.toMutableList()
        var accumulatedDist = 0f

        var i = 0
        while (i < srcPoints.size - 1) {
            val p1 = srcPoints[i]
            val p2 = srcPoints[i+1]
            val dist = (p1 - p2).getDistance()

            if (accumulatedDist + dist >= interval) {
                val remaining = interval - accumulatedDist
                val t = remaining / dist
                val newP = Offset(
                    p1.x + (p2.x - p1.x) * t,
                    p1.y + (p2.y - p1.y) * t
                )

                result.add(newP)
                srcPoints.add(i + 1, newP)
                accumulatedDist = 0f
                i++
            } else {
                accumulatedDist += dist
                i++
            }
            if (result.size >= targetCount) break
        }
        if (result.size < targetCount) result.add(points.last())
        return result
    }

    private fun calculatePathLength(points: List<Offset>): Float {
        var len = 0f
        for (i in 0 until points.lastIndex) {
            len += (points[i] - points[i+1]).getDistance()
        }
        return len
    }

    private fun generatePerfectShape(
        type: ShapeType,
        left: Float, top: Float, right: Float, bottom: Float,
        thickness: Float,
        startPoint: Offset,
        endPoint: Offset
    ): List<PathOffset> {
        val newPoints = mutableListOf<PathOffset>()
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        val width = right - left
        val height = bottom - top
        val radiusX = width / 2
        val radiusY = height / 2

        val steps = 60

        when (type) {
            ShapeType.LINE -> {
                addLerpPoints(newPoints, startPoint, endPoint, thickness)
                return newPoints
            }
            ShapeType.CIRCLE -> {
                val r = (radiusX + radiusY) / 2
                for (i in 0..steps) {
                    val theta = (i.toFloat() / steps) * 2 * PI
                    val x = centerX + r * cos(theta).toFloat()
                    val y = centerY + r * sin(theta).toFloat()
                    newPoints.add(PathOffset(Offset(x, y), thickness))
                }
            }
            ShapeType.OVAL -> {
                for (i in 0..steps) {
                    val theta = (i.toFloat() / steps) * 2 * PI
                    val x = centerX + radiusX * cos(theta).toFloat()
                    val y = centerY + radiusY * sin(theta).toFloat()
                    newPoints.add(PathOffset(Offset(x, y), thickness))
                }
            }
            ShapeType.SQUARE, ShapeType.RECTANGLE -> {
                addLerpPoints(newPoints, Offset(left, top), Offset(right, top), thickness)
                addLerpPoints(newPoints, Offset(right, top), Offset(right, bottom), thickness)
                addLerpPoints(newPoints, Offset(right, bottom), Offset(left, bottom), thickness)
                addLerpPoints(newPoints, Offset(left, bottom), Offset(left, top), thickness)
            }
            ShapeType.TRIANGLE -> {
                // Isosceles triangle
                addLerpPoints(newPoints, Offset(centerX, top), Offset(right, bottom), thickness)
                addLerpPoints(newPoints, Offset(right, bottom), Offset(left, bottom), thickness)
                addLerpPoints(newPoints, Offset(left, bottom), Offset(centerX, top), thickness)
            }
        }

        if (newPoints.isNotEmpty()) {
            newPoints.add(newPoints.first().copy())
        }

        return newPoints.map { PathOffset(it.offset, thickness) }
    }

    private fun addLerpPoints(list: MutableList<PathOffset>, start: Offset, end: Offset, thickness: Float) {
        val lerpSteps = 15
        for (i in 0 until lerpSteps) {
            val t = i.toFloat() / lerpSteps
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            list.add(PathOffset(Offset(x, y), thickness))
        }
        list.add(PathOffset(end, thickness))
    }
}