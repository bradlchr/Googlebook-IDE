package com.bradlchr.googlebookide.compiler

import com.bradlchr.googlebookide.model.CompilationError
import java.io.File

object BuildLogParser {

    private val ANSI_REGEX = Regex("\u001B\\[[;\\d]*m")
    // e.g. e: /path/to/Main.kt: (42, 15): Unresolved reference: foo
    // or e: /path/to/Main.kt:42:15: Unresolved reference: foo
    private val ERROR_REGEX = Regex("""e:\s+(.+?):\s*(?:\((\d+),\s*(\d+)\)|(\d+):(\d+)):\s*(.+)""")

    fun stripAnsi(text: String): String {
        return ANSI_REGEX.replace(text, "")
    }

    fun isSuccessful(log: String): Boolean {
        return log.contains("BUILD SUCCESSFUL")
    }

    fun isFailed(log: String): Boolean {
        return log.contains("BUILD FAILED") || log.contains("FAILURE: Build failed")
    }

    fun extractErrors(log: String): List<CompilationError> {
        val clean = stripAnsi(log)
        val errors = mutableListOf<CompilationError>()

        clean.lineSequence().forEach { line ->
            val match = ERROR_REGEX.find(line.trim())
            if (match != null) {
                val fullPath = match.groupValues[1]
                val fileName = File(fullPath).name
                val lineNum = match.groupValues[2].ifEmpty { match.groupValues[4] }.toIntOrNull() ?: 1
                val colNum = match.groupValues[3].ifEmpty { match.groupValues[5] }.toIntOrNull() ?: 1
                val message = match.groupValues[6].trim()

                errors.add(CompilationError(
                    fileName = fileName,
                    line = lineNum,
                    column = colNum,
                    message = message
                ))
            }
        }
        return errors
    }
}
