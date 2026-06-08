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

package org.uwuaosp.settingsext.attestation;

import android.content.Context;
import android.provider.Settings;

public final class KeyAttestationSecureSettings {
    private KeyAttestationSecureSettings() {
    }

    public static String getKeyboxData(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.KEYBOX_DATA);
    }

    public static void setKeyboxData(Context context, String value) {
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.KEYBOX_DATA, value);
    }

    public static String getPifData(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.PIF_DATA);
    }

    public static void setPifData(Context context, String value) {
        Settings.Secure.putString(context.getContentResolver(), Settings.Secure.PIF_DATA, value);
    }
}
