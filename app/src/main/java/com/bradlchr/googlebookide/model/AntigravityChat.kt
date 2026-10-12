package com.bradlchr.googlebookide.model

import kotlinx.serialization.Serializable

@Serializable
data class AntigravityMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String? = null,
    val task: String? = null,
    val suggestedCode: String? = null,
    val messageType: String = "chat"
)
