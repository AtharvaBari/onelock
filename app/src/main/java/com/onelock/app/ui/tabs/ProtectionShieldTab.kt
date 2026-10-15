package com.onelock.app.ui.tabs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelock.app.data.ProtectionShieldConfig
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
import com.onelock.app.engine.LockState

@Composable
fun ProtectionShieldTab(
    shieldConfig: ProtectionShieldConfig,
    isNotificationListenerGranted: Boolean,
    isCameraGranted: Boolean,
    onSaveShieldConfig: (ProtectionShieldConfig) -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onRefreshPermissions()
        if (isGranted) {
            onSaveShieldConfig(shieldConfig.copy(intruderSelfieEnabled = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Protection Shield",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Advanced Anti-Tamper & Privacy Engines",
                    fontSize = 13.sp,
                    color = OneUITextSecondary
                )
            }
        }

        // Module 1: Anti-Tamper & Force-Stop Protection
        item {
            ShieldModuleCard(
                icon = Icons.Rounded.Security,
                iconTint = OneUIAccent,
                title = "Anti-Tamper Protection",
                description = "Prevents intruders from force-stopping OneLock, uninstalling it, or disabling Accessibility in Settings without your master credential.",
                isChecked = shieldConfig.antiTamperEnabled,
                onCheckedChange = { checked ->
                    onSaveShieldConfig(shieldConfig.copy(antiTamperEnabled = checked))
                }
            )
        }

        // Module 2: Recent Apps Blur / Privacy Shield
        item {
            ShieldModuleCard(
                icon = Icons.Rounded.Layers,
                iconTint = OneUIAccent,
                title = "Recent Apps Privacy Shield",
                description = "Automatically masks chat contents and sensitive app screens with a frosted privacy overlay in the Android multitasking overview.",
                isChecked = shieldConfig.recentAppsBlurEnabled,
                onCheckedChange = { checked ->
                    onSaveShieldConfig(shieldConfig.copy(recentAppsBlurEnabled = checked))
                }
            )
        }

        // Module 3: Notification Content Masking
        item {
            ShieldModuleCard(
                icon = Icons.Rounded.Notifications,
                iconTint = Color(0xFFAF52DE),
                title = "Notification Privacy Masking",
                description = "Intercepts sensitive notification previews from locked apps and replaces them with private masked cards on the status bar and Samsung lock screen.",
                isChecked = shieldConfig.notificationMaskingEnabled,
                isPermissionRequired = true,
                isPermissionGranted = isNotificationListenerGranted,
                permissionActionText = "Grant Listener Access",
                onGrantPermission = {
                    PermissionUtils.openNotificationListenerSettings(context)
                },
                onCheckedChange = { checked ->
                    if (checked && !isNotificationListenerGranted) {
                        PermissionUtils.openNotificationListenerSettings(context)
                    }
                    onSaveShieldConfig(shieldConfig.copy(notificationMaskingEnabled = checked))
                }
            )
        }

        // Module 4: CameraX Intruder Selfie on 2 Wrong Attempts
        item {
            ShieldModuleCard(
                icon = Icons.Rounded.CameraAlt,
                iconTint = Color(0xFFFF9F0A),
                title = "Intruder Camera Trap",
                description = "Silently captures a photo using the front camera after 2 failed unlock attempts and stores it privately in your Vault log.",
                isChecked = shieldConfig.intruderSelfieEnabled,
                isPermissionRequired = true,
                isPermissionGranted = isCameraGranted,
                permissionActionText = "Grant Camera Access",
                onGrantPermission = {
                    LockState.isLaunchingPermissionIntent = true
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCheckedChange = { checked ->
                    if (checked && !isCameraGranted) {
                        LockState.isLaunchingPermissionIntent = true
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        onSaveShieldConfig(shieldConfig.copy(intruderSelfieEnabled = checked))
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding().height(100.dp))
        }
    }
}

@Composable
private fun ShieldModuleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    isChecked: Boolean,
    isPermissionRequired: Boolean = false,
    isPermissionGranted: Boolean = true,
    permissionActionText: String = "",
    onGrantPermission: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
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

            Text(
                text = description,
                fontSize = 13.sp,
                color = OneUITextSecondary,
                lineHeight = 18.sp
            )

            // Permission status or action button if required
            if (isPermissionRequired) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1E22))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPermissionGranted) OneUISuccess else OneUIAccent)
                        )
                        Text(
                            text = if (isPermissionGranted) "Permission Granted" else "Permission Required",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPermissionGranted) OneUISuccess else OneUITextSecondary
                        )
                    }

                    if (!isPermissionGranted) {
                        Button(
                            onClick = onGrantPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = permissionActionText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Granted",
                            tint = OneUISuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
