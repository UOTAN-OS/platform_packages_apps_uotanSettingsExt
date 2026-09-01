/*
 * Copyright (C) 2026 The uwuAOSP Project
 * Copyright (C) 2026 The UotanOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uwuaosp.settingsext.background

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackgroundManagementSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                BackgroundManagementSettingsScreen(onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun BackgroundManagementSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSystemApps by remember {
        mutableStateOf(BackgroundListPreferences.showSystemApps(context))
    }
    var ignoreTaskRemoval by remember {
        mutableStateOf(BackgroundModeSecureSettings.isIgnoreTaskRemovalEnabled(context))
    }
    var exporting by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        exporting = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BackgroundLogExporter.export(context, uri) }
            }
            exporting = false
            Toast.makeText(
                context,
                if (result.isSuccess) {
                    R.string.background_log_exported
                } else {
                    R.string.background_log_export_failed
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.background_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsCategory(title = stringResource(R.string.background_display_category))
        SwitchPreferenceRow(
            title = stringResource(R.string.background_show_system_apps),
            summary = "",
            showSummary = false,
            checked = showSystemApps,
            onCheckedChange = { show ->
                showSystemApps = show
                BackgroundListPreferences.setShowSystemApps(context, show)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.background_behavior_category))
        SwitchPreferenceRow(
            title = stringResource(R.string.background_ignore_task_removal),
            summary = stringResource(R.string.background_ignore_task_removal_summary),
            showSummary = true,
            checked = ignoreTaskRemoval,
            onCheckedChange = { enabled ->
                if (BackgroundModeSecureSettings.setIgnoreTaskRemovalEnabled(context, enabled)) {
                    ignoreTaskRemoval = enabled
                } else {
                    Toast.makeText(
                        context,
                        R.string.background_setting_update_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.background_diagnostics_category))
        PreferenceRow(
            title = stringResource(R.string.background_export_logs),
            summary = "",
            showSummary = false,
            enabled = !exporting,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_background_log_description)
            },
            onClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                exportLauncher.launch("uwu-background-$timestamp.log")
            },
        )
    }
}

private object BackgroundLogExporter {
    fun export(context: Context, uri: Uri) {
        val modes = BackgroundModeSecureSettings.getModes(context)
        val logcat = ProcessBuilder(
            "logcat",
            "-d",
            "-v",
            "threadtime",
            "-s",
            "UwuAppBackground:V",
            "*:S",
        )
            .redirectErrorStream(true)
            .start()
        val output = logcat.inputStream.bufferedReader().use { it.readText() }
        val exitCode = logcat.waitFor()
        check(exitCode == 0) { "logcat exited with $exitCode" }

        val report = buildString {
            appendLine("UotanOS background management log")
            appendLine("Generated: ${Date()}")
            appendLine("Build: ${Build.DISPLAY}")
            appendLine("Fingerprint: ${Build.FINGERPRINT}")
            appendLine(
                "Ignore task removal: " +
                    BackgroundModeSecureSettings.isIgnoreTaskRemovalEnabled(context),
            )
            appendLine("Modes:")
            if (modes.isEmpty()) {
                appendLine("  (none)")
            } else {
                for (index in 0 until modes.size) {
                    append("  ")
                    append(modes.keyAt(index))
                    append('=')
                    appendLine(modeName(modes.valueAt(index)))
                }
            }
            appendLine()
            append(output)
        }
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(report)
        } ?: error("Unable to open export destination")
    }

    private fun modeName(mode: Int): String {
        return when (mode) {
            BackgroundModeSecureSettings.MODE_TOMBSTONE -> "TOMBSTONE"
            BackgroundModeSecureSettings.MODE_FULL -> "FULL"
            else -> "DEFAULT"
        }
    }
}
