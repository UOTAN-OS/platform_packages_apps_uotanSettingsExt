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

import android.app.ActivityOptions;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.uwuaosp.settingsext.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllAppsActivity extends CollapsingToolbarBaseActivity {
    private static final int SPAN_COUNT = 4;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final AllAppsAdapter mAdapter = new AllAppsAdapter();

    private RecyclerView mAppGrid;
    private ProgressBar mLoading;
    private int mLoadGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_apps);
        setTitle(R.string.popup_all_apps);

        mAppGrid = findViewById(R.id.app_grid);
        mLoading = findViewById(R.id.loading);

        mAppGrid.setLayoutManager(new GridLayoutManager(this, SPAN_COUNT));
        mAppGrid.setAdapter(mAdapter);
        mAppGrid.setItemAnimator(null);

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

    public static void startInPopup(Context context) {
        Intent intent = new Intent(context, AllAppsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_MINI_WINDOW_EXT);
        options.setPendingIntentBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
        context.startActivity(intent, options.toBundle());
    }

    private void loadApps() {
        final int loadGeneration = ++mLoadGeneration;
        mLoading.setVisibility(View.VISIBLE);
        mAppGrid.setVisibility(View.GONE);
        mExecutor.execute(() -> {
            List<AppEntry> apps = loadLaunchableApps();
            mMainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || loadGeneration != mLoadGeneration) {
                    return;
                }
                mAdapter.submitList(apps);
                mLoading.setVisibility(View.GONE);
                mAppGrid.setVisibility(View.VISIBLE);
            });
        });
    }

    private List<AppEntry> loadLaunchableApps() {
        final PackageManager pm = getPackageManager();
        final Map<String, AppEntry> deduped = new LinkedHashMap<>();
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<android.content.pm.ResolveInfo> resolveInfos = pm.queryIntentActivities(
                intent, PackageManager.GET_META_DATA | PackageManager.MATCH_ALL);
        for (android.content.pm.ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo == null) {
                continue;
            }
            addAppEntry(deduped, pm, resolveInfo.activityInfo.applicationInfo);
        }

        if (deduped.isEmpty()) {
            final List<ApplicationInfo> applications =
                    pm.getInstalledApplications(PackageManager.MATCH_ALL);
            for (ApplicationInfo appInfo : applications) {
                if (!isLaunchableUserApp(pm, appInfo)) {
                    continue;
                }
                addAppEntry(deduped, pm, appInfo);
            }
        }

        final ArrayList<AppEntry> apps = new ArrayList<>(deduped.values());
        apps.sort(Comparator.comparing(AppEntry::getAppName, String.CASE_INSENSITIVE_ORDER));
        return apps;
    }

    private void addAppEntry(Map<String, AppEntry> deduped, PackageManager pm,
            ApplicationInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        final String packageName = appInfo.packageName;
        if (packageName == null || deduped.containsKey(packageName)) {
            return;
        }
        final CharSequence label = pm.getApplicationLabel(appInfo);
        final Drawable icon = pm.getApplicationIcon(appInfo);
        if (icon == null) {
            return;
        }
        deduped.put(packageName, new AppEntry(
                label != null ? label.toString() : packageName,
                packageName,
                icon));
    }

    private boolean isLaunchableUserApp(PackageManager pm, ApplicationInfo appInfo) {
        if (appInfo == null || appInfo.packageName == null) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return false;
        }
        return pm.getLaunchIntentForPackage(appInfo.packageName) != null;
    }

    private void launchApp(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            showLaunchFailToast();
            return;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchWindowingMode(WindowConfiguration.WINDOWING_MODE_MINI_WINDOW_EXT);
            options.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            startActivity(launchIntent, options.toBundle());
            finish();
        } catch (Exception e) {
            showLaunchFailToast();
        }
    }

    private void showLaunchFailToast() {
        Toast.makeText(this, R.string.popup_cannot_launch_app, Toast.LENGTH_SHORT).show();
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

    private final class AllAppsAdapter extends RecyclerView.Adapter<AppViewHolder> {
        private final ArrayList<AppEntry> mItems = new ArrayList<>();

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_all_app_grid, parent, false);
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        void submitList(List<AppEntry> apps) {
            mItems.clear();
            mItems.addAll(apps);
            notifyDataSetChanged();
        }
    }

    private final class AppViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mName;

        AppViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.app_icon);
            mName = itemView.findViewById(R.id.app_name);
        }

        void bind(AppEntry app) {
            mIcon.setImageDrawable(app.getIcon());
            mIcon.setContentDescription(app.getAppName());
            mName.setText(app.getAppName());
            itemView.setOnClickListener(v -> launchApp(app.getPackageName()));
        }
    }
}
