package com.xenonware.notes.viewmodel.classes

import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val id: String = "",
    val text: String = ""
)