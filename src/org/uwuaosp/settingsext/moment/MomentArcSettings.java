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

package org.uwuaosp.settingsext.moment;

import android.content.Context;

import org.uwuaosp.settingsext.util.SettingsUtils;

import java.util.List;

final class MomentArcSettings {
    private static final String INNER_RING_TARGETS =
            "moment_arc_selected_targets";
    private static final String OUTER_RING_TARGETS =
            "moment_arc_outer_ring_selected_targets";

    private MomentArcSettings() {
    }

    static void saveInnerRingTargets(Context context, List<String> targets) {
        SettingsUtils.putSystemString(context, INNER_RING_TARGETS,
                SettingsUtils.joinList(targets, "|"));
    }

    static List<String> getInnerRingTargets(Context context) {
        return SettingsUtils.splitList(
                SettingsUtils.getSystemString(context, INNER_RING_TARGETS), "|");
    }

    static void saveOuterRingTargets(Context context, List<String> targets) {
        SettingsUtils.putSystemString(context, OUTER_RING_TARGETS,
                SettingsUtils.joinList(targets, "|"));
    }

    static List<String> getOuterRingTargets(Context context) {
        return SettingsUtils.splitList(
                SettingsUtils.getSystemString(context, OUTER_RING_TARGETS), "|");
    }
}
