package com.onelock.app.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.onelock.app.data.AppInfo
import com.onelock.app.data.AppLockConfig
import com.onelock.app.data.LockType
import com.onelock.app.data.ReLockTimeout
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUISurfaceCard
import com.onelock.app.ui.theme.OneUISwitchThumbOff
import com.onelock.app.ui.theme.OneUISwitchTrackOff
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary

@Composable
fun AppDetailDialog(
    app: AppInfo,
    icon: Drawable?,
    isLocked: Boolean,
    initialConfig: AppLockConfig,
    onDismiss: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onSaveConfig: (AppLockConfig) -> Unit,
    onResetToGlobal: () -> Unit
) {
    var useCustomLock by remember { mutableStateOf(initialConfig.useCustomLock) }
    var selectedLockType by remember { mutableStateOf(initialConfig.customLockType) }
    var currentCredential by remember { mutableStateOf(initialConfig.customCredential) }
    var biometricEnabled by remember { mutableStateOf(initialConfig.biometricEnabled) }
    var selectedTimeout by remember { mutableStateOf(initialConfig.customTimeout) }
    var showSetupDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = OneUISurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // App Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF222226))
                        .border(1.dp, OneUIDivider, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        val bitmap = remember(icon) {
                            try {
                                icon.toBitmap(150, 150).asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = app.name,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = app.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = app.packageName,
                    fontSize = 12.sp,
                    color = OneUITextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary One UI Lock Switch Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isLocked) OneUIAccent.copy(alpha = 0.15f) else Color(0xFF1E1E22))
                        .border(1.dp, if (isLocked) OneUIAccent else Color.Transparent, RoundedCornerShape(18.dp))
                        .clickable { onToggleLock(!isLocked) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (isLocked) OneUIAccent else OneUITextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Lock this app",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUITextPrimary
                            )
                            Text(
                                text = if (isLocked) "Protected by OneLock" else "App is unlocked",
                                fontSize = 12.sp,
                                color = OneUITextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isLocked,
                        onCheckedChange = { onToggleLock(it) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Lock Override Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E22))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Custom lock toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = OneUIAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Custom Lock",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = "Override Global Settings for this app",
                                    fontSize = 11.sp,
                                    color = OneUITextTertiary
                                )
                            }
                        }

                        Switch(
                            checked = useCustomLock,
                            onCheckedChange = { checked ->
                                useCustomLock = checked
                                if (checked && currentCredential.isEmpty()) {
                                    showSetupDialog = true
                                } else {
                                    onSaveConfig(
                                        AppLockConfig(
                                            packageName = app.packageName,
                                            useCustomLock = checked,
                                            customLockType = selectedLockType,
                                            customCredential = currentCredential,
                                            biometricEnabled = biometricEnabled,
                                            customTimeout = selectedTimeout
                                        )
                                    )
                                }
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

                    // Expanded Custom Lock Options
                    AnimatedVisibility(visible = useCustomLock) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(OneUIDivider)
                            )

                            Text(
                                text = "Select Custom Lock Type",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUITextSecondary
                            )

                            // Lock Type Selection
                            LockType.values().forEach { type ->
                                val isSelected = selectedLockType == type
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) OneUIAccent.copy(alpha = 0.2f) else Color(0xFF252528))
                                        .border(
                                            1.dp,
                                            if (isSelected) OneUIAccent else Color.Transparent,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedLockType = type
                                            showSetupDialog = true
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
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

                                    if (isSelected && currentCredential.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(OneUIAccent)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Configured",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Button to change/reconfigure credential
                            OutlinedButton(
                                onClick = { showSetupDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = OneUIAccent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentCredential.isEmpty()) "Set Credential" else "Change Custom Credential",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Biometric Toggle for this app
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = null,
                                        tint = OneUIAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Allow Fingerprint",
                                        fontSize = 14.sp,
                                        color = OneUITextPrimary
                                    )
                                }

                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { checked ->
                                        biometricEnabled = checked
                                        onSaveConfig(
                                            AppLockConfig(
                                                packageName = app.packageName,
                                                useCustomLock = useCustomLock,
                                                customLockType = selectedLockType,
                                                customCredential = currentCredential,
                                                biometricEnabled = checked,
                                                customTimeout = selectedTimeout
                                            )
                                        )
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

                            // Re-Lock Timeout Override
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = OneUIAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Re-Lock Timeout",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OneUITextSecondary
                                    )
                                }

                                ReLockTimeout.values().forEach { timeout ->
                                    val isSelected = selectedTimeout == timeout
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) OneUIAccent.copy(alpha = 0.2f) else Color(0xFF252528))
                                            .clickable {
                                                selectedTimeout = timeout
                                                onSaveConfig(
                                                    AppLockConfig(
                                                        packageName = app.packageName,
                                                        useCustomLock = useCustomLock,
                                                        customLockType = selectedLockType,
                                                        customCredential = currentCredential,
                                                        biometricEnabled = biometricEnabled,
                                                        customTimeout = timeout
                                                    )
                                                )
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = timeout.displayName,
                                            fontSize = 12.sp,
                                            color = if (isSelected) OneUITextPrimary else OneUITextSecondary
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = "✓",
                                                color = OneUIAccent,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Reset to Global Default button
                if (useCustomLock || initialConfig.useCustomLock) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            useCustomLock = false
                            currentCredential = ""
                            selectedTimeout = null
                            onResetToGlobal()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2022),
                            contentColor = OneUIDanger
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reset to Global Default",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showSetupDialog) {
        CredentialSetupDialog(
            lockType = selectedLockType,
            titlePrefix = "Set",
            onDismiss = { showSetupDialog = false },
            onCredentialConfirmed = { newCredential ->
                currentCredential = newCredential
                useCustomLock = true
                onSaveConfig(
                    AppLockConfig(
                        packageName = app.packageName,
                        useCustomLock = true,
                        customLockType = selectedLockType,
                        customCredential = newCredential,
                        biometricEnabled = biometricEnabled,
                        customTimeout = selectedTimeout
                    )
                )
                showSetupDialog = false
            }
        )
    }
}
