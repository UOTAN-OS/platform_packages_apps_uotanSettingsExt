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

package org.uwuaosp.settingsext.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;

public final class IconUtils {

    private static final int HOME_ENTRY_ICON_SIZE_DP = 40;
    private static final int HOME_ENTRY_GLYPH_SIZE_DP = 24;

    private IconUtils() {
    }

    public static Drawable createHomeEntryIcon(Context context, int iconResId) {
        final int iconSize = dpToPx(context, HOME_ENTRY_ICON_SIZE_DP);
        final int maxGlyphSize = dpToPx(context, HOME_ENTRY_GLYPH_SIZE_DP);

        final GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(context.getColor(com.android.internal.R.color
                .materialColorSecondaryContainer));
        background.setSize(iconSize, iconSize);

        final Drawable glyph = context.getDrawable(iconResId).mutate();
        glyph.setTint(context.getColor(com.android.internal.R.color
                .materialColorOnSecondaryContainer));

        final int intrinsicWidth = glyph.getIntrinsicWidth();
        final int intrinsicHeight = glyph.getIntrinsicHeight();
        final int glyphWidth;
        final int glyphHeight;
        if (intrinsicWidth > 0 && intrinsicHeight > 0) {
            final float scale = Math.min(
                    (float) maxGlyphSize / intrinsicWidth,
                    (float) maxGlyphSize / intrinsicHeight);
            glyphWidth = Math.round(intrinsicWidth * scale);
            glyphHeight = Math.round(intrinsicHeight * scale);
        } else {
            glyphWidth = maxGlyphSize;
            glyphHeight = maxGlyphSize;
        }
        final int horizontalInset = (iconSize - glyphWidth) / 2;
        final int verticalInset = (iconSize - glyphHeight) / 2;

        final LayerDrawable icon = new LayerDrawable(new Drawable[]{background, glyph});
        icon.setLayerInset(1, horizontalInset, verticalInset, horizontalInset, verticalInset);
        icon.setLayerSize(1, glyphWidth, glyphHeight);
        return icon;
    }

    public static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
