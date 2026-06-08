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

package org.uwuaosp.settingsext.smartsuggestions.clipboard;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import org.uwuaosp.settingsext.R;
import org.uwuaosp.settingsext.apppicker.LaunchableAppPicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ClipboardRuleStore {
    public static final String SECURE_KEY_RULES = "uwuaosp_clipboard_app_rules";
    public static final String SECURE_KEY_ENABLED = "uwuaosp_clipboard_app_rules_enabled";
    private static final String JSON_PRESET_ID = "preset_id";
    private static final String JSON_NAME = "name";
    private static final String JSON_PACKAGE = "package_name";
    private static final String JSON_PATTERN = "pattern";
    private static final String JSON_ENABLED = "enabled";

    private ClipboardRuleStore() {
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

    public static List<ClipboardRule> loadRules(Context context) {
        String json = Settings.Secure.getString(context.getContentResolver(), SECURE_KEY_RULES);
        if (TextUtils.isEmpty(json)) {
            return getDefaultRules(context);
        }

        ArrayList<ClipboardRule> rules = new ArrayList<>();
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
                String packageName = object.optString(JSON_PACKAGE);
                String pattern = object.optString(JSON_PATTERN);
                boolean enabled = object.optBoolean(JSON_ENABLED, true);
                if (TextUtils.isEmpty(presetId)) {
                    presetId = inferPresetId(packageName);
                    if (!TextUtils.isEmpty(presetId)) {
                        changed = true;
                        ClipboardRule presetRule = getDefaultRule(context, presetId);
                        if (presetRule != null
                                && enabled
                                && !LaunchableAppPicker.isAppInstalled(context, packageName)
                                && TextUtils.equals(name, presetRule.getName())
                                && TextUtils.equals(packageName, presetRule.getPackageName())
                                && TextUtils.equals(pattern, presetRule.getPattern())) {
                            enabled = false;
                        }
                    }
                }
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(packageName)
                        || TextUtils.isEmpty(pattern)) {
                    continue;
                }
                rules.add(new ClipboardRule(presetId, name, packageName, pattern, enabled));
            }
        } catch (JSONException e) {
            return getDefaultRules(context);
        }
        if (changed) {
            saveRules(context, rules);
        }
        return rules;
    }

    public static void saveRules(Context context, List<ClipboardRule> rules) {
        JSONArray array = new JSONArray();
        for (ClipboardRule rule : rules) {
            JSONObject object = new JSONObject();
            try {
                if (!TextUtils.isEmpty(rule.getPresetId())) {
                    object.put(JSON_PRESET_ID, rule.getPresetId());
                }
                object.put(JSON_NAME, rule.getName());
                object.put(JSON_PACKAGE, rule.getPackageName());
                object.put(JSON_PATTERN, rule.getPattern());
                object.put(JSON_ENABLED, rule.isEnabled());
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        Settings.Secure.putString(context.getContentResolver(), SECURE_KEY_RULES, array.toString());
    }

    public static List<ClipboardRule> getDefaultRules(Context context) {
        ArrayList<ClipboardRule> rules = new ArrayList<>();
        rules.add(buildPresetRule(context, "taobao", "com.taobao.taobao",
                "(https?://(?:[\\w-]+\\.)?(?:e\\.tb\\.cn|m\\.tb\\.cn|item\\.taobao\\.com|a\\.m\\.taobao\\.com|s\\.taobao\\.com|detail\\.tmall\\.com)[^\\s\"]*)|(￥[a-zA-Z0-9]{8,15}￥|《[a-zA-Z0-9]{8,15}《|喵口令.{0,10}￥[a-zA-Z0-9]{8,15}￥)",
                LaunchableAppPicker.isAppInstalled(context, "com.taobao.taobao")));
        rules.add(buildPresetRule(context, "baidu_netdisk", "com.baidu.netdisk",
                "https?://pan\\.baidu\\.com/s/[\\w-]+",
                LaunchableAppPicker.isAppInstalled(context, "com.baidu.netdisk")));
        rules.add(buildPresetRule(context, "123pan", "com.mfcloudcalculate.networkdisk",
                "https?://(?:www\\.)?(?:123pan\\.com|123865\\.com)/s/[\\w-]+",
                LaunchableAppPicker.isAppInstalled(context, "com.mfcloudcalculate.networkdisk")));
        rules.add(buildPresetRule(context, "bilibili", "tv.danmaku.bili",
                "https?://(?:[\\w-]+\\.)?(?:b23\\.tv|bilibili\\.com)[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "tv.danmaku.bili")));
        rules.add(buildPresetRule(context, "douyin", "com.ss.android.ugc.aweme",
                "https?://(?:[\\w-]+\\.)?douyin\\.com[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.ss.android.ugc.aweme")));
        rules.add(buildPresetRule(context, "pinduoduo", "com.xunmeng.pinduoduo",
                "https?://mobile\\.yangkeduo\\.com[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.xunmeng.pinduoduo")));
        rules.add(buildPresetRule(context, "jd", "com.jingdong.app.mall",
                "https?://(?:[\\w-]+\\.)?(?:jd\\.com|3\\.cn)[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.jingdong.app.mall")));
        rules.add(buildPresetRule(context, "xiaohongshu", "com.xingin.xhs",
                "https?://(?:[\\w-]+\\.)?(?:xiaohongshu\\.com|xhslink\\.com)[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.xingin.xhs")));
        rules.add(buildPresetRule(context, "weibo", "com.sina.weibo",
                "https?://(?:[\\w-]+\\.)?(?:weibo\\.com|weibo\\.cn)[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.sina.weibo")));
        rules.add(buildPresetRule(context, "zhihu", "com.zhihu.android",
                "https?://(?:[\\w-]+\\.)?zhihu\\.com[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.zhihu.android")));
        rules.add(buildPresetRule(context, "netease_music", "com.netease.cloudmusic",
                "https?://music\\.163\\.com[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.netease.cloudmusic")));
        rules.add(buildPresetRule(context, "qq_music", "com.tencent.qqmusic",
                "https?://(?:y\\.qq\\.com|c\\.y\\.qq\\.com)[^\\s\"]*",
                LaunchableAppPicker.isAppInstalled(context, "com.tencent.qqmusic")));
        rules.add(buildPresetRule(context, "idlefish", "com.taobao.idlefish",
                "(https?://(?:[\\w-]+\\.)?(?:goofish\\.com|idlefish\\.com)[^\\s\"]*)|(m\\.tb\\.cn/h\\.[\\w-]+)",
                LaunchableAppPicker.isAppInstalled(context, "com.taobao.idlefish")));
        rules.add(buildPresetRule(context, "alipay", "com.eg.android.AlipayGphone",
                "https?://(?:[\\w-]+\\.)?alipay(?:objects)?\\.com/(?:_|[\\w-?=&/])+",
                LaunchableAppPicker.isAppInstalled(context, "com.eg.android.AlipayGphone")));
        rules.add(buildPresetRule(context, "dingtalk", "com.alibaba.android.rimet",
                "(https?://(?:[\\w-]+\\.)?(?:dingtalk\\.com|dg\\.alipay\\.com)[^\\s\"]*)|(dtk://dingtalkweb/business/[^\\s\"]*)",
                LaunchableAppPicker.isAppInstalled(context, "com.alibaba.android.rimet")));
        rules.add(buildPresetRule(context, "quark", "com.quark.browser",
                "https?://pan\\.quark\\.cn/s/[\\w-]+",
                LaunchableAppPicker.isAppInstalled(context, "com.quark.browser")));
        rules.add(buildPresetRule(context, "feishu", "com.ss.android.lark",
                "https?://(?:[\\w-]+\\.)?(?:feishu\\.cn|larksuite\\.com)/(?:docx|base|wiki|file)/[\\w-]+",
                LaunchableAppPicker.isAppInstalled(context, "com.ss.android.lark")));
        return rules;
    }

    public static ClipboardRule getDefaultRule(Context context, String presetId) {
        for (ClipboardRule rule : getDefaultRules(context)) {
            if (TextUtils.equals(presetId, rule.getPresetId())) {
                return rule;
            }
        }
        return null;
    }

    private static ClipboardRule buildPresetRule(
            Context context,
            String presetId,
            String packageName,
            String pattern,
            boolean enabled) {
        return new ClipboardRule(
                presetId,
                context.getString(getPresetNameResId(presetId)),
                packageName,
                pattern,
                enabled);
    }

    private static String inferPresetId(String packageName) {
        if (TextUtils.equals(packageName, "com.taobao.taobao")) return "taobao";
        if (TextUtils.equals(packageName, "com.baidu.netdisk")) return "baidu_netdisk";
        if (TextUtils.equals(packageName, "com.mfcloudcalculate.networkdisk")) return "123pan";
        if (TextUtils.equals(packageName, "tv.danmaku.bili")) return "bilibili";
        if (TextUtils.equals(packageName, "com.ss.android.ugc.aweme")) return "douyin";
        if (TextUtils.equals(packageName, "com.xunmeng.pinduoduo")) return "pinduoduo";
        if (TextUtils.equals(packageName, "com.jingdong.app.mall")) return "jd";
        if (TextUtils.equals(packageName, "com.xingin.xhs")) return "xiaohongshu";
        if (TextUtils.equals(packageName, "com.sina.weibo")) return "weibo";
        if (TextUtils.equals(packageName, "com.zhihu.android")) return "zhihu";
        if (TextUtils.equals(packageName, "com.netease.cloudmusic")) return "netease_music";
        if (TextUtils.equals(packageName, "com.tencent.qqmusic")) return "qq_music";
        if (TextUtils.equals(packageName, "com.taobao.idlefish")) return "idlefish";
        if (TextUtils.equals(packageName, "com.eg.android.AlipayGphone")) return "alipay";
        if (TextUtils.equals(packageName, "com.alibaba.android.rimet")) return "dingtalk";
        if (TextUtils.equals(packageName, "com.quark.browser")) return "quark";
        if (TextUtils.equals(packageName, "com.ss.android.lark")) return "feishu";
        return null;
    }

    private static int getPresetNameResId(String presetId) {
        switch (presetId) {
            case "taobao":
                return R.string.clipboard_rule_preset_taobao;
            case "baidu_netdisk":
                return R.string.clipboard_rule_preset_baidu_netdisk;
            case "123pan":
                return R.string.clipboard_rule_preset_123pan;
            case "bilibili":
                return R.string.clipboard_rule_preset_bilibili;
            case "douyin":
                return R.string.clipboard_rule_preset_douyin;
            case "pinduoduo":
                return R.string.clipboard_rule_preset_pinduoduo;
            case "jd":
                return R.string.clipboard_rule_preset_jd;
            case "xiaohongshu":
                return R.string.clipboard_rule_preset_xiaohongshu;
            case "weibo":
                return R.string.clipboard_rule_preset_weibo;
            case "zhihu":
                return R.string.clipboard_rule_preset_zhihu;
            case "netease_music":
                return R.string.clipboard_rule_preset_netease_music;
            case "qq_music":
                return R.string.clipboard_rule_preset_qq_music;
            case "idlefish":
                return R.string.clipboard_rule_preset_idlefish;
            case "alipay":
                return R.string.clipboard_rule_preset_alipay;
            case "dingtalk":
                return R.string.clipboard_rule_preset_dingtalk;
            case "quark":
                return R.string.clipboard_rule_preset_quark;
            case "feishu":
                return R.string.clipboard_rule_preset_feishu;
            default:
                throw new IllegalArgumentException("Unknown preset id: " + presetId);
        }
    }

    private static String sanitizePresetId(String presetId) {
        return TextUtils.isEmpty(presetId) ? null : presetId;
    }
}
