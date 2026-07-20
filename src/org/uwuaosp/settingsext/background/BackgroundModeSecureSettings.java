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

package org.uwuaosp.settingsext.background;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class BackgroundModeSecureSettings {
    public static final int MODE_DEFAULT =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_DEFAULT;
    public static final int MODE_TOMBSTONE =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_TOMBSTONE;
    public static final int MODE_FULL =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_FULL;

    private BackgroundModeSecureSettings() {
    }

    public static boolean isIgnoreTaskRemovalEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_IGNORE_TASK_REMOVAL, 0,
                UserHandle.myUserId()) != 0;
    }

    public static boolean setIgnoreTaskRemovalEnabled(Context context, boolean enabled) {
        return Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_IGNORE_TASK_REMOVAL, enabled ? 1 : 0,
                UserHandle.myUserId());
    }

    public static synchronized ArrayMap<String, Integer> getModes(Context context) {
        final ArrayMap<String, Integer> modes = new ArrayMap<>();
        final String value = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_MODES, UserHandle.myUserId());
        if (value == null || value.isBlank()) {
            return modes;
        }
        try {
            final JSONObject object = new JSONObject(value);
            final Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                final String packageName = keys.next();
                final int mode = object.optInt(packageName, MODE_DEFAULT);
                if (mode == MODE_TOMBSTONE || mode == MODE_FULL) {
                    modes.put(packageName, mode);
                }
            }
        } catch (JSONException ignored) {
            // system_server normalizes malformed values; the UI treats them as default.
        }
        return modes;
    }

    public static synchronized boolean setMode(
            Context context, String packageName, int mode) {
        final TreeMap<String, Integer> modes = new TreeMap<>();
        modes.putAll(getModes(context));
        if (mode == MODE_TOMBSTONE || mode == MODE_FULL) {
            modes.put(packageName, mode);
        } else {
            modes.remove(packageName);
        }

        final JSONObject object = new JSONObject();
        for (Map.Entry<String, Integer> entry : modes.entrySet()) {
            try {
                object.put(entry.getKey(), entry.getValue());
            } catch (JSONException impossible) {
                throw new AssertionError(impossible);
            }
        }
        final String value = modes.isEmpty() ? null : object.toString();
        return Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_MODES, value, UserHandle.myUserId());
    }
}
