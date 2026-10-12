package com.bradlchr.googlebookide.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

enum class TokenType {
    KEYWORD,
    STRING,
    NUMBER,
    COMMENT,
    ANNOTATION,
    TAG,
    ATTRIBUTE,
    FUNCTION,
    PLAIN
}

data class SyntaxToken(
    val text: String,
    val type: TokenType,
    val start: Int,
    val end: Int
)

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "val", "var", "fun",
        "return", "if", "else", "when", "for", "while", "do", "break", "continue",
        "try", "catch", "finally", "throw", "this", "super", "null", "true", "false",
        "is", "in", "as", "by", "data", "sealed", "open", "override", "private",
        "protected", "public", "internal", "lateinit", "suspend", "inline", "enum",
        "companion", "typealias", "constructor", "init"
    )

    private val GRADLE_KEYWORDS = KOTLIN_KEYWORDS + setOf(
        "plugins", "alias", "apply", "dependencies", "implementation", "testImplementation",
        "androidTestImplementation", "debugImplementation", "android", "defaultConfig",
        "buildTypes", "release", "debug", "compileOptions", "kotlin", "base", "archivesName",
        "compileSdk", "minSdk", "targetSdk", "versionCode", "versionName", "namespace"
    )

    fun tokenize(code: String, extension: String): List<SyntaxToken> {
        val ext = extension.lowercase()
        val tokens = mutableListOf<SyntaxToken>()

        if (ext == "xml") {
            return tokenizeXml(code)
        }

        val keywords = if (ext == "gradle" || ext == "kts") GRADLE_KEYWORDS else KOTLIN_KEYWORDS

        var i = 0
        val len = code.length

        while (i < len) {
            val c = code[i]

            // Comments
            if (c == '/' && i + 1 < len) {
                if (code[i + 1] == '/') {
                    val start = i
                    val end = code.indexOf('\n', start).let { if (it == -1) len else it }
                    tokens.add(SyntaxToken(code.substring(start, end), TokenType.COMMENT, start, end))
                    i = end
                    continue
                } else if (code[i + 1] == '*') {
                    val start = i
                    val endIdx = code.indexOf("*/", start + 2)
                    val end = if (endIdx == -1) len else endIdx + 2
                    tokens.add(SyntaxToken(code.substring(start, end), TokenType.COMMENT, start, end))
                    i = end
                    continue
                }
            }

            // String literals
            if (c == '"') {
                val start = i
                var end = i + 1
                if (i + 2 < len && code[i + 1] == '"' && code[i + 2] == '"') {
                    // Triple quote string
                    val triEnd = code.indexOf("\"\"\"", i + 3)
                    end = if (triEnd == -1) len else triEnd + 3
                } else {
                    while (end < len) {
                        if (code[end] == '\\' && end + 1 < len) {
                            end += 2
                        } else if (code[end] == '"') {
                            end++
                            break
                        } else if (code[end] == '\n') {
                            break
                        } else {
                            end++
                        }
                    }
                }
                tokens.add(SyntaxToken(code.substring(start, end), TokenType.STRING, start, end))
                i = end
                continue
            }

            // Annotations (e.g. @Composable, @Test)
            if (c == '@' && i + 1 < len && (code[i + 1].isLetter() || code[i + 1] == '_')) {
                val start = i
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) {
                    end++
                }
                tokens.add(SyntaxToken(code.substring(start, end), TokenType.ANNOTATION, start, end))
                i = end
                continue
            }

            // Numbers
            if (c.isDigit()) {
                val start = i
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == '.')) {
                    end++
                }
                tokens.add(SyntaxToken(code.substring(start, end), TokenType.NUMBER, start, end))
                i = end
                continue
            }

            // Identifiers & Keywords
            if (c.isLetter() || c == '_') {
                val start = i
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == '_')) {
                    end++
                }
                val word = code.substring(start, end)
                val type = if (word in keywords) TokenType.KEYWORD else TokenType.PLAIN
                tokens.add(SyntaxToken(word, type, start, end))
                i = end
                continue
            }

            // Plain characters
            tokens.add(SyntaxToken(c.toString(), TokenType.PLAIN, i, i + 1))
            i++
        }

        return tokens
    }

    private fun tokenizeXml(code: String): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        var i = 0
        val len = code.length

        while (i < len) {
            val c = code[i]

            // XML Comments <!-- ... -->
            if (code.startsWith("<!--", i)) {
                val start = i
                val endIdx = code.indexOf("-->", i + 4)
                val end = if (endIdx == -1) len else endIdx + 3
                tokens.add(SyntaxToken(code.substring(start, end), TokenType.COMMENT, start, end))
                i = end
                continue
            }

            // XML Strings inside attributes
            if (c == '"') {
                val start = i
                var end = i + 1
                while (end < len && code[end] != '"') {
                    end++
                }
                if (end < len) end++
                tokens.add(SyntaxToken(code.substring(start, end), TokenType.STRING, start, end))
                i = end
                continue
            }

            // XML Tags e.g. <TextView or </activity>
            if (c == '<') {
                tokens.add(SyntaxToken("<", TokenType.PLAIN, i, i + 1))
                i++
                if (i < len && code[i] == '/') {
                    tokens.add(SyntaxToken("/", TokenType.PLAIN, i, i + 1))
                    i++
                }
                if (i < len && (code[i].isLetter() || code[i] == '_')) {
                    val start = i
                    var end = i + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '.' || code[end] == '_')) {
                        end++
                    }
                    tokens.add(SyntaxToken(code.substring(start, end), TokenType.TAG, start, end))
                    i = end
                }
                continue
            }

            if (c.isLetter() || c == '_') {
                val start = i
                var end = i + 1
                while (end < len && (code[end].isLetterOrDigit() || code[end] == ':' || code[end] == '-' || code[end] == '_')) {
                    end++
                }
                val word = code.substring(start, end)
                val type = if (word.contains(":")) TokenType.ATTRIBUTE else TokenType.PLAIN
                tokens.add(SyntaxToken(word, type, start, end))
                i = end
                continue
            }

            tokens.add(SyntaxToken(c.toString(), TokenType.PLAIN, i, i + 1))
            i++
        }
        return tokens
    }

    /**
     * Converts raw source code into an AnnotatedString with syntax coloring.
     */
    fun highlight(code: String, extension: String): AnnotatedString {
        val tokens = tokenize(code, extension)
        return buildAnnotatedString {
            append(code)
            tokens.forEach { token ->
                val style = when (token.type) {
                    TokenType.KEYWORD -> SpanStyle(color = Color(0xFFCBA6F7), fontWeight = FontWeight.Bold) // Mauve
                    TokenType.STRING -> SpanStyle(color = Color(0xFFA6E3A1)) // Green
                    TokenType.NUMBER -> SpanStyle(color = Color(0xFFFAB387)) // Peach
                    TokenType.COMMENT -> SpanStyle(color = Color(0xFF6C7086)) // Overlay gray
                    TokenType.ANNOTATION -> SpanStyle(color = Color(0xFFF9E2AF), fontWeight = FontWeight.SemiBold) // Yellow
                    TokenType.TAG -> SpanStyle(color = Color(0xFF89B4FA), fontWeight = FontWeight.Bold) // Blue
                    TokenType.ATTRIBUTE -> SpanStyle(color = Color(0xFF89DCEB)) // Sky
                    TokenType.FUNCTION -> SpanStyle(color = Color(0xFF89B4FA)) // Blue
                    TokenType.PLAIN -> SpanStyle(color = Color(0xFFCDD6F4)) // Text
                }
                addStyle(style, token.start, token.end)
            }
        }
    }
}
