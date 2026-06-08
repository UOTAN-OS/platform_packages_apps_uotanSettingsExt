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

package org.uwuaosp.settingsext.smartsuggestions;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public final class SmartSuggestionsSecureSettings {
    // Keep the existing secure keys so current user settings survive the rename.
    public static final String KEY_TORCH_ENABLED = "uwuaosp_torch_suggestion_enabled";
    public static final String KEY_MUSIC_ENABLED = "uwuaosp_music_suggestion_enabled";
    public static final String KEY_MUSIC_PACKAGE = "uwuaosp_music_suggestion_package";

    private SmartSuggestionsSecureSettings() {
    }

    public static void setTorchEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(), KEY_TORCH_ENABLED, enabled ? 1 : 0);
    }

    public static boolean isTorchEnabled(Context context, boolean defaultValue) {
        return Settings.Secure.getInt(context.getContentResolver(), KEY_TORCH_ENABLED,
                defaultValue ? 1 : 0) == 1;
    }

    public static void setMusicEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(), KEY_MUSIC_ENABLED, enabled ? 1 : 0);
    }

    public static boolean isMusicEnabled(Context context, boolean defaultValue) {
        return Settings.Secure.getInt(context.getContentResolver(), KEY_MUSIC_ENABLED,
                defaultValue ? 1 : 0) == 1;
    }

    public static void setMusicPackage(Context context, String packageName) {
        Settings.Secure.putString(context.getContentResolver(), KEY_MUSIC_PACKAGE, packageName);
    }

    public static String getMusicPackage(Context context, String defaultPackage) {
        String packageName = Settings.Secure.getString(
                context.getContentResolver(), KEY_MUSIC_PACKAGE);
        return TextUtils.isEmpty(packageName) ? defaultPackage : packageName;
    }
}
