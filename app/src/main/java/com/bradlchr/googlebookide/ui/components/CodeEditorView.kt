package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.editor.SyntaxHighlighter
import com.bradlchr.googlebookide.ui.EditorTab
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun CodeEditorView(
    openTabs: List<EditorTab>,
    activeTabPath: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTab = openTabs.find { it.path == activeTabPath }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Tab Bar
        if (openTabs.isNotEmpty()) {
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(openTabs) { tab ->
                        val isActive = tab.path == activeTabPath
                        EditorTabItem(
                            tab = tab,
                            isActive = isActive,
                            onClick = { onSelectTab(tab.path) },
                            onClose = { onCloseTab(tab.path) }
                        )
                    }
                }
            }
        }

        if (activeTab == null) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No file open",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a file from the Project Explorer or create a new one.",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            // Code Editor Toolbar
            Surface(
                color = DarkSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quick Snippets
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val snippets = listOf(
                            "@Composable",
                            "remember { mutableStateOf() }",
                            "Button",
                            "Column",
                            "Row",
                            "Card",
                            "Text"
                        )
                        items(snippets) { snippet ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurface,
                                modifier = Modifier.clickable {
                                    val insertion = when (snippet) {
                                        "@Composable" -> "\n@Composable\nfun NewComponent() {\n    // TODO\n}\n"
                                        "remember { mutableStateOf() }" -> "var state by remember { mutableStateOf(\"\") }"
                                        "Button" -> "Button(onClick = { /* action */ }) {\n    Text(\"Button\")\n}"
                                        "Column" -> "Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {\n    \n}"
                                        "Row" -> "Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n    \n}"
                                        "Card" -> "Card(modifier = Modifier.fillMaxWidth()) {\n    \n}"
                                        "Text" -> "Text(\"Hello\")"
                                        else -> snippet
                                    }
                                    onContentChange(activeTab.content + "\n" + insertion)
                                }
                            ) {
                                Text(
                                    text = snippet,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Mauve,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Save Button
                    Button(
                        onClick = onSaveFile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab.isDirty) GoogleBlue else DarkSurface,
                            contentColor = if (activeTab.isDirty) DarkBackground else TextSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeTab.isDirty) "Save *" else "Saved",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Editor Lines & Text Area
            val scrollState = rememberScrollState()
            val lines = remember(activeTab.content) { activeTab.content.lines() }
            val lineCount = lines.size.coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Line Numbers Gutter
                Column(
                    modifier = Modifier
                        .background(CodeGutter)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .widthIn(min = 36.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Main Code Text Field
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    val horizontalScroll = rememberScrollState()
                    Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                        BasicTextField(
                            value = activeTab.content,
                            onValueChange = onContentChange,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTabItem(
    tab: EditorTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = if (isActive) TabActive else TabInactive,
        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
        modifier = Modifier
            .clickable { onClick() }
            .padding(end = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tint = when (tab.extension) {
                "kt" -> Mauve
                "xml" -> Sky
                "gradle", "kts" -> GoogleGreen
                else -> TextSecondary
            }

            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = tab.name + if (tab.isDirty) " *" else "",
                fontSize = 12.sp,
                color = if (isActive) TextPrimary else TextSecondary,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
