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

package org.uwuaosp.settingsext.popup;

import android.content.Context;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

final class PopupSystemSettings {
    static final String MODE_POPUP_VIEW = "popup_view";
    static final String SELECTION_MODE_QUICK_MENU = "quick_menu";
    static final String SELECTION_MODE_NOTIFICATION_BLACKLIST = "notification_blacklist";

    private PopupSystemSettings() {
    }

    static void setPopupGestureEnabled(Context context, boolean enabled) {
        putSystemBoolean(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_ENABLED, enabled);
    }

    static boolean isPopupGestureEnabled(Context context, boolean defaultValue) {
        return getBooleanSetting(context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_ENABLED, defaultValue);
    }

    static void setGestureAreaWidth(Context context, float value) {
        putSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_WIDTH_DP, value);
    }

    static float getGestureAreaWidth(Context context, float defaultValue) {
        return getFloatSetting(context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_WIDTH_DP, defaultValue);
    }

    static void setGestureAreaHeight(Context context, float value) {
        putSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_HEIGHT_DP, value);
    }

    static float getGestureAreaHeight(Context context, float defaultValue) {
        return getFloatSetting(context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_HEIGHT_DP, defaultValue);
    }

    static void setPopupViewNotifsEnabled(Context context, boolean enabled) {
        putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_ENTRY_ENABLED, enabled);
    }

    static boolean isPopupViewNotifsEnabled(Context context, boolean defaultValue) {
        return getBooleanSetting(context,
                Settings.System.POP_UP_NOTIFICATION_ENTRY_ENABLED, defaultValue);
    }

    static void setNotificationJumpPortraitEnabled(Context context, boolean enabled) {
        putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_PORTRAIT, enabled);
    }

    static boolean isNotificationJumpPortraitEnabled(Context context, boolean defaultValue) {
        return getBooleanSetting(context,
                Settings.System.POP_UP_NOTIFICATION_JUMP_PORTRAIT, defaultValue);
    }

    static void setNotificationJumpLandscapeEnabled(Context context, boolean enabled) {
        putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE, enabled);
    }

    static boolean isNotificationJumpLandscapeEnabled(Context context, boolean defaultValue) {
        return getBooleanSetting(context,
                Settings.System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE, defaultValue);
    }

    static void setNotificationLaunchMode(Context context, String mode) {
        putSystemString(context, Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE, mode);
    }

    static String getNotificationLaunchMode(Context context, String defaultValue) {
        return getStringSetting(context,
                Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE, defaultValue);
    }

    static void ensureNotificationLaunchMode(Context context) {
        if (getSystemRawString(context, Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE) == null) {
            setNotificationLaunchMode(context, getNotificationLaunchMode(context, MODE_POPUP_VIEW));
        }
    }

    static void setAllowMultiplePopupViewsEnabled(Context context, boolean enabled) {
        putSystemBoolean(context, Settings.System.POP_UP_VIEW_ALLOW_MULTIPLE, enabled);
    }

    static boolean isAllowMultiplePopupViewsEnabled(Context context, boolean defaultValue) {
        return getBooleanSetting(context,
                Settings.System.POP_UP_VIEW_ALLOW_MULTIPLE, defaultValue);
    }

    static void saveSelectedApps(Context context, List<String> selectedApps) {
        putSystemString(
                context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS,
                joinFiltered(selectedApps, "|"));
    }

    static List<String> getSelectedApps(Context context) {
        return splitAndFilter(getStringSetting(
                context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS,
                ""
        ), "|");
    }

    static void saveQuickMenuTargets(Context context, List<String> targets) {
        putSystemString(
                context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS,
                joinFiltered(targets, "|"));
    }

    static List<String> getQuickMenuTargets(Context context) {
        return splitAndFilter(getStringSetting(
                context,
                Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS,
                ""
        ), "|");
    }

    static void saveNotificationBlacklist(Context context, List<String> packages) {
        putSystemString(
                context,
                Settings.System.POP_UP_NOTIFICATION_BLACKLIST,
                joinFiltered(packages, ";"));
    }

    static List<String> getNotificationBlacklist(Context context) {
        return splitAndFilter(getStringSetting(
                context,
                Settings.System.POP_UP_NOTIFICATION_BLACKLIST,
                ""
        ), ";");
    }

    private static String getSystemRawString(Context context, String key) {
        try {
            return Settings.System.getString(context.getContentResolver(), key);
        } catch (Exception e) {
            return null;
        }
    }

    private static void putSystemString(Context context, String key, String value) {
        try {
            Settings.System.putString(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    private static void putSystemBoolean(Context context, String key, boolean value) {
        try {
            Settings.System.putInt(context.getContentResolver(), key, value ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    private static void putSystemFloat(Context context, String key, float value) {
        try {
            Settings.System.putFloat(context.getContentResolver(), key, value);
        } catch (Exception ignored) {
        }
    }

    private static Boolean parseBoolean(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        switch (rawValue.toLowerCase()) {
            case "1":
            case "true":
                return true;
            case "0":
            case "false":
                return false;
            default:
                return null;
        }
    }

    private static Float parseFloat(String rawValue) {
        try {
            return rawValue == null || rawValue.trim().isEmpty()
                    ? null : Float.parseFloat(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean getBooleanSetting(Context context, String key, boolean defaultValue) {
        Boolean stored = parseBoolean(getSystemRawString(context, key));
        if (stored != null) {
            return stored;
        }
        putSystemBoolean(context, key, defaultValue);
        return defaultValue;
    }

    private static float getFloatSetting(Context context, String key, float defaultValue) {
        Float stored = parseFloat(getSystemRawString(context, key));
        if (stored != null) {
            return stored;
        }
        putSystemFloat(context, key, defaultValue);
        return defaultValue;
    }

    private static String getStringSetting(Context context, String key, String defaultValue) {
        String stored = getSystemRawString(context, key);
        if (stored != null) {
            return stored;
        }
        putSystemString(context, key, defaultValue);
        return defaultValue;
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
