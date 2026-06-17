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

package org.uwuaosp.settingsext.util;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class SettingsUtils {

    private SettingsUtils() {
    }

    // Secure Settings
    public static void putSecureBoolean(Context context, String key, boolean value) {
        try {
            Settings.Secure.putInt(context.getContentResolver(), key, value ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean getSecureBoolean(Context context, String key, boolean defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(),
                    key, defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void putSecureInt(Context context, String key, int value) {
        try {
            Settings.Secure.putInt(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    public static int getSecureInt(Context context, String key, int defaultValue) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), key, defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void putSecureString(Context context, String key, String value) {
        try {
            Settings.Secure.putString(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    public static String getSecureString(Context context, String key) {
        try {
            return Settings.Secure.getString(context.getContentResolver(), key);
        } catch (Exception ignored) {
            return null;
        }
    }

    // System Settings
    public static void putSystemBoolean(Context context, String key, boolean value) {
        try {
            Settings.System.putInt(context.getContentResolver(), key, value ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    public static boolean getSystemBoolean(Context context, String key, boolean defaultValue) {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    key, defaultValue ? 1 : 0) == 1;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void putSystemInt(Context context, String key, int value) {
        try {
            Settings.System.putInt(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    public static int getSystemInt(Context context, String key, int defaultValue) {
        try {
            return Settings.System.getInt(context.getContentResolver(), key, defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void putSystemFloat(Context context, String key, float value) {
        try {
            Settings.System.putFloat(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    public static float getSystemFloat(Context context, String key, float defaultValue) {
        try {
            return Settings.System.getFloat(context.getContentResolver(), key, defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static void putSystemString(Context context, String key, String value) {
        try {
            Settings.System.putString(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    public static String getSystemString(Context context, String key) {
        try {
            return Settings.System.getString(context.getContentResolver(), key);
        } catch (Exception ignored) {
            return null;
        }
    }

    // List helpers
    public static String joinList(List<String> values, String delimiter) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(delimiter);
            builder.append(value.trim());
        }
        return builder.toString();
    }

    public static List<String> splitList(String value, String delimiter) {
        ArrayList<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) return result;
        for (String part : value.split(Pattern.quote(delimiter))) {
            if (part != null && !part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
