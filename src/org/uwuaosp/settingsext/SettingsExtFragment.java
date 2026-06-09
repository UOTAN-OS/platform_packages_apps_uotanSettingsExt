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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

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

public class SettingsExtFragment extends SettingsBasePreferenceFragment {
    private static final int HOME_ENTRY_ICON_SIZE_DP = 40;
    private static final int HOME_ENTRY_GLYPH_SIZE_DP = 24;
    private static final String KEY_APP_JUMP_SETTINGS = "app_jump_settings";
    private static final String KEY_SETTINGS_EXT_HEADER = "settings_ext_header";
    private static final String KEY_KEY_ATTESTATION_SETTINGS = "key_attestation_settings";
    private static final String KEY_LAUNCHER_ALLAPPS_THEMED_ICONS =
            "launcher_allapps_themed_icons";
    private static final String KEY_LAUNCHER_LENS_ICON = "launcher_lens_icon";
    private static final String KEY_LYRIC_FETCH_SETTINGS = "lyric_fetch_settings";
    private static final String KEY_NAVIGATION_BAR_HINT = "navigation_bar_hint";
    private static final String KEY_POPUP_SETTINGS = "popup_settings";
    private static final String KEY_SMART_SUGGESTIONS_SETTINGS = "smart_suggestions_settings";

    private PrimarySwitchPreference mLyricFetchPreference;
    private SwitchPreferenceCompat mLauncherAllAppsThemedIconsPreference;
    private SwitchPreferenceCompat mLauncherLensIconPreference;
    private SwitchPreferenceCompat mNavigationBarHintPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_ext);
        initHeaderPreference();

        Preference appJumpSettings = findPreference(KEY_APP_JUMP_SETTINGS);
        if (appJumpSettings != null) {
            appJumpSettings.setIcon(createHomeEntryIcon(
                    requireContext(), R.drawable.ic_appjump));
            appJumpSettings.setOnPreferenceClickListener(preference -> {
                startActivity(AppJumpSettingsActivity.createIntent(requireContext()));
                return true;
            });
        }

        Preference keyAttestationSettings = findPreference(KEY_KEY_ATTESTATION_SETTINGS);
        if (keyAttestationSettings != null) {
            keyAttestationSettings.setIcon(createHomeEntryIcon(
                    requireContext(), R.drawable.ic_spoofing));
            keyAttestationSettings.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), KeyAttestationSettingsActivity.class));
                return true;
            });
        }

        Preference popupSettings = findPreference(KEY_POPUP_SETTINGS);
        if (popupSettings != null) {
            popupSettings.setIcon(createHomeEntryIcon(
                    requireContext(), R.drawable.ic_popup));
            popupSettings.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), SettingsExtActivity.class)
                        .putExtra(SettingsExtActivity.EXTRA_OPEN_POPUP_SETTINGS, true));
                return true;
            });
        }

        Preference smartSuggestionsSettings = findPreference(KEY_SMART_SUGGESTIONS_SETTINGS);
        if (smartSuggestionsSettings != null) {
            smartSuggestionsSettings.setIcon(createHomeEntryIcon(
                    requireContext(), R.drawable.ic_smart_suggestions));
            smartSuggestionsSettings.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(requireContext(), SmartSuggestionsSettingsActivity.class));
                return true;
            });
        }

        mLyricFetchPreference = findPreference(KEY_LYRIC_FETCH_SETTINGS);
        if (mLyricFetchPreference != null) {
            Intent lyricIntent = new Intent(requireContext(), LyricSettingsActivity.class);
            mLyricFetchPreference.setIcon(createHomeEntryIcon(
                    requireContext(), R.drawable.ic_statusbarlyric));
            mLyricFetchPreference.setEnabled(true);
            mLyricFetchPreference.setSwitchEnabled(true);
            mLyricFetchPreference.setPersistent(false);
            mLyricFetchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    LyricSecureSettings.setEnabled(context, (Boolean) newValue);
                }
                return true;
            });
            mLyricFetchPreference.setOnPreferenceClickListener(preference -> {
                startActivity(lyricIntent);
                return true;
            });
        }

        mNavigationBarHintPreference = findPreference(KEY_NAVIGATION_BAR_HINT);
        if (mNavigationBarHintPreference != null) {
            mNavigationBarHintPreference.setPersistent(false);
            mNavigationBarHintPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    NavigationSecureSettings.setNavigationBarHintEnabled(
                            context, (Boolean) newValue);
                }
                return true;
            });
        }

        mLauncherAllAppsThemedIconsPreference =
                findPreference(KEY_LAUNCHER_ALLAPPS_THEMED_ICONS);
        if (mLauncherAllAppsThemedIconsPreference != null) {
            mLauncherAllAppsThemedIconsPreference.setPersistent(false);
            mLauncherAllAppsThemedIconsPreference.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        Context context = getContext();
                        if (context != null) {
                            LauncherSecureSettings.setAllAppsThemedIconsEnabled(
                                    context, (Boolean) newValue);
                        }
                        return true;
                    });
        }

        mLauncherLensIconPreference = findPreference(KEY_LAUNCHER_LENS_ICON);
        if (mLauncherLensIconPreference != null) {
            mLauncherLensIconPreference.setPersistent(false);
            mLauncherLensIconPreference.setOnPreferenceChangeListener(
                    (preference, newValue) -> {
                        Context context = getContext();
                        if (context != null) {
                            LauncherSecureSettings.setLensIconEnabled(
                                    context, (Boolean) newValue);
                        }
                        return true;
                    });
        }

        syncLyricFetchState();
        syncLauncherAllAppsThemedIconsState();
        syncLauncherLensIconState();
        syncNavigationBarHintState();
    }

    private void initHeaderPreference() {
        IllustrationPreference headerPreference = findPreference(KEY_SETTINGS_EXT_HEADER);
        if (headerPreference == null) {
            return;
        }
        headerPreference.setPersistent(false);
        headerPreference.setImageDrawable(requireContext().getDrawable(
                R.drawable.settings_ext_header_image));
    }

    private Drawable createHomeEntryIcon(Context context, int iconResId) {
        final int iconSize = dpToPx(context, HOME_ENTRY_ICON_SIZE_DP);
        final int maxGlyphSize = dpToPx(context, HOME_ENTRY_GLYPH_SIZE_DP);

        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(context.getColor(com.android.internal.R.color
                .materialColorSecondaryContainer));
        background.setSize(iconSize, iconSize);

        final Drawable glyph = context.getDrawable(iconResId).mutate();
        glyph.setTint(context.getColor(com.android.internal.R.color
                .materialColorOnSecondaryContainer));

        final int intrinsicWidth = glyph.getIntrinsicWidth();
        final int intrinsicHeight = glyph.getIntrinsicHeight();
        final int glyphWidth;
        final int glyphHeight;
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            final float scale = Math.min(
                    (float) maxGlyphSize / intrinsicWidth,
                    (float) maxGlyphSize / intrinsicHeight);
            glyphWidth = Math.round(intrinsicWidth * scale);
            glyphHeight = Math.round(intrinsicHeight * scale);
        } else {
            glyphWidth = maxGlyphSize;
            glyphHeight = maxGlyphSize;
        }
        final int horizontalInset = (iconSize - glyphWidth) / 2;
        final int verticalInset = (iconSize - glyphHeight) / 2;

        final LayerDrawable icon = new LayerDrawable(new Drawable[]{background, glyph});
        icon.setLayerInset(1, horizontalInset, verticalInset, horizontalInset, verticalInset);
        icon.setLayerSize(1, glyphWidth, glyphHeight);
        return icon;
    }

    private int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onResume() {
        super.onResume();
        syncLyricFetchState();
        syncLauncherAllAppsThemedIconsState();
        syncLauncherLensIconState();
        syncNavigationBarHintState();
    }

    private void syncLyricFetchState() {
        Context context = getContext();
        if (context == null || mLyricFetchPreference == null) {
            return;
        }
        mLyricFetchPreference.setChecked(LyricSecureSettings.isEnabled(context, false));
    }

    private void syncNavigationBarHintState() {
        Context context = getContext();
        if (context == null || mNavigationBarHintPreference == null) {
            return;
        }
        mNavigationBarHintPreference.setChecked(
                NavigationSecureSettings.isNavigationBarHintEnabled(context, true));
    }

    private void syncLauncherAllAppsThemedIconsState() {
        Context context = getContext();
        if (context == null || mLauncherAllAppsThemedIconsPreference == null) {
            return;
        }
        mLauncherAllAppsThemedIconsPreference.setChecked(
                LauncherSecureSettings.isAllAppsThemedIconsEnabled(context, false));
    }

    private void syncLauncherLensIconState() {
        Context context = getContext();
        if (context == null || mLauncherLensIconPreference == null) {
            return;
        }
        mLauncherLensIconPreference.setChecked(
                LauncherSecureSettings.isLensIconEnabled(context, false));
    }
}
