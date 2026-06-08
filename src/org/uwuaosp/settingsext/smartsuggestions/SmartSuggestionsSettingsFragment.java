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
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.R;
import org.uwuaosp.settingsext.apppicker.LaunchableAppPicker;
import org.uwuaosp.settingsext.popup.AppSelectionActivity;
import org.uwuaosp.settingsext.smartsuggestions.clipboard.ClipboardRuleStore;
import org.uwuaosp.settingsext.smartsuggestions.sms.SmsCodeRuleStore;

public class SmartSuggestionsSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_SMART_SUGGESTIONS_HEADER = "smart_suggestions_header";
    private static final String KEY_TORCH_SWITCH = "torch_suggestion";
    private static final String KEY_MUSIC_SWITCH = "music_suggestion_switch";
    private static final String KEY_SMS_SWITCH = "sms_code_suggestion";
    private static final String KEY_SMS_RULE_SETTINGS = "sms_code_rule_settings";
    private static final String KEY_URL_SWITCH = "url_suggestion";
    private static final String KEY_URL_RULE_SETTINGS = "url_rule_settings";

    private PrimarySwitchPreference mMusicPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.smart_suggestions_settings);
        ClipboardRuleStore.ensureInitialized(requireContext());
        SmsCodeRuleStore.ensureInitialized(requireContext());
        initHeaderPreference();
        initTorchPreference();
        initMusicPreference();
        initSmsRulePreference();
        initClipboardRulePreference();
        syncStates();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncStates();
    }

    private void initHeaderPreference() {
        IllustrationPreference headerPreference = findPreference(KEY_SMART_SUGGESTIONS_HEADER);
        if (headerPreference == null) {
            return;
        }
        headerPreference.setPersistent(false);
        headerPreference.setImageDrawable(requireContext().getDrawable(
                R.drawable.smart_suggestions_header_image));
    }

    private void initTorchPreference() {
        SwitchPreferenceCompat torchSuggestion = findPreference(KEY_TORCH_SWITCH);
        if (torchSuggestion != null) {
            torchSuggestion.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    SmartSuggestionsSecureSettings.setTorchEnabled(context, (Boolean) newValue);
                }
                return true;
            });
        }
    }

    private void initMusicPreference() {
        mMusicPreference = findPreference(KEY_MUSIC_SWITCH);
        if (mMusicPreference == null) {
            return;
        }

        mMusicPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            Context context = getContext();
            boolean enabled = (Boolean) newValue;
            if (context != null) {
                SmartSuggestionsSecureSettings.setMusicEnabled(context, enabled);
            }
            updateMusicSummary();
            return true;
        });
        mMusicPreference.setOnPreferenceClickListener(preference -> {
            openMusicAppSelection();
            return true;
        });
    }

    private void initClipboardRulePreference() {
        Preference urlRuleSettings = findPreference(KEY_URL_RULE_SETTINGS);
        if (urlRuleSettings != null) {
            urlRuleSettings.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context != null) {
                    startActivity(new Intent(
                            context, SmartSuggestionsClipboardRulesActivity.class));
                }
                return true;
            });
        }

        SwitchPreferenceCompat urlSuggestion = findPreference(KEY_URL_SWITCH);
        if (urlSuggestion != null) {
            urlSuggestion.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    ClipboardRuleStore.setEnabled(context, (Boolean) newValue);
                }
                return true;
            });
        }
    }

    private void initSmsRulePreference() {
        Preference smsRuleSettings = findPreference(KEY_SMS_RULE_SETTINGS);
        if (smsRuleSettings != null) {
            smsRuleSettings.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context != null) {
                    startActivity(new Intent(context, SmartSuggestionsSmsRulesActivity.class));
                }
                return true;
            });
        }

        SwitchPreferenceCompat smsSuggestion = findPreference(KEY_SMS_SWITCH);
        if (smsSuggestion != null) {
            smsSuggestion.setOnPreferenceChangeListener((preference, newValue) -> {
                Context context = getContext();
                if (context != null) {
                    SmsCodeRuleStore.setEnabled(context, (Boolean) newValue);
                }
                return true;
            });
        }
    }

    private void openMusicAppSelection() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        startActivity(new Intent(context, AppSelectionActivity.class)
                .putExtra(AppSelectionActivity.EXTRA_SELECTION_MODE,
                        AppSelectionActivity.SELECTION_MODE_MUSIC_SUGGESTION));
    }

    private void syncStates() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        SwitchPreferenceCompat torchSuggestion = findPreference(KEY_TORCH_SWITCH);
        if (torchSuggestion != null) {
            torchSuggestion.setChecked(
                    SmartSuggestionsSecureSettings.isTorchEnabled(context, false));
        }

        if (mMusicPreference != null) {
            mMusicPreference.setChecked(
                    SmartSuggestionsSecureSettings.isMusicEnabled(context, false));
            updateMusicSummary();
        }

        SwitchPreferenceCompat urlSuggestion = findPreference(KEY_URL_SWITCH);
        if (urlSuggestion != null) {
            urlSuggestion.setChecked(ClipboardRuleStore.isEnabled(context, false));
        }

        SwitchPreferenceCompat smsSuggestion = findPreference(KEY_SMS_SWITCH);
        if (smsSuggestion != null) {
            smsSuggestion.setChecked(SmsCodeRuleStore.isEnabled(context, false));
        }
    }

    private void updateMusicSummary() {
        Context context = getContext();
        if (context == null || mMusicPreference == null) {
            return;
        }

        if (!SmartSuggestionsSecureSettings.isMusicEnabled(context, false)) {
            mMusicPreference.setSummary(R.string.switch_music_suggestion_summary_off);
            return;
        }

        String packageName = SmartSuggestionsSecureSettings.getMusicPackage(
                context, getString(R.string.default_music_app));
        String label = LaunchableAppPicker.resolveAppName(context, packageName);
        mMusicPreference.setSummary(
                getString(R.string.switch_music_suggestion_summary_on, label));
    }
}
