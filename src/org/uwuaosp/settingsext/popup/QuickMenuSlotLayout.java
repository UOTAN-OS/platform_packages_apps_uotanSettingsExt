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

package org.uwuaosp.settingsext.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.uwuaosp.settingsext.R;

import java.util.ArrayList;
import java.util.List;

public class QuickMenuSlotLayout extends ViewGroup {
    static final int INNER_SLOTS = 6;
    static final int OUTER_SLOTS = 7;
    static final int TOTAL_SLOTS = 13;
    static final int CONFIGURABLE_SLOT_COUNT = 12;
    static final int INNER_CONFIGURABLE = 5;
    static final int OUTER_CONFIGURABLE = 7;
    private static final int MORE_APPS_SLOT = 5;

    private static final int SLOT_SIZE_DP = 40;
    private static final int CIRCLE_RADIUS_DP = 144;
    private static final int OUTER_SLOT_SIZE_DP = 32;
    private static final int OUTER_CIRCLE_RADIUS_DP = 210;
    private static final int SIDE_OFFSET_DP = 36;
    private static final int BOTTOM_OFFSET_DP = 28;
    private static final float ICON_SPACING_MULTIPLIER = 1.5f;
    private static final float OUTER_ICON_SPACING_MULTIPLIER = 1.2f;
    private static final int INNER_ANGLE_START = 45;
    private static final int INNER_ANGLE_END = 135;
    private static final int OUTER_ANGLE_START = 30;
    private static final int OUTER_ANGLE_END = 150;

    private final int mSlotSizePx;
    private final int mCircleRadiusPx;
    private final int mOuterSlotSizePx;
    private final int mOuterCircleRadiusPx;
    private final int mSideOffsetPx;
    private final int mBottomOffsetPx;

    private final ArrayList<SlotItem> mSlots = new ArrayList<>(TOTAL_SLOTS);
    private boolean mIsLeftSide = true;

    @Nullable
    private OnSlotClickListener mOnSlotClickListener;
    @Nullable
    private OnSlotLongClickListener mOnSlotLongClickListener;
    @Nullable
    private OnSlotDropListener mOnSlotDropListener;

    public QuickMenuSlotLayout(Context context) {
        this(context, null);
    }

    public QuickMenuSlotLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickMenuSlotLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSlotSizePx = dpToPx(SLOT_SIZE_DP);
        mCircleRadiusPx = dpToPx(CIRCLE_RADIUS_DP);
        mOuterSlotSizePx = dpToPx(OUTER_SLOT_SIZE_DP);
        mOuterCircleRadiusPx = dpToPx(OUTER_CIRCLE_RADIUS_DP);
        mSideOffsetPx = dpToPx(SIDE_OFFSET_DP);
        mBottomOffsetPx = dpToPx(BOTTOM_OFFSET_DP);
        setClipChildren(false);
        setClipToPadding(false);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            final int slotIndex = i;
            ImageView slotView = new ImageView(context);
            slotView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            slotView.setOnClickListener(v -> {
                if (!isEditableSlot(slotIndex) || mOnSlotClickListener == null) {
                    return;
                }
                mOnSlotClickListener.onSlotClick(slotIndex);
            });
            slotView.setOnLongClickListener(v -> isEditableSlot(slotIndex)
                    && mOnSlotLongClickListener != null
                    && mOnSlotLongClickListener.onSlotLongClick(slotIndex));
            slotView.setOnDragListener((v, event) -> handleDragEvent(slotIndex, event));
            addView(slotView);
            mSlots.add(SlotItem.empty());
        }
        updateChildStates();
    }

    public void setLeftSide(boolean isLeftSide) {
        if (mIsLeftSide == isLeftSide) {
            return;
        }
        mIsLeftSide = isLeftSide;
        requestLayout();
    }

    public void setOnSlotClickListener(@Nullable OnSlotClickListener listener) {
        mOnSlotClickListener = listener;
    }

    public void setOnSlotLongClickListener(@Nullable OnSlotLongClickListener listener) {
        mOnSlotLongClickListener = listener;
    }

    public void setOnSlotDropListener(@Nullable OnSlotDropListener listener) {
        mOnSlotDropListener = listener;
    }

    public void setSlots(@NonNull List<SlotItem> slots) {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (i < slots.size() && slots.get(i) != null) {
                mSlots.set(i, slots.get(i));
            } else if (i == MORE_APPS_SLOT) {
                mSlots.set(i, SlotItem.moreApps(
                        getContext().getDrawable(R.drawable.ic_popup_more_apps),
                        getContext().getString(R.string.popup_editor_more_apps)));
            } else {
                mSlots.set(i, SlotItem.empty());
            }
        }
        updateChildStates();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int innerSpec = MeasureSpec.makeMeasureSpec(mSlotSizePx, MeasureSpec.EXACTLY);
        int outerSpec = MeasureSpec.makeMeasureSpec(mOuterSlotSizePx, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            boolean isInner = i < INNER_SLOTS;
            getChildAt(i).measure(isInner ? innerSpec : outerSpec,
                    isInner ? innerSpec : outerSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }

        int width = right - left;
        int height = bottom - top;
        float centerX = mIsLeftSide
                ? getPaddingLeft() + mSideOffsetPx
                : width - getPaddingRight() - mSideOffsetPx;
        float centerY = height - getPaddingBottom() - mBottomOffsetPx;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            boolean isInner = i < INNER_SLOTS;
            int localIndex = isInner ? i : i - INNER_SLOTS;
            int localTotal = isInner ? INNER_SLOTS : OUTER_SLOTS;
            float slotRadius = (isInner ? mSlotSizePx : mOuterSlotSizePx) / 2f;
            float radius = isInner ? mCircleRadiusPx : mOuterCircleRadiusPx;
            float spacing = isInner ? ICON_SPACING_MULTIPLIER : OUTER_ICON_SPACING_MULTIPLIER;
            float angleStart = isInner ? INNER_ANGLE_START : OUTER_ANGLE_START;
            float angleEnd = isInner ? INNER_ANGLE_END : OUTER_ANGLE_END;
            float angleRange = angleEnd - angleStart;

            int position = localIndex + 1;
            float angle = angleStart + ((localTotal + 1) / 2f - position)
                    * (angleRange / (localTotal + 1)) * spacing;

            float childCenterX;
            if (mIsLeftSide) {
                childCenterX = centerX
                        + (float) (radius * Math.cos(Math.toRadians(angle)));
            } else {
                childCenterX = centerX
                        - (float) (radius * Math.cos(Math.toRadians(angle)));
            }
            float childCenterY = centerY
                    - (float) (radius * Math.sin(Math.toRadians(angle)));

            int childLeft = Math.round(childCenterX - slotRadius);
            int childTop = Math.round(childCenterY - slotRadius);
            child.layout(
                    childLeft,
                    childTop,
                    childLeft + child.getMeasuredWidth(),
                    childTop + child.getMeasuredHeight());
        }
    }

    private boolean handleDragEvent(int slotIndex, DragEvent event) {
        if (!isEditableSlot(slotIndex)) {
            return false;
        }
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return event.getLocalState() != null;
            case DragEvent.ACTION_DROP:
                return mOnSlotDropListener != null
                        && mOnSlotDropListener.onSlotDrop(slotIndex, event.getLocalState());
            default:
                return true;
        }
    }

    private boolean isEditableSlot(int slotIndex) {
        return (slotIndex >= 0 && slotIndex < INNER_CONFIGURABLE)
                || (slotIndex >= INNER_SLOTS && slotIndex < TOTAL_SLOTS);
    }

    private void updateChildStates() {
        for (int i = 0; i < getChildCount(); i++) {
            ImageView slotView = (ImageView) getChildAt(i);
            SlotItem item = mSlots.get(i);
            slotView.setImageDrawable(item.getIcon());
            if (item.getBackgroundResId() != 0) {
                slotView.setBackgroundResource(item.getBackgroundResId());
            } else {
                slotView.setBackground(null);
            }
            slotView.setContentDescription(item.getContentDescription());
            slotView.setClickable(isEditableSlot(i));
            slotView.setLongClickable(isEditableSlot(i));
        }
        invalidate();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null;
    }

    public interface OnSlotClickListener {
        void onSlotClick(int slotIndex);
    }

    public interface OnSlotLongClickListener {
        boolean onSlotLongClick(int slotIndex);
    }

    public interface OnSlotDropListener {
        boolean onSlotDrop(int slotIndex, @Nullable Object payload);
    }

    public static final class SlotItem {
        @Nullable
        private final Drawable mIcon;
        private final int mBackgroundResId;
        @NonNull
        private final CharSequence mContentDescription;

        private SlotItem(@Nullable Drawable icon, int backgroundResId,
                @NonNull CharSequence contentDescription) {
            mIcon = icon;
            mBackgroundResId = backgroundResId;
            mContentDescription = contentDescription;
        }

        @NonNull
        public static SlotItem empty() {
            return new SlotItem(null, R.drawable.quick_menu_editor_slot_empty, "");
        }

        @NonNull
        public static SlotItem empty(@NonNull CharSequence contentDescription) {
            return new SlotItem(null, R.drawable.quick_menu_editor_slot_empty, contentDescription);
        }

        @NonNull
        public static SlotItem filled(@NonNull Drawable icon, @NonNull CharSequence contentDescription) {
            return new SlotItem(icon, 0, contentDescription);
        }

        @NonNull
        public static SlotItem moreApps(@Nullable Drawable icon,
                @NonNull CharSequence contentDescription) {
            return new SlotItem(icon, 0, contentDescription);
        }

        @Nullable
        Drawable getIcon() {
            return mIcon;
        }

        int getBackgroundResId() {
            return mBackgroundResId;
        }

        @NonNull
        CharSequence getContentDescription() {
            return mContentDescription;
        }
    }
}
