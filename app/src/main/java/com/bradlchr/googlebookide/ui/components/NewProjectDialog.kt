package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.ui.theme.GoogleGreen
import com.bradlchr.googlebookide.ui.theme.TextSecondary

@Composable
fun NewProjectDialog(
    onCreateProject: (name: String, pkg: String) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf("MyAndroidApp") }
    var packageName by remember { mutableStateOf("com.bradlchr.myandroidapp") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Android, contentDescription = null, tint = GoogleGreen)
                Text("New Android Project")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Scaffolds a complete Jetpack Compose + Material 3 Android app with Gradle 9 & modern SDK 36.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        val clean = it.lowercase().replace(" ", "")
                        packageName = "com.bradlchr.$clean"
                    },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (applicationId)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank() && packageName.isNotBlank()) {
                        onCreateProject(projectName.trim(), packageName.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
