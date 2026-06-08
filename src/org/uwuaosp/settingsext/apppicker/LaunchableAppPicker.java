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

package org.uwuaosp.settingsext.apppicker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public final class LaunchableAppPicker {

    private LaunchableAppPicker() {
    }

    public static String resolveAppName(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return packageName;
        }
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    public static Drawable resolveAppIcon(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        if (!TextUtils.isEmpty(packageName)) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                return pm.getApplicationIcon(appInfo);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return pm.getDefaultActivityIcon();
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
