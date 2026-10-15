package com.onelock.app.ui.tabs

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelock.app.data.IntruderLogItem
import com.onelock.app.data.MaskedNotificationItem
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUISurfaceCard
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultLogsTab(
    intruderLogs: List<IntruderLogItem>,
    maskedNotifications: List<MaskedNotificationItem>,
    onDeleteIntruderLog: (String) -> Unit,
    onClearIntruderLogs: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearNotifications: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Intruder Snaps, 1: Masked Notifications

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Vault & Logs",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Security Feed & Intercepted Data",
                    fontSize = 13.sp,
                    color = OneUITextSecondary
                )
            }
        }

        // Sub-Tab Switcher Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(OneUISurfaceCard)
                .border(1.dp, OneUIDivider, RoundedCornerShape(50))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selectedSubTab == 0) OneUIAccent else Color.Transparent)
                    .clickable { selectedSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Intruders (${intruderLogs.size})",
                    fontSize = 13.sp,
                    fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedSubTab == 0) Color.White else OneUITextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selectedSubTab == 1) OneUIAccent else Color.Transparent)
                    .clickable { selectedSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Notifications (${maskedNotifications.size})",
                    fontSize = 13.sp,
                    fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedSubTab == 1) Color.White else OneUITextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (selectedSubTab == 0) {
            IntruderLogsContent(
                logs = intruderLogs,
                onDelete = onDeleteIntruderLog,
                onClearAll = onClearIntruderLogs
            )
        } else {
            MaskedNotificationsContent(
                notifications = maskedNotifications,
                onDelete = onDeleteNotification,
                onClearAll = onClearNotifications
            )
        }
    }
}

@Composable
private fun IntruderLogsContent(
    logs: List<IntruderLogItem>,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OneUISurfaceCard)
                        .border(1.dp, OneUIDivider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = OneUITextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "No Intruder Attempts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Text(
                    text = "When someone enters a wrong PIN or pattern twice, their photo will silently appear here.",
                    fontSize = 13.sp,
                    color = OneUITextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Captured Photos (${logs.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUITextSecondary
                    )

                    Text(
                        text = "Clear All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUIDanger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(logs, key = { it.id }) { item ->
                IntruderLogCard(item = item, onDelete = { onDelete(item.id) })
            }
        }
    }
}

@Composable
private fun IntruderLogCard(
    item: IntruderLogItem,
    onDelete: () -> Unit
) {
    val bitmap = remember(item.imagePath) {
        try {
            val file = File(item.imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = OneUISurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Preview
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF222226)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Intruder Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = OneUITextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(OneUIDanger)
                    )
                    Text(
                        text = "Attempted: ${item.appName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUITextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = OneUITextSecondary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.packageName,
                    fontSize = 11.sp,
                    color = OneUITextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF242022))
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = OneUIDanger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MaskedNotificationsContent(
    notifications: List<MaskedNotificationItem>,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(OneUISurfaceCard)
                        .border(1.dp, OneUIDivider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = OneUITextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "No Masked Notifications",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Text(
                    text = "Notifications from locked apps are safely intercepted so content never leaks on the lock screen.",
                    fontSize = 13.sp,
                    color = OneUITextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intercepted Alerts (${notifications.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUITextSecondary
                    )

                    Text(
                        text = "Clear All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUIDanger,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(notifications, key = { it.id }) { item ->
                MaskedNotificationCard(
                    item = item,
                    onOpenApp = {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                            if (intent != null) {
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) {}
                    },
                    onDelete = { onDelete(item.id) }
                )
            }
        }
    }
}

@Composable
private fun MaskedNotificationCard(
    item: MaskedNotificationItem,
    onOpenApp: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d • hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = OneUISurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = OneUIAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUIAccent
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = OneUITextTertiary
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = OneUITextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenApp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open App",
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = OneUIDanger,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            if (item.title.isNotEmpty()) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUITextPrimary
                )
            }

            if (item.text.isNotEmpty()) {
                Text(
                    text = item.text,
                    fontSize = 13.sp,
                    color = OneUITextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
