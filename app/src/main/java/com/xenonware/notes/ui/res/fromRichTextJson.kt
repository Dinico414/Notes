package com.xenonware.notes.ui.res

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.json.JSONArray
import org.json.JSONObject

// ---------- SERIALIZATION ----------
fun AnnotatedString.toRichTextJson(): String {
    val json = JSONObject()
    json.put("text", this.text)

    val spans = JSONArray()
    this.spanStyles.forEach { range ->
        val spanJson = JSONObject()
        spanJson.put("start", range.start)
        spanJson.put("end", range.end)

        val style = range.item
        if (style.fontWeight == FontWeight.Bold) spanJson.put("bold", true)
        if (style.fontStyle == FontStyle.Italic) spanJson.put("italic", true)
        if (style.textDecoration?.contains(TextDecoration.Underline) == true)
            spanJson.put("underline", true)

        if (spanJson.length() > 2) spans.put(spanJson)
    }
    json.put("spans", spans)
    return json.toString()
}

fun String.fromRichTextJson(): AnnotatedString = try {
    val json = JSONObject(this)
    val text = json.getString("text")
    val spans = json.getJSONArray("spans")

    buildAnnotatedString {
        append(text)
        for (i in 0 until spans.length()) {
            val s = spans.getJSONObject(i)
            val start = s.getInt("start")
            val end = s.getInt("end")
            val style = SpanStyle(
                fontWeight = if (s.optBoolean("bold")) FontWeight.Bold else null,
                fontStyle = if (s.optBoolean("italic")) FontStyle.Italic else null,
                textDecoration = if (s.optBoolean("underline")) TextDecoration.Underline else null
            )
            addStyle(style, start, end)
        }
    }
} catch (e: Exception) {
    AnnotatedString(this) // fallback if corrupted
}