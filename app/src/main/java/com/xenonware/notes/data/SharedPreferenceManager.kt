package com.xenonware.notes.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferenceManager(context: Context) {
    private val prefsName = "NotesMain"
    private val lineSmoothnessKey = "smoothness"

    internal val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var smoothness: Float
        get() = sharedPreferences.getFloat(lineSmoothnessKey, 0.1f)
        set(value) = sharedPreferences.edit { putFloat(lineSmoothnessKey, value) }
}