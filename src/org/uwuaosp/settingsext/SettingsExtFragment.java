/*
 * Copyright (C) 2026 The uwuAOSP Project
 */

package org.uwuaosp.settingsext;

import android.os.Bundle;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class SettingsExtFragment extends SettingsBasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_ext);
    }
}
