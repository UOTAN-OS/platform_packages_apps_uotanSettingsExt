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

package org.uwuaosp.settingsext.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class BackgroundManagementActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)
    private var observersRegistered = false

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            requestRefresh()
        }
    }

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            requestRefresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                BackgroundManagementScreen(
                    refreshToken = refreshToken.intValue,
                    onNavigateUp = ::finish,
                    onOpenSettings = {
                        startActivity(
                            Intent(this, BackgroundManagementSettingsActivity::class.java),
                        )
                    },
                    onRefresh = ::requestRefresh,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!observersRegistered) {
            val packageFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_EXPORTED)
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.UWU_APP_BACKGROUND_MODES),
                false,
                settingsObserver,
            )
            observersRegistered = true
        }
        requestRefresh()
    }

    override fun onStop() {
        if (observersRegistered) {
            unregisterReceiver(packageReceiver)
            contentResolver.unregisterContentObserver(settingsObserver)
            observersRegistered = false
        }
        super.onStop()
    }

    private fun requestRefresh() {
        refreshToken.intValue += 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundManagementScreen(
    refreshToken: Int,
    onNavigateUp: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { BackgroundAppRepository(context) }
    var apps by remember { mutableStateOf<List<BackgroundAppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(refreshToken) {
        loading = true
        loadFailed = false
        val result = runCatching {
            withContext(Dispatchers.IO) {
                repository.loadApps(BackgroundListPreferences.showSystemApps(context))
            }
        }
        result.onSuccess { apps = it }
        result.onFailure { loadFailed = true }
        loading = false
    }

    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.background_management_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
        actions = {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.background_settings_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text(stringResource(R.string.background_search_apps)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(
                                        R.string.background_search_close,
                                    ),
                                )
                            }
                        }
                    } else {
                        null
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {}

        SettingsCategory(title = stringResource(R.string.background_apps_category))
        when {
            loading -> BackgroundLoadingState()
            loadFailed -> BackgroundErrorState(onRefresh)
            filteredApps.isEmpty() -> BackgroundEmptyState()
            else -> filteredApps.forEachIndexed { index, app ->
                AppModePreferenceRow(
                    app = app,
                    position = preferencePosition(index, filteredApps.lastIndex),
                    onModeSelected = { mode ->
                        if (BackgroundModeSecureSettings.setMode(
                                context,
                                app.packageName,
                                mode,
                            )
                        ) {
                            apps = apps.map { entry ->
                                if (entry.packageName == app.packageName) {
                                    entry.copy(mode = mode)
                                } else {
                                    entry
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                R.string.background_mode_update_failed,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
                if (index != filteredApps.lastIndex) {
                    PreferenceGroupSpacer()
                }
            }
        }
    }
}

@Composable
private fun AppModePreferenceRow(
    app: BackgroundAppEntry,
    position: PreferencePosition,
    onModeSelected: (Int) -> Unit,
) {
    var expanded by remember(app.packageName) { mutableStateOf(false) }
    val shape = when (position) {
        PreferencePosition.Single -> RoundedCornerShape(20.dp)
        PreferencePosition.Top -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp,
        )
        PreferencePosition.Middle -> RoundedCornerShape(4.dp)
        PreferencePosition.Bottom -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp,
        )
    }
    val modeLabel = backgroundModeLabel(app.mode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (app.configurable) 1f else 0.38f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        enabled = app.configurable,
                        role = Role.Button,
                        onClick = { expanded = true },
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = modeLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_down_line),
                    contentDescription = stringResource(R.string.background_app_mode),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
            ) {
                val modes = listOf(
                    BackgroundModeSecureSettings.MODE_DEFAULT,
                    BackgroundModeSecureSettings.MODE_TOMBSTONE,
                    BackgroundModeSecureSettings.MODE_FULL,
                )
                modes.forEachIndexed { index, mode ->
                    ExpressiveModeMenuItem(
                        text = backgroundModeLabel(mode),
                        selected = app.mode == mode,
                        position = index,
                        itemCount = modes.size,
                        onClick = {
                            expanded = false
                            onModeSelected(mode)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveModeMenuItem(
    text: String,
    selected: Boolean,
    position: Int,
    itemCount: Int,
    onClick: () -> Unit,
) {
    val shape = if (selected) {
        RoundedCornerShape(12.dp)
    } else {
        when (position) {
            0 -> RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp,
            )
            itemCount - 1 -> RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            )
            else -> RoundedCornerShape(4.dp)
        }
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun backgroundModeLabel(mode: Int): String {
    return stringResource(
        when (mode) {
            BackgroundModeSecureSettings.MODE_TOMBSTONE -> R.string.background_mode_tombstone
            BackgroundModeSecureSettings.MODE_FULL -> R.string.background_mode_full
            else -> R.string.background_mode_default
        },
    )
}

@Composable
private fun BackgroundLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BackgroundErrorState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(44.dp))
        Text(
            text = stringResource(R.string.background_load_failed),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRefresh) {
            Text(stringResource(R.string.background_retry))
        }
    }
}

@Composable
private fun BackgroundEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.background_no_apps),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun preferencePosition(index: Int, lastIndex: Int): PreferencePosition {
    return when {
        lastIndex == 0 -> PreferencePosition.Single
        index == 0 -> PreferencePosition.Top
        index == lastIndex -> PreferencePosition.Bottom
        else -> PreferencePosition.Middle
    }
}
