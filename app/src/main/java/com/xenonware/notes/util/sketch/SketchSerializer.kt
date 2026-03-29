package com.xenonware.notes.util.sketch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.xenonware.notes.viewmodel.PathData
import com.xenonware.notes.viewmodel.PathOffset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SerializablePathOffset(
    val offset: SerializableOffset,
    val thickness: Float,
    val cp1: SerializableOffset,
    val cp2: SerializableOffset
)

@Serializable
data class SerializableOffset(val x: Float, val y: Float)

@Serializable
data class SerializablePathData(
    val id: String,
    val colorValue: ULong,
    val colorIndex: Int,
    val pathPoints: List<SerializablePathOffset>,
    val isShape: Boolean = false,
    val fillColorValue: ULong = Color.Transparent.value
)

object SketchSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    fun serializePaths(paths: List<PathData>): String {
        val serializablePaths = paths.map { pd ->
            SerializablePathData(
                id = pd.id,
                colorValue = pd.color.value,
                colorIndex = pd.colorIndex,
                pathPoints = pd.path.map { po ->
                    SerializablePathOffset(
                        offset = SerializableOffset(po.offset.x, po.offset.y),
                        thickness = po.thickness,
                        cp1 = SerializableOffset(po.controlPoint1.x, po.controlPoint1.y),
                        cp2 = SerializableOffset(po.controlPoint2.x, po.controlPoint2.y)
                    )
                },
                isShape = pd.isShape,
                fillColorValue = pd.fillColor.value
            )
        }
        return json.encodeToString(serializablePaths)
    }

    fun deserializePaths(jsonString: String?): List<PathData> {
        if (jsonString.isNullOrBlank()) return emptyList()
        return try {
            val serializablePaths = json.decodeFromString<List<SerializablePathData>>(jsonString)
            serializablePaths.map { spd ->
                PathData(
                    id = spd.id,
                    color = Color(spd.colorValue),
                    colorIndex = spd.colorIndex,
                    path = spd.pathPoints.map { spo ->
                        val po = PathOffset(
                            offset = Offset(spo.offset.x, spo.offset.y), thickness = spo.thickness
                        )
                        po.controlPoint1 = Offset(spo.cp1.x, spo.cp1.y)
                        po.controlPoint2 = Offset(spo.cp2.x, spo.cp2.y)
                        po
                    },
                    isShape = spd.isShape,
                    fillColor = Color(spd.fillColorValue)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
