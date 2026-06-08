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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.uwuaosp.settingsext.R;

public class AppJumpSettingsActivity extends CollapsingToolbarBaseActivity {
    private static final String EXTRA_DESTINATION = "destination";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_SOURCE_PACKAGE = "source_package";
    private static final String EXTRA_TARGET_PACKAGE = "target_package";

    private static final String DESTINATION_OVERVIEW = "overview";
    private static final String DESTINATION_ALL_APPS = "all_apps";
    private static final String DESTINATION_CATEGORY = "category";
    private static final String DESTINATION_APP_DETAIL = "app_detail";
    private static final String DESTINATION_SOURCE_RULES = "source_rules";
    private static final String DESTINATION_TARGET_RULES = "target_rules";
    private static final String DESTINATION_TARGET_RULE_DETAIL = "target_rule_detail";

    public static Intent createIntent(Context context) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_OVERVIEW);
    }

    public static Intent createCategoryIntent(Context context, int category) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_CATEGORY)
                .putExtra(EXTRA_CATEGORY, category);
    }

    public static Intent createAllAppsIntent(Context context) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_ALL_APPS);
    }

    public static Intent createAppDetailIntent(Context context, String packageName) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_APP_DETAIL)
                .putExtra(EXTRA_PACKAGE_NAME, packageName);
    }

    public static Intent createTargetRulesIntent(Context context, String sourcePackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_TARGET_RULES)
                .putExtra(EXTRA_SOURCE_PACKAGE, sourcePackage);
    }

    public static Intent createSourceRulesIntent(Context context, String targetPackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_SOURCE_RULES)
                .putExtra(EXTRA_TARGET_PACKAGE, targetPackage);
    }

    public static Intent createTargetRuleDetailIntent(Context context, String sourcePackage,
            String targetPackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_TARGET_RULE_DETAIL)
                .putExtra(EXTRA_SOURCE_PACKAGE, sourcePackage)
                .putExtra(EXTRA_TARGET_PACKAGE, targetPackage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            createInitialFragment())
                    .commit();
        }
    }

    private Fragment createInitialFragment() {
        final Intent intent = getIntent();
        final String destination = intent.getStringExtra(EXTRA_DESTINATION);
        if (DESTINATION_APP_DETAIL.equals(destination)) {
            setTitle(R.string.app_jump_detail_title);
            return AppJumpAppDetailFragment.newInstance(intent.getStringExtra(EXTRA_PACKAGE_NAME));
        }
        if (DESTINATION_ALL_APPS.equals(destination)) {
            setTitle(R.string.app_jump_all_apps_title);
            return new AppJumpAllAppsFragment();
        }
        if (DESTINATION_TARGET_RULE_DETAIL.equals(destination)) {
            setTitle(R.string.app_jump_target_rule_detail_title);
            return AppJumpTargetRuleDetailFragment.newInstance(
                    intent.getStringExtra(EXTRA_SOURCE_PACKAGE),
                    intent.getStringExtra(EXTRA_TARGET_PACKAGE));
        }
        if (DESTINATION_TARGET_RULES.equals(destination)) {
            setTitle(R.string.app_jump_target_rules_title);
            return AppJumpTargetRulesFragment.newInstance(
                    intent.getStringExtra(EXTRA_SOURCE_PACKAGE));
        }
        if (DESTINATION_SOURCE_RULES.equals(destination)) {
            setTitle(R.string.app_jump_source_rules_title);
            return AppJumpSourceRulesFragment.newInstance(
                    intent.getStringExtra(EXTRA_TARGET_PACKAGE));
        }
        if (DESTINATION_CATEGORY.equals(destination)) {
            final int category = intent.getIntExtra(
                    EXTRA_CATEGORY, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK);
            setTitle(AppJumpPolicyBackend.getCategoryTitleRes(category));
            return AppJumpCategoryFragment.newInstance(category);
        }
        setTitle(R.string.app_jump_settings_title);
        return new AppJumpSettingsFragment();
    }
}
