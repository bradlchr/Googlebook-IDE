package com.bradlchr.googlebookide

import com.bradlchr.googlebookide.antigravity.SlashCommand
import com.bradlchr.googlebookide.antigravity.SlashCommandHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlashCommandHandlerTest {

    @Test
    fun testParsePlanCommand() {
        val input = "/plan Add Room database storage"
        val parsed = SlashCommandHandler.parse(input)

        assertEquals(SlashCommand.PLAN, parsed.command)
        assertEquals("Add Room database storage", parsed.argument)
    }

    @Test
    fun testParseBuildCommand() {
        val input = "/build"
        val parsed = SlashCommandHandler.parse(input)

        assertEquals(SlashCommand.BUILD, parsed.command)
        assertEquals("", parsed.argument)
    }

    @Test
    fun testParseUnknownOrPlainChat() {
        val input = "How do I fix Compose recomposition?"
        val parsed = SlashCommandHandler.parse(input)

        assertEquals(SlashCommand.CHAT, parsed.command)
        assertEquals("How do I fix Compose recomposition?", parsed.argument)
    }
}
