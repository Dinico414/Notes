package com.xenonware.notes.util

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

fun calculateBezierControlPoints(
    p1: Offset, p2: Offset, p3: Offset, p4: Offset,
    smoothness: Float = 0.1f
): Pair<Offset, Offset> {
    // Line goes: p1---p2---p3---p4
    // Calculates cubic bezier control points for p2 and p3

    val m13 = (p1.y - p3.y) / (p1.x - p3.x)
    val m24 = (p2.y - p4.y) / (p2.x - p4.x)
    val m12 = (p1.y - p2.y) / (p1.x - p2.x)
    val m34 = (p3.y - p4.y) / (p3.x - p4.x)

    // Calculate intersection of p1p3 with p1p2
    val control1 = if (abs(m13 - m12) < 0.001) p2 else {
        val t13 = p2.y - m13 * p2.x
        val center1X = smoothness * p3.x + (1 - smoothness) * p2.x
        val center1Y = smoothness * p3.y + (1 - smoothness) * p2.y
        val t12 = center1Y - m12 * center1X
        val intersectX1 = (t12 - t13) / (m13 - m12)
        val intersectY1 = m13 * intersectX1 + t13
        Offset(intersectX1, intersectY1)
    }

    // Calculate intersection of p2p4 with p3p4
    val control2 = if (abs(m24 - m34) < 0.001) p3 else {
        val t24 = p3.y - m24 * p3.x
        val center2X = (1 - smoothness) * p3.x + smoothness * p2.x
        val center2Y = (1 - smoothness) * p3.y + smoothness * p2.y
        val t34 = center2Y - m34 * center2X // Corrected this line
        val intersectX2 = (t34 - t24) / (m24 - m34)
        val intersectY2 = m24 * intersectX2 + t24
        Offset(intersectX2, intersectY2)
    }

    return Pair(control1, control2)
}