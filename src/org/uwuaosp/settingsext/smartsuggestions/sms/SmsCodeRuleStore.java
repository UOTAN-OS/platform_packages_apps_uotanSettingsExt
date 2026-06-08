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

package org.uwuaosp.settingsext.smartsuggestions.sms;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import org.uwuaosp.settingsext.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SmsCodeRuleStore {
    public static final String SECURE_KEY_RULES = "uwuaosp_sms_code_rules";
    public static final String SECURE_KEY_ENABLED = "uwuaosp_sms_code_suggestion_enabled";
    private static final String JSON_PRESET_ID = "preset_id";
    private static final String JSON_NAME = "name";
    private static final String JSON_PATTERN = "pattern";
    private static final String JSON_ENABLED = "enabled";

    private SmsCodeRuleStore() {
    }

    public static void ensureInitialized(Context context) {
        if (TextUtils.isEmpty(Settings.Secure.getString(context.getContentResolver(), SECURE_KEY_RULES))) {
            saveRules(context, getDefaultRules(context));
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(), SECURE_KEY_ENABLED, enabled ? 1 : 0);
    }

    public static boolean isEnabled(Context context, boolean defaultValue) {
        return Settings.Secure.getInt(context.getContentResolver(), SECURE_KEY_ENABLED,
                defaultValue ? 1 : 0) == 1;
    }

    public static List<SmsCodeRule> loadRules(Context context) {
        String json = Settings.Secure.getString(context.getContentResolver(), SECURE_KEY_RULES);
        if (TextUtils.isEmpty(json)) {
            return getDefaultRules(context);
        }

        ArrayList<SmsCodeRule> rules = new ArrayList<>();
        boolean changed = false;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String presetId = sanitizePresetId(object.optString(JSON_PRESET_ID));
                String name = object.optString(JSON_NAME);
                String pattern = object.optString(JSON_PATTERN);
                boolean enabled = object.optBoolean(JSON_ENABLED, true);
                if (TextUtils.isEmpty(presetId)) {
                    presetId = inferPresetId(context, pattern);
                    if (!TextUtils.isEmpty(presetId)) {
                        changed = true;
                    }
                }
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pattern)) {
                    continue;
                }
                rules.add(new SmsCodeRule(presetId, name, pattern, enabled));
            }
        } catch (JSONException e) {
            return getDefaultRules(context);
        }
        if (changed) {
            saveRules(context, rules);
        }
        return rules;
    }

    public static void saveRules(Context context, List<SmsCodeRule> rules) {
        JSONArray array = new JSONArray();
        for (SmsCodeRule rule : rules) {
            JSONObject object = new JSONObject();
            try {
                if (!TextUtils.isEmpty(rule.getPresetId())) {
                    object.put(JSON_PRESET_ID, rule.getPresetId());
                }
                object.put(JSON_NAME, rule.getName());
                object.put(JSON_PATTERN, rule.getPattern());
                object.put(JSON_ENABLED, rule.isEnabled());
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        Settings.Secure.putString(context.getContentResolver(), SECURE_KEY_RULES, array.toString());
    }

    public static List<SmsCodeRule> getDefaultRules(Context context) {
        ArrayList<SmsCodeRule> rules = new ArrayList<>();
        rules.add(buildPresetRule(context, "generic_6_digit",
                context.getString(R.string.sms_code_rule_preset_generic_6_digit_pattern), true));
        rules.add(buildPresetRule(context, "generic_4_to_8",
                context.getString(R.string.sms_code_rule_preset_generic_4_to_8_pattern), true));
        rules.add(buildPresetRule(context, "keyword_followed",
                context.getString(R.string.sms_code_rule_preset_keyword_followed_pattern), true));
        return rules;
    }

    public static SmsCodeRule getDefaultRule(Context context, String presetId) {
        for (SmsCodeRule rule : getDefaultRules(context)) {
            if (TextUtils.equals(presetId, rule.getPresetId())) {
                return rule;
            }
        }
        return null;
    }

    private static SmsCodeRule buildPresetRule(
            Context context, String presetId, String pattern, boolean enabled) {
        return new SmsCodeRule(
                presetId,
                context.getString(getPresetNameResId(presetId)),
                pattern,
                enabled);
    }

    private static String inferPresetId(Context context, String pattern) {
        if (TextUtils.equals(
                pattern, context.getString(R.string.sms_code_rule_preset_generic_6_digit_pattern))) {
            return "generic_6_digit";
        }
        if (TextUtils.equals(
                pattern, context.getString(R.string.sms_code_rule_preset_generic_4_to_8_pattern))) {
            return "generic_4_to_8";
        }
        if (TextUtils.equals(
                pattern, context.getString(R.string.sms_code_rule_preset_keyword_followed_pattern))) {
            return "keyword_followed";
        }
        return null;
    }

    private static int getPresetNameResId(String presetId) {
        switch (presetId) {
            case "generic_6_digit":
                return R.string.sms_code_rule_preset_generic_6_digit_name;
            case "generic_4_to_8":
                return R.string.sms_code_rule_preset_generic_4_to_8_name;
            case "keyword_followed":
                return R.string.sms_code_rule_preset_keyword_followed_name;
            default:
                throw new IllegalArgumentException("Unknown preset id: " + presetId);
        }
    }

    private static String sanitizePresetId(String presetId) {
        return TextUtils.isEmpty(presetId) ? null : presetId;
    }
}
