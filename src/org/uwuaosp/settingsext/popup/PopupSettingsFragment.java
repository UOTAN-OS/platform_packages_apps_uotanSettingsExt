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
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SliderPreference;

import org.uwuaosp.settingsext.R;

public class PopupSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_POPUP_HEADER = "popup_header";
    private static final String KEY_MANAGE_APPS = "manage_apps";
    private static final String KEY_MANAGE_APPS_EDITOR = "manage_apps_editor";
    private static final String KEY_MANAGE_NOTIFICATION_BLACKLIST = "manage_notification_blacklist";
    private static final String KEY_EXPERIENCE_POPUP_VIEW = "experience_popup_view";
    private static final String KEY_POPUP_GESTURE = "popup_gesture";
    private static final String KEY_NOTIFICATION_JUMP_PORTRAIT = "popup_notif_jump_portrait";
    private static final String KEY_NOTIFICATION_JUMP_LANDSCAPE = "popup_notif_jump_landscape";
    private static final String KEY_ALLOW_MULTIPLE_POPUP_VIEWS = "allow_multiple_popup_views";
    private static final String KEY_POPUP_VIEW_NOTIFS = "popup_view_notifs";
    private static final String KEY_GESTURE_AREA_WIDTH = "gesture_area_width";
    private static final String KEY_GESTURE_AREA_HEIGHT = "gesture_area_height";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.popup_settings);
        bindHeaderPreference();
        bindActionPreferences();
        bindSwitchPreferences();
        bindSliderPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncStates();
    }

    private void bindHeaderPreference() {
        IllustrationPreference headerPreference = findPreference(KEY_POPUP_HEADER);
        if (headerPreference == null) {
            return;
        }
        headerPreference.setPersistent(false);
        headerPreference.setImageDrawable(requireContext().getDrawable(
                R.drawable.popup_view_header_image));
    }

    private void bindActionPreferences() {
        Preference manageApps = findPreference(KEY_MANAGE_APPS);
        if (manageApps != null) {
            manageApps.setPersistent(false);
            manageApps.setOnPreferenceClickListener(preference -> {
                startAppSelectionActivity(PopupSystemSettings.SELECTION_MODE_QUICK_MENU);
                return true;
            });
        }

        Preference manageAppsEditor = findPreference(KEY_MANAGE_APPS_EDITOR);
        if (manageAppsEditor != null) {
            manageAppsEditor.setPersistent(false);
            manageAppsEditor.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), QuickMenuEditorActivity.class));
                return true;
            });
        }

        Preference manageNotificationBlacklist = findPreference(KEY_MANAGE_NOTIFICATION_BLACKLIST);
        if (manageNotificationBlacklist != null) {
            manageNotificationBlacklist.setPersistent(false);
            manageNotificationBlacklist.setOnPreferenceClickListener(preference -> {
                startAppSelectionActivity(PopupSystemSettings.SELECTION_MODE_NOTIFICATION_BLACKLIST);
                return true;
            });
        }

        Preference experiencePopUpView = findPreference(KEY_EXPERIENCE_POPUP_VIEW);
        if (experiencePopUpView != null) {
            experiencePopUpView.setPersistent(false);
            experiencePopUpView.setOnPreferenceClickListener(preference -> {
                AllAppsActivity.startInPopup(requireContext());
                return true;
            });
        }
    }

    private void bindSwitchPreferences() {
        Context context = requireContext();

        bindSwitch(KEY_POPUP_GESTURE, enabled ->
                PopupSystemSettings.setPopupGestureEnabled(context, enabled));
        bindSwitch(KEY_NOTIFICATION_JUMP_PORTRAIT, enabled ->
                PopupSystemSettings.setNotificationJumpPortraitEnabled(context, enabled));
        bindSwitch(KEY_NOTIFICATION_JUMP_LANDSCAPE, enabled ->
                PopupSystemSettings.setNotificationJumpLandscapeEnabled(context, enabled));
        bindSwitch(KEY_ALLOW_MULTIPLE_POPUP_VIEWS, enabled ->
                PopupSystemSettings.setAllowMultiplePopupViewsEnabled(context, enabled));

        PopupSystemSettings.ensureNotificationLaunchMode(context);

        bindSwitch(KEY_POPUP_VIEW_NOTIFS, enabled -> {
            if (enabled) {
                PopupSystemSettings.setNotificationLaunchMode(
                        context, PopupSystemSettings.MODE_POPUP_VIEW);
            }
            PopupSystemSettings.setPopupViewNotifsEnabled(context, enabled);
        });
    }

    private void bindSliderPreferences() {
        Context context = requireContext();
        bindSlider(KEY_GESTURE_AREA_WIDTH, value ->
                PopupSystemSettings.setGestureAreaWidth(context, value));
        bindSlider(KEY_GESTURE_AREA_HEIGHT, value ->
                PopupSystemSettings.setGestureAreaHeight(context, value));
    }

    private void syncStates() {
        Context context = requireContext();

        syncSwitch(KEY_POPUP_GESTURE,
                PopupSystemSettings.isPopupGestureEnabled(context, false));
        syncSwitch(KEY_NOTIFICATION_JUMP_PORTRAIT,
                PopupSystemSettings.isNotificationJumpPortraitEnabled(context, false));
        syncSwitch(KEY_NOTIFICATION_JUMP_LANDSCAPE,
                PopupSystemSettings.isNotificationJumpLandscapeEnabled(context, false));
        syncSwitch(KEY_ALLOW_MULTIPLE_POPUP_VIEWS,
                PopupSystemSettings.isAllowMultiplePopupViewsEnabled(context, true));
        syncSwitch(KEY_POPUP_VIEW_NOTIFS,
                PopupSystemSettings.isPopupViewNotifsEnabled(context, false));

        syncSlider(KEY_GESTURE_AREA_WIDTH,
                Math.round(PopupSystemSettings.getGestureAreaWidth(context, 20f)));
        syncSlider(KEY_GESTURE_AREA_HEIGHT,
                Math.round(PopupSystemSettings.getGestureAreaHeight(context, 20f)));
    }

    private void bindSwitch(String key, BooleanConsumer onChange) {
        SwitchPreferenceCompat preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            onChange.accept((Boolean) newValue);
            return true;
        });
    }

    private void syncSwitch(String key, boolean value) {
        SwitchPreferenceCompat preference = findPreference(key);
        if (preference != null) {
            preference.setPersistent(false);
            preference.setChecked(value);
        }
    }

    private void bindSlider(String key, FloatConsumer onChange) {
        SliderPreference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener((pref, newValue) -> {
            onChange.accept(((Integer) newValue).floatValue());
            return true;
        });
    }

    private void syncSlider(String key, int value) {
        SliderPreference preference = findPreference(key);
        if (preference != null) {
            preference.setPersistent(false);
            int clamped = Math.max(preference.getMin(), Math.min(preference.getMax(), value));
            preference.setValue(clamped);
        }
    }

    private void startAppSelectionActivity(String mode) {
        Intent intent = new Intent(requireContext(), AppSelectionActivity.class)
                .putExtra(AppSelectionActivity.EXTRA_SELECTION_MODE, mode);
        startActivity(intent);
    }

    private interface BooleanConsumer {
        void accept(boolean value);
    }

    private interface FloatConsumer {
        void accept(float value);
    }
}
