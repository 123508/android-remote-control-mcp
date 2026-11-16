@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "LongParameterList")

package com.danielealbano.androidremotecontrolmcp.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielealbano.androidremotecontrolmcp.R
import com.danielealbano.androidremotecontrolmcp.services.shell.ShizukuState
import com.danielealbano.androidremotecontrolmcp.ui.components.TopBarBackButton
import com.danielealbano.androidremotecontrolmcp.ui.components.TopBarTitle
import com.danielealbano.androidremotecontrolmcp.ui.viewmodels.AccessibilityShizukuViewModel
import com.danielealbano.androidremotecontrolmcp.ui.viewmodels.MainViewModel
import com.danielealbano.androidremotecontrolmcp.utils.PermissionUtils

@Composable
private fun enabledColor(): Color = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF4CAF50)

@Composable
private fun disabledColor(): Color = if (isSystemInDarkTheme()) Color(0xFFEF5350) else Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    shizukuViewModel: AccessibilityShizukuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val isNotificationPermissionGranted by viewModel.isNotificationPermissionGranted.collectAsStateWithLifecycle()
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled.collectAsStateWithLifecycle()
    val isCameraPermissionGranted by viewModel.isCameraPermissionGranted.collectAsStateWithLifecycle()
    val isMicrophonePermissionGranted by viewModel.isMicrophonePermissionGranted.collectAsStateWithLifecycle()
    val isLocationPermissionGranted by viewModel.isLocationPermissionGranted.collectAsStateWithLifecycle()
    val shizukuState by shizukuViewModel.state.collectAsStateWithLifecycle()
    val shizukuBusy by shizukuViewModel.busy.collectAsStateWithLifecycle()
    val shizukuResult by shizukuViewModel.result.collectAsStateWithLifecycle()
    var confirmChange by remember { mutableStateOf<Boolean?>(null) }

    confirmChange?.let { enable ->
        AlertDialog(
            onDismissRequest = { confirmChange = null },
            title = {
                Text(
                    stringResource(if (enable) R.string.accessibility_shizuku_confirm else R.string.accessibility_shizuku_disable_confirm),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (enable) R.string.accessibility_shizuku_explanation else R.string.accessibility_shizuku_disable_explanation,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmChange = null
                    shizukuViewModel.setEnabled(enable) { viewModel.refreshPermissionStatus(context) }
                }) { Text(stringResource(R.string.accessibility_shizuku_continue)) }
            },
            dismissButton = { TextButton(onClick = { confirmChange = null }) { Text(stringResource(R.string.network_warning_cancel)) } },
        )
    }

    // Refresh permissions on ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshPermissionStatus(context)
                    shizukuViewModel.refresh()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { TopBarTitle(stringResource(R.string.settings_permissions_title)) },
            navigationIcon = { TopBarBackButton(onClick = onBack) },
            windowInsets = WindowInsets(0),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            // ---- Required ----
            PermissionSectionHeader(
                title = stringResource(R.string.permission_section_required_title),
                subtitle = stringResource(R.string.permission_section_required_subtitle),
            )
            PermissionRow(
                label = stringResource(R.string.permission_accessibility),
                rationale = stringResource(R.string.permission_accessibility_rationale),
                isEnabled = isAccessibilityEnabled,
                buttonText =
                    if (isAccessibilityEnabled) {
                        stringResource(R.string.permission_enabled)
                    } else {
                        stringResource(R.string.permission_enable)
                    },
                onAction = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                actionEnabled = !isAccessibilityEnabled,
            )

            Spacer(modifier = Modifier.height(8.dp))
            val shizukuLabel =
                when (shizukuState) {
                    ShizukuState.NotInstalled -> R.string.shell_shizuku_not_installed
                    ShizukuState.NotRunning -> R.string.shell_shizuku_not_running
                    ShizukuState.NeedsRestart -> R.string.accessibility_shizuku_prepare_description
                    ShizukuState.Outdated -> R.string.shell_shizuku_outdated
                    ShizukuState.NotGranted -> R.string.accessibility_shizuku_grant_description
                    is ShizukuState.Ready -> R.string.accessibility_shizuku_ready
                }
            Text(stringResource(shizukuLabel), style = MaterialTheme.typography.bodySmall)
            when (shizukuState) {
                ShizukuState.NeedsRestart -> {
                    OutlinedButton(onClick = shizukuViewModel::prepare) {
                        Text(stringResource(R.string.accessibility_shizuku_prepare))
                    }
                }

                ShizukuState.NotGranted -> {
                    OutlinedButton(onClick = shizukuViewModel::requestPermission) {
                        Text(stringResource(R.string.accessibility_shizuku_grant))
                    }
                }

                is ShizukuState.Ready -> {
                    OutlinedButton(
                        onClick = { confirmChange = !isAccessibilityEnabled },
                        enabled = !shizukuBusy,
                    ) {
                        Text(
                            stringResource(
                                if (isAccessibilityEnabled) {
                                    R.string.accessibility_shizuku_disable
                                } else {
                                    R.string.accessibility_shizuku_enable
                                },
                            ),
                        )
                    }
                }

                else -> {}
            }
            shizukuResult?.let { success ->
                Text(stringResource(if (success) R.string.accessibility_shizuku_success else R.string.accessibility_shizuku_failure))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Required if you use a feature ----
            PermissionSectionHeader(
                title = stringResource(R.string.permission_section_required_if_title),
                subtitle = stringResource(R.string.permission_section_required_if_subtitle),
            )
            PermissionRow(
                label = stringResource(R.string.permission_notifications),
                rationale = stringResource(R.string.permission_notifications_rationale),
                isEnabled = isNotificationPermissionGranted,
                buttonText =
                    if (isNotificationPermissionGranted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_grant)
                    },
                onAction = onRequestNotificationPermission,
                actionEnabled = !isNotificationPermissionGranted,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = stringResource(R.string.permission_notification_listener),
                rationale = stringResource(R.string.permission_notification_listener_rationale),
                isEnabled = isNotificationListenerEnabled,
                buttonText =
                    if (isNotificationListenerEnabled) {
                        stringResource(R.string.permission_enabled)
                    } else {
                        stringResource(R.string.permission_enable)
                    },
                onAction = { PermissionUtils.openNotificationListenerSettings(context) },
                actionEnabled = !isNotificationListenerEnabled,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = stringResource(R.string.permission_camera),
                rationale = stringResource(R.string.permission_camera_rationale),
                isEnabled = isCameraPermissionGranted,
                buttonText =
                    if (isCameraPermissionGranted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_grant)
                    },
                onAction = onRequestCameraPermission,
                actionEnabled = !isCameraPermissionGranted,
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionRow(
                label = stringResource(R.string.permission_location),
                rationale = stringResource(R.string.permission_location_rationale),
                isEnabled = isLocationPermissionGranted,
                buttonText =
                    if (isLocationPermissionGranted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_grant)
                    },
                onAction = onRequestLocationPermission,
                actionEnabled = !isLocationPermissionGranted,
            )

            // Background Location is geofence-only; rendered via a flavor seam (gms only).
            // The gms seam owns its own leading spacer so foss shows no dangling gap.
            BackgroundLocationPermissionRow()

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Optional ----
            PermissionSectionHeader(
                title = stringResource(R.string.permission_section_optional_title),
                subtitle = stringResource(R.string.permission_section_optional_subtitle),
            )
            PermissionRow(
                label = stringResource(R.string.permission_microphone),
                rationale = stringResource(R.string.permission_microphone_rationale),
                isEnabled = isMicrophonePermissionGranted,
                buttonText =
                    if (isMicrophonePermissionGranted) {
                        stringResource(R.string.permission_granted)
                    } else {
                        stringResource(R.string.permission_grant)
                    },
                onAction = onRequestMicrophonePermission,
                actionEnabled = !isMicrophonePermissionGranted,
            )
        }
    }
}

@Composable
internal fun PermissionRow(
    label: String,
    rationale: String,
    isEnabled: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    actionEnabled: Boolean,
    spokenButtonText: String = buttonText,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative: the button says the state ("Granted", "Grant", ...), or a [spokenButtonText] does
        Icon(
            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (isEnabled) enabledColor() else disabledColor(),
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        val actionDescription = stringResource(R.string.permission_action_description, label, spokenButtonText)
        OutlinedButton(onClick = onAction, enabled = actionEnabled) {
            // On the text: the button reads its content, and the description replaces the bare "Grant"
            Text(text = buttonText, modifier = Modifier.semantics { contentDescription = actionDescription })
        }
    }
}

@Composable
private fun PermissionSectionHeader(
    title: String,
    subtitle: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
}
