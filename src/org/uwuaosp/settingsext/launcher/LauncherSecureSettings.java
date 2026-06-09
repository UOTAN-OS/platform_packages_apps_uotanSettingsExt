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

package org.uwuaosp.settingsext.launcher;

import android.content.Context;
import android.provider.Settings;

public final class LauncherSecureSettings {

    private LauncherSecureSettings() {
    }

    public static void setAllAppsThemedIconsEnabled(Context context, boolean enabled) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LAUNCHER_ALLAPPS_THEMED_ICONS, enabled ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean isAllAppsThemedIconsEnabled(Context context, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LAUNCHER_ALLAPPS_THEMED_ICONS,
                    defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static final String LENS_ICON_KEY = "launcher_lens_icon";

    public static void setLensIconEnabled(Context context, boolean enabled) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    LENS_ICON_KEY, enabled ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean isLensIconEnabled(Context context, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    LENS_ICON_KEY, defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
