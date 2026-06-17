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

import org.uwuaosp.settingsext.util.SettingsUtils;

import java.util.List;

final class PopupSystemSettings {
    static final String MODE_POPUP_VIEW = "popup_view";
    static final String SELECTION_MODE_QUICK_MENU = "quick_menu";
    static final String SELECTION_MODE_NOTIFICATION_BLACKLIST = "notification_blacklist";

    private PopupSystemSettings() {
    }

    static void setPopupGestureEnabled(Context context, boolean enabled) {
        SettingsUtils.putSystemBoolean(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_ENABLED, enabled);
    }

    static boolean isPopupGestureEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSystemBoolean(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_ENABLED, defaultValue);
    }

    static void setGestureAreaWidth(Context context, float value) {
        SettingsUtils.putSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_WIDTH_DP, value);
    }

    static float getGestureAreaWidth(Context context, float defaultValue) {
        return SettingsUtils.getSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_WIDTH_DP, defaultValue);
    }

    static void setGestureAreaHeight(Context context, float value) {
        SettingsUtils.putSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_HEIGHT_DP, value);
    }

    static float getGestureAreaHeight(Context context, float defaultValue) {
        return SettingsUtils.getSystemFloat(context, Settings.System.POP_UP_VIEW_QUICK_MENU_GESTURE_AREA_HEIGHT_DP, defaultValue);
    }

    static void setPopupViewNotifsEnabled(Context context, boolean enabled) {
        SettingsUtils.putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_ENTRY_ENABLED, enabled);
    }

    static boolean isPopupViewNotifsEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_ENTRY_ENABLED, defaultValue);
    }

    static void setNotificationJumpPortraitEnabled(Context context, boolean enabled) {
        SettingsUtils.putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_PORTRAIT, enabled);
    }

    static boolean isNotificationJumpPortraitEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_PORTRAIT, defaultValue);
    }

    static void setNotificationJumpLandscapeEnabled(Context context, boolean enabled) {
        SettingsUtils.putSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE, enabled);
    }

    static boolean isNotificationJumpLandscapeEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSystemBoolean(context, Settings.System.POP_UP_NOTIFICATION_JUMP_LANDSCAPE, defaultValue);
    }

    static void setNotificationLaunchMode(Context context, String mode) {
        SettingsUtils.putSystemString(context, Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE, mode);
    }

    static String getNotificationLaunchMode(Context context, String defaultValue) {
        String mode = SettingsUtils.getSystemString(context, Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE);
        return mode != null ? mode : defaultValue;
    }

    static void ensureNotificationLaunchMode(Context context) {
        if (SettingsUtils.getSystemString(context, Settings.System.POP_UP_NOTIFICATION_LAUNCH_MODE) == null) {
            setNotificationLaunchMode(context, MODE_POPUP_VIEW);
        }
    }

    static void setAllowMultiplePopupViewsEnabled(Context context, boolean enabled) {
        SettingsUtils.putSystemBoolean(context, Settings.System.POP_UP_VIEW_ALLOW_MULTIPLE, enabled);
    }

    static boolean isAllowMultiplePopupViewsEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSystemBoolean(context, Settings.System.POP_UP_VIEW_ALLOW_MULTIPLE, defaultValue);
    }

    static void saveSelectedApps(Context context, List<String> selectedApps) {
        SettingsUtils.putSystemString(context, Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS, SettingsUtils.joinList(selectedApps, "|"));
    }

    static List<String> getSelectedApps(Context context) {
        return SettingsUtils.splitList(SettingsUtils.getSystemString(context, Settings.System.POP_UP_VIEW_QUICK_MENU_SELECTED_APPS), "|");
    }

    static void saveQuickMenuTargets(Context context, List<String> targets) {
        saveSelectedApps(context, targets);
    }

    static List<String> getQuickMenuTargets(Context context) {
        return getSelectedApps(context);
    }

    static void saveOuterRingSelectedApps(Context context, List<String> selectedApps) {
        SettingsUtils.putSystemString(context, Settings.System.POP_UP_VIEW_QUICK_MENU_OUTER_RING_SELECTED_APPS, SettingsUtils.joinList(selectedApps, "|"));
    }

    static List<String> getOuterRingSelectedApps(Context context) {
        return SettingsUtils.splitList(SettingsUtils.getSystemString(context, Settings.System.POP_UP_VIEW_QUICK_MENU_OUTER_RING_SELECTED_APPS), "|");
    }

    static void saveOuterRingQuickMenuTargets(Context context, List<String> targets) {
        saveOuterRingSelectedApps(context, targets);
    }

    static List<String> getOuterRingQuickMenuTargets(Context context) {
        return getOuterRingSelectedApps(context);
    }

    static void saveNotificationBlacklist(Context context, List<String> packages) {
        SettingsUtils.putSystemString(context, Settings.System.POP_UP_NOTIFICATION_BLACKLIST, SettingsUtils.joinList(packages, ";"));
    }

    static List<String> getNotificationBlacklist(Context context) {
        return SettingsUtils.splitList(SettingsUtils.getSystemString(context, Settings.System.POP_UP_NOTIFICATION_BLACKLIST), ";");
    }
}
