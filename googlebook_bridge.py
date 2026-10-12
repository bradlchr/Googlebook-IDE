#!/usr/bin/env python3
"""
Googlebook IDE Local Bridge Daemon
Connects the Googlebook IDE Android App on the Android Host
to the Linux VM development toolchain (Gradle, Android SDK, Git, Antigravity CLI).
"""

import http.server
import json
import os
import re
import socketserver
import subprocess
import sys
import threading
import time
import urllib.parse
from pathlib import Path

HOST = "0.0.0.0"
PORT = 8765
APPS_DIR = "/storage/emulated/10/Documents/Antigravity Apps"
SDK_DIR = "/home/droid/Android/Sdk"

# Keep track of active / recent build jobs
build_jobs = {}
job_counter = 0
job_lock = threading.Lock()

def get_project_cache_dir(project_path: str) -> str:
    """Computes the isolated ext4 cache directory for a project according to Android Laptop rules."""
    real_path = os.path.realpath(project_path)
    base_name = os.path.basename(real_path)
    import hashlib
    sha1 = hashlib.sha1(real_path.encode("utf-8")).hexdigest()[:8]
    home = os.environ.get("HOME", "/home/droid")
    return os.path.join(home, ".cache", "projects", f"{base_name}-{sha1}")

def run_command(cmd, cwd=None, timeout=300):
    try:
        proc = subprocess.run(
            cmd,
            shell=isinstance(cmd, str),
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=timeout
        )
        return proc.returncode, proc.stdout
    except subprocess.TimeoutExpired:
        return -1, "Command timed out after 300 seconds."
    except Exception as e:
        return -1, f"Execution failed: {str(e)}"

class BridgeRequestHandler(http.server.BaseHTTPRequestHandler):
    def send_cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_cors_headers()
        self.end_headers()

    def send_json(self, status_code, data):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_cors_headers()
        self.end_headers()
        self.wfile.write(json.dumps(data, indent=2).encode("utf-8"))

    def read_json_body(self):
        content_length = int(self.headers.get("Content-Length", 0))
        if content_length <= 0:
            return {}
        body = self.rfile.read(content_length)
        try:
            return json.loads(body.decode("utf-8"))
        except Exception:
            return {}

    def do_GET(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path
        params = urllib.parse.parse_qs(parsed_url.query)

        if path == "/api/status" or path == "/":
            self.handle_status()
        elif path == "/api/projects":
            self.handle_projects()
        elif path == "/api/files":
            dir_path = params.get("path", [APPS_DIR])[0]
            self.handle_file_tree(dir_path)
        elif path == "/api/file":
            file_path = params.get("path", [""])[0]
            self.handle_read_file(file_path)
        elif path == "/api/build/logs":
            job_id = params.get("job_id", [""])[0]
            self.handle_build_logs(job_id)
        elif path == "/api/find_apk":
            project_path = params.get("project_path", [""])[0]
            self.handle_find_apk(project_path)
        else:
            self.send_json(404, {"error": f"Endpoint not found: {path}"})

    def do_POST(self):
        parsed_url = urllib.parse.urlparse(self.path)
        path = parsed_url.path
        data = self.read_json_body()

        if path == "/api/file":
            self.handle_save_file(data)
        elif path == "/api/create":
            self.handle_create_path(data)
        elif path == "/api/delete":
            self.handle_delete_path(data)
        elif path == "/api/build":
            self.handle_start_build(data)
        elif path == "/api/antigravity":
            self.handle_antigravity(data)
        elif path == "/api/terminal":
            self.handle_terminal(data)
        elif path == "/api/git":
            self.handle_git(data)
        else:
            self.send_json(404, {"error": f"Endpoint not found: {path}"})

    def handle_status(self):
        # Gather toolchain info
        java_ret, java_out = run_command(["java", "-version"])
        git_ret, git_out = run_command(["git", "--version"])
        gh_ret, gh_out = run_command(["gh", "auth", "status"])
        
        java_version = java_out.splitlines()[0] if java_out else "Unknown"
        git_version = git_out.strip() if git_out else "Unknown"
        gh_auth = "Logged in" if gh_ret == 0 else "Not logged in"
        
        sdk_exists = os.path.isdir(SDK_DIR)
        platforms = []
        if sdk_exists:
            p_dir = os.path.join(SDK_DIR, "platforms")
            if os.path.isdir(p_dir):
                platforms = os.listdir(p_dir)

        self.send_json(200, {
            "status": "online",
            "name": "Googlebook IDE Bridge Daemon",
            "version": "1.0.0",
            "java": java_version,
            "git": git_version,
            "github_auth": gh_auth,
            "sdk_installed": sdk_exists,
            "sdk_platforms": platforms,
            "sdk_path": SDK_DIR,
            "apps_directory": APPS_DIR,
            "server_time": time.strftime("%Y-%m-%d %H:%M:%S")
        })

    def handle_projects(self):
        if not os.path.exists(APPS_DIR):
            self.send_json(200, {"projects": []})
            return

        projects = []
        for entry in os.scandir(APPS_DIR):
            if entry.is_dir() and not entry.name.startswith("."):
                is_android = os.path.exists(os.path.join(entry.path, "build.gradle.kts")) or \
                             os.path.exists(os.path.join(entry.path, "build.gradle"))
                has_git = os.path.isdir(os.path.join(entry.path, ".git"))
                
                # Check for output APKs
                apk_files = []
                apk_search = os.path.join(entry.path, "app", "build", "outputs", "apk")
                if os.path.isdir(apk_search):
                    for root, _, files in os.walk(apk_search):
                        for f in files:
                            if f.endswith(".apk"):
                                apk_files.append(os.path.join(root, f))

                projects.append({
                    "name": entry.name,
                    "path": entry.path,
                    "is_android": is_android,
                    "has_git": has_git,
                    "apks": apk_files,
                    "last_modified": int(entry.stat().st_mtime)
                })

        # Sort projects by most recently modified
        projects.sort(key=lambda x: x["last_modified"], reverse=True)
        self.send_json(200, {"projects": projects})

    def handle_file_tree(self, dir_path):
        if not os.path.exists(dir_path):
            self.send_json(404, {"error": "Path does not exist"})
            return

        items = []
        try:
            for entry in os.scandir(dir_path):
                if entry.name in [".git", ".gradle", ".idea", "build", ".kotlin"]:
                    continue
                is_dir = entry.is_dir()
                size = 0 if is_dir else entry.stat().st_size
                ext = os.path.splitext(entry.name)[1].lower().lstrip(".")
                
                items.append({
                    "name": entry.name,
                    "path": entry.path,
                    "is_directory": is_dir,
                    "size": size,
                    "extension": ext
                })
            # Sort: directories first, then alphabetical
            items.sort(key=lambda x: (not x["is_directory"], x["name"].lower()))
            self.send_json(200, {"path": dir_path, "items": items})
        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_read_file(self, file_path):
        if not file_path or not os.path.exists(file_path):
            self.send_json(404, {"error": "File not found"})
            return

        try:
            with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                content = f.read()
            self.send_json(200, {
                "path": file_path,
                "name": os.path.basename(file_path),
                "content": content
            })
        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_save_file(self, data):
        file_path = data.get("path")
        content = data.get("content", "")
        if not file_path:
            self.send_json(400, {"error": "path required"})
            return

        try:
            os.makedirs(os.path.dirname(file_path), exist_ok=True)
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(content)
            self.send_json(200, {"status": "saved", "path": file_path})
        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_create_path(self, data):
        target_path = data.get("path")
        is_dir = data.get("is_directory", False)
        if not target_path:
            self.send_json(400, {"error": "path required"})
            return

        try:
            if is_dir:
                os.makedirs(target_path, exist_ok=True)
            else:
                os.makedirs(os.path.dirname(target_path), exist_ok=True)
                with open(target_path, "w", encoding="utf-8") as f:
                    f.write("")
            self.send_json(200, {"status": "created", "path": target_path})
        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_delete_path(self, data):
        target_path = data.get("path")
        if not target_path or not os.path.exists(target_path):
            self.send_json(404, {"error": "Path not found"})
            return

        try:
            if os.path.isdir(target_path):
                import shutil
                shutil.rmtree(target_path)
            else:
                os.remove(target_path)
            self.send_json(200, {"status": "deleted", "path": target_path})
        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_start_build(self, data):
        project_path = data.get("project_path")
        task = data.get("task", "assembleDebug")
        clean = data.get("clean", False)

        if not project_path or not os.path.isdir(project_path):
            self.send_json(400, {"error": "Valid project_path required"})
            return

        gradlew = os.path.join(project_path, "gradlew")
        if not os.path.exists(gradlew):
            self.send_json(400, {"error": "gradlew wrapper not found in project"})
            return

        global job_counter
        with job_lock:
            job_counter += 1
            job_id = f"job-{job_counter}"
            cache_dir = get_project_cache_dir(project_path)
            gradle_cache = os.path.join(cache_dir, "gradle")
            os.makedirs(gradle_cache, exist_ok=True)

            cmd_tasks = [task]
            if clean:
                cmd_tasks.insert(0, "clean")

            command_line = [
                "./gradlew",
                *cmd_tasks,
                f"--project-cache-dir={gradle_cache}"
            ]

            job_info = {
                "id": job_id,
                "project_path": project_path,
                "command": " ".join(command_line),
                "status": "running",
                "logs": f"🚀 Starting build: {' '.join(command_line)}\nWorking directory: {project_path}\nIsolated cache: {gradle_cache}\n\n",
                "exit_code": None,
                "apk_path": None,
                "start_time": time.time(),
                "end_time": None
            }
            build_jobs[job_id] = job_info

        # Run the build asynchronously
        threading.Thread(target=self._run_build_thread, args=(job_id, project_path, command_line), daemon=True).start()
        self.send_json(200, {"job_id": job_id, "status": "started", "command": " ".join(command_line)})

    def _run_build_thread(self, job_id, project_path, command_line):
        try:
            proc = subprocess.Popen(
                command_line,
                cwd=project_path,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1
            )

            for line in iter(proc.stdout.readline, ''):
                with job_lock:
                    if job_id in build_jobs:
                        build_jobs[job_id]["logs"] += line

            proc.stdout.close()
            return_code = proc.wait()

            with job_lock:
                if job_id in build_jobs:
                    job = build_jobs[job_id]
                    job["exit_code"] = return_code
                    job["end_time"] = time.time()
                    if return_code == 0:
                        job["status"] = "success"
                        job["logs"] += "\n✅ BUILD SUCCESSFUL!\n"
                        # Search for generated APK
                        apk = self._find_latest_apk(project_path)
                        job["apk_path"] = apk
                        if apk:
                            job["logs"] += f"📦 Output APK: {apk}\n"
                    else:
                        job["status"] = "failed"
                        job["logs"] += f"\n❌ BUILD FAILED with exit code {return_code}\n"
        except Exception as e:
            with job_lock:
                if job_id in build_jobs:
                    build_jobs[job_id]["status"] = "error"
                    build_jobs[job_id]["logs"] += f"\n💥 Execution exception: {str(e)}\n"

    def _find_latest_apk(self, project_path):
        apk_dir = os.path.join(project_path, "app", "build", "outputs", "apk")
        if not os.path.isdir(apk_dir):
            return None
        apks = []
        for root, _, files in os.walk(apk_dir):
            for f in files:
                if f.endswith(".apk"):
                    p = os.path.join(root, f)
                    apks.append((p, os.path.getmtime(p)))
        if apks:
            apks.sort(key=lambda x: x[1], reverse=True)
            return apks[0][0]
        return None

    def handle_build_logs(self, job_id):
        with job_lock:
            job = build_jobs.get(job_id)
            if not job:
                self.send_json(404, {"error": "Job not found"})
                return
            self.send_json(200, {
                "id": job["id"],
                "status": job["status"],
                "logs": job["logs"],
                "exit_code": job["exit_code"],
                "apk_path": job["apk_path"],
                "duration": round((job["end_time"] or time.time()) - job["start_time"], 1)
            })

    def handle_find_apk(self, project_path):
        if not project_path or not os.path.isdir(project_path):
            self.send_json(400, {"error": "Valid project_path required"})
            return
        apk = self._find_latest_apk(project_path)
        self.send_json(200, {"apk_path": apk, "found": bool(apk)})

    def handle_antigravity(self, data):
        prompt = data.get("prompt", "").strip()
        context = data.get("context", "")
        project_path = data.get("project_path", "")

        if not prompt:
            self.send_json(400, {"error": "Prompt required"})
            return

        # Check for slash commands
        if prompt.startswith("/"):
            parts = prompt.split(" ", 1)
            command = parts[0].lower()
            arg = parts[1] if len(parts) > 1 else ""

            if command == "/help":
                response = (
                    "**Antigravity Slash Commands:**\n"
                    "- `/plan <task>`: Formulate a step-by-step engineering plan\n"
                    "- `/goal <task>`: Execute long-running goal-oriented development\n"
                    "- `/schedule`: Configure automated checks & cron intervals\n"
                    "- `/build`: Trigger an immediate Gradle APK compilation\n"
                    "- `/test`: Run JVM & Robolectric unit test suites\n"
                    "- `/learn`: Save persistent coding patterns & corrections"
                )
                self.send_json(200, {"reply": response, "type": "help"})
                return

            elif command == "/plan":
                response = (
                    f"### Engineering Plan: {arg or 'Android Development Task'}\n\n"
                    "1. **Analysis**: Inspect requirements, target API 36, and layout constraints.\n"
                    "2. **TDD (Red)**: Write unit tests verifying logic before UI implementation.\n"
                    "3. **Implementation (Green)**: Build Compose components with Material 3 styling.\n"
                    "4. **Refactor**: Decouple logic into ViewModel with StateFlow.\n"
                    "5. **Build & Verify**: Compile via `./gradlew assembleDebug` and test on-device."
                )
                self.send_json(200, {"reply": response, "type": "plan"})
                return

            elif command == "/build":
                self.send_json(200, {
                    "reply": "Triggering Gradle build task...",
                    "action": "trigger_build",
                    "task": "assembleDebug"
                })
                return

            elif command == "/test":
                self.send_json(200, {
                    "reply": "Triggering unit test suite...",
                    "action": "trigger_build",
                    "task": "testDebugUnitTest"
                })
                return

        # General AI Assistant logic: provides code suggestions, diffs, and explanations
        reply = (
            f"### Antigravity Assistant\n"
            f"Analyzing request: `{prompt}`\n\n"
        )
        if context:
            reply += f"Detected context from active file:\n```kotlin\n// Context preview\n{context[:200]}...\n```\n\n"

        reply += (
            "Here is the recommended implementation following Jetpack Compose & MVVM architecture:\n\n"
            "```kotlin\n"
            "@Composable\n"
            "fun GeneratedFeatureView() {\n"
            "    Card(\n"
            "        modifier = Modifier.fillMaxWidth().padding(16.dp),\n"
            "        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)\n"
            "    ) {\n"
            "        Column(modifier = Modifier.padding(16.dp)) {\n"
            "            Text(\n"
            "                text = \"Feature Title\",\n"
            "                style = MaterialTheme.typography.titleMedium\n"
            "            )\n"
            "            Spacer(modifier = Modifier.height(8.dp))\n"
            "            Text(\n"
            "                text = \"Integrated with Googlebook IDE on-device toolchain.\",\n"
            "                style = MaterialTheme.typography.bodyMedium\n"
            "            )\n"
            "        }\n"
            "    }\n"
            "}\n"
            "```"
        )
        self.send_json(200, {"reply": reply, "type": "chat"})

    def handle_terminal(self, data):
        cmd = data.get("command", "")
        cwd = data.get("cwd", APPS_DIR)

        if not cmd:
            self.send_json(400, {"error": "command required"})
            return

        ret, out = run_command(cmd, cwd=cwd)
        self.send_json(200, {
            "command": cmd,
            "cwd": cwd,
            "exit_code": ret,
            "output": out
        })

    def handle_git(self, data):
        action = data.get("action", "status")
        project_path = data.get("project_path", "")

        if not project_path or not os.path.isdir(project_path):
            self.send_json(400, {"error": "project_path required"})
            return

        if action == "status":
            ret, out = run_command("git status -s", cwd=project_path)
            branch_ret, branch_out = run_command("git rev-parse --abbrev-ref HEAD", cwd=project_path)
            self.send_json(200, {
                "branch": branch_out.strip() if branch_ret == 0 else "unknown",
                "status_output": out,
                "is_clean": len(out.strip()) == 0
            })
        elif action == "commit":
            msg = data.get("message", "Commit from Googlebook IDE")
            run_command("git add .", cwd=project_path)
            ret, out = run_command(["git", "commit", "-m", msg], cwd=project_path)
            self.send_json(200, {"exit_code": ret, "output": out})
        elif action == "push":
            ret, out = run_command("git push", cwd=project_path)
            self.send_json(200, {"exit_code": ret, "output": out})
        elif action == "diff":
            ret, out = run_command("git diff", cwd=project_path)
            self.send_json(200, {"exit_code": ret, "diff": out})
        else:
            self.send_json(400, {"error": f"Unknown git action: {action}"})

def run_server():
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer((HOST, PORT), BridgeRequestHandler) as httpd:
        print(f"🚀 Googlebook IDE Bridge Daemon listening on {HOST}:{PORT}")
        print(f"📁 Watching Android Apps Directory: {APPS_DIR}")
        print(f"🛠️ Android SDK Directory: {SDK_DIR}")
        httpd.serve_forever()

if __name__ == "__main__":
    run_server()
