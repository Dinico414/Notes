package com.xenonware.notes.util

import androidx.compose.ui.geometry.Offset
import com.xenonware.notes.viewmodel.PathOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object ShapeRecognizer {

    // Configuration
    private const val CLOSED_THRESHOLD = 0.20f // Gap allowed to consider a shape "closed"
    private const val LINE_LINEARITY_THRESHOLD = 0.96f // How straight a line must be (0.0 - 1.0)
    private const val MIN_CORNER_ANGLE = 30.0 // Min degrees to be considered a corner

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
        val start = rawPoints.first()
        val end = rawPoints.last()
        val distStartEnd = (start - end).getDistance()
        val totalPathLength = calculatePathLength(rawPoints)

        // Safety check for dots/taps
        if (totalPathLength < 20f) return null

        // 2. Check for LINE (Open Shape)
        // If the distance between start/end is close to the total length drawn, it's a line.
        val linearity = distStartEnd / totalPathLength
        if (linearity > LINE_LINEARITY_THRESHOLD) {
            // It's a straight line
            return generatePerfectShape(ShapeType.LINE, minX, minY, maxX, maxY, originalThickness, start, end)
        }

        // 3. Check for CLOSED Shape
        val perimeterApprox = (width + height) * 2
        val isClosed = distStartEnd < perimeterApprox * CLOSED_THRESHOLD

        // If it's not a straight line and not closed, we ignore it (or you could force close it)
        if (!isClosed) return null

        // 4. Geometric Analysis (Resample -> Count Corners)
        // We resample to ~32 points to remove hand jitter and speed variations
        val resampled = resamplePoints(rawPoints)
        val corners = detectCorners(resampled)

        // Aspect Ratio for Square vs Rect / Circle vs Oval
        val aspectRatio = if (width > height) width / height else height / width
        val isEquilateral = aspectRatio < 1.25f // Square or Circle

        val detectedShape = when (corners) {
            // 0 or 1 "soft" corners usually means a round shape
            0, 1 -> {
                // Double check standard deviation to ensure it's not a blob
                if (isCircleIsh(rawPoints, Offset(minX + width/2, minY + height/2))) {
                    if (isEquilateral) ShapeType.CIRCLE else ShapeType.OVAL
                } else {
                    // If it has no corners but isn't round (e.g. a kidney bean), default to Oval or ignore
                    ShapeType.OVAL
                }
            }
            // 3 Corners is a Triangle
            3 -> ShapeType.TRIANGLE
            // 4 Corners is a Box
            4 -> if (isEquilateral) ShapeType.SQUARE else ShapeType.RECTANGLE
            // 5+ Corners: Usually a messy rectangle or polygon.
            // We bias towards Rectangle if it's roughly 4-ish.
            else -> if (isEquilateral) ShapeType.SQUARE else ShapeType.RECTANGLE
        }

        return generatePerfectShape(detectedShape, minX, minY, maxX, maxY, originalThickness, start, end)
    }

    /**
     * Analyzes the angles between segments to count distinct corners.
     */
    private fun detectCorners(points: List<Offset>): Int {
        if (points.size < 3) return 0

        var corners = 0
        val angles = mutableListOf<Double>()

        // Calculate angle changes at every point
        for (i in 0 until points.size) {
            val pPrev = points[(i - 1 + points.size) % points.size]
            val pCurr = points[i]
            val pNext = points[(i + 1) % points.size]

            // Vectors
            val v1x = (pCurr.x - pPrev.x).toDouble()
            val v1y = (pCurr.y - pPrev.y).toDouble()
            val v2x = (pNext.x - pCurr.x).toDouble()
            val v2y = (pNext.y - pCurr.y).toDouble()

            val angle1 = atan2(v1y, v1x)
            val angle2 = atan2(v2y, v2x)

            // Difference in radians, normalized to -PI..PI
            var diff = angle2 - angle1
            while (diff <= -PI) diff += 2 * PI
            while (diff > PI) diff -= 2 * PI

            // Convert to degrees
            val degrees = Math.toDegrees(abs(diff))
            angles.add(degrees)
        }

        // Filter noise: A real corner usually has a sharp angle change > 30 degrees
        // We also need to ensure corners aren't right next to each other (clustering)
        var i = 0
        while (i < angles.size) {
            if (angles[i] > MIN_CORNER_ANGLE) {
                corners++
                // Skip next few points to avoid counting the same rounded corner twice
                i += 3
            } else {
                i++
            }
        }

        return corners
    }

    /**
     * Resamples the path into a set number of equidistant points.
     * This is crucial for corner detection independent of drawing speed.
     */
    private fun resamplePoints(points: List<Offset>): List<Offset> {
        if (points.isEmpty()) return emptyList()

        val targetCount = 32
        val result = mutableListOf<Offset>()
        result.add(points.first())

        val totalLength = calculatePathLength(points)
        val interval = totalLength / (targetCount - 1)

        // Make a mutable copy to consume
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
                val newX = p1.x + (p2.x - p1.x) * t
                val newY = p1.y + (p2.y - p1.y) * t
                val newP = Offset(newX, newY)

                result.add(newP)
                srcPoints.add(i + 1, newP) // Insert logic to continue from here
                accumulatedDist = 0f
                i++ // Move to the inserted point
            } else {
                accumulatedDist += dist
                i++
            }

            if (result.size >= targetCount) break
        }

        // Ensure polygon is closed for logic (append start to end)
        if (result.size < targetCount) result.add(points.last())

        return result
    }

    /**
     * Stricter check for roundness using Variance.
     */
    private fun isCircleIsh(points: List<Offset>, center: Offset): Boolean {
        var totalDist = 0f
        points.forEach { totalDist += (it - center).getDistance() }
        val avgDist = totalDist / points.size

        var varianceSum = 0f
        points.forEach { varianceSum += (it - center).getDistance().minus(avgDist).pow(2) }
        val variance = varianceSum / points.size
        val stdDev = sqrt(variance)

        // Threshold 0.15 is strict. A square usually has > 0.25
        return stdDev < (avgDist * 0.15f)
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
        startPoint: Offset, // Used for Line
        endPoint: Offset    // Used for Line
    ): List<PathOffset> {
        val newPoints = mutableListOf<PathOffset>()
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        val width = right - left
        val height = bottom - top
        val radiusX = width / 2
        val radiusY = height / 2

        val steps = 60 // Resolution

        when (type) {
            ShapeType.LINE -> {
                // Simple straight line from actual start to actual end
                addLerpPoints(newPoints, startPoint, endPoint, thickness)
                return newPoints // Return early, don't close loop
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
                // Isosceles triangle logic based on bounding box
                addLerpPoints(newPoints, Offset(centerX, top), Offset(right, bottom), thickness)
                addLerpPoints(newPoints, Offset(right, bottom), Offset(left, bottom), thickness)
                addLerpPoints(newPoints, Offset(left, bottom), Offset(centerX, top), thickness)
            }
        }

        // Close the loop for shapes
        if (newPoints.isNotEmpty()) {
            newPoints.add(newPoints.first().copy())
        }

        // Apply color
        return newPoints.map { PathOffset(it.offset, thickness).apply {
            // Ensure clean state for new points
            controlPoint1 = Offset.Unspecified
            controlPoint2 = Offset.Unspecified
        }}
    }

    private fun addLerpPoints(list: MutableList<PathOffset>, start: Offset, end: Offset, thickness: Float) {
        val steps = 15
        for (i in 0 until steps) {
            val t = i.toFloat() / steps
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            list.add(PathOffset(Offset(x, y), thickness))
        }
        // Add the exact end point to ensure corners are sharp
        list.add(PathOffset(end, thickness))
    }
}


//TODO Circle not working as well as triangle