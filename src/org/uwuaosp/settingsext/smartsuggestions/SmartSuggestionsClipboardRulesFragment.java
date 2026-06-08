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

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.R;
import org.uwuaosp.settingsext.apppicker.LaunchableAppPicker;
import org.uwuaosp.settingsext.smartsuggestions.clipboard.ClipboardRule;
import org.uwuaosp.settingsext.smartsuggestions.clipboard.ClipboardRuleStore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SmartSuggestionsClipboardRulesFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_PRESET_RULE_LIST = "preset_rule_list";
    private static final String KEY_CUSTOM_RULES = "custom_rules";
    private static final char CUSTOM_RULE_SEPARATOR = '|';

    private final ArrayList<ClipboardRule> mPresetRules = new ArrayList<>();
    private final ArrayList<ClipboardRule> mCustomRules = new ArrayList<>();
    private PreferenceCategory mPresetRuleListCategory;
    private Preference mCustomRulesPreference;

    @Override
    public void onCreatePreferences(android.os.Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.smart_suggestions_clipboard_rules);
        ClipboardRuleStore.ensureInitialized(requireContext());
        mPresetRuleListCategory = findPreference(KEY_PRESET_RULE_LIST);
        mCustomRulesPreference = findPreference(KEY_CUSTOM_RULES);
        if (mCustomRulesPreference != null) {
            mCustomRulesPreference.setOnPreferenceClickListener(preference -> {
                showCustomRulesDialog();
                return true;
            });
        }
        reloadRules();
    }

    private void reloadRules() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        mPresetRules.clear();
        mCustomRules.clear();
        final List<ClipboardRule> rules = ClipboardRuleStore.loadRules(context);
        for (ClipboardRule rule : rules) {
            if (rule.isPreset()) {
                mPresetRules.add(rule);
            } else {
                mCustomRules.add(rule);
            }
        }
        rebuildPresetPreferences();
        updateCustomRulesSummary();
    }

    private void rebuildPresetPreferences() {
        if (mPresetRuleListCategory == null) {
            return;
        }
        mPresetRuleListCategory.removeAll();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        for (int i = 0; i < mPresetRules.size(); i++) {
            final ClipboardRule rule = mPresetRules.get(i);
            if (LaunchableAppPicker.isAppInstalled(context, rule.getPackageName())) {
                addPresetPreference(context, i, rule, true);
            }
        }
        for (int i = 0; i < mPresetRules.size(); i++) {
            final ClipboardRule rule = mPresetRules.get(i);
            if (!LaunchableAppPicker.isAppInstalled(context, rule.getPackageName())) {
                addPresetPreference(context, i, rule, false);
            }
        }
    }

    private void addPresetPreference(
            Context context, int index, ClipboardRule rule, boolean appInstalled) {
        final SwitchPreferenceCompat preference = new SwitchPreferenceCompat(context);
        preference.setTitle(rule.getName());
        preference.setChecked(rule.isEnabled());
        if (!appInstalled) {
            preference.setSummary(R.string.clipboard_rule_preset_app_missing);
            preference.setEnabled(false);
        } else {
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                final boolean enabled = (Boolean) newValue;
                mPresetRules.set(index, new ClipboardRule(
                        rule.getPresetId(),
                        rule.getName(),
                        rule.getPackageName(),
                        rule.getPattern(),
                        enabled));
                persistRules();
                return true;
            });
        }
        mPresetRuleListCategory.addPreference(preference);
    }

    private void updateCustomRulesSummary() {
        if (mCustomRulesPreference == null) {
            return;
        }
        if (mCustomRules.isEmpty()) {
            mCustomRulesPreference.setSummary(R.string.clipboard_rule_custom_summary_empty);
            return;
        }
        mCustomRulesPreference.setSummary(getString(
                R.string.clipboard_rule_custom_summary_count, mCustomRules.size()));
    }

    private void showCustomRulesDialog() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final EditText editText = new EditText(context);
        editText.setMinLines(8);
        editText.setMaxLines(16);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(serializeCustomRules());
        editText.setHint(R.string.clipboard_rule_custom_dialog_hint);
        final int horizontalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        final int verticalPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.clipboard_rule_custom_dialog_title)
                .setMessage(R.string.clipboard_rule_custom_dialog_message)
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.clipboard_rule_save, null)
                .create();
        dialog.setOnShowListener(d -> {
            editText.setPadding(horizontalPadding, verticalPadding,
                    horizontalPadding, verticalPadding);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final ParseResult parseResult = parseCustomRules(editText.getText().toString());
                if (parseResult.errorMessageResId != 0) {
                    Toast.makeText(context, getString(parseResult.errorMessageResId,
                            parseResult.errorLineNumber), Toast.LENGTH_SHORT).show();
                    return;
                }
                mCustomRules.clear();
                mCustomRules.addAll(parseResult.rules);
                persistRules();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private String serializeCustomRules() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mCustomRules.size(); i++) {
            final ClipboardRule rule = mCustomRules.get(i);
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(rule.getPackageName())
                    .append(CUSTOM_RULE_SEPARATOR)
                    .append(rule.getPattern());
        }
        return builder.toString();
    }

    private ParseResult parseCustomRules(String rawInput) {
        final ArrayList<ClipboardRule> rules = new ArrayList<>();
        if (TextUtils.isEmpty(rawInput)) {
            return new ParseResult(rules, 0, 0);
        }
        final Context context = requireContext();
        final String[] lines = rawInput.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            final int separatorIndex = line.indexOf(CUSTOM_RULE_SEPARATOR);
            if (separatorIndex <= 0 || separatorIndex >= line.length() - 1) {
                return new ParseResult(null, R.string.clipboard_rule_custom_error_format, i + 1);
            }
            final String packageName = line.substring(0, separatorIndex).trim();
            final String pattern = line.substring(separatorIndex + 1).trim();
            if (packageName.isEmpty() || pattern.isEmpty()) {
                return new ParseResult(null, R.string.clipboard_rule_custom_error_format, i + 1);
            }
            try {
                Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                return new ParseResult(null, R.string.clipboard_rule_custom_error_regex, i + 1);
            }
            final String name = LaunchableAppPicker.resolveAppName(context, packageName);
            rules.add(new ClipboardRule(name, packageName, pattern, true));
        }
        return new ParseResult(rules, 0, 0);
    }

    private void persistRules() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final ArrayList<ClipboardRule> allRules = new ArrayList<>(mPresetRules.size()
                + mCustomRules.size());
        allRules.addAll(mPresetRules);
        allRules.addAll(mCustomRules);
        ClipboardRuleStore.saveRules(context, allRules);
        updateCustomRulesSummary();
    }

    private static final class ParseResult {
        final ArrayList<ClipboardRule> rules;
        final int errorMessageResId;
        final int errorLineNumber;

        ParseResult(ArrayList<ClipboardRule> rules, int errorMessageResId, int errorLineNumber) {
            this.rules = rules;
            this.errorMessageResId = errorMessageResId;
            this.errorLineNumber = errorLineNumber;
        }
    }
}
