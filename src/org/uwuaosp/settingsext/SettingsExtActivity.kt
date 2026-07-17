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

package org.uwuaosp.settingsext

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsIllustrationHeader
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.rememberSettingsTypography
import org.uwuaosp.settingsext.attestation.KeyAttestationSettingsActivity
import org.uwuaosp.settingsext.appjump.AppJumpSettingsActivity
import org.uwuaosp.settingsext.lyric.LyricSecureSettings
import org.uwuaosp.settingsext.lyric.LyricSettingsActivity
import org.uwuaosp.settingsext.moment.MomentSettingsActivity
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSettingsActivity
import org.uwuaosp.settingsext.util.FeatureUtils

class SettingsExtActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                SettingsExtHomeScreen(onNavigateUp = ::finish)
            }
        }
    }

}

private const val AI_CORE_PACKAGE = "org.uwuaosp.aicore"
private const val AI_CORE_ACTIVITY = "org.uwuaosp.aicore.AiSettingsActivity"
private const val LAUNCHER_PACKAGE = "com.android.launcher3"

@Composable
private fun SettingsExtTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberSettingsTypography(),
        content = content,
    )
}

@Composable
private fun SettingsExtHomeScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var lyricEnabled by remember {
        mutableStateOf(LyricSecureSettings.isEnabled(context, false))
    }

    SettingsScaffold(
        title = stringResource(R.string.app_name),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsIllustrationHeader(
            imageRes = R.drawable.settings_ext_header_image,
            height = 180.dp,
        )

        SettingsCategory(title = stringResource(R.string.settings_ext_category_privacy))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_key_attestation_title),
            summary = stringResource(R.string.settings_ext_key_attestation_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_spoofing)
            },
            position = PreferencePosition.Top,
            onClick = {
                context.startActivity(Intent(context, KeyAttestationSettingsActivity::class.java))
            },
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.settings_ext_app_jump_title),
            summary = stringResource(R.string.settings_ext_app_jump_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_appjump)
            },
            position = PreferencePosition.Bottom,
            onClick = {
                context.startActivity(AppJumpSettingsActivity.createIntent(context))
            },
        )

        if (FeatureUtils.isMomentSettingsEnabled(context)) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCategory(title = stringResource(R.string.moment_settings_title))
            PreferenceRow(
                title = stringResource(R.string.moment_settings_title),
                summary = stringResource(R.string.moment_settings_summary),
                iconContent = {
                    SettingsHomepageIcon(iconRes = R.drawable.ic_moment)
                },
                onClick = {
                    context.startActivity(Intent(context, MomentSettingsActivity::class.java))
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_smart_suggestions))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_smart_suggestions_title),
            summary = stringResource(R.string.settings_ext_smart_suggestions_summary),
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_smart_suggestions)
            },
            onClick = {
                context.startActivity(Intent(context, SmartSuggestionsSettingsActivity::class.java))
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_launcher))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_launcher_settings_title),
            summary = "",
            showSummary = false,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_launcher_settings)
            },
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(LAUNCHER_PACKAGE),
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_lyric))
        PrimarySwitchPreferenceRow(
            title = stringResource(R.string.settings_ext_lyric_fetch_title),
            summary = stringResource(R.string.settings_ext_lyric_fetch_summary),
            checked = lyricEnabled,
            onCheckedChange = { enabled ->
                lyricEnabled = enabled
                LyricSecureSettings.setEnabled(context, enabled)
            },
            onClick = {
                context.startActivity(Intent(context, LyricSettingsActivity::class.java))
            },
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_statusbarlyric)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_ai))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_ai_core_title),
            summary = "",
            showSummary = false,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_ai)
            },
            onClick = {
                context.startActivity(
                    Intent().setComponent(ComponentName(AI_CORE_PACKAGE, AI_CORE_ACTIVITY)),
                )
            },
        )
    }
}
