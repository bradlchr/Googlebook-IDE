package com.bradlchr.googlebookide.model

import kotlinx.serialization.Serializable

@Serializable
enum class BuildStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
    ERROR
}

@Serializable
data class BuildJob(
    val id: String,
    val projectPath: String,
    val task: String,
    val status: BuildStatus = BuildStatus.IDLE,
    val logs: String = "",
    val exitCode: Int? = null,
    val apkPath: String? = null,
    val durationSeconds: Double = 0.0
)

data class CompilationError(
    val fileName: String,
    val line: Int,
    val column: Int = 0,
    val message: String
)
