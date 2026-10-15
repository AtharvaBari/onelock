package com.onelock.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.onelock.app.data.LockType
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUIPinPadButton
import com.onelock.app.ui.theme.OneUISurfaceCard
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary

/**
 * Guided One UI setup dialog for setting up or updating a credential
 * (PIN_4, PIN_6, PATTERN, or PASSWORD).
 */
@Composable
fun CredentialSetupDialog(
    lockType: LockType,
    titlePrefix: String = "Set",
    onDismiss: () -> Unit,
    onCredentialConfirmed: (String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var firstEntry by remember { mutableStateOf("") }
    var secondEntry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = OneUISurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, OneUIDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OneUIAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = OneUIAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "$titlePrefix ${lockType.displayName}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitle = when (lockType) {
                    LockType.PIN_4 -> if (step == 1) "Enter a 4-digit PIN" else "Confirm your 4-digit PIN"
                    LockType.PIN_6 -> if (step == 1) "Enter a 6-digit PIN" else "Confirm your 6-digit PIN"
                    LockType.PATTERN -> if (step == 1) "Connect at least 4 dots" else "Draw the pattern again to confirm"
                    LockType.PASSWORD -> if (step == 1) "Enter your password (min 4 characters)" else "Confirm your password"
                }

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = OneUITextSecondary,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = OneUIDanger,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content per LockType
                when (lockType) {
                    LockType.PIN_4, LockType.PIN_6 -> {
                        val maxDigits = if (lockType == LockType.PIN_4) 4 else 6
                        val currentEntry = if (step == 1) firstEntry else secondEntry

                        // Dots row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until maxDigits) {
                                val isFilled = i < currentEntry.length
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) OneUIAccent else Color.Transparent)
                                        .border(
                                            width = 2.dp,
                                            color = if (isFilled) OneUIAccent else Color(0xFF3E3E44),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Compact keypad
                        SetupKeypad(
                            onDigit = { digit ->
                                if (step == 1) {
                                    if (firstEntry.length < maxDigits) {
                                        val next = firstEntry + digit
                                        firstEntry = next
                                        if (next.length == maxDigits) {
                                            step = 2
                                            errorMessage = null
                                        }
                                    }
                                } else {
                                    if (secondEntry.length < maxDigits) {
                                        val next = secondEntry + digit
                                        secondEntry = next
                                        if (next.length == maxDigits) {
                                            if (next == firstEntry) {
                                                onCredentialConfirmed(next)
                                            } else {
                                                errorMessage = "PINs do not match. Try again."
                                                firstEntry = ""
                                                secondEntry = ""
                                                step = 1
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspace = {
                                if (step == 1 && firstEntry.isNotEmpty()) {
                                    firstEntry = firstEntry.dropLast(1)
                                    errorMessage = null
                                } else if (step == 2 && secondEntry.isNotEmpty()) {
                                    secondEntry = secondEntry.dropLast(1)
                                    errorMessage = null
                                }
                            }
                        )
                    }

                    LockType.PATTERN -> {
                        PatternLockView(
                            sizeDp = 260.dp,
                            isError = isPatternError,
                            onPatternStarted = {
                                isPatternError = false
                                errorMessage = null
                            },
                            onPatternCompleted = { pattern ->
                                if (step == 1) {
                                    if (pattern.length < 4) {
                                        errorMessage = "Connect at least 4 dots"
                                        isPatternError = true
                                    } else {
                                        firstEntry = pattern
                                        step = 2
                                        errorMessage = null
                                    }
                                } else {
                                    if (pattern == firstEntry) {
                                        onCredentialConfirmed(pattern)
                                    } else {
                                        errorMessage = "Pattern doesn't match. Try again."
                                        isPatternError = true
                                        firstEntry = ""
                                        step = 1
                                    }
                                }
                            }
                        )
                    }

                    LockType.PASSWORD -> {
                        var passwordVisible by remember { mutableStateOf(false) }
                        val currentEntry = if (step == 1) firstEntry else secondEntry

                        OutlinedTextField(
                            value = currentEntry,
                            onValueChange = {
                                if (step == 1) firstEntry = it else secondEntry = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = OneUITextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIAccent,
                                unfocusedBorderColor = OneUIDivider,
                                focusedTextColor = OneUITextPrimary,
                                unfocusedTextColor = OneUITextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (step == 1) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (firstEntry.length < 4) {
                                        errorMessage = "Must be at least 4 characters"
                                    } else {
                                        step = 2
                                        errorMessage = null
                                    }
                                },
                                onDone = {
                                    if (step == 2) {
                                        if (secondEntry == firstEntry) {
                                            onCredentialConfirmed(secondEntry)
                                        } else {
                                            errorMessage = "Passwords do not match"
                                            firstEntry = ""
                                            secondEntry = ""
                                            step = 1
                                        }
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (step == 1) {
                                    if (firstEntry.length < 4) {
                                        errorMessage = "Must be at least 4 characters"
                                    } else {
                                        step = 2
                                        errorMessage = null
                                    }
                                } else {
                                    if (secondEntry == firstEntry) {
                                        onCredentialConfirmed(secondEntry)
                                    } else {
                                        errorMessage = "Passwords do not match"
                                        firstEntry = ""
                                        secondEntry = ""
                                        step = 1
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (step == 1) "Continue" else "Save Password",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = OneUITextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    when (key) {
                        "" -> {
                            Box(modifier = Modifier.size(56.dp))
                        }
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(OneUIPinPadButton)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Backspace",
                                    tint = OneUITextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(OneUIPinPadButton)
                                    .border(1.dp, Color(0xFF252528), CircleShape)
                                    .clickable { onDigit(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = OneUITextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
