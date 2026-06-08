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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.uwuaosp.settingsext.R;

public class KeyAttestationSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_KEYBOX_DATA = "keybox_data_setting";
    private static final String KEY_KEYBOX_INFO = "keybox_data_info";
    private static final String KEY_PIF_DATA = "pif_data_setting";
    private static final String KEY_PIF_INFO = "pif_data_info";

    private final ActivityResultLauncher<Intent> mKeyboxFilePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                            return;
                        }
                        Uri uri = result.getData().getData();
                        Preference preference = findPreference(KEY_KEYBOX_DATA);
                        if (preference instanceof KeyboxDataPreference) {
                            ((KeyboxDataPreference) preference).handleFileSelected(uri);
                            updateInfoFooters();
                        }
                    });

    private final ActivityResultLauncher<Intent> mPifFilePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                            return;
                        }
                        Uri uri = result.getData().getData();
                        Preference preference = findPreference(KEY_PIF_DATA);
                        if (preference instanceof PifDataPreference) {
                            ((PifDataPreference) preference).handleFileSelected(uri);
                            updateInfoFooters();
                        }
                    });

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.key_attestation_settings);

        Preference keyboxPreference = findPreference(KEY_KEYBOX_DATA);
        if (keyboxPreference instanceof KeyboxDataPreference) {
            ((KeyboxDataPreference) keyboxPreference).setFilePickerLauncher(
                    mKeyboxFilePickerLauncher);
            keyboxPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateInfoFooters();
                return true;
            });
        }

        Preference pifPreference = findPreference(KEY_PIF_DATA);
        if (pifPreference instanceof PifDataPreference) {
            ((PifDataPreference) pifPreference).setFilePickerLauncher(mPifFilePickerLauncher);
            pifPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                updateInfoFooters();
                return true;
            });
        }

        updateInfoFooters();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInfoFooters();
    }

    private void updateInfoFooters() {
        FooterPreference keyboxInfo = findPreference(KEY_KEYBOX_INFO);
        if (keyboxInfo != null) {
            keyboxInfo.setTitle(KeyAttestationSummaryUtils.buildKeyboxFooterSummary(
                    requireContext(),
                    KeyAttestationSecureSettings.getKeyboxData(requireContext())));
        }

        FooterPreference pifInfo = findPreference(KEY_PIF_INFO);
        if (pifInfo != null) {
            pifInfo.setTitle(KeyAttestationSummaryUtils.buildPifFooterSummary(
                    requireContext(),
                    KeyAttestationSecureSettings.getPifData(requireContext())));
        }
    }
}
