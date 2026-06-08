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

import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppJumpSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_ALL_APPS = "app_jump_all_apps";
    private static final String KEY_SOURCE_ALLOW = "app_jump_always_allow";
    private static final String KEY_SOURCE_BLOCK = "app_jump_always_block";
    private static final String KEY_SOURCE_ASK = "app_jump_ask";
    private static final String KEY_TARGET_ALLOW = "app_jump_target_allow";
    private static final String KEY_TARGET_ASK = "app_jump_target_ask";
    private static final String KEY_TARGET_BLOCK = "app_jump_target_block";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private AppJumpPolicyBackend mBackend;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_jump_settings);
        requireActivity().setTitle(R.string.app_jump_settings_title);
        mBackend = new AppJumpPolicyBackend(requireContext());
        bindAllApps();
        bindCategory(KEY_SOURCE_ALLOW, AppJumpPolicyBackend.CATEGORY_SOURCE_ALLOW);
        bindCategory(KEY_SOURCE_BLOCK, AppJumpPolicyBackend.CATEGORY_SOURCE_BLOCK);
        bindCategory(KEY_SOURCE_ASK, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK);
        bindCategory(KEY_TARGET_ALLOW, AppJumpPolicyBackend.CATEGORY_TARGET_ALLOW);
        bindCategory(KEY_TARGET_ASK, AppJumpPolicyBackend.CATEGORY_TARGET_ASK);
        bindCategory(KEY_TARGET_BLOCK, AppJumpPolicyBackend.CATEGORY_TARGET_BLOCK);

        FooterPreference footerPreference = findPreference("app_jump_settings_footer");
        if (footerPreference != null) {
            footerPreference.setTitle(R.string.app_jump_settings_footer);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadSummaries();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    private void bindCategory(String key, int category) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(pref -> {
            startActivity(AppJumpSettingsActivity.createCategoryIntent(requireContext(), category));
            return true;
        });
    }

    private void bindAllApps() {
        Preference preference = findPreference(KEY_ALL_APPS);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(pref -> {
            startActivity(AppJumpSettingsActivity.createAllAppsIntent(requireContext()));
            return true;
        });
    }

    private void reloadSummaries() {
        mExecutor.execute(() -> {
            try {
                final List<AppJumpPolicyBackend.AppEntry> apps = mBackend.loadUserApps();
                int sourceAllowCount = 0;
                int sourceBlockCount = 0;
                int sourceAskCount = 0;
                int targetAllowCount = 0;
                int targetAskCount = 0;
                int targetBlockCount = 0;
                for (AppJumpPolicyBackend.AppEntry app : apps) {
                    final AppJumpPolicyBackend.AppPolicyState state =
                            mBackend.getPolicyState(app.getPackageName());
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_SOURCE_ALLOW)) {
                        sourceAllowCount++;
                    }
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_SOURCE_BLOCK)) {
                        sourceBlockCount++;
                    }
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK)) {
                        sourceAskCount++;
                    }
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_TARGET_ALLOW)) {
                        targetAllowCount++;
                    }
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_TARGET_ASK)) {
                        targetAskCount++;
                    }
                    if (mBackend.matchesCategory(state, AppJumpPolicyBackend.CATEGORY_TARGET_BLOCK)) {
                        targetBlockCount++;
                    }
                }
                final int totalApps = apps.size();
                final int finalSourceAllowCount = sourceAllowCount;
                final int finalSourceBlockCount = sourceBlockCount;
                final int finalSourceAskCount = sourceAskCount;
                final int finalTargetAllowCount = targetAllowCount;
                final int finalTargetAskCount = targetAskCount;
                final int finalTargetBlockCount = targetBlockCount;
                final android.app.Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(() -> {
                    updateSummary(KEY_ALL_APPS,
                            getString(R.string.app_jump_all_apps_summary, totalApps));
                    updateSummary(KEY_SOURCE_ALLOW, finalSourceAllowCount, totalApps);
                    updateSummary(KEY_SOURCE_BLOCK, finalSourceBlockCount, totalApps);
                    updateSummary(KEY_SOURCE_ASK, finalSourceAskCount, totalApps);
                    updateSummary(KEY_TARGET_ALLOW, finalTargetAllowCount, totalApps);
                    updateSummary(KEY_TARGET_ASK, finalTargetAskCount, totalApps);
                    updateSummary(KEY_TARGET_BLOCK, finalTargetBlockCount, totalApps);
                });
            } catch (RemoteException | RuntimeException e) {
                final android.app.Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, R.string.app_jump_load_failed,
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateSummary(String key, int count, int total) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(getString(R.string.app_jump_category_count_summary, count, total));
        }
    }

    private void updateSummary(String key, String summary) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(summary);
        }
    }
}
