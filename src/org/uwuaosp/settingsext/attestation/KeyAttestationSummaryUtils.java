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
import android.text.TextUtils;

import org.json.JSONObject;
import org.uwuaosp.settingsext.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class KeyAttestationSummaryUtils {
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private KeyAttestationSummaryUtils() {
    }

    public static CharSequence buildKeyboxFooterSummary(Context context, String keyboxXml) {
        if (TextUtils.isEmpty(keyboxXml)) {
            return context.getString(R.string.key_attestation_keybox_footer_empty);
        }

        KeyboxInfo info = parseKeyboxInfo(keyboxXml);
        StringBuilder summary = new StringBuilder();
        appendLine(summary, context.getString(R.string.key_attestation_status_imported));
        appendLine(summary, context.getString(
                R.string.key_attestation_keybox_algorithms_value,
                info.hasEcdsa ? context.getString(R.string.key_attestation_keybox_algorithm_ecdsa)
                        : context.getString(R.string.key_attestation_keybox_algorithm_missing),
                info.hasRsa ? context.getString(R.string.key_attestation_keybox_algorithm_rsa)
                        : context.getString(R.string.key_attestation_keybox_algorithm_missing)));
        appendLine(summary, context.getString(
                R.string.key_attestation_keybox_cert_counts_value,
                info.ecdsaCertCount, info.rsaCertCount));
        if (!TextUtils.isEmpty(info.leafSubject)) {
            appendLine(summary, context.getString(
                    R.string.key_attestation_keybox_subject_value, info.leafSubject));
        }
        if (!TextUtils.isEmpty(info.leafIssuer)) {
            appendLine(summary, context.getString(
                    R.string.key_attestation_keybox_issuer_value, info.leafIssuer));
        }
        if (!TextUtils.isEmpty(info.validFrom) || !TextUtils.isEmpty(info.validUntil)) {
            appendLine(summary, context.getString(
                    R.string.key_attestation_keybox_validity_value,
                    emptyToFallback(context, info.validFrom),
                    emptyToFallback(context, info.validUntil)));
        }
        return summary;
    }

    public static CharSequence buildPifFooterSummary(Context context, String pifJson) {
        if (TextUtils.isEmpty(pifJson)) {
            return context.getString(R.string.key_attestation_pif_footer_empty);
        }

        StringBuilder summary = new StringBuilder();
        appendLine(summary, context.getString(R.string.key_attestation_status_imported));
        try {
            JSONObject json = new JSONObject(pifJson);
            String brand = firstNonEmpty(json, "BRAND", "brand", "Build.BRAND");
            String manufacturer = firstNonEmpty(
                    json, "MANUFACTURER", "manufacturer", "Build.MANUFACTURER");
            String product = firstNonEmpty(json, "PRODUCT", "product", "Build.PRODUCT");
            String device = firstNonEmpty(json, "DEVICE", "device", "Build.DEVICE");
            String model = firstNonEmpty(json, "MODEL", "model", "Build.MODEL");
            String fingerprint = firstNonEmpty(
                    json, "FINGERPRINT", "fingerprint", "Build.FINGERPRINT");
            String securityPatch = firstNonEmpty(
                    json,
                    "SECURITY_PATCH",
                    "security_patch",
                    "VERSION.SECURITY_PATCH",
                    "Build.VERSION.SECURITY_PATCH");

            if (!TextUtils.isEmpty(brand) || !TextUtils.isEmpty(manufacturer)) {
                appendLine(summary, context.getString(
                        R.string.key_attestation_pif_brand_value,
                        joinNonEmpty(" / ", brand, manufacturer)));
            }
            if (!TextUtils.isEmpty(model)) {
                appendLine(summary, context.getString(
                        R.string.key_attestation_pif_model_value, model));
            }
            if (!TextUtils.isEmpty(product) || !TextUtils.isEmpty(device)) {
                appendLine(summary, context.getString(
                        R.string.key_attestation_pif_device_value,
                        joinNonEmpty(" / ", product, device)));
            }
            if (!TextUtils.isEmpty(securityPatch)) {
                appendLine(summary, context.getString(
                        R.string.key_attestation_pif_security_patch_value, securityPatch));
            }
            if (!TextUtils.isEmpty(fingerprint)) {
                appendLine(summary, context.getString(
                        R.string.key_attestation_pif_fingerprint_value, fingerprint));
            }
        } catch (Exception e) {
            appendLine(summary, context.getString(R.string.key_attestation_status_unparsed));
        }
        return summary;
    }

    private static KeyboxInfo parseKeyboxInfo(String keyboxXml) {
        KeyboxInfo info = new KeyboxInfo();
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new java.io.StringReader(keyboxXml));

            String currentAlg = null;
            for (int eventType = parser.next();
                    eventType != XmlPullParser.END_DOCUMENT;
                    eventType = parser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("Key".equals(name)) {
                        currentAlg = parser.getAttributeValue(null, "algorithm");
                        if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                            info.hasEcdsa = true;
                        } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                            info.hasRsa = true;
                        }
                    } else if ("Certificate".equals(name)) {
                        String format = parser.getAttributeValue(null, "format");
                        if (!"pem".equalsIgnoreCase(format)) {
                            continue;
                        }
                        String pem = readTagText(parser);
                        if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                            info.ecdsaCertCount++;
                        } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                            info.rsaCertCount++;
                        }
                        if (TextUtils.isEmpty(info.leafSubject)) {
                            X509Certificate certificate = parsePemCertificate(pem);
                            if (certificate != null) {
                                info.leafSubject = certificate.getSubjectX500Principal().getName();
                                info.leafIssuer = certificate.getIssuerX500Principal().getName();
                                info.validFrom = formatDate(certificate.getNotBefore());
                                info.validUntil = formatDate(certificate.getNotAfter());
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "Key".equals(parser.getName())) {
                    currentAlg = null;
                }
            }
        } catch (Exception ignored) {
        }
        return info;
    }

    private static String readTagText(XmlPullParser parser) throws Exception {
        StringBuilder text = new StringBuilder();
        for (int eventType = parser.next();
                !(eventType == XmlPullParser.END_TAG && "Certificate".equals(parser.getName()));
                eventType = parser.next()) {
            if (eventType == XmlPullParser.TEXT
                    || eventType == XmlPullParser.CDSECT
                    || eventType == XmlPullParser.ENTITY_REF) {
                text.append(parser.getText());
            }
        }
        return text.toString().trim();
    }

    private static X509Certificate parsePemCertificate(String pem) {
        if (TextUtils.isEmpty(pem)) {
            return null;
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonEmpty(JSONObject json, String... keys) {
        for (String key : keys) {
            String value = json.optString(key, null);
            if (!TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private static String joinNonEmpty(String delimiter, String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(date);
    }

    private static String emptyToFallback(Context context, String value) {
        return TextUtils.isEmpty(value)
                ? context.getString(R.string.key_attestation_unknown)
                : value;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (TextUtils.isEmpty(line)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private static final class KeyboxInfo {
        boolean hasEcdsa;
        boolean hasRsa;
        int ecdsaCertCount;
        int rsaCertCount;
        String leafSubject;
        String leafIssuer;
        String validFrom;
        String validUntil;
    }
}
