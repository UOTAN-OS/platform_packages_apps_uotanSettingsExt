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

package org.uwuaosp.settingsext.appjump;

import android.app.ActivityTaskManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.uwuaosp.settingsext.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class AppJumpPolicyBackend {
    static final int CATEGORY_SOURCE_ALLOW = 1;
    static final int CATEGORY_SOURCE_BLOCK = 2;
    static final int CATEGORY_SOURCE_ASK = 3;
    static final int CATEGORY_TARGET_ALLOW = 4;
    static final int CATEGORY_TARGET_ASK = 5;
    static final int CATEGORY_TARGET_BLOCK = 6;

    private static final String PREFS_NAME = "app_jump_settings";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";

    private static final Comparator<AppEntry> APP_ENTRY_COMPARATOR =
            Comparator.comparingInt(AppJumpPolicyBackend::getEntrySortBucket)
                    .thenComparingInt((AppEntry app) -> -getPriorityScore(app.getPackageName()))
                    .thenComparing(AppEntry::getLabel, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AppEntry::getPackageName, String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<AppEntry> RECENT_APP_ENTRY_COMPARATOR =
            Comparator.comparingLong(AppEntry::getLastTimeUsed).reversed()
                    .thenComparingInt(AppJumpPolicyBackend::getEntrySortBucket)
                    .thenComparingInt((AppEntry app) -> -getPriorityScore(app.getPackageName()))
                    .thenComparing(AppEntry::getLabel, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AppEntry::getPackageName, String.CASE_INSENSITIVE_ORDER);

    private static final long USAGE_LOOKBACK_MILLIS = TimeUnit.DAYS.toMillis(180);

    private static final String[] PRIORITY_PACKAGES = new String[] {
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.apps.maps",
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.android.browser",
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.eg.android.AlipayGphone",
            "com.ss.android.ugc.aweme",
            "com.xingin.xhs",
            "com.taobao.taobao",
            "com.jd.jrapp",
            "com.jingdong.app.mall",
    };

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final SharedPreferences mPreferences;

    AppJumpPolicyBackend(Context context) {
        mContext = context.getApplicationContext();
        mPackageManager = mContext.getPackageManager();
        mPreferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    boolean isShowSystemAppsEnabled() {
        return mPreferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false);
    }

    void setShowSystemAppsEnabled(boolean enabled) {
        mPreferences.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, enabled).apply();
    }

    boolean isEnabled() throws RemoteException {
        return ActivityTaskManager.getService().isAppJumpEnabled(UserHandle.myUserId());
    }

    void setEnabled(boolean enabled) throws RemoteException {
        ActivityTaskManager.getService().setAppJumpEnabled(UserHandle.myUserId(), enabled);
    }

    List<AppEntry> loadUserApps() {
        return loadUserApps(false);
    }

    List<AppEntry> loadUserAppsSortedByRecentUsage() {
        return loadUserApps(true);
    }

    private List<AppEntry> loadUserApps(boolean sortByRecent) {
        final Map<String, AppEntry> deduped = new LinkedHashMap<>();
        final Map<String, Long> lastTimeUsedByPackage = sortByRecent
                ? loadLastTimeUsedByPackage()
                : Collections.emptyMap();
        final List<ApplicationInfo> applications =
                mPackageManager.getInstalledApplications(PackageManager.MATCH_ALL);
        final boolean showSystemApps = isShowSystemAppsEnabled();
        for (ApplicationInfo appInfo : applications) {
            if (!shouldIncludeApp(appInfo, showSystemApps)) {
                continue;
            }
            addAppEntry(deduped, appInfo, lastTimeUsedByPackage);
        }

        if (deduped.isEmpty()) {
            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List<android.content.pm.ResolveInfo> resolveInfos =
                    mPackageManager.queryIntentActivities(
                            intent, PackageManager.GET_META_DATA | PackageManager.MATCH_ALL);
            for (android.content.pm.ResolveInfo resolveInfo : resolveInfos) {
                if (resolveInfo.activityInfo == null) {
                    continue;
                }
                try {
                    final ApplicationInfo appInfo = mPackageManager.getApplicationInfo(
                            resolveInfo.activityInfo.packageName, PackageManager.MATCH_ALL);
                    if (shouldIncludeApp(appInfo, showSystemApps)) {
                        addAppEntry(deduped, appInfo, lastTimeUsedByPackage);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }

        final ArrayList<AppEntry> apps = new ArrayList<>(deduped.values());
        apps.sort(sortByRecent ? RECENT_APP_ENTRY_COMPARATOR : APP_ENTRY_COMPARATOR);
        return apps;
    }

    List<AppEntry> filterApps(List<AppEntry> apps, @Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            return apps;
        }
        final String needle = query.trim().toLowerCase(Locale.ROOT);
        final ArrayList<AppEntry> filtered = new ArrayList<>();
        for (AppEntry app : apps) {
            if (app.matchesQuery(needle)) {
                filtered.add(app);
            }
        }
        return filtered;
    }

    List<AppEntry> loadUserAppsExcluding(@Nullable String packageName) {
        final List<AppEntry> apps = loadUserApps();
        if (packageName == null) {
            return apps;
        }
        apps.removeIf(app -> packageName.equals(app.getPackageName()));
        return apps;
    }

    AppPolicyState getPolicyState(String packageName) throws RemoteException {
        final int sourceMode = ActivityTaskManager.getService().getAppJumpSourceMode(
                packageName, UserHandle.myUserId());
        final int targetMode = ActivityTaskManager.getService().getAppJumpTargetMode(
                packageName, UserHandle.myUserId());
        return new AppPolicyState(sourceMode, targetMode);
    }

    int getPairMode(String sourcePackage, String targetPackage) throws RemoteException {
        return ActivityTaskManager.getService().getAppJumpPairMode(
                sourcePackage, targetPackage, UserHandle.myUserId());
    }

    void setSourceMode(String packageName, int sourceMode) throws RemoteException {
        ActivityTaskManager.getService().setAppJumpSourceMode(
                packageName, UserHandle.myUserId(), sourceMode);
    }

    void setTargetMode(String packageName, int targetMode) throws RemoteException {
        ActivityTaskManager.getService().setAppJumpTargetMode(
                packageName, UserHandle.myUserId(), targetMode);
    }

    void setPairMode(String sourcePackage, String targetPackage, int mode) throws RemoteException {
        ActivityTaskManager.getService().setAppJumpPairMode(
                sourcePackage, targetPackage, UserHandle.myUserId(), mode);
    }

    boolean matchesCategory(AppPolicyState state, int category) {
        if (category == CATEGORY_SOURCE_ALLOW) {
            return state.sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW;
        }
        if (category == CATEGORY_SOURCE_BLOCK) {
            return state.sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK;
        }
        if (category == CATEGORY_SOURCE_ASK) {
            return state.sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK;
        }
        if (category == CATEGORY_TARGET_ALLOW) {
            return state.targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW;
        }
        if (category == CATEGORY_TARGET_BLOCK) {
            return state.targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK;
        }
        return state.targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK;
    }

    static int getCategoryTitleRes(int category) {
        if (category == CATEGORY_SOURCE_ALLOW) {
            return R.string.app_jump_category_source_always_allow_title;
        }
        if (category == CATEGORY_SOURCE_BLOCK) {
            return R.string.app_jump_category_source_always_block_title;
        }
        if (category == CATEGORY_TARGET_ALLOW) {
            return R.string.app_jump_category_target_always_allow_title;
        }
        if (category == CATEGORY_TARGET_BLOCK) {
            return R.string.app_jump_category_target_always_block_title;
        }
        if (category == CATEGORY_TARGET_ASK) {
            return R.string.app_jump_category_target_ask_title;
        }
        return R.string.app_jump_category_source_ask_title;
    }

    @NonNull
    String getModeSummary(@NonNull Context context, int mode) {
        if (mode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW) {
            return context.getString(R.string.app_jump_mode_allow_summary);
        }
        if (mode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK) {
            return context.getString(R.string.app_jump_mode_block_summary);
        }
        return context.getString(R.string.app_jump_mode_ask_summary);
    }

    @NonNull
    String getPairModeSummary(@NonNull Context context, int mode) {
        if (mode == ActivityTaskManager.APP_JUMP_PAIR_MODE_INHERIT) {
            return context.getString(R.string.app_jump_pair_mode_inherit_summary);
        }
        return getModeSummary(context, mode);
    }

    @NonNull
    String getPairModeSummary(@NonNull Context context, int sourceMode, int targetMode, int mode) {
        if (mode != ActivityTaskManager.APP_JUMP_PAIR_MODE_INHERIT) {
            return getModeSummary(context, mode);
        }
        return context.getString(R.string.app_jump_pair_mode_inherit_effective_summary,
                getModeSummary(context, resolveEffectiveMode(sourceMode, targetMode, mode)));
    }

    boolean isSystemApp(String packageName) {
        try {
            return isSystemApp(mPackageManager.getApplicationInfo(packageName, PackageManager.MATCH_ALL));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    boolean isSystemApp(@Nullable ApplicationInfo appInfo) {
        return appInfo != null
                && ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    static int resolveEffectiveMode(int sourceMode, int targetMode, int pairMode) {
        if (pairMode != ActivityTaskManager.APP_JUMP_PAIR_MODE_INHERIT) {
            return pairMode;
        }
        if (sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK
                || targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK) {
            return ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK;
        }
        if (sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW
                || targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW) {
            return ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW;
        }
        return ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK;
    }

    private static boolean shouldIncludeApp(@Nullable ApplicationInfo appInfo,
            boolean showSystemApps) {
        if (appInfo == null || appInfo.packageName == null) {
            return false;
        }
        final boolean systemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        return showSystemApps || !systemApp;
    }

    private static int getEntrySortBucket(AppEntry app) {
        final int priority = getPriorityScore(app.getPackageName());
        if (priority > 0) {
            return 0;
        }
        if (app.isSystemApp()) {
            return 2;
        }
        return 1;
    }

    private static int getPriorityScore(String packageName) {
        for (int i = 0; i < PRIORITY_PACKAGES.length; i++) {
            if (PRIORITY_PACKAGES[i].equals(packageName)) {
                return PRIORITY_PACKAGES.length - i;
            }
        }
        return 0;
    }

    private Map<String, Long> loadLastTimeUsedByPackage() {
        final UsageStatsManager usageStatsManager =
                mContext.getSystemService(UsageStatsManager.class);
        if (usageStatsManager == null) {
            return Collections.emptyMap();
        }
        final long endTime = System.currentTimeMillis();
        final long startTime = endTime - USAGE_LOOKBACK_MILLIS;
        try {
            final Map<String, UsageStats> usageStatsMap =
                    usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
            if (usageStatsMap == null || usageStatsMap.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<String, Long> lastTimeUsedByPackage = new LinkedHashMap<>();
            for (Map.Entry<String, UsageStats> entry : usageStatsMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                lastTimeUsedByPackage.put(entry.getKey(), entry.getValue().getLastTimeUsed());
            }
            return lastTimeUsedByPackage;
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    private void addAppEntry(Map<String, AppEntry> deduped, ApplicationInfo appInfo,
            Map<String, Long> lastTimeUsedByPackage) {
        final String packageName = appInfo.packageName;
        if (packageName == null || deduped.containsKey(packageName)) {
            return;
        }
        final CharSequence label = mPackageManager.getApplicationLabel(appInfo);
        final Drawable icon = mPackageManager.getApplicationIcon(appInfo);
        deduped.put(packageName, new AppEntry(
                label != null ? label.toString() : packageName,
                packageName,
                icon,
                isSystemApp(appInfo),
                lastTimeUsedByPackage.getOrDefault(packageName, 0L)));
    }

    static final class AppEntry {
        private final String mLabel;
        private final String mPackageName;
        private final Drawable mIcon;
        private final boolean mSystemApp;
        private final long mLastTimeUsed;

        AppEntry(String label, String packageName, Drawable icon, boolean systemApp,
                long lastTimeUsed) {
            mLabel = label;
            mPackageName = packageName;
            mIcon = icon;
            mSystemApp = systemApp;
            mLastTimeUsed = lastTimeUsed;
        }

        String getLabel() {
            return mLabel;
        }

        String getPackageName() {
            return mPackageName;
        }

        Drawable getIcon() {
            return mIcon;
        }

        boolean isSystemApp() {
            return mSystemApp;
        }

        long getLastTimeUsed() {
            return mLastTimeUsed;
        }

        boolean matchesQuery(String needle) {
            return mLabel.toLowerCase(Locale.ROOT).contains(needle)
                    || mPackageName.toLowerCase(Locale.ROOT).contains(needle);
        }
    }

    static final class AppPolicyState {
        final int sourceMode;
        final int targetMode;

        AppPolicyState(int sourceMode, int targetMode) {
            this.sourceMode = sourceMode;
            this.targetMode = targetMode;
        }
    }
}
