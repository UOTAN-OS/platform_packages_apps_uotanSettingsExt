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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class KeyboxDataPreference extends Preference {
    private static final String TAG = "KeyboxDataPref";
    private static final String[] XML_MIME_TYPES = {
            "text/xml",
            "application/xml",
            "application/xhtml+xml",
            "text/plain",
    };

    private ActivityResultLauncher<Intent> mFilePickerLauncher;

    public KeyboxDataPreference(Context context, AttributeSet attrs) {
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
            intent.putExtra(Intent.EXTRA_MIME_TYPES, XML_MIME_TYPES);
            mFilePickerLauncher.launch(intent);
        });

        deleteButton.setOnClickListener(v -> {
            KeyAttestationSecureSettings.setKeyboxData(getContext(), null);
            Toast.makeText(getContext(), R.string.key_attestation_xml_cleared, Toast.LENGTH_SHORT)
                    .show();
            callChangeListener(null);
        });
    }

    public void handleFileSelected(Uri uri) {
        if (uri == null || !isXmlFile(uri)) {
            Toast.makeText(getContext(), R.string.key_attestation_invalid_file, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder xmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xmlContent.append(line).append('\n');
            }

            String xml = xmlContent.toString();
            if (!validateXml(xml)) {
                Toast.makeText(getContext(), R.string.key_attestation_invalid_xml,
                                Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            KeyAttestationSecureSettings.setKeyboxData(getContext(), xml);
            Toast.makeText(getContext(), R.string.key_attestation_xml_loaded, Toast.LENGTH_SHORT)
                    .show();
            callChangeListener(xml);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read XML file", e);
            Toast.makeText(getContext(), R.string.key_attestation_xml_read_failed,
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private boolean isXmlFile(Uri uri) {
        final String uriString = uri.toString().toLowerCase();
        if (uriString.endsWith(".xml")) {
            return true;
        }
        final String type = getContext().getContentResolver().getType(uri);
        if (type == null) {
            return false;
        }
        return ClipDescription.compareMimeTypes(type, "text/xml")
                || ClipDescription.compareMimeTypes(type, "application/xml")
                || ClipDescription.compareMimeTypes(type, "application/*+xml")
                || ClipDescription.compareMimeTypes(type, "text/plain");
    }

    private boolean validateXml(String xml) {
        boolean hasEcdsaKey = false;
        boolean hasRsaKey = false;
        boolean hasEcdsaPrivKey = false;
        boolean hasRsaPrivKey = false;
        int ecdsaCertCount = 0;
        int rsaCertCount = 0;
        int numberOfKeyboxes = -1;

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xml));

            String currentAlg = null;

            for (int eventType = parser.next();
                    eventType != XmlPullParser.END_DOCUMENT;
                    eventType = parser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    switch (name) {
                        case "NumberOfKeyboxes":
                            parser.next();
                            if (parser.getEventType() == XmlPullParser.TEXT) {
                                try {
                                    numberOfKeyboxes = Integer.parseInt(parser.getText().trim());
                                } catch (NumberFormatException e) {
                                    numberOfKeyboxes = -1;
                                }
                            }
                            break;
                        case "Key":
                            currentAlg = parser.getAttributeValue(null, "algorithm");
                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                hasEcdsaKey = true;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                hasRsaKey = true;
                            } else {
                                currentAlg = null;
                            }
                            break;
                        case "PrivateKey":
                            String privateKeyFormat = parser.getAttributeValue(null, "format");
                            if (!"pem".equalsIgnoreCase(privateKeyFormat)) {
                                Log.w(TAG, "Invalid or missing format for PrivateKey");
                                return false;
                            }
                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                hasEcdsaPrivKey = true;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                hasRsaPrivKey = true;
                            }
                            break;
                        case "Certificate":
                            String certificateFormat = parser.getAttributeValue(null, "format");
                            if (!"pem".equalsIgnoreCase(certificateFormat)) {
                                Log.w(TAG, "Invalid or missing format for Certificate");
                                return false;
                            }
                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                ecdsaCertCount++;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                rsaCertCount++;
                            }
                            break;
                        default:
                            break;
                    }
                } else if (eventType == XmlPullParser.END_TAG && "Key".equals(parser.getName())) {
                    currentAlg = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "XML validation failed", e);
            return false;
        }

        return numberOfKeyboxes == 1
                && hasEcdsaKey && hasEcdsaPrivKey && ecdsaCertCount >= 1
                && hasRsaKey && hasRsaPrivKey && rsaCertCount >= 1;
    }
}
