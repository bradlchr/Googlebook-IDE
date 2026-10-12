package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.model.BridgeStatus
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun BridgeSettingsDialog(
    bridgeStatus: BridgeStatus,
    bridgeUrl: String,
    onUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onDismiss: () -> Unit
) {
    var urlText by remember { mutableStateOf(bridgeUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Cable, contentDescription = null, tint = GoogleBlue)
                Text("VM Bridge Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "The local bridge connects Googlebook IDE on Android to the Linux VM to execute Gradle, SDK builds, and Antigravity CLI.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        onUrlChange(it)
                    },
                    label = { Text("Bridge Daemon URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Card
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Daemon Status: ", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = if (bridgeStatus.isOnline) "ONLINE" else "OFFLINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bridgeStatus.isOnline) GoogleGreen else GoogleRed
                            )
                        }
                        if (bridgeStatus.isOnline) {
                            Text("Java: ${bridgeStatus.javaVersion}", fontSize = 11.sp, color = TextPrimary)
                            Text("SDK: ${bridgeStatus.sdkPath}", fontSize = 11.sp, color = TextPrimary)
                            Text("Platforms: ${bridgeStatus.sdkPlatforms.joinToString()}", fontSize = 11.sp, color = TextPrimary)
                            Text("GitHub: ${bridgeStatus.githubAuth}", fontSize = 11.sp, color = TextPrimary)
                            Text("Ping: ${bridgeStatus.pingLatencyMs} ms", fontSize = 11.sp, color = GoogleGreen)
                        } else {
                            Text(
                                text = "To launch the bridge daemon, open Android Terminal and run:\n./start_bridge.sh",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = GoogleYellow
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onTestConnection) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Connection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
