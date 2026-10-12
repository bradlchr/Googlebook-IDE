package com.bradlchr.googlebookide.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectInfo(
    val name: String,
    val path: String,
    val isAndroid: Boolean = true,
    val hasGit: Boolean = false,
    val apks: List<String> = emptyList(),
    val lastModified: Long = 0L
)
