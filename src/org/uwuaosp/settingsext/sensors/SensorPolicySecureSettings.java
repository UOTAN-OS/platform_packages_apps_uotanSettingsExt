/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.uwuaosp.settingsext.sensors;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class SensorPolicySecureSettings {
    public static final int POLICY_ALLOW = Settings.Secure.UWU_APP_SENSOR_POLICY_ALLOW;
    public static final int POLICY_DENY_ON_LAUNCH =
            Settings.Secure.UWU_APP_SENSOR_POLICY_DENY_ON_LAUNCH;
    public static final int POLICY_DENY_ALWAYS = Settings.Secure.UWU_APP_SENSOR_POLICY_DENY_ALWAYS;

    private SensorPolicySecureSettings() {}

    public static synchronized ArrayMap<String, Integer> getPolicies(Context context) {
        final ArrayMap<String, Integer> result = new ArrayMap<>();
        final String value = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_SENSOR_POLICIES, UserHandle.myUserId());
        if (value == null || value.isBlank()) return result;
        try {
            final JSONObject object = new JSONObject(value);
            final Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                final String packageName = keys.next();
                final int policy = object.optInt(packageName, POLICY_ALLOW);
                if (policy == POLICY_DENY_ON_LAUNCH || policy == POLICY_DENY_ALWAYS) {
                    result.put(packageName, policy);
                }
            }
        } catch (JSONException ignored) {
            // The system service ignores malformed values as well.
        }
        return result;
    }

    public static synchronized boolean setPolicy(Context context, String packageName, int policy) {
        final TreeMap<String, Integer> policies = new TreeMap<>();
        policies.putAll(getPolicies(context));
        if (policy == POLICY_DENY_ON_LAUNCH || policy == POLICY_DENY_ALWAYS) {
            policies.put(packageName, policy);
        } else {
            policies.remove(packageName);
        }

        final JSONObject object = new JSONObject();
        try {
            for (Map.Entry<String, Integer> entry : policies.entrySet()) {
                object.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException impossible) {
            throw new AssertionError(impossible);
        }
        return Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_SENSOR_POLICIES,
                policies.isEmpty() ? null : object.toString(), UserHandle.myUserId());
    }
}
