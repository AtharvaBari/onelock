package com.onelock.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelock.app.data.GlobalLockConfig
import com.onelock.app.data.LockType
import com.onelock.app.data.ReLockTimeout
import com.onelock.app.ui.CredentialSetupDialog
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUISuccess
import com.onelock.app.ui.theme.OneUISurfaceCard
import com.onelock.app.ui.theme.OneUISwitchThumbOff
import com.onelock.app.ui.theme.OneUISwitchTrackOff
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary
import com.onelock.app.util.PermissionUtils

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun SettingsTab(
    globalConfig: GlobalLockConfig,
    isAccessibilityEnabled: Boolean,
    isOverlayGranted: Boolean,
    isNotificationListenerGranted: Boolean,
    isCameraGranted: Boolean,
    isBatteryOptimizationsIgnored: Boolean,
    onSaveGlobalConfig: (GlobalLockConfig) -> Unit
) {
    val context = LocalContext.current
    var showSetupDialogForType by remember { mutableStateOf<LockType?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Security Settings",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Global Lock Engine & Health Check",
                    fontSize = 13.sp,
                    color = OneUITextSecondary
                )
            }
        }

        // Section 1: Default Lock Method Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = OneUISurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Global Lock Type",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )

                    Text(
                        text = "Base security method used for all locked apps and self-protection.",
                        fontSize = 12.sp,
                        color = OneUITextSecondary
                    )

                    // 4 Lock Types
                    LockType.values().forEach { type ->
                        val isSelected = globalConfig.defaultLockType == type

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) OneUIAccent.copy(alpha = 0.18f) else Color(0xFF1E1E22))
                                .border(
                                    1.dp,
                                    if (isSelected) OneUIAccent else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (!isSelected) {
                                        showSetupDialogForType = type
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = type.subtitle,
                                    fontSize = 11.sp,
                                    color = OneUITextTertiary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(OneUIAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Change Credential Button
                    Button(
                        onClick = { showSetupDialogForType = globalConfig.defaultLockType },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF252528),
                            contentColor = OneUIAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Base ${globalConfig.defaultLockType.displayName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Biometric Unlock Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = OneUIAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Fingerprint Unlock",
                                fontSize = 14.sp,
                                color = OneUITextPrimary
                            )
                        }

                        Switch(
                            checked = globalConfig.biometricEnabled,
                            onCheckedChange = { checked ->
                                onSaveGlobalConfig(globalConfig.copy(biometricEnabled = checked))
                            },
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
            }
        }

        // Section 2: Keypad Enhancements Card
        item {
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
                    Text(
                        text = "Keypad & Input Privacy",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )

                    // Scramble Keypad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scramble Keypad Numbers",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Randomizes digit order on each unlock to prevent shoulder-surfing",
                                fontSize = 12.sp,
                                color = OneUITextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = globalConfig.scrambleKeypad,
                            onCheckedChange = { checked ->
                                onSaveGlobalConfig(globalConfig.copy(scrambleKeypad = checked))
                            },
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

                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(OneUIDivider))

                    // Hide Pattern Trail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Pattern Trail",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = "Does not draw connecting lines while swiping pattern",
                                fontSize = 12.sp,
                                color = OneUITextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = globalConfig.hidePatternTrail,
                            onCheckedChange = { checked ->
                                onSaveGlobalConfig(globalConfig.copy(hidePatternTrail = checked))
                            },
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
            }
        }

        // Section 3: Smart Re-Lock Timeout
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = OneUISurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Smart Re-Lock Timeout",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )

                    Text(
                        text = "Choose when locked apps re-lock after exiting.",
                        fontSize = 12.sp,
                        color = OneUITextSecondary
                    )

                    ReLockTimeout.values().forEach { timeout ->
                        val isSelected = globalConfig.reLockTimeout == timeout

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) OneUIAccent.copy(alpha = 0.18f) else Color(0xFF1E1E22))
                                .clickable {
                                    onSaveGlobalConfig(globalConfig.copy(reLockTimeout = timeout))
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = timeout.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OneUITextPrimary else OneUITextSecondary
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(OneUIAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Permission Health Check
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = OneUISurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Permission Health Check",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )

                    Text(
                        text = "Ensures Samsung One UI does not pause protection background services.",
                        fontSize = 12.sp,
                        color = OneUITextSecondary
                    )

                    HealthItemRow(
                        icon = Icons.Rounded.Security,
                        title = "Accessibility Engine",
                        isGranted = isAccessibilityEnabled,
                        onFix = { PermissionUtils.openAccessibilitySettings(context) }
                    )

                    HealthItemRow(
                        icon = Icons.Rounded.Layers,
                        title = "Appear on Top (Overlay)",
                        isGranted = isOverlayGranted,
                        onFix = { PermissionUtils.openOverlaySettings(context) }
                    )

                    HealthItemRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notification Listener",
                        isGranted = isNotificationListenerGranted,
                        onFix = { PermissionUtils.openNotificationListenerSettings(context) }
                    )

                    HealthItemRow(
                        icon = Icons.Rounded.CameraAlt,
                        title = "Camera for Intruders",
                        isGranted = isCameraGranted,
                        onFix = {}
                    )

                    HealthItemRow(
                        icon = Icons.Rounded.BatteryAlert,
                        title = "Battery Unrestricted",
                        isGranted = isBatteryOptimizationsIgnored,
                        onFix = { PermissionUtils.requestIgnoreBatteryOptimizations(context) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding().height(100.dp))
        }
    }

    showSetupDialogForType?.let { targetType ->
        CredentialSetupDialog(
            lockType = targetType,
            titlePrefix = "Set Base",
            onDismiss = { showSetupDialogForType = null },
            onCredentialConfirmed = { newCredential ->
                onSaveGlobalConfig(
                    globalConfig.copy(
                        defaultLockType = targetType,
                        baseCredential = newCredential,
                        isOnboardingCompleted = true
                    )
                )
                showSetupDialogForType = null
            }
        )
    }
}

@Composable
private fun HealthItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E22))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) OneUISuccess else OneUIAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                color = OneUITextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Active",
                    tint = OneUISuccess,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Ready",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUISuccess
                )
            }
        } else {
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUIAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Grant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
