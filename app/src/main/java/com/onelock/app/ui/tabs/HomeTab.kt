package com.onelock.app.ui.tabs

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.onelock.app.data.AppInfo
import com.onelock.app.ui.AppFilter
import com.onelock.app.ui.MainUiState
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUISearchBar
import com.onelock.app.ui.theme.OneUISuccess
import com.onelock.app.ui.theme.OneUISurfaceCard
import com.onelock.app.ui.theme.OneUISwitchThumbOff
import com.onelock.app.ui.theme.OneUISwitchTrackOff
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary
import com.onelock.app.ui.theme.OneUIWarning
import com.onelock.app.util.PermissionUtils

@Composable
fun HomeTab(
    uiState: MainUiState,
    getAppIcon: (String) -> Drawable?,
    onAppRowClick: (AppInfo) -> Unit,
    onToggleLock: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (AppFilter) -> Unit
) {
    val context = LocalContext.current
    val isSystemProtected = uiState.isAccessibilityEnabled && uiState.isOverlayGranted

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Collapsible One UI Header with live protection badge
        item {
            OneUIHomeHeader(isSystemProtected = isSystemProtected)
        }

        // Top Status Banner Card (only visible if permission is missing)
        val needsAction = !uiState.isAccessibilityEnabled || !uiState.isOverlayGranted || !uiState.isBatteryOptimizationsIgnored
        if (needsAction) {
            item {
                StatusBannerCard(
                    isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                    isOverlayGranted = uiState.isOverlayGranted,
                    isBatteryOptimizationsIgnored = uiState.isBatteryOptimizationsIgnored,
                    onEnableAccessibility = { PermissionUtils.openAccessibilitySettings(context) },
                    onGrantOverlay = { PermissionUtils.openOverlaySettings(context) },
                    onIgnoreBattery = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
                )
            }
        }

        // 2x2 Bento Summary Grid (Locked, Total, Masked, Intruders)
        item {
            BentoSummaryGrid(
                lockedCount = uiState.lockedPackageNames.size,
                totalCount = uiState.apps.size,
                maskedCount = uiState.maskedNotifications.size,
                intruderCount = uiState.intruderLogs.size
            )
        }

        // Search Bar
        item {
            PillSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        // Horizontally Scrollable Categorized Filter Chips
        item {
            CategorizedFilterChipsRow(
                activeFilter = uiState.activeFilter,
                onSelectFilter = onFilterSelect
            )
        }

        // App List Section
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = OneUIAccent)
                }
            }
        } else if (uiState.filteredApps.isEmpty()) {
            item {
                EmptyAppsPlaceholder(
                    searchQuery = uiState.searchQuery,
                    activeFilter = uiState.activeFilter
                )
            }
        } else {
            item {
                AppsGroupedCard(
                    apps = uiState.filteredApps,
                    lockedPackages = uiState.lockedPackageNames,
                    getAppIcon = getAppIcon,
                    onAppRowClick = onAppRowClick,
                    onToggleLock = onToggleLock
                )
            }
        }

        // Extra spacing so bottom nav bar never obscures content
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun OneUIHomeHeader(isSystemProtected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "OneLock",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Samsung One UI Security Guard",
                fontSize = 13.sp,
                color = OneUITextSecondary
            )
        }

        // Live Protection Status Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (isSystemProtected) OneUISuccess.copy(alpha = 0.18f) else OneUIWarning.copy(alpha = 0.18f))
                .border(
                    1.dp,
                    if (isSystemProtected) OneUISuccess else OneUIWarning,
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSystemProtected) OneUISuccess else OneUIWarning)
                )
                Text(
                    text = if (isSystemProtected) "Active Shield" else "Action Needed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemProtected) OneUISuccess else OneUIWarning
                )
            }
        }
    }
}

@Composable
private fun BentoSummaryGrid(
    lockedCount: Int,
    totalCount: Int,
    maskedCount: Int,
    intruderCount: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Locked Apps & Total Apps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoItem(
                modifier = Modifier.weight(1f),
                title = "Locked Apps",
                count = "$lockedCount",
                icon = Icons.Rounded.Lock,
                iconTint = OneUIAccent,
                badgeText = "Secured"
            )

            BentoItem(
                modifier = Modifier.weight(1f),
                title = "Total Apps",
                count = "$totalCount",
                icon = Icons.Rounded.Smartphone,
                iconTint = OneUITextPrimary,
                badgeText = "User Apps"
            )
        }

        // Row 2: Masked Notifications & Intruder Alerts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoItem(
                modifier = Modifier.weight(1f),
                title = "Masked Alerts",
                count = "$maskedCount",
                icon = Icons.Rounded.Notifications,
                iconTint = Color(0xFFAF52DE),
                badgeText = "Privacy Feed"
            )

            BentoItem(
                modifier = Modifier.weight(1f),
                title = "Intruder Snaps",
                count = "$intruderCount",
                icon = Icons.Rounded.CameraAlt,
                iconTint = if (intruderCount > 0) Color(0xFFFF453A) else OneUITextSecondary,
                badgeText = if (intruderCount > 0) "Alerts" else "Safe"
            )
        }
    }
}

@Composable
private fun BentoItem(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    badgeText: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = OneUISurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF222226))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = OneUITextSecondary
                    )
                }
            }

            Text(
                text = count,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OneUITextPrimary
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = OneUITextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategorizedFilterChipsRow(
    activeFilter: AppFilter,
    onSelectFilter: (AppFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.values().forEach { filter ->
            val isSelected = activeFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) OneUIAccent else OneUISurfaceCard)
                    .border(
                        1.dp,
                        if (isSelected) OneUIAccent else OneUIDivider,
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelectFilter(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else OneUITextSecondary
                )
            }
        }
    }
}

@Composable
private fun PillSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(OneUISearchBar)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search",
            tint = OneUITextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Search apps...",
                    color = OneUITextTertiary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = OneUITextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                cursorBrush = SolidColor(OneUIAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear",
                    tint = OneUITextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AppsGroupedCard(
    apps: List<AppInfo>,
    lockedPackages: Set<String>,
    getAppIcon: (String) -> Drawable?,
    onAppRowClick: (AppInfo) -> Unit,
    onToggleLock: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = OneUISurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            apps.forEachIndexed { index, app ->
                val isLocked = lockedPackages.contains(app.packageName)
                val iconDrawable = remember(app.packageName) { getAppIcon(app.packageName) }

                AppItemRow(
                    app = app,
                    icon = iconDrawable,
                    isLocked = isLocked,
                    onRowClick = { onAppRowClick(app) },
                    onToggle = { onToggleLock(app.packageName) }
                )

                if (index < apps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 78.dp, end = 20.dp)
                            .height(0.5.dp)
                            .background(OneUIDivider)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItemRow(
    app: AppInfo,
    icon: Drawable?,
    isLocked: Boolean,
    onRowClick: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRowClick() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF222226)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    val bitmap = remember(icon) {
                        try {
                            icon.toBitmap(120, 120).asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.name,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUITextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (app.hasCustomLock) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(OneUIAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Custom",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUIAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = OneUITextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Switch(
            checked = isLocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OneUIAccent,
                uncheckedThumbColor = OneUISwitchThumbOff,
                uncheckedTrackColor = OneUISwitchTrackOff,
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun StatusBannerCard(
    isAccessibilityEnabled: Boolean,
    isOverlayGranted: Boolean,
    isBatteryOptimizationsIgnored: Boolean,
    onEnableAccessibility: () -> Unit,
    onGrantOverlay: () -> Unit,
    onIgnoreBattery: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = OneUISurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OneUIWarning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Action Required",
                        tint = OneUIWarning,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Protection Setup",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )
            }

            Text(
                text = "Enable required permissions to ensure instant locking and prevent Samsung One UI from sleeping.",
                fontSize = 13.sp,
                color = OneUITextSecondary,
                lineHeight = 18.sp
            )

            PermissionActionRow(
                icon = Icons.Rounded.Security,
                title = "Accessibility Service",
                description = "Detects app launch events in real time",
                isGranted = isAccessibilityEnabled,
                actionLabel = "Enable",
                onAction = onEnableAccessibility
            )

            PermissionActionRow(
                icon = Icons.Rounded.Layers,
                title = "Appear on Top",
                description = "Shows lock screen over protected apps",
                isGranted = isOverlayGranted,
                actionLabel = "Grant",
                onAction = onGrantOverlay
            )

            PermissionActionRow(
                icon = Icons.Rounded.BatteryAlert,
                title = "Keep Alive on One UI",
                description = "Exempt from battery sleeping limits",
                isGranted = isBatteryOptimizationsIgnored,
                actionLabel = "Allow",
                onAction = onIgnoreBattery
            )
        }
    }
}

@Composable
private fun PermissionActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E22))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) OneUISuccess else OneUIAccent,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = OneUITextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Active",
                    tint = OneUISuccess,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUISuccess
                )
            }
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUIAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyAppsPlaceholder(
    searchQuery: String,
    activeFilter: AppFilter
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (activeFilter == AppFilter.LOCKED) Icons.Rounded.LockOpen else Icons.Rounded.Search,
                contentDescription = null,
                tint = OneUITextTertiary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No apps matching \"$searchQuery\""
                } else if (activeFilter == AppFilter.LOCKED) {
                    "No apps currently locked"
                } else {
                    "No apps found in this category"
                },
                fontSize = 15.sp,
                color = OneUITextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
