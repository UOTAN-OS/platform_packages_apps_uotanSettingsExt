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
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.R;
import org.uwuaosp.settingsext.apppicker.AppSelectionActivity;

import java.util.List;

public class LyricSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_MAIN_SWITCH = "status_bar_lyric_main_switch";
    private static final String KEY_NOTIFICATION_ACCESS = "notification_listener";
    private static final String KEY_ALLOWED_APPS = "allowed_packages";
    private static final String KEY_POSITION = "lyric_position";
    private static final String KEY_SHOW_TRANSLATION = "lyric_show_translation";
    private static final String KEY_HIDE_ICON_CLOCK_RIGHT = "lyric_hide_icon_clock_right";
    private static final String KEY_DETAILS = "lyric_details";
    private static final String KEY_APPS = "lyric_apps";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    private MainSwitchPreference mMainSwitchPreference;
    private PreferenceCategory mDetailsCategory;
    private PreferenceCategory mAppsCategory;
    private ListPreference mPositionPreference;
    private SwitchPreferenceCompat mShowTranslationPreference;
    private SwitchPreferenceCompat mHideClockRightIconPreference;
    private Preference mNotificationListenerPreference;
    private Preference mAllowedAppsPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.lyric_settings);

        mMainSwitchPreference = findPreference(KEY_MAIN_SWITCH);
        mDetailsCategory = findPreference(KEY_DETAILS);
        mAppsCategory = findPreference(KEY_APPS);
        mPositionPreference = findPreference(KEY_POSITION);
        mShowTranslationPreference = findPreference(KEY_SHOW_TRANSLATION);
        mHideClockRightIconPreference = findPreference(KEY_HIDE_ICON_CLOCK_RIGHT);
        mNotificationListenerPreference = findPreference(KEY_NOTIFICATION_ACCESS);
        mAllowedAppsPreference = findPreference(KEY_ALLOWED_APPS);

        if (mMainSwitchPreference != null) {
            mMainSwitchPreference.setPersistent(false);
            mMainSwitchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    LyricSecureSettings.setEnabled(context, (Boolean) newValue);
                    updateDetailPreferencesEnabled((Boolean) newValue);
                }
                return true;
            });
        }

        if (mPositionPreference != null) {
            mPositionPreference.setPersistent(false);
            mPositionPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    int position = Integer.parseInt(String.valueOf(newValue));
                    LyricSecureSettings.setPosition(context, position);
                    updatePositionState(position, LyricSecureSettings.isEnabled(context, false));
                }
                return true;
            });
        }

        if (mShowTranslationPreference != null) {
            mShowTranslationPreference.setPersistent(false);
            mShowTranslationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    LyricSecureSettings.setShowTranslation(context, (Boolean) newValue);
                }
                return true;
            });
        }

        if (mHideClockRightIconPreference != null) {
            mHideClockRightIconPreference.setPersistent(false);
            mHideClockRightIconPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    LyricSecureSettings.setHideIconOnClockRight(context, (Boolean) newValue);
                }
                return true;
            });
        }

        if (mNotificationListenerPreference != null) {
            mNotificationListenerPreference.setPersistent(false);
            mNotificationListenerPreference.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                return true;
            });
        }

        if (mAllowedAppsPreference != null) {
            mAllowedAppsPreference.setPersistent(false);
            mAllowedAppsPreference.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), AppSelectionActivity.class)
                        .putExtra(AppSelectionActivity.EXTRA_SELECTION_MODE,
                                AppSelectionActivity.SELECTION_MODE_LYRIC_WHITELIST));
                return true;
            });
        }

        syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncState();
    }

    private void syncState() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        boolean enabled = LyricSecureSettings.isEnabled(context, false);
        if (mMainSwitchPreference != null) {
            mMainSwitchPreference.setChecked(enabled);
        }
        syncPositionState(context);
        syncToggleState(context);
        updateDetailPreferencesEnabled(enabled);
        updateNotificationAccessSummary(context);
        updateAllowedAppsSummary(context);
    }

    private void updateDetailPreferencesEnabled(boolean enabled) {
        if (mDetailsCategory != null) {
            mDetailsCategory.setEnabled(enabled);
        }
        if (mAppsCategory != null) {
            mAppsCategory.setEnabled(enabled);
        }
        if (mNotificationListenerPreference != null) {
            mNotificationListenerPreference.setEnabled(enabled);
        }
        if (mPositionPreference != null) {
            mPositionPreference.setEnabled(enabled);
        }
        if (mShowTranslationPreference != null) {
            mShowTranslationPreference.setEnabled(enabled);
        }
        updateClockRightIconPreferenceEnabled(enabled, getSelectedPosition());
        if (mAllowedAppsPreference != null) {
            mAllowedAppsPreference.setEnabled(enabled);
        }
    }

    private void syncPositionState(Context context) {
        int position = LyricSecureSettings.getPosition(
                context, LyricSecureSettings.POSITION_OVERLAY);
        updatePositionState(position, LyricSecureSettings.isEnabled(context, false));
    }

    private void syncToggleState(Context context) {
        if (mShowTranslationPreference != null) {
            mShowTranslationPreference.setChecked(
                    LyricSecureSettings.isShowTranslationEnabled(context, false));
        }
        if (mHideClockRightIconPreference != null) {
            mHideClockRightIconPreference.setChecked(
                    LyricSecureSettings.isHideIconOnClockRightEnabled(context, false));
        }
    }

    private void updatePositionState(int position, boolean enabled) {
        if (mPositionPreference != null) {
            mPositionPreference.setValue(String.valueOf(position));
            updatePositionSummary(position);
        }
        updateClockRightIconPreferenceEnabled(enabled, position);
    }

    private void updatePositionSummary(int position) {
        if (mPositionPreference == null) {
            return;
        }
        mPositionPreference.setSummary(position == LyricSecureSettings.POSITION_CLOCK_RIGHT
                ? R.string.lyric_position_summary_clock_right
                : R.string.lyric_position_summary_overlay);
    }

    private void updateClockRightIconPreferenceEnabled(boolean enabled, int position) {
        if (mHideClockRightIconPreference == null) {
            return;
        }
        mHideClockRightIconPreference.setEnabled(
                enabled && position == LyricSecureSettings.POSITION_CLOCK_RIGHT);
    }

    private int getSelectedPosition() {
        if (mPositionPreference == null || mPositionPreference.getValue() == null) {
            return LyricSecureSettings.POSITION_OVERLAY;
        }
        return Integer.parseInt(mPositionPreference.getValue());
    }

    private void updateNotificationAccessSummary(Context context) {
        if (mNotificationListenerPreference == null) {
            return;
        }
        mNotificationListenerPreference.setSummary(
                isNotificationListenerEnabled(context)
                        ? R.string.lyric_notification_listener_summary_on
                        : R.string.lyric_notification_listener_summary_off);
    }

    private void updateAllowedAppsSummary(Context context) {
        if (mAllowedAppsPreference == null) {
            return;
        }
        List<String> packages = LyricSecureSettings.getAllowedPackages(context);
        if (packages.isEmpty()) {
            mAllowedAppsPreference.setSummary(R.string.lyric_whitelist_summary_empty);
            return;
        }
        mAllowedAppsPreference.setSummary(getResources().getQuantityString(
                R.plurals.lyric_whitelist_summary_count, packages.size(), packages.size()));
    }

    private boolean isNotificationListenerEnabled(Context context) {
        String flat = Settings.Secure.getString(
                context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (TextUtils.isEmpty(flat)) {
            return false;
        }
        String packageName = "cn.binbin323.statuslyricext";
        for (String name : flat.split(":")) {
            android.content.ComponentName componentName =
                    android.content.ComponentName.unflattenFromString(name);
            if (componentName != null && packageName.equals(componentName.getPackageName())) {
                return true;
            }
        }
        return false;
    }
}
