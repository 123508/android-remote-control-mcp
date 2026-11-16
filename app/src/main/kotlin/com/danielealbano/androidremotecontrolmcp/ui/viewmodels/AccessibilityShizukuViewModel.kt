package com.danielealbano.androidremotecontrolmcp.ui.viewmodels

import android.content.ComponentName
import android.content.Context
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielealbano.androidremotecontrolmcp.services.accessibility.McpAccessibilityService
import com.danielealbano.androidremotecontrolmcp.services.shell.AppRestarter
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuApi
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuProviderSwitch
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuShellBackend
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuState
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuStatusProvider
import com.danielealbano.androidremotecontrolmcp.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import javax.inject.Inject

@HiltViewModel
class AccessibilityShizukuViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val api: ShizukuApi,
        private val status: ShizukuStatusProvider,
        private val provider: ShizukuProviderSwitch,
        private val restarter: AppRestarter,
    ) : ViewModel() {
        private val backend = ShizukuShellBackend(context, api, "accessibility")
        private val mutableState = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
        val state = mutableState.asStateFlow()
        private val mutableBusy = MutableStateFlow(false)
        val busy = mutableBusy.asStateFlow()
        private val mutableResult = MutableStateFlow<Boolean?>(null)
        val result = mutableResult.asStateFlow()
        private val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

        init {
            api.addPermissionResultListener(listener)
            refresh()
        }

        fun refresh() {
            mutableState.value = status.state()
        }

        fun prepare() {
            provider.enableForAccessibility()
            restarter.restart()
        }

        fun requestPermission() {
            runCatching { api.requestPermission(6802) }.onFailure { mutableResult.value = false }
        }

        fun setEnabled(
            enabled: Boolean,
            onChanged: () -> Unit,
        ) {
            if (mutableBusy.value) return
            mutableBusy.value = true
            mutableResult.value = null
            viewModelScope.launch {
                val success = runCatching { changeAccessibility(enabled) }.getOrDefault(false)
                mutableResult.value = success
                mutableBusy.value = false
                onChanged()
            }
        }

        private suspend fun changeAccessibility(enabled: Boolean): Boolean =
            withContext(Dispatchers.IO) {
                val component = ComponentName(context, McpAccessibilityService::class.java)
                val full = component.flattenToString()
                val short = component.flattenToShortString()
                val user = Process.myUid() / 100000
                val script = accessibilitySettingsScript(user, full, short, enabled)
                val process = backend.spawn(1, script)
                try {
                    val exit =
                        withTimeout(15000) {
                            process.output.use { it.readBytes() }
                            process.exit.await()
                        }
                    if (exit.code != 0) return@withContext false
                    repeat(30) {
                        val listed = PermissionUtils.isAccessibilityServiceEnabled(context, McpAccessibilityService::class.java)
                        if (listed == enabled && (!enabled || McpAccessibilityService.instance != null)) return@withContext true
                        delay(200)
                    }
                    false
                } finally {
                    process.kill(null)
                    backend.release(1)
                }
            }

        override fun onCleared() {
            api.removePermissionResultListener(listener)
            backend.releaseAll()
        }
    }

internal fun accessibilitySettingsScript(
    user: Int,
    full: String,
    short: String,
    enabled: Boolean,
): String =
    """
    set -f
    current=${'$'}(settings --user $user get secure enabled_accessibility_services) || exit 1
    [ "${'$'}current" = null ] && current=""
    result=""
    oldifs=${'$'}IFS
    IFS=:
    for entry in ${'$'}current; do
        [ -z "${'$'}entry" ] && continue
        [ "${'$'}entry" = '$full' ] && continue
        [ "${'$'}entry" = '$short' ] && continue
        if [ -z "${'$'}result" ]; then result="${'$'}entry"; else result="${'$'}result:${'$'}entry"; fi
    done
    IFS=${'$'}oldifs
    ${if (enabled) "if [ -z \"${'$'}result\" ]; then result='$full'; else result=\"${'$'}result:$full\"; fi" else ":"}
    settings --user $user put secure enabled_accessibility_services "${'$'}result" || exit 1
    ${if (enabled) "settings --user $user put secure accessibility_enabled 1 || exit 1" else ":"}
    """.trimIndent()
