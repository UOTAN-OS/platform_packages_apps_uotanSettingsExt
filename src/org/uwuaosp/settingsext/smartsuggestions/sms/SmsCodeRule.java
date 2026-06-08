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

public final class SmsCodeRule {
    private final String mPresetId;
    private final String mName;
    private final String mPattern;
    private final boolean mEnabled;

    public SmsCodeRule(String name, String pattern, boolean enabled) {
        this(null, name, pattern, enabled);
    }

    public SmsCodeRule(String presetId, String name, String pattern, boolean enabled) {
        mPresetId = presetId;
        mName = name;
        mPattern = pattern;
        mEnabled = enabled;
    }

    public String getPresetId() {
        return mPresetId;
    }

    public String getName() {
        return mName;
    }

    public String getPattern() {
        return mPattern;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public boolean isPreset() {
        return mPresetId != null;
    }
}
