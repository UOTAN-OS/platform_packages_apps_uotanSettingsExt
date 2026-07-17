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

package org.uwuaosp.settingsext.apppicker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.DrawableStateLinearLayout;

import org.uwuaosp.settingsext.R;
import org.uwuaosp.settingsext.lyric.LyricSecureSettings;
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSecureSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppSelectionActivity extends CollapsingToolbarBaseActivity {
    public static final String EXTRA_SELECTION_MODE = "selection_mode";
    public static final String EXTRA_INITIAL_PACKAGE = "initial_package";
    public static final String EXTRA_RESULT_PACKAGE = "result_package";
    public static final String EXTRA_RESULT_LABEL = "result_label";
    public static final String SELECTION_MODE_LYRIC_WHITELIST = "lyric_whitelist";
    public static final String SELECTION_MODE_MUSIC_SUGGESTION = "music_suggestion";
    public static final String SELECTION_MODE_SINGLE_APP = "single_app";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final ArrayList<AppEntry> mAppList = new ArrayList<>();
    private final ArrayList<String> mSelectedApps = new ArrayList<>();

    private String mSelectionMode = SELECTION_MODE_SINGLE_APP;
    private boolean mAllowReorder = false;
    private boolean mSingleSelect = false;
    private boolean mResultOnly = false;
    private int mMaxSelectionCount = 1;
    private int mScreenTitleRes = R.string.app_picker_title;
    private int mSelectedSectionTitleRes = R.string.app_picker_selected_app;

    private RecyclerView mAppListView;
    private ProgressBar mLoading;
    private AppSelectionAdapter mAdapter;
    private ItemTouchHelper mTouchHelper;
    private int mLoadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selection);
        resolveSelectionMode();
        setTitle(getString(mScreenTitleRes));

        mAppListView = findViewById(R.id.app_list);
        mLoading = findViewById(R.id.loading);

        mSelectedApps.addAll(loadSelectedApps());

        mAdapter = new AppSelectionAdapter();
        mAppListView.setLayoutManager(new LinearLayoutManager(this));
        mAppListView.setAdapter(mAdapter);
        mAppListView.setItemAnimator(new DefaultItemAnimator());

        mTouchHelper = new ItemTouchHelper(new SelectedAppsTouchHelperCallback());
        mTouchHelper.attachToRecyclerView(mAppListView);
        mAdapter.setItemTouchHelper(mTouchHelper);

    }

    @Override
    protected void onStart() {
        super.onStart();
        loadApps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resolveSelectionMode() {
        String mode = getIntent().getStringExtra(EXTRA_SELECTION_MODE);
        mSelectionMode = mode != null ? mode : SELECTION_MODE_SINGLE_APP;
        if (SELECTION_MODE_MUSIC_SUGGESTION.equals(mSelectionMode)) {
            mAllowReorder = false;
            mSingleSelect = true;
            mMaxSelectionCount = 1;
            mScreenTitleRes = R.string.switch_music_suggestion_title;
            mSelectedSectionTitleRes = R.string.app_picker_selected_app;
        } else if (SELECTION_MODE_LYRIC_WHITELIST.equals(mSelectionMode)) {
            mAllowReorder = false;
            mMaxSelectionCount = Integer.MAX_VALUE;
            mScreenTitleRes = R.string.lyric_whitelist_title;
            mSelectedSectionTitleRes = R.string.lyric_whitelisted_apps;
        } else if (SELECTION_MODE_SINGLE_APP.equals(mSelectionMode)) {
            mAllowReorder = false;
            mSingleSelect = true;
            mResultOnly = true;
            mMaxSelectionCount = 1;
            mScreenTitleRes = R.string.app_picker_title;
            mSelectedSectionTitleRes = R.string.app_picker_selected_app;
        }
    }

    private void loadApps() {
        final int loadGeneration = ++mLoadGeneration;
        mLoading.setVisibility(View.VISIBLE);
        mAppListView.setVisibility(View.GONE);
        mExecutor.execute(() -> {
            List<AppEntry> loaded = loadLaunchableApps();
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || loadGeneration != mLoadGeneration) {
                    return;
                }
                mAppList.clear();
                mAppList.addAll(loaded);
                rebuildItems();
                mLoading.setVisibility(View.GONE);
                mAppListView.setVisibility(View.VISIBLE);
            });
        });
    }

    private List<AppEntry> loadLaunchableApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentActivities(
                intent, PackageManager.GET_META_DATA | PackageManager.MATCH_ALL);
        Map<String, AppEntry> deduped = new LinkedHashMap<>();
        for (android.content.pm.ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null) {
                continue;
            }
            String packageName = resolveInfo.activityInfo.packageName;
            if (packageName == null || deduped.containsKey(packageName)) {
                continue;
            }
            CharSequence label = resolveInfo.loadLabel(pm);
            Drawable icon = resolveInfo.loadIcon(pm);
            if (icon == null) {
                continue;
            }
            deduped.put(packageName, new AppEntry(
                    label != null ? label.toString() : packageName,
                    packageName,
                    icon));
        }
        ArrayList<AppEntry> apps = new ArrayList<>(deduped.values());
        apps.sort(Comparator.comparing(AppEntry::getAppName, String.CASE_INSENSITIVE_ORDER));
        return apps;
    }

    private void rebuildItems() {
        PackageManager pm = getPackageManager();
        ArrayList<String> validSelected = new ArrayList<>();
        for (String pkg : mSelectedApps) {
            try {
                pm.getApplicationInfo(pkg, 0);
                validSelected.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        if (!validSelected.equals(mSelectedApps)) {
            mSelectedApps.clear();
            mSelectedApps.addAll(validSelected);
            persistSelection();
        }
        mAdapter.buildItems();
    }

    private List<String> loadSelectedApps() {
        if (SELECTION_MODE_MUSIC_SUGGESTION.equals(mSelectionMode)) {
            ArrayList<String> selected = new ArrayList<>();
            selected.add(SmartSuggestionsSecureSettings.getMusicPackage(
                    this, getString(R.string.default_music_app)));
            return selected;
        }
        if (SELECTION_MODE_SINGLE_APP.equals(mSelectionMode)) {
            ArrayList<String> selected = new ArrayList<>();
            String initialPackage = getIntent().getStringExtra(EXTRA_INITIAL_PACKAGE);
            if (initialPackage != null && !initialPackage.trim().isEmpty()) {
                selected.add(initialPackage);
            }
            return selected;
        }
        if (SELECTION_MODE_LYRIC_WHITELIST.equals(mSelectionMode)) {
            return LyricSecureSettings.getAllowedPackages(this);
        }
        return new ArrayList<>();
    }

    private void persistSelection() {
        if (SELECTION_MODE_MUSIC_SUGGESTION.equals(mSelectionMode)) {
            if (!mSelectedApps.isEmpty()) {
                SmartSuggestionsSecureSettings.setMusicPackage(this, mSelectedApps.get(0));
                SmartSuggestionsSecureSettings.setMusicEnabled(this, true);
            }
            return;
        }
        if (SELECTION_MODE_LYRIC_WHITELIST.equals(mSelectionMode)) {
            LyricSecureSettings.setAllowedPackages(this, mSelectedApps);
            return;
        }
    }

    private static final class AppEntry {
        private final String mAppName;
        private final String mPackageName;
        private final Drawable mIcon;

        AppEntry(String appName, String packageName, Drawable icon) {
            mAppName = appName;
            mPackageName = packageName;
            mIcon = icon;
        }

        String getAppName() {
            return mAppName;
        }

        String getPackageName() {
            return mPackageName;
        }

        Drawable getIcon() {
            return mIcon;
        }
    }

    private interface ItemMoveCallback {
        boolean onItemMove(int fromPosition, int toPosition);
    }

    private static final class UiItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_EMPTY = 1;
        static final int TYPE_APP = 2;

        final int type;
        final int titleRes;
        @Nullable final AppEntry app;
        final boolean isSelectedSection;

        UiItem(int type, int titleRes, @Nullable AppEntry app, boolean isSelectedSection) {
            this.type = type;
            this.titleRes = titleRes;
            this.app = app;
            this.isSelectedSection = isSelectedSection;
        }

        static UiItem header(int titleRes) {
            return new UiItem(TYPE_HEADER, titleRes, null, false);
        }

        static UiItem empty(int titleRes) {
            return new UiItem(TYPE_EMPTY, titleRes, null, false);
        }

        static UiItem selected(AppEntry app) {
            return new UiItem(TYPE_APP, 0, app, true);
        }

        static UiItem all(AppEntry app) {
            return new UiItem(TYPE_APP, 0, app, false);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof UiItem)) return false;
            UiItem other = (UiItem) obj;
            if (type != other.type || titleRes != other.titleRes
                    || isSelectedSection != other.isSelectedSection) {
                return false;
            }
            if (app == null && other.app == null) {
                return true;
            }
            if (app == null || other.app == null) {
                return false;
            }
            return mEquals(app.getPackageName(), other.app.getPackageName())
                    && mEquals(app.getAppName(), other.app.getAppName());
        }

        private boolean mEquals(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    }

    private final class AppSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements ItemMoveCallback {
        private final ArrayList<UiItem> mItems = new ArrayList<>();
        private final PackageManager mPm = getPackageManager();
        private ItemTouchHelper mItemTouchHelper;

        void setItemTouchHelper(ItemTouchHelper helper) {
            mItemTouchHelper = helper;
        }

        void buildItems() {
            ArrayList<UiItem> newItems = new ArrayList<>();
            newItems.add(UiItem.header(mSelectedSectionTitleRes));

            ArrayList<AppEntry> selectedInfos = new ArrayList<>();
            for (String pkg : mSelectedApps) {
                AppEntry appInfo = resolveAppInfo(pkg);
                if (appInfo != null) {
                    selectedInfos.add(appInfo);
                }
            }
            if (selectedInfos.isEmpty()) {
                newItems.add(UiItem.empty(R.string.app_picker_no_apps_selected));
            } else {
                for (AppEntry app : selectedInfos) {
                    newItems.add(UiItem.selected(app));
                }
            }

            newItems.add(UiItem.header(R.string.app_picker_all_apps));
            ArrayList<AppEntry> unselected = new ArrayList<>();
            for (AppEntry app : mAppList) {
                if (!mSelectedApps.contains(app.getPackageName())) {
                    unselected.add(app);
                }
            }
            unselected.sort(Comparator.comparing(AppEntry::getAppName, String.CASE_INSENSITIVE_ORDER));
            for (AppEntry app : unselected) {
                newItems.add(UiItem.all(app));
            }

            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new UiItemDiffCallback(mItems, newItems));
            mItems.clear();
            mItems.addAll(newItems);
            diff.dispatchUpdatesTo(this);
        }

        boolean isSelectedItem(int position) {
            UiItem item = position >= 0 && position < mItems.size() ? mItems.get(position) : null;
            return item != null && item.isSelectedSection;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == UiItem.TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false));
            }
            if (viewType == UiItem.TYPE_EMPTY) {
                return new EmptyViewHolder(inflater.inflate(R.layout.item_empty_state, parent, false));
            }
            return new AppViewHolder(inflater.inflate(R.layout.item_app_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UiItem item = mItems.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind(item.titleRes);
            } else if (holder instanceof EmptyViewHolder) {
                ((EmptyViewHolder) holder).bind(item.titleRes);
            } else if (holder instanceof AppViewHolder) {
                ((AppViewHolder) holder).bind(item);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position).type;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            if (!isSelectedItem(fromPosition) || !isSelectedItem(toPosition)) {
                return false;
            }
            int fromIndex = selectedIndexForAdapterPos(fromPosition);
            int toIndex = selectedIndexForAdapterPos(toPosition);
            if (fromIndex < 0 || toIndex < 0) {
                return false;
            }

            String pkg = mSelectedApps.remove(fromIndex);
            int insertIndex = toIndex > fromIndex ? toIndex - 1 : toIndex;
            mSelectedApps.add(insertIndex, pkg);

            UiItem movedItem = mItems.remove(fromPosition);
            mItems.add(toPosition, movedItem);
            notifyItemMoved(fromPosition, toPosition);
            persistSelection();
            return true;
        }

        private int selectedIndexForAdapterPos(int position) {
            int idx = 0;
            for (int i = 0; i < mItems.size(); i++) {
                UiItem item = mItems.get(i);
                if (item.isSelectedSection) {
                    if (i == position) {
                        return idx;
                    }
                    idx++;
                }
            }
            return -1;
        }

        @Nullable
        private AppEntry resolveAppInfo(String packageName) {
            try {
                android.content.pm.ApplicationInfo appInfo = mPm.getApplicationInfo(packageName, 0);
                String label = mPm.getApplicationLabel(appInfo).toString();
                return new AppEntry(label, packageName, mPm.getApplicationIcon(appInfo));
            } catch (Exception e) {
                return null;
            }
        }

        private final class HeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTitle;

            HeaderViewHolder(View itemView) {
                super(itemView);
                mTitle = itemView.findViewById(R.id.section_title);
            }

            void bind(int titleRes) {
                mTitle.setText(titleRes);
            }
        }

        private final class EmptyViewHolder extends RecyclerView.ViewHolder {
            private final TextView mText;

            EmptyViewHolder(View itemView) {
                super(itemView);
                mText = itemView.findViewById(R.id.empty_text);
            }

            void bind(int textRes) {
                mText.setText(textRes);
            }
        }

        private final class AppViewHolder extends RecyclerView.ViewHolder {
            private final DrawableStateLinearLayout mRoot;
            private final ImageView mIcon;
            private final TextView mName;
            private final TextView mPkg;
            private final CheckBox mCheck;
            private final ImageView mDragHandle;

            AppViewHolder(View itemView) {
                super(itemView);
                mRoot = (DrawableStateLinearLayout) itemView;
                mIcon = itemView.findViewById(R.id.app_icon);
                mName = itemView.findViewById(R.id.app_name);
                mPkg = itemView.findViewById(R.id.app_package);
                mCheck = itemView.findViewById(R.id.app_check);
                mDragHandle = itemView.findViewById(R.id.app_drag);
            }

            void bind(UiItem item) {
                if (item.app == null) {
                    return;
                }
                mRoot.setExtraDrawableState(new int[] { android.R.attr.state_single });
                mIcon.setImageDrawable(item.app.getIcon());
                mIcon.setContentDescription(item.app.getAppName());
                mName.setText(item.app.getAppName());
                mPkg.setText(item.app.getPackageName());
                mCheck.setClickable(!mSingleSelect);
                mCheck.setFocusable(!mSingleSelect);
                mCheck.setOnCheckedChangeListener(null);
                mCheck.setChecked(item.isSelectedSection);
                mDragHandle.setVisibility(mAllowReorder && item.isSelectedSection ? View.VISIBLE : View.INVISIBLE);
                if (mSingleSelect) {
                    mCheck.setOnCheckedChangeListener(null);
                    itemView.setOnClickListener(v -> handleSingleSelect(item.app));
                } else {
                    mCheck.setOnCheckedChangeListener(
                            (buttonView, isChecked) -> handleToggle(item.app, isChecked));
                    itemView.setOnClickListener(v -> mCheck.toggle());
                }
                mDragHandle.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN && item.isSelectedSection) {
                        if (mItemTouchHelper != null) {
                            mItemTouchHelper.startDrag(this);
                        }
                        return true;
                    }
                    return false;
                });
            }
        }

        private void handleSingleSelect(AppEntry app) {
            if (app == null) {
                return;
            }
            mSelectedApps.clear();
            mSelectedApps.add(app.getPackageName());
            if (mResultOnly) {
                Intent result = new Intent()
                        .putExtra(EXTRA_RESULT_PACKAGE, app.getPackageName())
                        .putExtra(EXTRA_RESULT_LABEL, app.getAppName());
                setResult(RESULT_OK, result);
            } else {
                persistSelection();
                setResult(RESULT_OK);
            }
            finish();
        }

        private void handleToggle(AppEntry app, boolean checked) {
            if (checked) {
                if (mAllowReorder && mSelectedApps.size() >= mMaxSelectionCount) {
                    Toast.makeText(
                            AppSelectionActivity.this,
                            getString(R.string.app_picker_selection_limit_toast,
                                    mMaxSelectionCount),
                            Toast.LENGTH_SHORT)
                            .show();
                    buildItems();
                    return;
                }
                if (!mSelectedApps.contains(app.getPackageName())) {
                    mSelectedApps.add(app.getPackageName());
                }
            } else {
                mSelectedApps.remove(app.getPackageName());
            }
            persistSelection();
            buildItems();
        }
    }

    private final class SelectedAppsTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
        SelectedAppsTouchHelperCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder) {
            if (mAllowReorder && mAdapter.isSelectedItem(viewHolder.getBindingAdapterPosition())) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }
            return 0;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            return mAdapter.onItemMove(
                    viewHolder.getBindingAdapterPosition(),
                    target.getBindingAdapterPosition());
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
    }

    private static final class UiItemDiffCallback extends DiffUtil.Callback {
        private final List<UiItem> mOldItems;
        private final List<UiItem> mNewItems;

        UiItemDiffCallback(List<UiItem> oldItems, List<UiItem> newItems) {
            mOldItems = oldItems;
            mNewItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return mOldItems.size();
        }

        @Override
        public int getNewListSize() {
            return mNewItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            UiItem oldItem = mOldItems.get(oldItemPosition);
            UiItem newItem = mNewItems.get(newItemPosition);
            if (oldItem.type != newItem.type) {
                return false;
            }
            if (oldItem.type == UiItem.TYPE_APP) {
                return oldItem.app != null && newItem.app != null
                        && oldItem.app.getPackageName().equals(newItem.app.getPackageName());
            }
            return oldItem.titleRes == newItem.titleRes;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldItems.get(oldItemPosition).equals(mNewItems.get(newItemPosition));
        }
    }

}
