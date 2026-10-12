package com.bradlchr.googlebookide

import com.bradlchr.googlebookide.wizard.AndroidProjectTemplateGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidProjectTemplateGeneratorTest {

    @Test
    fun testGenerateProjectScaffoldFiles() {
        val files = AndroidProjectTemplateGenerator.generateFiles(
            projectName = "NotesApp",
            packageName = "com.bradlchr.notesapp"
        )

        assertTrue(files.containsKey("settings.gradle.kts"))
        assertTrue(files.containsKey("build.gradle.kts"))
        assertTrue(files.containsKey("gradle.properties"))
        assertTrue(files.containsKey("app/build.gradle.kts"))
        assertTrue(files.containsKey("app/src/main/AndroidManifest.xml"))
        assertTrue(files.containsKey("app/src/main/java/com/bradlchr/notesapp/MainActivity.kt"))

        // Check settings.gradle.kts has correct rootProject name
        val settings = files["settings.gradle.kts"]!!
        assertTrue(settings.contains("rootProject.name = \"NotesApp\""))

        // Check app/build.gradle.kts has modern SDK >= 34
        val appGradle = files["app/build.gradle.kts"]!!
        assertTrue(appGradle.contains("targetSdk = 36"))
        assertTrue(appGradle.contains("namespace = \"com.bradlchr.notesapp\""))
    }
}
