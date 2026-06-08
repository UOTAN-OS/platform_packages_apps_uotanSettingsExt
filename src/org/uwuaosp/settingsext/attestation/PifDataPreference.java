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

import android.app.ActivityManager;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.uwuaosp.settingsext.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PifDataPreference extends Preference {
    private static final String TAG = "PifDataPref";
    private static final String[] JSON_MIME_TYPES = {
            "application/json",
            "text/json",
            "text/plain",
    };
    private static final String[] TARGET_PACKAGES = {
            "com.google.android.gms",
            "com.android.vending",
    };

    private ActivityResultLauncher<Intent> mFilePickerLauncher;

    public PifDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_with_delete);
    }

    public void setFilePickerLauncher(ActivityResultLauncher<Intent> launcher) {
        mFilePickerLauncher = launcher;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView title = (TextView) holder.findViewById(R.id.title);
        TextView summary = (TextView) holder.findViewById(R.id.summary);
        ImageButton deleteButton = (ImageButton) holder.findViewById(R.id.delete_button);

        title.setText(getTitle());
        summary.setText(getSummary());

        holder.itemView.setOnClickListener(v -> {
            if (mFilePickerLauncher == null) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, JSON_MIME_TYPES);
            mFilePickerLauncher.launch(intent);
        });

        deleteButton.setOnClickListener(v -> {
            KeyAttestationSecureSettings.setPifData(getContext(), null);
            Toast.makeText(getContext(), R.string.key_attestation_json_cleared, Toast.LENGTH_SHORT)
                    .show();
            callChangeListener(null);
            killPackages();
        });
    }

    public void handleFileSelected(Uri uri) {
        if (uri == null || !isJsonFile(uri)) {
            Toast.makeText(getContext(), R.string.key_attestation_invalid_file, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append('\n');
            }

            String json = jsonContent.toString();
            KeyAttestationSecureSettings.setPifData(getContext(), json);
            Toast.makeText(getContext(), R.string.key_attestation_json_loaded, Toast.LENGTH_SHORT)
                    .show();
            callChangeListener(json);
            killPackages();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read JSON file", e);
            Toast.makeText(getContext(), R.string.key_attestation_json_read_failed,
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private boolean isJsonFile(Uri uri) {
        final String uriString = uri.toString().toLowerCase();
        if (uriString.endsWith(".json")) {
            return true;
        }
        final String type = getContext().getContentResolver().getType(uri);
        if (type == null) {
            return false;
        }
        return ClipDescription.compareMimeTypes(type, "application/json")
                || ClipDescription.compareMimeTypes(type, "text/json")
                || ClipDescription.compareMimeTypes(type, "text/plain");
    }

    private void killPackages() {
        ActivityManager activityManager =
                getContext().getSystemService(ActivityManager.class);
        if (activityManager == null) {
            return;
        }

        for (String packageName : TARGET_PACKAGES) {
            try {
                activityManager.getClass()
                        .getMethod("forceStopPackage", String.class)
                        .invoke(activityManager, packageName);
                Log.i(TAG, packageName + " process killed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to kill package " + packageName, e);
            }
        }
    }
}
