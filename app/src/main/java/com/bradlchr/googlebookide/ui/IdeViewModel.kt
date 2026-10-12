package com.bradlchr.googlebookide.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bradlchr.googlebookide.antigravity.SlashCommand
import com.bradlchr.googlebookide.antigravity.SlashCommandHandler
import com.bradlchr.googlebookide.bridge.BridgeApiClient
import com.bradlchr.googlebookide.compiler.BuildLogParser
import com.bradlchr.googlebookide.compiler.GradleCommandBuilder
import com.bradlchr.googlebookide.model.*
import com.bradlchr.googlebookide.wizard.AndroidProjectTemplateGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditorTab(
    val path: String,
    val name: String,
    val content: String,
    val originalContent: String,
    val extension: String
) {
    val isDirty: Boolean get() = content != originalContent
}

enum class ActiveSidebar {
    EXPLORER,
    AI,
    GIT
}

data class IdeUiState(
    val projects: List<ProjectInfo> = emptyList(),
    val selectedProject: ProjectInfo? = null,
    val fileTree: List<FileItem> = emptyList(),
    val openTabs: List<EditorTab> = emptyList(),
    val activeTabPath: String? = null,
    val activeSidebar: ActiveSidebar = ActiveSidebar.EXPLORER,
    val isBottomPanelOpen: Boolean = false,
    val bottomTabIndex: Int = 0, // 0 = Build Logs, 1 = Terminal, 2 = Errors
    val buildJob: BuildJob = BuildJob("none", "", "idle", BuildStatus.IDLE),
    val isBuilding: Boolean = false,
    val compilationErrors: List<CompilationError> = emptyList(),
    val antigravityMessages: List<AntigravityMessage> = emptyList(),
    val isAntigravityThinking: Boolean = false,
    val terminalOutput: String = "Googlebook IDE Terminal Ready.\nType any bash command to execute in Linux VM.\n",
    val gitBranch: String = "main",
    val gitStatusText: String = "",
    val bridgeStatus: BridgeStatus = BridgeStatus(),
    val notificationMessage: String? = null
)

class IdeViewModel(
    val bridgeClient: BridgeApiClient = BridgeApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private var buildPollingJob: Job? = null

    init {
        checkBridgeAndLoadProjects()
    }

    fun checkBridgeAndLoadProjects() {
        viewModelScope.launch {
            val status = bridgeClient.checkStatus()
            _uiState.update { it.copy(bridgeStatus = status) }
            refreshProjects()
        }
    }

    fun refreshProjects() {
        viewModelScope.launch {
            val list = bridgeClient.listProjects()
            _uiState.update { state ->
                val selected = state.selectedProject ?: list.firstOrNull()
                state.copy(projects = list, selectedProject = selected)
            }
            uiState.value.selectedProject?.let { selectProject(it) }
        }
    }

    fun selectProject(project: ProjectInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedProject = project) }
            loadFileTree(project.path)
            refreshGit()
        }
    }

    fun loadFileTree(path: String) {
        viewModelScope.launch {
            val items = bridgeClient.listDirectory(path)
            _uiState.update { it.copy(fileTree = items) }
        }
    }

    fun openFile(item: FileItem) {
        if (item.isDirectory) {
            loadFileTree(item.path)
            return
        }

        viewModelScope.launch {
            val existing = uiState.value.openTabs.find { it.path == item.path }
            if (existing != null) {
                _uiState.update { it.copy(activeTabPath = item.path) }
                return@launch
            }

            val text = bridgeClient.readFile(item.path)
            val tab = EditorTab(
                path = item.path,
                name = item.name,
                content = text,
                originalContent = text,
                extension = item.extension
            )
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs + tab,
                    activeTabPath = tab.path
                )
            }
        }
    }

    fun selectTab(path: String) {
        _uiState.update { it.copy(activeTabPath = path) }
    }

    fun closeTab(path: String) {
        _uiState.update { state ->
            val remaining = state.openTabs.filter { it.path != path }
            val nextActive = if (state.activeTabPath == path) {
                remaining.lastOrNull()?.path
            } else {
                state.activeTabPath
            }
            state.copy(openTabs = remaining, activeTabPath = nextActive)
        }
    }

    fun updateActiveEditorContent(newContent: String) {
        val path = uiState.value.activeTabPath ?: return
        _uiState.update { state ->
            val updated = state.openTabs.map {
                if (it.path == path) it.copy(content = newContent) else it
            }
            state.copy(openTabs = updated)
        }
    }

    fun saveActiveFile() {
        val path = uiState.value.activeTabPath ?: return
        val tab = uiState.value.openTabs.find { it.path == path } ?: return
        viewModelScope.launch {
            val ok = bridgeClient.saveFile(tab.path, tab.content)
            if (ok) {
                _uiState.update { state ->
                    val updated = state.openTabs.map {
                        if (it.path == path) it.copy(originalContent = tab.content) else it
                    }
                    state.copy(openTabs = updated, notificationMessage = "Saved ${tab.name}")
                }
            } else {
                _uiState.update { it.copy(notificationMessage = "Failed to save ${tab.name}") }
            }
        }
    }

    fun triggerBuild(task: String = "assembleDebug", clean: Boolean = false) {
        val project = uiState.value.selectedProject ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBuilding = true,
                    isBottomPanelOpen = true,
                    bottomTabIndex = 0,
                    buildJob = BuildJob(
                        id = "starting",
                        projectPath = project.path,
                        task = task,
                        status = BuildStatus.RUNNING,
                        logs = "🚀 Initiating Gradle build: $task (clean=$clean)\n"
                    )
                )
            }

            val jobId = bridgeClient.startBuild(project.path, task, clean)
            if (jobId.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isBuilding = false,
                        buildJob = it.buildJob.copy(
                            status = BuildStatus.ERROR,
                            logs = it.buildJob.logs + "❌ Could not reach bridge daemon to execute build.\nEnsure start_bridge.sh is running in Linux VM."
                        )
                    )
                }
                return@launch
            }

            pollBuildUntilFinished(jobId)
        }
    }

    private fun pollBuildUntilFinished(jobId: String) {
        buildPollingJob?.cancel()
        buildPollingJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val job = bridgeClient.getBuildLogs(jobId)
                val errors = BuildLogParser.extractErrors(job.logs)
                _uiState.update {
                    it.copy(
                        buildJob = job,
                        compilationErrors = errors,
                        isBuilding = (job.status == BuildStatus.RUNNING)
                    )
                }
                if (job.status != BuildStatus.RUNNING) {
                    if (job.status == BuildStatus.SUCCESS) {
                        _uiState.update { it.copy(notificationMessage = "Build Successful! Ready to deploy.") }
                    } else {
                        _uiState.update { it.copy(notificationMessage = "Build Failed. Check issues.") }
                    }
                    break
                }
            }
        }
    }

    fun deployApkToDevice(context: Context) {
        val project = uiState.value.selectedProject ?: return
        val apkPath = uiState.value.buildJob.apkPath
            ?: GradleCommandBuilder.getStandardApkPath(project.path, project.name, isDebug = true)
        val file = File(apkPath)
        if (!file.exists()) {
            _uiState.update { it.copy(notificationMessage = "APK not found at: $apkPath. Please compile first!") }
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.bradlchr.googlebookide.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            _uiState.update { it.copy(notificationMessage = "Opening package installer for ${file.name}...") }
        } catch (e: Exception) {
            _uiState.update { it.copy(notificationMessage = "Deployment error: ${e.message}") }
        }
    }

    fun sendAntigravityMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AntigravityMessage(text = prompt, isUser = true)
        _uiState.update {
            it.copy(
                antigravityMessages = it.antigravityMessages + userMsg,
                isAntigravityThinking = true
            )
        }

        val parsed = SlashCommandHandler.parse(prompt)
        if (parsed.command == SlashCommand.BUILD) {
            triggerBuild("assembleDebug")
            val botMsg = AntigravityMessage(text = "⚡ Triggered `assembleDebug` build task.", isUser = false)
            _uiState.update {
                it.copy(
                    antigravityMessages = it.antigravityMessages + botMsg,
                    isAntigravityThinking = false
                )
            }
            return
        } else if (parsed.command == SlashCommand.TEST) {
            triggerBuild("testDebugUnitTest")
            val botMsg = AntigravityMessage(text = "⚡ Triggered `testDebugUnitTest` task.", isUser = false)
            _uiState.update {
                it.copy(
                    antigravityMessages = it.antigravityMessages + botMsg,
                    isAntigravityThinking = false
                )
            }
            return
        }

        viewModelScope.launch {
            val activeTab = uiState.value.openTabs.find { it.path == uiState.value.activeTabPath }
            val context = activeTab?.content ?: ""
            val projectPath = uiState.value.selectedProject?.path ?: ""

            val botMsg = bridgeClient.queryAntigravity(prompt, context, projectPath)
            _uiState.update {
                it.copy(
                    antigravityMessages = it.antigravityMessages + botMsg,
                    isAntigravityThinking = false
                )
            }
        }
    }

    fun runTerminal(command: String) {
        val cwd = uiState.value.selectedProject?.path ?: "/storage/emulated/10/Documents/Antigravity Apps"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBottomPanelOpen = true,
                    bottomTabIndex = 1,
                    terminalOutput = it.terminalOutput + "\n$ $command\n"
                )
            }
            val (code, out) = bridgeClient.runTerminal(command, cwd)
            _uiState.update {
                it.copy(terminalOutput = it.terminalOutput + out + "\n[Exit code: $code]\n")
            }
        }
    }

    fun refreshGit() {
        val project = uiState.value.selectedProject ?: return
        viewModelScope.launch {
            val (branch, status) = bridgeClient.getGitStatus(project.path)
            _uiState.update { it.copy(gitBranch = branch, gitStatusText = status) }
        }
    }

    fun commitAndPush(message: String) {
        val project = uiState.value.selectedProject ?: return
        viewModelScope.launch {
            val (cCode, cOut) = bridgeClient.commitGit(project.path, message)
            if (cCode == 0) {
                val (pCode, pOut) = bridgeClient.pushGit(project.path)
                refreshGit()
                _uiState.update {
                    it.copy(notificationMessage = if (pCode == 0) "Committed & Pushed!" else "Pushed with warning: $pOut")
                }
            } else {
                _uiState.update { it.copy(notificationMessage = "Commit failed: $cOut") }
            }
        }
    }

    fun createProject(projectName: String, packageName: String) {
        viewModelScope.launch {
            val baseDir = "/storage/emulated/10/Documents/Antigravity Apps/$projectName"
            val files = AndroidProjectTemplateGenerator.generateFiles(projectName, packageName)

            files.forEach { (relPath, content) ->
                val fullPath = "$baseDir/$relPath"
                bridgeClient.saveFile(fullPath, content)
            }

            // Also copy gradlew wrapper if available
            bridgeClient.runTerminal("cp -r /storage/emulated/10/Documents/Antigravity\\ Apps/myDND/gradle* '$baseDir/' || true", baseDir)
            bridgeClient.runTerminal("chmod +x gradlew", baseDir)

            refreshProjects()
            _uiState.update { it.copy(notificationMessage = "Created project $projectName!") }
        }
    }

    fun toggleSidebar(sidebar: ActiveSidebar) {
        _uiState.update { it.copy(activeSidebar = sidebar) }
    }

    fun toggleBottomPanel(tabIndex: Int = 0) {
        _uiState.update {
            it.copy(
                isBottomPanelOpen = !it.isBottomPanelOpen || it.bottomTabIndex != tabIndex,
                bottomTabIndex = tabIndex
            )
        }
    }

    fun createPath(path: String, isDir: Boolean) {
        val project = uiState.value.selectedProject ?: return
        viewModelScope.launch {
            bridgeClient.createPath(path, isDir)
            loadFileTree(project.path)
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
