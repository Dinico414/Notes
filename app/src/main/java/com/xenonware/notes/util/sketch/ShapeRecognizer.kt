@file:Suppress("unused")

package com.xenonware.notes.util.sketch

import androidx.compose.ui.geometry.Offset
import com.xenonware.notes.viewmodel.PathOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object ShapeRecognizer {

    // --- CONFIGURATION (tuned for reliability) ---
    private const val LINE_LINEARITY = 0.92f       // unchanged
    private const val CIRCLE_VARIANCE = 0.28f      // higher = easier circles/ovals
    private const val ARC_VARIANCE = 0.35f         // allows partial arcs (Bögen)
    private const val MIN_CORNER_ANGLE = 160.0     // was 160 → much less false corners
    private const val MERGE_CORNER_DIST = 30f
    private const val CLOSE_THRESHOLD = 0.08f      // fraction of total length

    enum class ShapeType { CIRCLE, OVAL, TRIANGLE, QUAD, LINE, RECT, ARC }

    fun recognizeAndCreatePath(
        points: List<PathOffset>,
        originalThickness: Float,
    ): List<PathOffset>? {
        if (points.size < 10) return null

        val rawPoints = points.map { it.offset }
        val start = rawPoints.first()
        val end = rawPoints.last()
        val totalLength = calculatePathLength(rawPoints)
        val directDist = (start - end).getDistance()

        // 1. LINE CHECK (very straight open shape)
        if (directDist > totalLength * LINE_LINEARITY) {
            return createLinearPath(start, end, originalThickness)
        }

        val centroid = calculateCentroid(rawPoints)
        val (avgRadius, cv) = getCircularityMetrics(rawPoints, centroid)

        val isClosed = directDist < totalLength * CLOSE_THRESHOLD || directDist < 25f

        // === NEW: Much stricter circle/arc detection ===
        // Only trigger circle/arc if variance is quite low OR the shape is very clean and closed
        val isVeryCircular = cv < 0.18f   // was 0.28f / 0.35f → much stricter

        if (isVeryCircular) {
            return if (isClosed) {
                generateCircleOrOval(rawPoints, centroid, originalThickness)
            } else {
                // For open arcs (Bögen), require even cleaner curvature
                if (totalLength > 80f) {
                    generateArc(rawPoints, centroid, avgRadius, start, end, originalThickness)
                } else {
                    null  // don't force arc on slightly curved lines
                }
            }
        }

        // === Polygon detection (run when it's NOT very circular) ===
        val corners = findCorners(rawPoints)

        if (corners.size == 3) {
            // Extra check: triangles should NOT be too circular
            if (cv > 0.22f) {
                return createPolygonPath(corners, originalThickness)
            }
        }

        if (corners.size == 4) {
            return if (isRectangular(corners)) {
                createRectifiedBox(corners, originalThickness)
            } else {
                createPolygonPath(corners, originalThickness)
            }
        }

        if (corners.size == 5) {
            val reduced = mergeClosestPoints(corners)
            if (reduced.size == 4) {
                return if (isRectangular(reduced)) {
                    createRectifiedBox(reduced, originalThickness)
                } else {
                    createPolygonPath(reduced, originalThickness)
                }
            }
        }

        // Fallback: only allow loose circle if corner count is weird and it's closed
        if (isClosed && (corners.size !in 3..5) && cv < 0.25f) {
            return generateCircleOrOval(rawPoints, centroid, originalThickness)
        }

        return null  // keep original freehand stroke
    }

    // ==================== NEW: ARC (Bögen) ====================
    private fun generateArc(
        points: List<Offset>,
        centroid: Offset,
        radius: Float,
        start: Offset,
        end: Offset,
        thickness: Float
    ): List<PathOffset> {
        val startTheta = atan2((start - centroid).y.toDouble(), (start - centroid).x.toDouble())
        val endTheta = atan2((end - centroid).y.toDouble(), (end - centroid).x.toDouble())

        // Determine drawing direction using second point
        val second = if (points.size >= 3) points[1] else end
        val secondTheta = atan2((second - centroid).y.toDouble(), (second - centroid).x.toDouble())

        val deltaToSecond = normalizeAngleDiff(secondTheta - startTheta)
        val dirSign = if (deltaToSecond >= 0.0) 1.0 else -1.0

        // Full span in the same direction as the user drew
        val rawDelta = endTheta - startTheta
        val delta = if (dirSign > 0) {
            (rawDelta % (2 * PI) + 2 * PI) % (2 * PI)
        } else {
            -(((-rawDelta) % (2 * PI) + 2 * PI) % (2 * PI))
        }

        val list = mutableListOf<PathOffset>()
        val steps = 60
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val theta = startTheta + t * delta
            list.add(
                PathOffset(
                    Offset(
                        (centroid.x + radius * cos(theta)).toFloat(),
                        (centroid.y + radius * sin(theta)).toFloat()
                    ),
                    thickness
                )
            )
        }
        return list
    }

    private fun normalizeAngleDiff(d: Double): Double {
        var diff = d % (2 * PI)
        if (diff > PI) diff -= 2 * PI
        if (diff < -PI) diff += 2 * PI
        return diff
    }

    // ==================== REST OF YOUR FUNCTIONS (mostly unchanged) ====================
    private fun generateCircleOrOval(points: List<Offset>, centroid: Offset, thickness: Float): List<PathOffset> {
        val (minX, maxX, minY, maxY) = getBounds(points)
        val width = maxX - minX
        val height = maxY - minY
        val ratio = if (width > height) width / height else height / width

        val distances = points.map { (it - centroid).getDistance() }
        val avgRadius = distances.average().toFloat()

        return if (ratio < 1.2f) {
            createPerfectCircle(centroid, avgRadius, thickness)
        } else {
            createPerfectOval(minX, maxX, minY, maxY, thickness)
        }
    }

    private fun getCircularityMetrics(points: List<Offset>, centroid: Offset): Pair<Float, Float> {
        val distances = points.map { (it - centroid).getDistance() }
        val avgRadius = distances.average().toFloat()
        var sumSqDiff = 0.0
        for (d in distances) sumSqDiff += (d - avgRadius).pow(2)
        val stdDev = sqrt(sumSqDiff / points.size)
        val cv = stdDev / avgRadius
        return Pair(avgRadius, cv.toFloat())
    }

    private fun findCorners(points: List<Offset>): List<Offset> {
        val resampled = resample(points, 40)
        val potentialCorners = mutableListOf<Int>()
        val n = resampled.size

        for (i in 0 until n) {
            val prev = resampled[(i - 2 + n) % n]
            val curr = resampled[i]
            val next = resampled[(i + 2) % n]

            val angle = calculateAngle(prev, curr, next)

            if (angle < MIN_CORNER_ANGLE) {
                val prevAngle = calculateAngle(
                    resampled[(i - 3 + n) % n],
                    resampled[(i - 1 + n) % n],
                    resampled[(i + 1) % n]
                )
                val nextAngle = calculateAngle(
                    resampled[(i - 1 + n) % n],
                    resampled[(i + 1) % n],
                    resampled[(i + 3) % n]
                )

                if (angle <= prevAngle && angle <= nextAngle) {
                    potentialCorners.add(i)
                }
            }
        }

        if (potentialCorners.isEmpty()) return emptyList()

        val distinctCorners = mutableListOf<Offset>()
        var lastIdx = -999

        for (idx in potentialCorners) {
            if (abs(idx - lastIdx) > 4) {
                distinctCorners.add(resampled[idx])
                lastIdx = idx
            }
        }

        if (distinctCorners.size > 1 && (distinctCorners.first() - distinctCorners.last()).getDistance() < MERGE_CORNER_DIST) {
            distinctCorners.removeAt(distinctCorners.lastIndex)
        }

        return cleanCorners(distinctCorners, MERGE_CORNER_DIST)
    }

    private fun cleanCorners(corners: List<Offset>, minDist: Float): List<Offset> {
        if (corners.isEmpty()) return corners
        val result = mutableListOf<Offset>()
        result.add(corners[0])

        for (i in 1 until corners.size) {
            val last = result.last()
            val curr = corners[i]
            if ((curr - last).getDistance() > minDist) {
                result.add(curr)
            }
        }
        // Double check wrap around again after filtering
        if (result.size > 1 && (result.first() - result.last()).getDistance() < minDist) {
            result.removeAt(result.lastIndex)
        }
        return result
    }

    // --- RECTIFICATION ---

    private fun isRectangular(corners: List<Offset>): Boolean {
        // Check if angles are roughly 90 deg
        // Allow wider tolerance (40 deg) for hand drawings
        val tolerance = 40.0
        for (i in 0..3) {
            val prev = corners[(i - 1 + 4) % 4]
            val curr = corners[i]
            val next = corners[(i + 1) % 4]
            val angle = calculateAngle(prev, curr, next)
            if (abs(angle - 90) > tolerance) return false
        }
        return true
    }

    private fun createRectifiedBox(corners: List<Offset>, thickness: Float): List<PathOffset> {
        val center = calculateCentroid(corners)

        // Calculate widths/heights based on opposite sides
        val d01 = (corners[0] - corners[1]).getDistance()
        val d12 = (corners[1] - corners[2]).getDistance()
        val d23 = (corners[2] - corners[3]).getDistance()
        val d30 = (corners[3] - corners[0]).getDistance()

        var w = (d01 + d23) / 2
        var h = (d12 + d30) / 2

        // Rotation: Angle of longest side
        val angle01 = atan2(corners[1].y - corners[0].y, corners[1].x - corners[0].x)

        // Square Check
        if (w > 0 && h > 0) {
            val ratio = if (w > h) w/h else h/w
            if (ratio < 1.2f) {
                val avg = (w+h)/2
                w = avg
                h = avg
            }
        }

        val hw = w/2
        val hh = h/2

        // Unrotated corners relative to center
        val raw = listOf(
            Offset(-hw, -hh), Offset(hw, -hh), Offset(hw, hh), Offset(-hw, hh)
        )

        val rotated = raw.map { p ->
            val rx = p.x * cos(angle01) - p.y * sin(angle01)
            val ry = p.x * sin(angle01) + p.y * cos(angle01)
            Offset((center.x + rx), (center.y + ry))
        }

        return createPolygonPath(rotated, thickness)
    }

    // --- HELPERS ---

    private fun mergeClosestPoints(points: List<Offset>): List<Offset> {
        if (points.size < 2) return points
        var minD = Float.MAX_VALUE
        var p1 = -1; var p2 = -1
        for (i in 0 until points.size) {
            val j = (i + 1) % points.size
            val d = (points[i] - points[j]).getDistance()
            if (d < minD) { minD = d; p1 = i; p2 = j }
        }
        val newPoints = points.toMutableList()
        val mp = Offset((points[p1].x + points[p2].x) / 2, (points[p1].y + points[p2].y) / 2)
        val max = maxOf(p1,p2); val min = minOf(p1,p2)
        newPoints.removeAt(max); newPoints.removeAt(min)
        newPoints.add(min, mp)
        return newPoints
    }

    // Standard path generators (Circle, Oval, Poly, Line) same as before...
    private fun createLinearPath(start: Offset, end: Offset, thickness: Float): List<PathOffset> {
        val list = mutableListOf<PathOffset>()
        lerpLine(list, start, end, thickness)
        return list
    }
    private fun createPolygonPath(corners: List<Offset>, thickness: Float): List<PathOffset> {
        val list = mutableListOf<PathOffset>()
        for (i in 0 until corners.size) {
            val start = corners[i]; val end = corners[(i+1)%corners.size]
            lerpLine(list, start, end, thickness)
        }
        list.add(PathOffset(corners[0], thickness))
        return list
    }
    private fun createPerfectCircle(center: Offset, radius: Float, thickness: Float): List<PathOffset> {
        val list = mutableListOf<PathOffset>()
        val steps = 60
        for (i in 0..steps) {
            val theta = (i.toFloat()/steps)*2* PI
            list.add(
                PathOffset(
                    Offset(
                        (center.x + radius * cos(theta)).toFloat(),
                        (center.y + radius * sin(theta)).toFloat()
                    ), thickness
                )
            )
        }
        return list
    }
    private fun createPerfectOval(minX: Float, maxX: Float, minY: Float, maxY: Float, thickness: Float): List<PathOffset> {
        val list = mutableListOf<PathOffset>()
        val w = maxX-minX; val h = maxY-minY
        val cx = minX + w/2; val cy = minY + h/2
        val steps = 60
        for (i in 0..steps) {
            val theta = (i.toFloat()/steps)*2* PI
            list.add(
                PathOffset(
                    Offset(
                        (cx + (w / 2) * cos(theta)).toFloat(),
                        (cy + (h / 2) * sin(theta)).toFloat()
                    ), thickness
                )
            )
        }
        return list
    }

    // Math utilities
    private fun calculateCentroid(points: List<Offset>): Offset {
        var sx = 0f; var sy = 0f
        points.forEach { sx += it.x; sy += it.y }
        return Offset(sx / points.size, sy / points.size)
    }
    private fun getBounds(points: List<Offset>): FloatArray {
        var minX=Float.MAX_VALUE; var maxX=Float.MIN_VALUE; var minY=Float.MAX_VALUE; var maxY=Float.MIN_VALUE
        points.forEach { if(it.x<minX)minX=it.x; if(it.x>maxX)maxX=it.x; if(it.y<minY)minY=it.y; if(it.y>maxY)maxY=it.y }
        return floatArrayOf(minX, maxX, minY, maxY)
    }
    private fun calculatePathLength(points: List<Offset>): Float {
        var len = 0f
        for(i in 0 until points.lastIndex) len += (points[i]-points[i+1]).getDistance()
        return len
    }
    private fun calculateAngle(p1: Offset, p2: Offset, p3: Offset): Double {
        val v1x = p1.x - p2.x; val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x; val v2y = p3.y - p2.y
        val dot = v1x*v2x + v1y*v2y
        val det = v1x*v2y - v1y*v2x
        return abs(Math.toDegrees(atan2(det.toDouble(), dot.toDouble())))
    }
    private fun lerpLine(list: MutableList<PathOffset>, start: Offset, end: Offset, thickness: Float) {
        val steps = 15
        for(i in 0 until steps) {
            val t = i.toFloat()/steps
            list.add(
                PathOffset(
                    Offset(
                        start.x + (end.x - start.x) * t,
                        start.y + (end.y - start.y) * t
                    ), thickness
                )
            )
        }
    }
    private fun resample(points: List<Offset>, targetCount: Int): List<Offset> {
        if (points.isEmpty()) return emptyList()
        val result = mutableListOf<Offset>()
        result.add(points.first())
        val totalLen = calculatePathLength(points)
        val step = totalLen / (targetCount - 1)
        var currDist = 0f
        var srcIdx = 0
        val src = points.toMutableList()
        while (srcIdx < src.size - 1) {
            val p1 = src[srcIdx]; val p2 = src[srcIdx+1]
            val d = (p1 - p2).getDistance()
            if (currDist + d >= step) {
                val rem = step - currDist
                val t = rem / d
                val np = Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
                result.add(np)
                src.add(srcIdx + 1, np)
                currDist = 0f
                srcIdx++
            } else { currDist += d; srcIdx++ }
            if (result.size >= targetCount) break
        }
        if (result.size < targetCount) result.add(points.last())
        return result
    }
}