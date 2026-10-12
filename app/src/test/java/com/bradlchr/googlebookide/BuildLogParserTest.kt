package com.bradlchr.googlebookide

import com.bradlchr.googlebookide.compiler.BuildLogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildLogParserTest {

    @Test
    fun testStripAnsiCodes() {
        val raw = "\u001B[32mBUILD SUCCESSFUL\u001B[0m in 12s"
        val clean = BuildLogParser.stripAnsi(raw)
        assertEquals("BUILD SUCCESSFUL in 12s", clean)
    }

    @Test
    fun testExtractCompilationErrors() {
        val log = """
            > Task :app:compileDebugKotlin
            e: /storage/emulated/10/Documents/Antigravity Apps/Tebri/app/src/main/java/Main.kt: (42, 15): Unresolved reference: foo
            e: /storage/emulated/10/Documents/Antigravity Apps/Tebri/app/src/main/java/Utils.kt: (10, 8): Type mismatch: inferred type is Int but String was expected
            
            FAILURE: Build failed with an exception.
        """.trimIndent()

        val errors = BuildLogParser.extractErrors(log)
        assertEquals(2, errors.size)
        assertEquals("Main.kt", errors[0].fileName)
        assertEquals(42, errors[0].line)
        assertEquals("Unresolved reference: foo", errors[0].message)

        assertEquals("Utils.kt", errors[1].fileName)
        assertEquals(10, errors[1].line)
        assertEquals("Type mismatch: inferred type is Int but String was expected", errors[1].message)
    }

    @Test
    fun testIsSuccessOrFailure() {
        val successLog = "BUILD SUCCESSFUL in 4s\n45 actionable tasks: 45 executed"
        val failLog = "FAILURE: Build failed with an exception.\n* What went wrong:\nExecution failed for task ':app:compileDebugKotlin'."

        assertTrue(BuildLogParser.isSuccessful(successLog))
        assertTrue(BuildLogParser.isFailed(failLog))
    }
}
