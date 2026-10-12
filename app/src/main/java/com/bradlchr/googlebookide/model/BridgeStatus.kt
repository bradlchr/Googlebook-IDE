package com.bradlchr.googlebookide.model

import kotlinx.serialization.Serializable

@Serializable
data class BridgeStatus(
    val isOnline: Boolean = false,
    val javaVersion: String = "Unknown",
    val gitVersion: String = "Unknown",
    val githubAuth: String = "Not logged in",
    val sdkInstalled: Boolean = false,
    val sdkPlatforms: List<String> = emptyList(),
    val sdkPath: String = "",
    val appsDirectory: String = "",
    val serverTime: String = "",
    val pingLatencyMs: Long = 0L,
    val errorMessage: String? = null
)
