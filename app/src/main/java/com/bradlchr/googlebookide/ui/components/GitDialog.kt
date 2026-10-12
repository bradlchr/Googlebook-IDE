package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun GitDialog(
    branch: String,
    statusText: String,
    onCommitAndPush: (message: String) -> Unit,
    onDismiss: () -> Unit
) {
    var commitMessage by remember { mutableStateOf("Update Android project via Googlebook IDE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ForkRight, contentDescription = null, tint = GoogleYellow)
                Text("Git Version Control")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Branch: ", fontSize = 13.sp, color = TextSecondary)
                    Text(branch, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoogleBlue)
                }

                Text("Changed Files:", fontSize = 12.sp, color = TextSecondary)
                Surface(
                    color = TerminalBackground,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Text(
                        text = statusText.ifEmpty { "Working tree is clean." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = GoogleGreen,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commitMessage.isNotBlank()) {
                        onCommitAndPush(commitMessage.trim())
                        onDismiss()
                    }
                }
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Commit & Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
