package com.bradlchr.googlebookide.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val extension: String = "",
    val children: List<FileItem> = emptyList(),
    val isExpanded: Boolean = false
)
