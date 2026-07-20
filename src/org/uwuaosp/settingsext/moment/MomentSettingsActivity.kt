/*
 * Copyright (C) 2026 The uwuAOSP Project
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

package org.uwuaosp.settingsext.moment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.MainSwitchPreference
import org.uwuaosp.compose.settingslib.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.compose.settingslib.rememberSettingsTypography
import org.uwuaosp.settingsext.R

class MomentSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomentSettingsTheme {
                MomentSettingsScreen(onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun MomentSettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme =
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberSettingsTypography(),
        content = content,
    )
}

@Composable
private fun MomentSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var momentEnabled by remember {
        mutableStateOf(MomentSecureSettings.isEnabled(context, false))
    }
    var arcGestureEnabled by remember {
        mutableStateOf(MomentSecureSettings.isArcGestureEnabled(context, true))
    }
    var navHandleDoubleTapEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true))
    }
    val navHandleDoubleTapSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        navHandleDoubleTapEnabled =
            MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true)
    }

    SettingsScaffold(
        title = stringResource(R.string.moment_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        MainSwitchPreference(
            title = stringResource(R.string.moment_enabled_title),
            checked = momentEnabled,
            onCheckedChange = { enabled ->
                momentEnabled = enabled
                MomentSecureSettings.setEnabled(context, enabled)
            },
        )

        PrimarySwitchPreferenceRow(
            title = stringResource(R.string.moment_nav_handle_double_tap_title),
            summary = stringResource(R.string.moment_nav_handle_double_tap_summary),
            checked = navHandleDoubleTapEnabled,
            onCheckedChange = { enabled ->
                navHandleDoubleTapEnabled = enabled
                MomentSecureSettings.setNavHandleDoubleTapEnabled(context, enabled)
            },
            onClick = {
                navHandleDoubleTapSettingsLauncher.launch(
                    Intent(context, NavHandleDoubleTapSettingsActivity::class.java),
                )
            },
            enabled = momentEnabled,
        )

        SettingsCategory(title = stringResource(R.string.moment_arc_section))
        SwitchPreferenceRow(
            title = stringResource(R.string.moment_arc_gesture_title),
            summary = stringResource(R.string.moment_arc_gesture_summary),
            checked = arcGestureEnabled,
            onCheckedChange = { enabled ->
                arcGestureEnabled = enabled
                MomentSecureSettings.setArcGestureEnabled(context, enabled)
            },
            enabled = momentEnabled,
            position = PreferencePosition.Top,
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.moment_arc_editor_entry_title),
            summary = stringResource(R.string.moment_arc_editor_entry_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_moment_arc_edit)
            },
            enabled = momentEnabled,
            position = PreferencePosition.Bottom,
            onClick = {
                context.startActivity(Intent(context, MomentArcEditorActivity::class.java))
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.moment_section_experience))
        PreferenceRow(
            title = stringResource(R.string.moment_launch_title),
            summary = stringResource(R.string.moment_launch_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_moment)
            },
            enabled = momentEnabled,
            onClick = {
                MomentAllAppsActivity.startInMoment(context)
            },
        )
    }
}
