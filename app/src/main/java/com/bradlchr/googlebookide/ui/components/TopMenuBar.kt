package com.bradlchr.googlebookide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bradlchr.googlebookide.model.BuildStatus
import com.bradlchr.googlebookide.model.ProjectInfo
import com.bradlchr.googlebookide.ui.ActiveSidebar
import com.bradlchr.googlebookide.ui.IdeUiState
import com.bradlchr.googlebookide.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopMenuBar(
    uiState: IdeUiState,
    onSelectProject: (ProjectInfo) -> Unit,
    onNewProjectClick: () -> Unit,
    onTriggerBuild: (task: String, clean: Boolean) -> Unit,
    onDeployClick: () -> Unit,
    onToggleSidebar: (ActiveSidebar) -> Unit,
    onBridgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var buildMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = DarkSurface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: App Brand & Project Dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // IDE Logo Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoogleBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "IDE Logo",
                        tint = GoogleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Googlebook IDE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = DarkBorder
                )

                // Project Selector
                Box {
                    Button(
                        onClick = { projectMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = GoogleYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.selectedProject?.name ?: "Select Project",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = projectMenuExpanded,
                        onDismissRequest = { projectMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        uiState.projects.forEach { proj ->
                            DropdownMenuItem(
                                text = { Text(proj.name, color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = if (proj.isAndroid) GoogleGreen else TextSecondary
                                    )
                                },
                                onClick = {
                                    onSelectProject(proj)
                                    projectMenuExpanded = false
                                }
                            )
                        }
                        HorizontalDivider(color = DarkBorder)
                        DropdownMenuItem(
                            text = { Text("New Android Project...", color = GoogleBlue, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, tint = GoogleBlue)
                            },
                            onClick = {
                                onNewProjectClick()
                                projectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Center: Build & Deploy Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Build Button & Dropdown
                Box {
                    Button(
                        onClick = { onTriggerBuild("assembleDebug", false) },
                        enabled = !uiState.isBuilding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoogleGreen.copy(alpha = 0.2f),
                            contentColor = GoogleGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (uiState.isBuilding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = GoogleGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Build",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isBuilding) "Building..." else "Build APK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { buildMenuExpanded = true },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = buildMenuExpanded,
                        onDismissRequest = { buildMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Assemble Debug (Incremental)", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GoogleGreen) },
                            onClick = {
                                onTriggerBuild("assembleDebug", false)
                                buildMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clean & Assemble Debug", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = GoogleYellow) },
                            onClick = {
                                onTriggerBuild("assembleDebug", true)
                                buildMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Assemble Release Bundle", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Mauve) },
                            onClick = {
                                onTriggerBuild("assembleRelease", false)
                                buildMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Run Unit Tests", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoogleBlue) },
                            onClick = {
                                onTriggerBuild("testDebugUnitTest", false)
                                buildMenuExpanded = false
                            }
                        )
                    }
                }

                // Deploy Button
                Button(
                    onClick = onDeployClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue.copy(alpha = 0.2f),
                        contentColor = GoogleBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = "Deploy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deploy on Device", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Right: Sidebar Toggles & Bridge Daemon Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Explorer Toggle
                IconButton(
                    onClick = { onToggleSidebar(ActiveSidebar.EXPLORER) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (uiState.activeSidebar == ActiveSidebar.EXPLORER) GoogleBlue else TextSecondary
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Files")
                }

                // Antigravity AI Toggle
                IconButton(
                    onClick = { onToggleSidebar(ActiveSidebar.AI) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (uiState.activeSidebar == ActiveSidebar.AI) Mauve else TextSecondary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Antigravity AI")
                }

                // Git Toggle
                IconButton(
                    onClick = { onToggleSidebar(ActiveSidebar.GIT) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (uiState.activeSidebar == ActiveSidebar.GIT) GoogleYellow else TextSecondary
                    )
                ) {
                    Icon(Icons.Default.ForkRight, contentDescription = "Git")
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = DarkBorder
                )

                // Bridge Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.clickable { onBridgeClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.bridgeStatus.isOnline) GoogleGreen else GoogleRed)
                        )
                        Text(
                            text = if (uiState.bridgeStatus.isOnline) "Bridge 8765" else "Bridge Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.bridgeStatus.isOnline) TextPrimary else GoogleRed
                        )
                    }
                }
            }
        }
    }
}
