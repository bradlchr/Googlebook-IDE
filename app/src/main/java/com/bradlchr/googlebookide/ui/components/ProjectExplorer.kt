package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.model.FileItem
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun ProjectExplorer(
    currentDir: String,
    items: List<FileItem>,
    onOpenFile: (FileItem) -> Unit,
    onRefresh: () -> Unit,
    onCreateFile: (name: String, isDir: Boolean) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DarkSurface)
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PROJECT EXPLORER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Navigate Up
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Up",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // New File
                IconButton(
                    onClick = {
                        isCreatingFolder = false
                        newName = ""
                        showCreateDialog = true
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = "New File",
                        tint = GoogleBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // New Folder
                IconButton(
                    onClick = {
                        isCreatingFolder = true
                        newName = ""
                        showCreateDialog = true
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = GoogleYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Refresh
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(bottom = 4.dp))

        // File List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items) { item ->
                FileRowItem(
                    item = item,
                    onClick = { onOpenFile(item) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(if (isCreatingFolder) "Create New Directory" else "Create New File")
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(if (isCreatingFolder) "Folder Name" else "File Name (e.g. MyScreen.kt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateFile(newName.trim(), isCreatingFolder)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileRowItem(
    item: FileItem,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (icon, tint) = when {
                item.isDirectory -> Pair(Icons.Default.Folder, GoogleYellow)
                item.extension in listOf("kt", "java") -> Pair(Icons.Default.Code, Mauve)
                item.extension == "xml" -> Pair(Icons.Default.DataObject, Sky)
                item.extension in listOf("gradle", "kts") -> Pair(Icons.Default.Build, GoogleGreen)
                item.extension in listOf("json", "toml", "properties") -> Pair(Icons.Default.Settings, GoogleYellow)
                item.extension in listOf("md", "txt") -> Pair(Icons.Default.Description, GoogleBlue)
                else -> Pair(Icons.Default.InsertDriveFile, TextSecondary)
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = item.name,
                fontSize = 13.sp,
                color = if (item.isDirectory) TextPrimary else TextSecondary,
                fontWeight = if (item.isDirectory) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
