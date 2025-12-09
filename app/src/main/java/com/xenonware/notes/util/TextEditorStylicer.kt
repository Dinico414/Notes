package com.xenonware.notes.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.json.JSONObject

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
    androidx.compose.ui.text.AnnotatedString(this)
}