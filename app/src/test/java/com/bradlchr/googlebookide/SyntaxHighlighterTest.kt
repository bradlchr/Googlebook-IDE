package com.bradlchr.googlebookide

import com.bradlchr.googlebookide.editor.SyntaxHighlighter
import com.bradlchr.googlebookide.editor.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun testKotlinKeywordTokenization() {
        val code = "val name: String = \"Googlebook\""
        val tokens = SyntaxHighlighter.tokenize(code, "kt")

        val valToken = tokens.find { it.text == "val" }
        assertTrue("val should be detected as KEYWORD", valToken != null && valToken.type == TokenType.KEYWORD)

        val stringToken = tokens.find { it.text == "\"Googlebook\"" }
        assertTrue("string should be detected as STRING", stringToken != null && stringToken.type == TokenType.STRING)
    }

    @Test
    fun testKotlinAnnotationTokenization() {
        val code = "@Composable fun App() {}"
        val tokens = SyntaxHighlighter.tokenize(code, "kt")

        val annotToken = tokens.find { it.text == "@Composable" }
        assertTrue("@Composable should be detected as ANNOTATION", annotToken != null && annotToken.type == TokenType.ANNOTATION)
    }

    @Test
    fun testXmlTagTokenization() {
        val xml = "<TextView android:text=\"Hello\" />"
        val tokens = SyntaxHighlighter.tokenize(xml, "xml")

        val tagToken = tokens.find { it.text == "TextView" }
        assertTrue("TextView should be detected as TAG", tagToken != null && tagToken.type == TokenType.TAG)
    }
}
