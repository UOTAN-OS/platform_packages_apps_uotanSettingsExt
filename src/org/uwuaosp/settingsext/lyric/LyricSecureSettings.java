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

package org.uwuaosp.settingsext.lyric;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

public final class LyricSecureSettings {
    private static final String KEY_ALLOWED_PACKAGES = "status_bar_lyric_allowed_packages";
    public static final int POSITION_OVERLAY = 0;
    public static final int POSITION_CLOCK_RIGHT = 1;

    private LyricSecureSettings() {
    }

    public static void setEnabled(Context context, boolean enabled) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_SHOW_LYRIC, enabled ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean isEnabled(Context context, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_SHOW_LYRIC, defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void setPosition(Context context, int position) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_POSITION, position);
        } catch (Exception ignored) {
        }
    }

    public static int getPosition(Context context, int defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_POSITION, defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void setShowTranslation(Context context, boolean enabled) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_SHOW_TRANSLATION, enabled ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean isShowTranslationEnabled(Context context, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_SHOW_TRANSLATION,
                    defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void setHideIconOnClockRight(Context context, boolean enabled) {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_HIDE_ICON_CLOCK_RIGHT, enabled ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean isHideIconOnClockRightEnabled(Context context, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.STATUS_BAR_LYRIC_HIDE_ICON_CLOCK_RIGHT,
                    defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void setAllowedPackages(Context context, List<String> packages) {
        try {
            Settings.Secure.putString(context.getContentResolver(),
                    KEY_ALLOWED_PACKAGES, joinFiltered(packages, ";"));
        } catch (Exception ignored) {
        }
    }

    public static List<String> getAllowedPackages(Context context) {
        try {
            return splitAndFilter(Settings.Secure.getString(
                    context.getContentResolver(), KEY_ALLOWED_PACKAGES), ";");
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static String getAllowedPackagesKey() {
        return KEY_ALLOWED_PACKAGES;
    }

    private static String joinFiltered(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(trimmed);
        }
        return builder.toString();
    }

    private static List<String> splitAndFilter(String value, String delimiter) {
        ArrayList<String> values = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return values;
        }
        for (String part : value.split(java.util.regex.Pattern.quote(delimiter))) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
