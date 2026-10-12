package com.bradlchr.googlebookide

import com.bradlchr.googlebookide.compiler.GradleCommandBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleCommandBuilderTest {

    @Test
    fun testAssembleDebugCommandIncludesCacheRedirection() {
        val projectPath = "/storage/emulated/10/Documents/Antigravity Apps/Tebri"
        val cmd = GradleCommandBuilder.build(
            projectPath = projectPath,
            task = "assembleDebug",
            clean = false
        )

        // Must start with ./gradlew
        assertEquals("./gradlew", cmd.first())
        // Must contain assembleDebug
        assertTrue(cmd.contains("assembleDebug"))
        // Must contain --project-cache-dir pointing to VM ext4 ~/.cache/projects/...
        val cacheArg = cmd.find { it.startsWith("--project-cache-dir=") }
        assertTrue("Must include --project-cache-dir", cacheArg != null)
        assertTrue("Cache dir must be in .cache/projects", cacheArg!!.contains(".cache/projects/Tebri-"))
        assertTrue("Cache dir must end with /gradle", cacheArg.endsWith("/gradle"))
    }

    @Test
    fun testCleanBuildCommandOrdersCleanFirst() {
        val projectPath = "/storage/emulated/10/Documents/Antigravity Apps/myDND"
        val cmd = GradleCommandBuilder.build(
            projectPath = projectPath,
            task = "assembleRelease",
            clean = true
        )

        val cleanIndex = cmd.indexOf("clean")
        val taskIndex = cmd.indexOf("assembleRelease")
        assertTrue("Clean must be included", cleanIndex >= 0)
        assertTrue("Task must be included", taskIndex >= 0)
        assertTrue("Clean must execute before assembleRelease", cleanIndex < taskIndex)
    }

    @Test
    fun testDetectExpectedApkPath() {
        val projectPath = "/storage/emulated/10/Documents/Antigravity Apps/Googlebook IDE"
        val expected = GradleCommandBuilder.getStandardApkPath(projectPath, "GooglebookIDE", isDebug = true)
        assertEquals(
            "/storage/emulated/10/Documents/Antigravity Apps/Googlebook IDE/app/build/outputs/apk/debug/GooglebookIDE-debug.apk",
            expected
        )
    }
}
