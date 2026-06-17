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

package org.uwuaosp.settingsext;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.uwuaosp.settingsext.popup.PopupSettingsFragment;

public class SettingsExtActivity extends CollapsingToolbarBaseActivity {
    public static final String EXTRA_OPEN_POPUP_SETTINGS = "open_popup_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean openPopup = getIntent().getBooleanExtra(EXTRA_OPEN_POPUP_SETTINGS, false);
        setTitle(openPopup ? R.string.popup_settings_title : R.string.app_name);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            openPopup ? new PopupSettingsFragment() : new SettingsExtFragment())
                    .commit();
        }
    }
}
