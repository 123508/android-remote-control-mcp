package com.danielealbano.androidremotecontrolmcp.ui.viewmodels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AccessibilitySettingsScriptTest {
    @TempDir lateinit var directory: File

    @Test
    fun `enable and disable preserve other services and avoid duplicates`() {
        val settings = File(directory, "settings")
        val state = File(directory, "state")
        val flag = File(directory, "flag")
        settings.writeText(
            """#!/bin/sh
if [ "${'$'}3" = get ]; then cat "${'$'}STATE_FILE"; exit; fi
if [ "${'$'}5" = enabled_accessibility_services ]; then printf '%s' "${'$'}6" > "${'$'}STATE_FILE"; else printf '%s' "${'$'}6" > "${'$'}FLAG_FILE"; fi
""",
        )
        settings.setExecutable(true)
        state.writeText("other/.Service:example/.Service:third/.Service")
        fun run(enabled: Boolean) {
            val script = accessibilitySettingsScript(10, "example/example.Service", "example/.Service", enabled)
            val process = ProcessBuilder("/bin/sh", "-c", script)
                .apply {
                    environment()["PATH"] = directory.absolutePath + ":/usr/bin:/bin"
                    environment()["STATE_FILE"] = state.absolutePath
                    environment()["FLAG_FILE"] = flag.absolutePath
                }.start()
            assertEquals(0, process.waitFor(), process.errorStream.bufferedReader().readText())
        }
        run(true)
        assertEquals("other/.Service:third/.Service:example/example.Service", state.readText())
        assertEquals("1", flag.readText())
        run(false)
        assertEquals("other/.Service:third/.Service", state.readText())
    }
}
