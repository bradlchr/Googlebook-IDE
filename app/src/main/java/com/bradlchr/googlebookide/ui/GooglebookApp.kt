package com.bradlchr.googlebookide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bradlchr.googlebookide.ui.components.*
import com.bradlchr.googlebookide.ui.theme.*

@Composable
fun GooglebookApp(
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showGitDialog by remember { mutableStateOf(false) }
    var showBridgeDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopMenuBar(
                uiState = uiState,
                onSelectProject = { viewModel.selectProject(it) },
                onNewProjectClick = { showNewProjectDialog = true },
                onTriggerBuild = { task, clean -> viewModel.triggerBuild(task, clean) },
                onDeployClick = { viewModel.deployApkToDevice(context) },
                onToggleSidebar = { sidebar ->
                    if (sidebar == ActiveSidebar.GIT) {
                        showGitDialog = true
                    } else {
                        viewModel.toggleSidebar(sidebar)
                    }
                },
                onBridgeClick = { showBridgeDialog = true }
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Sidebar
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                when (uiState.activeSidebar) {
                    ActiveSidebar.EXPLORER -> {
                        ProjectExplorer(
                            currentDir = uiState.selectedProject?.path ?: "",
                            items = uiState.fileTree,
                            onOpenFile = { viewModel.openFile(it) },
                            onRefresh = { uiState.selectedProject?.let { viewModel.loadFileTree(it.path) } },
                            onCreateFile = { name, isDir ->
                                val base = uiState.selectedProject?.path ?: return@ProjectExplorer
                                val fullPath = "$base/$name"
                                viewModel.createPath(fullPath, isDir)
                            },
                            onNavigateUp = {
                                val current = uiState.selectedProject?.path ?: return@ProjectExplorer
                                val parent = java.io.File(current).parent ?: return@ProjectExplorer
                                viewModel.loadFileTree(parent)
                            }
                        )
                    }
                    ActiveSidebar.AI -> {
                        AntigravityAiPanel(
                            messages = uiState.antigravityMessages,
                            isThinking = uiState.isAntigravityThinking,
                            onSendMessage = { viewModel.sendAntigravityMessage(it) },
                            onInsertCode = { code ->
                                val active = uiState.openTabs.find { it.path == uiState.activeTabPath }
                                if (active != null) {
                                    viewModel.updateActiveEditorContent(active.content + "\n\n" + code)
                                }
                            }
                        )
                    }
                    ActiveSidebar.GIT -> {
                        // Handled via dialog
                    }
                }
            }

            VerticalDivider(color = DarkBorder, thickness = 1.dp)

            // Center Area: Editor + Bottom Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Code Editor
                Box(modifier = Modifier.weight(1f)) {
                    CodeEditorView(
                        openTabs = uiState.openTabs,
                        activeTabPath = uiState.activeTabPath,
                        onSelectTab = { viewModel.selectTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onContentChange = { viewModel.updateActiveEditorContent(it) },
                        onSaveFile = { viewModel.saveActiveFile() }
                    )
                }

                // Collapsible Bottom Panel (Build Logs / Terminal / Errors)
                if (uiState.isBottomPanelOpen) {
                    HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                    BottomTerminalPanel(
                        selectedTab = uiState.bottomTabIndex,
                        onTabSelected = { viewModel.toggleBottomPanel(it) },
                        buildJob = uiState.buildJob,
                        terminalOutput = uiState.terminalOutput,
                        compilationErrors = uiState.compilationErrors,
                        onRunTerminalCommand = { viewModel.runTerminal(it) },
                        onErrorClick = { err ->
                            // Attempt to open the error file
                            val base = uiState.selectedProject?.path ?: return@BottomTerminalPanel
                            val file = java.io.File(base, "app/src/main/java")
                            // Search for matching file
                            file.walkTopDown().find { it.name == err.fileName }?.let { found ->
                                viewModel.openFile(
                                    com.bradlchr.googlebookide.model.FileItem(
                                        name = found.name,
                                        path = found.absolutePath,
                                        isDirectory = false,
                                        extension = found.extension
                                    )
                                )
                            }
                        },
                        onClose = { viewModel.toggleBottomPanel(uiState.bottomTabIndex) }
                    )
                }
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onCreateProject = { name, pkg ->
                viewModel.createProject(name, pkg)
            },
            onDismiss = { showNewProjectDialog = false }
        )
    }

    if (showGitDialog) {
        GitDialog(
            branch = uiState.gitBranch,
            statusText = uiState.gitStatusText,
            onCommitAndPush = { msg ->
                viewModel.commitAndPush(msg)
            },
            onDismiss = { showGitDialog = false }
        )
    }

    if (showBridgeDialog) {
        BridgeSettingsDialog(
            bridgeStatus = uiState.bridgeStatus,
            bridgeUrl = viewModel.bridgeClient.baseUrl,
            onUrlChange = { viewModel.bridgeClient.baseUrl = it },
            onTestConnection = { viewModel.checkBridgeAndLoadProjects() },
            onDismiss = { showBridgeDialog = false }
        )
    }
}
