package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.model.BuildJob
import com.bradlchr.googlebookide.model.BuildStatus
import com.bradlchr.googlebookide.model.CompilationError
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun BottomTerminalPanel(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    buildJob: BuildJob,
    terminalOutput: String,
    compilationErrors: List<CompilationError>,
    onRunTerminalCommand: (String) -> Unit,
    onErrorClick: (CompilationError) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(TerminalBackground)
    ) {
        // Tab Header
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PanelTabItem(
                        title = "Build Output",
                        badge = when (buildJob.status) {
                            BuildStatus.RUNNING -> "●"
                            BuildStatus.SUCCESS -> "✓"
                            BuildStatus.FAILED -> "✗"
                            else -> null
                        },
                        badgeColor = when (buildJob.status) {
                            BuildStatus.RUNNING -> GoogleYellow
                            BuildStatus.SUCCESS -> GoogleGreen
                            BuildStatus.FAILED -> GoogleRed
                            else -> TextSecondary
                        },
                        isSelected = selectedTab == 0,
                        onClick = { onTabSelected(0) }
                    )

                    PanelTabItem(
                        title = "Terminal",
                        isSelected = selectedTab == 1,
                        onClick = { onTabSelected(1) }
                    )

                    PanelTabItem(
                        title = "Problems",
                        badge = if (compilationErrors.isNotEmpty()) "${compilationErrors.size}" else null,
                        badgeColor = GoogleRed,
                        isSelected = selectedTab == 2,
                        onClick = { onTabSelected(2) }
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Build Logs
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                    ) {
                        Text(
                            text = buildJob.logs.ifEmpty { "No active build. Click 'Build APK' above to compile with Gradle." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = when (buildJob.status) {
                                BuildStatus.SUCCESS -> GoogleGreen
                                BuildStatus.FAILED, BuildStatus.ERROR -> GoogleRed
                                else -> TextPrimary
                            },
                            lineHeight = 18.sp
                        )
                    }
                }
                1 -> {
                    // Terminal Runner
                    Column(modifier = Modifier.fillMaxSize()) {
                        val scroll = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scroll)
                        ) {
                            Text(
                                text = terminalOutput,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = GoogleGreen,
                                lineHeight = 18.sp
                            )
                        }

                        // Terminal Input Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$ ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GoogleBlue,
                                fontSize = 13.sp
                            )
                            TextField(
                                value = commandInput,
                                onValueChange = { commandInput = it },
                                placeholder = { Text("Run bash command (e.g. ./gradlew tasks, git status)...", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (commandInput.isNotBlank()) {
                                        onRunTerminalCommand(commandInput.trim())
                                        commandInput = ""
                                    }
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (commandInput.isNotBlank()) {
                                        onRunTerminalCommand(commandInput.trim())
                                        commandInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Run", tint = GoogleBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                2 -> {
                    // Problems / Compilation Errors
                    if (compilationErrors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No problems detected in the project.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(compilationErrors) { err ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { onErrorClick(err) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = GoogleRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${err.fileName} (Line ${err.line}, Col ${err.column})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = GoogleRed
                                            )
                                            Text(
                                                text = err.message,
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PanelTabItem(
    title: String,
    badge: String? = null,
    badgeColor: Color = GoogleBlue,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) TabActive else Color.Transparent,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            if (badge != null) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
