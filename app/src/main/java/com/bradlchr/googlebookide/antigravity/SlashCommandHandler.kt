package com.bradlchr.googlebookide.antigravity

enum class SlashCommand {
    PLAN,
    GOAL,
    SCHEDULE,
    BUILD,
    TEST,
    HELP,
    CHAT
}

data class ParsedCommand(
    val command: SlashCommand,
    val argument: String
)

object SlashCommandHandler {

    fun parse(input: String): ParsedCommand {
        val trimmed = input.trim()
        if (!trimmed.startsWith("/")) {
            return ParsedCommand(SlashCommand.CHAT, trimmed)
        }

        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val cmdStr = parts[0].lowercase()
        val arg = if (parts.size > 1) parts[1].trim() else ""

        val command = when (cmdStr) {
            "/plan" -> SlashCommand.PLAN
            "/goal" -> SlashCommand.GOAL
            "/schedule" -> SlashCommand.SCHEDULE
            "/build" -> SlashCommand.BUILD
            "/test" -> SlashCommand.TEST
            "/help" -> SlashCommand.HELP
            else -> SlashCommand.CHAT
        }

        return ParsedCommand(command, if (command == SlashCommand.CHAT) trimmed else arg)
    }
}
