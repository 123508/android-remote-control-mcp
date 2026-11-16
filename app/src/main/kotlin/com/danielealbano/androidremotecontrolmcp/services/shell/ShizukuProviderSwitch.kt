package com.danielealbano.androidremotecontrolmcp.services.shell

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.ShizukuProvider
import javax.inject.Inject

interface ShizukuProviderSwitch {
    fun setEnabled(enabled: Boolean)

    fun enabledAtProcessStart(): Boolean

    fun enableForAccessibility()
}

class ShizukuProviderSwitchImpl internal constructor(
    private val packageManager: PackageManager,
    private val provider: ComponentName,
    private val accessibilityEnabled: () -> Boolean = { false },
    private val saveAccessibilityEnabled: () -> Unit = {},
) : ShizukuProviderSwitch {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        context.packageManager,
        ComponentName(context, ShizukuProvider::class.java),
        { context.getSharedPreferences("shizuku_accessibility", Context.MODE_PRIVATE).getBoolean("enabled", false) },
        {
            context
                .getSharedPreferences("shizuku_accessibility", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enabled", true)
                .commit()
        },
    )

    override fun setEnabled(enabled: Boolean) {
        val state =
            if (enabled || accessibilityEnabled()) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        packageManager.setComponentEnabledSetting(provider, state, PackageManager.DONT_KILL_APP)
    }

    override fun enabledAtProcessStart(): Boolean = enabledAtStart

    override fun enableForAccessibility() {
        saveAccessibilityEnabled()
        setEnabled(true)
    }

    companion object {
        @Volatile
        private var enabledAtStart = false

        fun recordProcessStart(context: Context) {
            val provider = ComponentName(context, ShizukuProvider::class.java)
            enabledAtStart =
                context.packageManager.getComponentEnabledSetting(provider) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }
}
