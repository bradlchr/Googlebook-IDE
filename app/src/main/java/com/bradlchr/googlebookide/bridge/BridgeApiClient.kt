package com.bradlchr.googlebookide.bridge

import com.bradlchr.googlebookide.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class BridgeApiClient(
    var baseUrl: String = "http://127.0.0.1:8765"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkStatus(): BridgeStatus = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val req = Request.Builder().url("$baseUrl/api/status").get().build()
            client.newCall(req).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                if (!response.isSuccessful) {
                    return@withContext BridgeStatus(isOnline = false, pingLatencyMs = latency)
                }
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val platforms = mutableListOf<String>()
                val arr = json.optJSONArray("sdk_platforms")
                if (arr != null) {
                    for (i in 0 until arr.length()) platforms.add(arr.getString(i))
                }

                BridgeStatus(
                    isOnline = true,
                    javaVersion = json.optString("java", "Unknown"),
                    gitVersion = json.optString("git", "Unknown"),
                    githubAuth = json.optString("github_auth", "Not logged in"),
                    sdkInstalled = json.optBoolean("sdk_installed", false),
                    sdkPlatforms = platforms,
                    sdkPath = json.optString("sdk_path", ""),
                    appsDirectory = json.optString("apps_directory", ""),
                    serverTime = json.optString("server_time", ""),
                    pingLatencyMs = latency
                )
            }
        } catch (e: Exception) {
            BridgeStatus(isOnline = false, errorMessage = e.message)
        }
    }

    suspend fun listProjects(): List<ProjectInfo> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/projects").get().build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val json = JSONObject(response.body?.string() ?: "")
                val arr = json.optJSONArray("projects") ?: return@withContext emptyList()
                val list = mutableListOf<ProjectInfo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val apks = mutableListOf<String>()
                    val apkArr = obj.optJSONArray("apks")
                    if (apkArr != null) {
                        for (j in 0 until apkArr.length()) apks.add(apkArr.getString(j))
                    }
                    list.add(
                        ProjectInfo(
                            name = obj.getString("name"),
                            path = obj.getString("path"),
                            isAndroid = obj.optBoolean("is_android", true),
                            hasGit = obj.optBoolean("has_git", false),
                            apks = apks,
                            lastModified = obj.optLong("last_modified", 0L)
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            // Fallback: direct scan of local /storage/emulated/10/Documents/Antigravity Apps
            fallbackLocalScan()
        }
    }

    private fun fallbackLocalScan(): List<ProjectInfo> {
        val root = File("/storage/emulated/10/Documents/Antigravity Apps")
        if (!root.exists() || !root.isDirectory) return emptyList()
        val list = mutableListOf<ProjectInfo>()
        root.listFiles()?.forEach { file ->
            if (file.isDirectory && !file.name.startsWith(".")) {
                val isAndroid = File(file, "build.gradle.kts").exists() || File(file, "build.gradle").exists()
                val hasGit = File(file, ".git").isDirectory
                list.add(
                    ProjectInfo(
                        name = file.name,
                        path = file.absolutePath,
                        isAndroid = isAndroid,
                        hasGit = hasGit,
                        lastModified = file.lastModified()
                    )
                )
            }
        }
        return list.sortedByDescending { it.lastModified }
    }

    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/files?path=" + java.net.URLEncoder.encode(path, "UTF-8")
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) return@withContext fallbackListDirectory(path)
                val json = JSONObject(response.body?.string() ?: "")
                val arr = json.optJSONArray("items") ?: return@withContext emptyList()
                val list = mutableListOf<FileItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        FileItem(
                            name = obj.getString("name"),
                            path = obj.getString("path"),
                            isDirectory = obj.getBoolean("is_directory"),
                            size = obj.optLong("size", 0L),
                            extension = obj.optString("extension", "")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            fallbackListDirectory(path)
        }
    }

    private fun fallbackListDirectory(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val items = mutableListOf<FileItem>()
        dir.listFiles()?.forEach { file ->
            if (file.name in listOf(".git", ".gradle", ".idea", "build", ".kotlin")) return@forEach
            val isDir = file.isDirectory
            items.add(
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = isDir,
                    size = if (isDir) 0L else file.length(),
                    extension = file.extension.lowercase()
                )
            )
        }
        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/file?path=" + java.net.URLEncoder.encode(path, "UTF-8")
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    return@withContext json.optString("content", "")
                }
            }
        } catch (_: Exception) {}
        // Fallback to local file read
        val file = File(path)
        if (file.exists()) file.readText() else ""
    }

    suspend fun saveFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("path", path)
            json.put("content", content)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/file").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}
        // Fallback to direct write
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createPath(path: String, isDir: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("path", path)
            json.put("is_directory", isDir)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/create").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}
        try {
            val file = File(path)
            if (isDir) file.mkdirs() else { file.parentFile?.mkdirs(); file.createNewFile() }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePath(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("path", path)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/delete").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) return@withContext true
            }
        } catch (_: Exception) {}
        try {
            val file = File(path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun startBuild(projectPath: String, task: String = "assembleDebug", clean: Boolean = false): String = withContext(Dispatchers.IO) {
        val json = JSONObject()
        json.put("project_path", projectPath)
        json.put("task", task)
        json.put("clean", clean)
        val body = json.toString().toRequestBody(jsonMediaType)
        val req = Request.Builder().url("$baseUrl/api/build").post(body).build()
        client.newCall(req).execute().use { response ->
            if (response.isSuccessful) {
                val resObj = JSONObject(response.body?.string() ?: "")
                return@withContext resObj.optString("job_id", "")
            }
        }
        ""
    }

    suspend fun getBuildLogs(jobId: String): BuildJob = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/build/logs?job_id=" + java.net.URLEncoder.encode(jobId, "UTF-8")
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { response ->
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val statusStr = json.optString("status", "running")
                val status = when (statusStr) {
                    "success" -> BuildStatus.SUCCESS
                    "failed" -> BuildStatus.FAILED
                    "error" -> BuildStatus.ERROR
                    else -> BuildStatus.RUNNING
                }
                return@withContext BuildJob(
                    id = json.optString("id", jobId),
                    projectPath = "",
                    task = "",
                    status = status,
                    logs = json.optString("logs", ""),
                    exitCode = if (json.has("exit_code") && !json.isNull("exit_code")) json.getInt("exit_code") else null,
                    apkPath = if (json.has("apk_path") && !json.isNull("apk_path")) json.getString("apkPath") else null,
                    durationSeconds = json.optDouble("duration", 0.0)
                )
            }
        }
        BuildJob(id = jobId, projectPath = "", task = "", status = BuildStatus.ERROR)
    }

    suspend fun queryAntigravity(prompt: String, context: String = "", projectPath: String = ""): AntigravityMessage = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("prompt", prompt)
            json.put("context", context)
            json.put("project_path", projectPath)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/antigravity").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val resObj = JSONObject(response.body?.string() ?: "")
                    val action = if (resObj.has("action") && !resObj.isNull("action")) resObj.getString("action") else null
                    val task = if (resObj.has("task") && !resObj.isNull("task")) resObj.getString("task") else null
                    return@withContext AntigravityMessage(
                        text = resObj.optString("reply", "No response"),
                        isUser = false,
                        action = action,
                        task = task,
                        messageType = resObj.optString("type", "chat")
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback local Antigravity logic if bridge is disconnected
        val reply = "⚡ **Antigravity Offline Mode**\n\nPrompt: `$prompt`\n\n*Note: Start the Googlebook Bridge (`./start_bridge.sh`) in the VM terminal for live toolchain execution and full AI generation.*"
        AntigravityMessage(text = reply, isUser = false)
    }

    suspend fun runTerminal(command: String, cwd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("command", command)
            json.put("cwd", cwd)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/terminal").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val res = JSONObject(response.body?.string() ?: "")
                    return@withContext Pair(res.optInt("exit_code", 0), res.optString("output", ""))
                }
            }
        } catch (e: Exception) {
            return@withContext Pair(-1, "Terminal request failed: ${e.message}")
        }
        Pair(-1, "Failed to run command")
    }

    suspend fun getGitStatus(projectPath: String): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("action", "status")
            json.put("project_path", projectPath)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/git").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val res = JSONObject(response.body?.string() ?: "")
                    return@withContext Pair(res.optString("branch", "main"), res.optString("status_output", ""))
                }
            }
        } catch (_: Exception) {}
        Pair("unknown", "")
    }

    suspend fun commitGit(projectPath: String, message: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("action", "commit")
            json.put("project_path", projectPath)
            json.put("message", message)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/git").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val res = JSONObject(response.body?.string() ?: "")
                    return@withContext Pair(res.optInt("exit_code", 0), res.optString("output", ""))
                }
            }
        } catch (e: Exception) {
            return@withContext Pair(-1, e.message ?: "Commit failed")
        }
        Pair(-1, "Commit request failed")
    }

    suspend fun pushGit(projectPath: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("action", "push")
            json.put("project_path", projectPath)
            val body = json.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url("$baseUrl/api/git").post(body).build()
            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val res = JSONObject(response.body?.string() ?: "")
                    return@withContext Pair(res.optInt("exit_code", 0), res.optString("output", ""))
                }
            }
        } catch (e: Exception) {
            return@withContext Pair(-1, e.message ?: "Push failed")
        }
        Pair(-1, "Push request failed")
    }
}
