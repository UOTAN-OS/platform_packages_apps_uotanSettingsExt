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

package org.uwuaosp.settingsext.appjump;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.uwuaosp.settingsext.R;

final class AppJumpSearchUtils {
    private AppJumpSearchUtils() {
    }

    static SearchView addSearchMenuItem(@NonNull MenuInflater inflater, @NonNull Menu menu,
            @NonNull AppJumpPolicyBackend backend, @NonNull CharSequence hint,
            @Nullable String currentQuery,
            @NonNull SearchView.OnQueryTextListener listener) {
        inflater.inflate(org.uwuaosp.settingsext.R.menu.app_jump_search, menu);
        final MenuItem systemAppsItem =
                menu.findItem(org.uwuaosp.settingsext.R.id.app_jump_system_apps_menu);
        if (systemAppsItem != null) {
            systemAppsItem.setTitle(backend.isShowSystemAppsEnabled()
                    ? R.string.app_jump_hide_system_apps_title
                    : R.string.app_jump_show_system_apps_title);
        }
        final MenuItem searchItem = menu.findItem(org.uwuaosp.settingsext.R.id.app_jump_search_menu);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(hint);
        searchView.setOnQueryTextListener(listener);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        final int searchTextId = searchView.getContext().getResources().getIdentifier(
                "search_src_text", "id", "android");
        final TextView searchText = searchTextId != 0
                ? searchView.findViewById(searchTextId)
                : null;
        if (searchText != null) {
            searchText.setTextCursorDrawable(searchText.getContext().getDrawable(
                    R.drawable.search_cursor));
        }
        if (currentQuery != null && !currentQuery.isEmpty()) {
            searchItem.expandActionView();
            searchView.setIconified(false);
            searchView.setQuery(currentQuery, false);
            searchView.clearFocus();
        }
        return searchView;
    }

    static boolean handleListOptionsItemSelected(@NonNull MenuItem item,
            @NonNull AppJumpPolicyBackend backend, @NonNull Runnable reloadCallback,
            @NonNull Runnable invalidateMenuCallback) {
        if (item.getItemId() != R.id.app_jump_system_apps_menu) {
            return false;
        }
        backend.setShowSystemAppsEnabled(!backend.isShowSystemAppsEnabled());
        invalidateMenuCallback.run();
        reloadCallback.run();
        return true;
    }
}
