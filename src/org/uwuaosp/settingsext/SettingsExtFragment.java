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

package org.uwuaosp.settingsext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.attestation.KeyAttestationSettingsActivity;
import org.uwuaosp.settingsext.appjump.AppJumpSettingsActivity;
import org.uwuaosp.settingsext.launcher.LauncherSecureSettings;
import org.uwuaosp.settingsext.lyric.LyricSecureSettings;
import org.uwuaosp.settingsext.lyric.LyricSettingsActivity;
import org.uwuaosp.settingsext.navigation.NavigationSecureSettings;
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSettingsActivity;
import org.uwuaosp.settingsext.util.IconUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SettingsExtFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_APP_JUMP_SETTINGS = "app_jump_settings";
    private static final String KEY_SETTINGS_EXT_HEADER = "settings_ext_header";
    private static final String KEY_KEY_ATTESTATION_SETTINGS = "key_attestation_settings";
    private static final String KEY_LAUNCHER_ALLAPPS_THEMED_ICONS = "launcher_allapps_themed_icons";
    private static final String KEY_LAUNCHER_LENS_ICON = "launcher_lens_icon";
    private static final String KEY_LYRIC_FETCH_SETTINGS = "lyric_fetch_settings";
    private static final String KEY_NAVIGATION_BAR_HINT = "navigation_bar_hint";
    private static final String KEY_POPUP_SETTINGS = "popup_settings";
    private static final String KEY_SMART_SUGGESTIONS_SETTINGS = "smart_suggestions_settings";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_ext);

        setupHeader();

        setupPreference(KEY_APP_JUMP_SETTINGS, R.drawable.ic_appjump,
                () -> startActivity(AppJumpSettingsActivity.createIntent(requireContext())));

        setupPreference(KEY_KEY_ATTESTATION_SETTINGS, R.drawable.ic_spoofing,
                () -> startActivity(new Intent(requireContext(), KeyAttestationSettingsActivity.class)));

        setupPreference(KEY_POPUP_SETTINGS, R.drawable.ic_popup,
                () -> startActivity(new Intent(requireContext(), SettingsExtActivity.class)
                        .putExtra(SettingsExtActivity.EXTRA_OPEN_POPUP_SETTINGS, true)));

        setupPreference(KEY_SMART_SUGGESTIONS_SETTINGS, R.drawable.ic_smart_suggestions,
                () -> startActivity(new Intent(requireContext(), SmartSuggestionsSettingsActivity.class)));

        setupSwitchPreference(KEY_LYRIC_FETCH_SETTINGS, R.drawable.ic_statusbarlyric,
                () -> LyricSecureSettings.isEnabled(requireContext(), false),
                enabled -> LyricSecureSettings.setEnabled(requireContext(), enabled),
                () -> startActivity(new Intent(requireContext(), LyricSettingsActivity.class)));

        setupSwitchPreference(KEY_NAVIGATION_BAR_HINT, 0,
                () -> NavigationSecureSettings.isNavigationBarHintEnabled(requireContext(), true),
                enabled -> NavigationSecureSettings.setNavigationBarHintEnabled(requireContext(), enabled),
                null);

        setupSwitchPreference(KEY_LAUNCHER_ALLAPPS_THEMED_ICONS, 0,
                () -> LauncherSecureSettings.isAllAppsThemedIconsEnabled(requireContext(), false),
                enabled -> LauncherSecureSettings.setAllAppsThemedIconsEnabled(requireContext(), enabled),
                null);

        setupSwitchPreference(KEY_LAUNCHER_LENS_ICON, 0,
                () -> LauncherSecureSettings.isLensIconEnabled(requireContext(), false),
                enabled -> LauncherSecureSettings.setLensIconEnabled(requireContext(), enabled),
                null);
    }

    private void setupHeader() {
        IllustrationPreference header = findPreference(KEY_SETTINGS_EXT_HEADER);
        if (header != null) {
            header.setPersistent(false);
            header.setImageDrawable(requireContext().getDrawable(R.drawable.settings_ext_header_image));
        }
    }

    private void setupPreference(String key, @DrawableRes int iconRes, Runnable onClick) {
        Preference pref = findPreference(key);
        if (pref != null) {
            if (iconRes != 0) {
                pref.setIcon(IconUtils.createHomeEntryIcon(requireContext(), iconRes));
            }
            pref.setOnPreferenceClickListener(p -> {
                onClick.run();
                return true;
            });
        }
    }

    private void setupSwitchPreference(String key, @DrawableRes int iconRes,
                                       Supplier<Boolean> getter, Consumer<Boolean> setter,
                                       Runnable onClick) {
        Preference pref = findPreference(key);
        if (pref == null) return;

        if (iconRes != 0) {
            pref.setIcon(IconUtils.createHomeEntryIcon(requireContext(), iconRes));
        }

        if (pref instanceof TwoStatePreference) {
            TwoStatePreference twoStatePref = (TwoStatePreference) pref;
            twoStatePref.setPersistent(false);
            twoStatePref.setChecked(getter.get());
            twoStatePref.setOnPreferenceChangeListener((p, newValue) -> {
                setter.accept((Boolean) newValue);
                return true;
            });
        } else if (pref instanceof PrimarySwitchPreference) {
            PrimarySwitchPreference primaryPref = (PrimarySwitchPreference) pref;
            primaryPref.setPersistent(false);
            primaryPref.setChecked(getter.get());
            primaryPref.setOnPreferenceChangeListener((p, newValue) -> {
                setter.accept((Boolean) newValue);
                return true;
            });
        }

        if (onClick != null) {
            pref.setOnPreferenceClickListener(p -> {
                onClick.run();
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStates();
    }

    private void updateStates() {
        updateSwitchState(KEY_LYRIC_FETCH_SETTINGS, () -> LyricSecureSettings.isEnabled(requireContext(), false));
        updateSwitchState(KEY_NAVIGATION_BAR_HINT, () -> NavigationSecureSettings.isNavigationBarHintEnabled(requireContext(), true));
        updateSwitchState(KEY_LAUNCHER_ALLAPPS_THEMED_ICONS, () -> LauncherSecureSettings.isAllAppsThemedIconsEnabled(requireContext(), false));
        updateSwitchState(KEY_LAUNCHER_LENS_ICON, () -> LauncherSecureSettings.isLensIconEnabled(requireContext(), false));
    }

    private void updateSwitchState(String key, Supplier<Boolean> getter) {
        Preference pref = findPreference(key);
        if (pref instanceof TwoStatePreference) {
            ((TwoStatePreference) pref).setChecked(getter.get());
        } else if (pref instanceof PrimarySwitchPreference) {
            ((PrimarySwitchPreference) pref).setChecked(getter.get());
        }
    }
}
