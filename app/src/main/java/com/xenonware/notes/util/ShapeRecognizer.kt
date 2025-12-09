package com.xenonware.notes.util

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

    // --- CONFIGURATION ---
    private const val LINE_LINEARITY = 0.96f       // Threshold for straight lines
    private const val CIRCLE_VARIANCE = 0.20f      // Increased to allow messier circles
    private const val MIN_CORNER_ANGLE = 160.0     // Degrees. Angles less than this are corners.
    private const val MERGE_CORNER_DIST = 30f      // Reduced from 40f to stop Rect->Tri merging

    enum class ShapeType { CIRCLE, OVAL, TRIANGLE, QUAD, LINE, RECT }

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

        // 1. LINE CHECK (Open Shape)
        // If endpoints are far apart relative to path length, it's a line.
        if (directDist > totalLength * 0.92f) {
            return createLinearPath(start, end, originalThickness)
        }

        // 2. ANALYZE GEOMETRY
        val centroid = calculateCentroid(rawPoints)

        // Detect Corners
        val corners = findCorners(rawPoints)

        // 3. DECISION TREE

        // CASE A: It looks like a Triangle (3 corners)
        if (corners.size == 3) {
            // Triangles have straight sides. Circles have bulging sides.
            if (areSidesBulging(corners, rawPoints)) {
                // It's a circle/oval disguised as a triangle
                return generateCircleOrOval(rawPoints, centroid, originalThickness)
            }
            return createPolygonPath(corners, originalThickness)
        }

        // CASE B: It looks like a Quad (4 corners)
        if (corners.size == 4) {
            if (isRectangular(corners)) {
                return createRectifiedBox(corners, originalThickness)
            }
            return createPolygonPath(corners, originalThickness)
        }

        // CASE C: 5 Corners (Often a messy square)
        if (corners.size == 5) {
            val reduced = mergeClosestPoints(corners)
            if (reduced.size == 4) {
                if (isRectangular(reduced)) return createRectifiedBox(reduced, originalThickness)
                return createPolygonPath(reduced, originalThickness)
            }
        }

        // CASE D: Fallback to Circle/Oval analysis
        // If we didn't find distinct corners, OR if the corner check failed, check variance.
        val (isCircle, avgRadius) = analyzeCircularity(rawPoints, centroid)

        // If variance is low, it's a circle. 
        // Note: We accept higher variance if corner count was 0 or >5 (messy blob)
        if (isCircle || corners.size < 3 || corners.size > 5) {
            return generateCircleOrOval(rawPoints, centroid, originalThickness)
        }

        return null
    }

    // --- ANALYSIS ---

    /**
     * Checks if the path segments connecting corners bulge significantly.
     * Returns TRUE if it's likely a circle, FALSE if it's a triangle.
     */
    private fun areSidesBulging(corners: List<Offset>, allPoints: List<Offset>): Boolean {
        var maxBulge = 0f
        val threshold = 40f // Pixels of deviation allowed for a straight side

        val center = calculateCentroid(allPoints)
        val (_, cv) = getCircularityMetrics(allPoints, center)

        // A Triangle usually has CV > 0.22. A Circle usually has CV < 0.18.
        // If the CV is low, it's a Circle, even if we found 3 corners.
        return cv < 0.18f
    }

    private fun generateCircleOrOval(points: List<Offset>, centroid: Offset, thickness: Float): List<PathOffset> {
        val (minX, maxX, minY, maxY) = getBounds(points)
        val width = maxX - minX
        val height = maxY - minY

        // Distinguish Circle vs Oval
        val ratio = if (width > height) width / height else height / width

        // Calculate average radius for Circle
        val distances = points.map { (it - centroid).getDistance() }
        val avgRadius = distances.average().toFloat()

        return if (ratio < 1.2f) {
            createPerfectCircle(centroid, avgRadius, thickness)
        } else {
            createPerfectOval(minX, maxX, minY, maxY, thickness)
        }
    }

    private fun analyzeCircularity(points: List<Offset>, centroid: Offset): Pair<Boolean, Float> {
        val (avgRadius, cv) = getCircularityMetrics(points, centroid)
        return Pair(cv < CIRCLE_VARIANCE, avgRadius)
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
        // 1. Resample to ~40 points to normalize data
        val resampled = resample(points, 40)
        val potentialCorners = mutableListOf<Int>()
        val n = resampled.size

        // 2. Find Local Minima in Angle (Sharpest turns)
        // We use a sliding window.
        for (i in 0 until n) {
            val prev = resampled[(i - 2 + n) % n]
            val curr = resampled[i]
            val next = resampled[(i + 2) % n]

            val angle = calculateAngle(prev, curr, next)

            // If angle is sharp enough to be a corner
            if (angle < MIN_CORNER_ANGLE) {
                // Check if it is a local minimum (sharpest point in neighborhood)
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

                // Loose local extrema check
                if (angle <= prevAngle && angle <= nextAngle) {
                    potentialCorners.add(i)
                }
            }
        }

        // 3. Cluster and Clean
        if (potentialCorners.isEmpty()) return emptyList()

        val distinctCorners = mutableListOf<Offset>()
        var lastIdx = -999

        for (idx in potentialCorners) {
            // If this index is far from the last one, it's a new corner
            // Indices wrap around, so simple diff is okay for sorted list except for start/end loop
            if (abs(idx - lastIdx) > 4) {
                distinctCorners.add(resampled[idx])
                lastIdx = idx
            }
        }

        // 4. Merge First/Last if they are the same corner (Loop wrap)
        if (distinctCorners.size > 1) {
            val d = (distinctCorners.first() - distinctCorners.last()).getDistance()
            if (d < MERGE_CORNER_DIST) {
                distinctCorners.removeAt(distinctCorners.lastIndex)
            }
        }

        // 5. Distance Check for internal corners
        // (Fixes Rectangles becoming Triangles by not merging incorrectly)
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
            Offset((center.x + rx).toFloat(), (center.y + ry).toFloat())
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
        val mp = Offset((points[p1].x + points[p2].x)/2, (points[p1].y + points[p2].y)/2)
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
            val theta = (i.toFloat()/steps)*2*PI
            list.add(PathOffset(Offset((center.x + radius*cos(theta)).toFloat(), (center.y + radius*sin(theta)).toFloat()), thickness))
        }
        return list
    }
    private fun createPerfectOval(minX: Float, maxX: Float, minY: Float, maxY: Float, thickness: Float): List<PathOffset> {
        val list = mutableListOf<PathOffset>()
        val w = maxX-minX; val h = maxY-minY
        val cx = minX + w/2; val cy = minY + h/2
        val steps = 60
        for (i in 0..steps) {
            val theta = (i.toFloat()/steps)*2*PI
            list.add(PathOffset(Offset((cx + (w/2)*cos(theta)).toFloat(), (cy + (h/2)*sin(theta)).toFloat()), thickness))
        }
        return list
    }

    // Math utilities
    private fun calculateCentroid(points: List<Offset>): Offset {
        var sx = 0f; var sy = 0f
        points.forEach { sx += it.x; sy += it.y }
        return Offset(sx/points.size, sy/points.size)
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
            list.add(PathOffset(Offset(start.x + (end.x-start.x)*t, start.y + (end.y-start.y)*t), thickness))
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
