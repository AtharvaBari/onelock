package com.onelock.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import com.onelock.app.OneLockApp
import com.onelock.app.data.EffectiveLockConfig
import com.onelock.app.data.GlobalLockConfig
import com.onelock.app.data.LockType
import com.onelock.app.engine.IntruderCaptureManager
import com.onelock.app.engine.LockState
import com.onelock.app.ui.theme.OneLockTheme
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUIPinPadButton
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LockScreenActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package_name"
    }

    private var targetPackage: String? = null
    private var isBiometricShowing = false
    private var appName: String = "Locked App"
    private var appIconDrawable: Drawable? = null
    private lateinit var effectiveConfig: EffectiveLockConfig
    private lateinit var globalConfig: GlobalLockConfig
    private var failedAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyZeroTransition()

        // Enable FLAG_SECURE on the lock screen so screenshots / screen recordings are blocked
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (targetPackage == null || LockState.isUnlocked(targetPackage!!)) {
            finish()
            return
        }

        loadTargetAppMetadata(targetPackage!!)
        val prefs = OneLockApp.preferencesManager
        effectiveConfig = prefs.getEffectiveLockConfig(targetPackage!!)
        globalConfig = prefs.getGlobalConfig()

        // Override back button so user is redirected to Home screen instead of bypassing lock
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToHomeScreen()
            }
        })

        LockState.isLockScreenActive = true
        setContent {
            OneLockTheme {
                SamsungKeyguardLockScreen(
                    appName = appName,
                    appIcon = appIconDrawable,
                    effectiveConfig = effectiveConfig,
                    globalConfig = globalConfig,
                    onVerified = {
                        unlockAndExit()
                    },
                    onWrongAttempt = {
                        failedAttempts++
                        if (failedAttempts >= 2) {
                            IntruderCaptureManager.captureIntruderSelfie(
                                context = this,
                                lifecycleOwner = this,
                                targetPackage = targetPackage!!,
                                appName = appName
                            )
                        }
                    },
                    onTriggerBiometrics = {
                        showBiometricPrompt()
                    },
                    onCancelToHome = {
                        exitToHomeScreen()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LockState.isLockScreenActive = true
        targetPackage?.let {
            if (LockState.isUnlocked(it)) {
                finish()
                applyZeroTransition()
                return
            }
        }

        // Auto trigger biometric prompt if available and enabled for this app
        if (effectiveConfig.biometricEnabled && canUseBiometrics()) {
            showBiometricPrompt()
        }
    }

    private fun loadTargetAppMetadata(pkg: String) {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            appIconDrawable = pm.getApplicationIcon(appInfo)
        } catch (_: Exception) {
            appName = pkg
        }
    }

    private fun canUseBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        if (isBiometricShowing) return
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricShowing = false
                unlockAndExit()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricShowing = false
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $appName")
            .setSubtitle("Confirm fingerprint to access")
            .setNegativeButtonText("Use ${effectiveConfig.lockType.displayName}")
            .build()

        isBiometricShowing = true
        prompt.authenticate(promptInfo)
    }

    private fun unlockAndExit() {
        targetPackage?.let {
            LockState.unlock(it)
        }
        finish()
        applyZeroTransition()
    }

    private fun exitToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
        applyZeroTransition()
    }

    override fun finish() {
        LockState.isLockScreenActive = false
        super.finish()
        applyZeroTransition()
    }

    override fun onDestroy() {
        LockState.isLockScreenActive = false
        super.onDestroy()
    }

    private fun applyZeroTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

@Composable
fun SamsungKeyguardLockScreen(
    appName: String,
    appIcon: Drawable?,
    effectiveConfig: EffectiveLockConfig,
    globalConfig: GlobalLockConfig,
    onVerified: () -> Unit,
    onWrongAttempt: () -> Unit,
    onTriggerBiometrics: () -> Unit,
    onCancelToHome: () -> Unit
) {
    val context = LocalContext.current
    var enteredInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorActive by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Keypad layout (scrambled if enabled)
    val keypadNumbers = remember(globalConfig.scrambleKeypad) {
        if (globalConfig.scrambleKeypad) {
            (0..9).shuffled()
        } else {
            (1..9).toList() + listOf(0)
        }
    }

    val triggerErrorAnimation: (String) -> Unit = { msg ->
        errorMessage = msg
        isErrorActive = true
        onWrongAttempt()
        triggerWaveformHaptic(context)

        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 380
                    0f at 0
                    (-28f) at 50
                    28f at 100
                    (-22f) at 150
                    22f at 200
                    (-12f) at 250
                    12f at 300
                    0f at 380
                }
            )
            delay(180)
            enteredInput = ""
            isErrorActive = false
            errorMessage = null
        }
    }

    val verifyInput: (String) -> Unit = { input ->
        if (input == effectiveConfig.credential) {
            onVerified()
        } else {
            triggerErrorAnimation("Incorrect ${effectiveConfig.lockType.displayName}")
        }
    }

    // Frosted charcoal/AMOLED backdrop (#F508080A) with subtle radial glow behind app icon
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF141926),
                        Color(0xFF0A0A0D),
                        Color(0xFF050507)
                    ),
                    radius = 1200f
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Squircle App Icon (64.dp)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF16161A))
                    .border(1.dp, Color(0xFF28282E), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    val bitmap = remember(appIcon) {
                        try {
                            appIcon.toBitmap(160, 160).asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = appName,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Locked with OneLock",
                color = OneUIAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Unlock $appName",
                color = OneUITextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            val defaultPrompt = when (effectiveConfig.lockType) {
                LockType.PIN_4 -> "Enter 4-digit PIN"
                LockType.PIN_6 -> "Enter 6-digit PIN"
                LockType.PATTERN -> "Draw your pattern"
                LockType.PASSWORD -> "Enter your password"
            }

            Text(
                text = errorMessage ?: defaultPrompt,
                color = if (errorMessage != null) OneUIDanger else OneUITextSecondary,
                fontSize = 14.sp,
                fontWeight = if (errorMessage != null) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Authentication UI
            when (effectiveConfig.lockType) {
                LockType.PIN_4, LockType.PIN_6 -> {
                    val maxDigits = if (effectiveConfig.lockType == LockType.PIN_4) 4 else 6

                    // Animated scaling PIN indicator dots
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until maxDigits) {
                            val isFilled = i < enteredInput.length
                            val scale by animateFloatAsState(
                                targetValue = if (isFilled) 1.25f else 1.0f,
                                animationSpec = tween(120),
                                label = "scale"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    isErrorActive -> OneUIDanger
                                    isFilled -> OneUIAccent
                                    else -> Color.Transparent
                                },
                                animationSpec = tween(100),
                                label = "color"
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(
                                        width = 2.dp,
                                        color = when {
                                            isErrorActive -> OneUIDanger
                                            isFilled -> OneUIAccent
                                            else -> Color(0xFF3E3E46)
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(34.dp))

                    SamsungOneUIKeypad(
                        numbers = keypadNumbers,
                        showBiometrics = effectiveConfig.biometricEnabled,
                        onDigitClick = { digit ->
                            if (enteredInput.length < maxDigits) {
                                val next = enteredInput + digit
                                enteredInput = next
                                if (next.length == maxDigits) {
                                    verifyInput(next)
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredInput.isNotEmpty()) {
                                enteredInput = enteredInput.dropLast(1)
                                errorMessage = null
                            }
                        },
                        onBiometricClick = onTriggerBiometrics
                    )
                }

                LockType.PATTERN -> {
                    PatternLockView(
                        sizeDp = 290.dp,
                        isError = isErrorActive,
                        hideTrail = globalConfig.hidePatternTrail,
                        onPatternStarted = {
                            isErrorActive = false
                            errorMessage = null
                        },
                        onPatternCompleted = { pattern ->
                            if (pattern == effectiveConfig.credential) {
                                onVerified()
                            } else {
                                triggerErrorAnimation("Wrong pattern")
                            }
                        }
                    )
                }

                LockType.PASSWORD -> {
                    var passwordVisible by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = enteredInput,
                            onValueChange = {
                                enteredInput = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Password", color = OneUITextSecondary) },
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
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUIAccent,
                                unfocusedBorderColor = OneUIDivider,
                                focusedTextColor = OneUITextPrimary,
                                unfocusedTextColor = OneUITextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { verifyInput(enteredInput) }
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { verifyInput(enteredInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Unlock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel action
            Text(
                text = "Cancel",
                color = OneUITextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCancelToHome() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun SamsungOneUIKeypad(
    numbers: List<Int>,
    showBiometrics: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val subLabels = mapOf(
        "1" to "",
        "2" to "ABC",
        "3" to "DEF",
        "4" to "GHI",
        "5" to "JKL",
        "6" to "MNO",
        "7" to "PQRS",
        "8" to "TUV",
        "9" to "WXYZ",
        "0" to "+"
    )

    val rows = listOf(
        numbers.subList(0, 3).map { it.toString() },
        numbers.subList(3, 6).map { it.toString() },
        numbers.subList(6, 9).map { it.toString() },
        listOf(if (showBiometrics) "BIO" else "", numbers[9].toString(), "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    when (key) {
                        "" -> Box(modifier = Modifier.size(76.dp))
                        "BIO" -> {
                            FrostedKeypadCircle(
                                onClick = onBiometricClick,
                                content = {
                                    Icon(
                                        imageVector = Icons.Rounded.Fingerprint,
                                        contentDescription = "Fingerprint",
                                        tint = OneUIAccent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            )
                        }
                        "DEL" -> {
                            FrostedKeypadCircle(
                                onClick = onBackspace,
                                content = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "Backspace",
                                        tint = OneUITextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            val sub = subLabels[key] ?: ""
                            FrostedKeypadCircle(
                                onClick = { onDigitClick(key) },
                                content = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = OneUITextPrimary,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (sub.isNotEmpty()) {
                                            Text(
                                                text = sub,
                                                color = OneUITextTertiary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostedKeypadCircle(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0xFF1C1C20))
            .border(1.dp, Color(0xFF282830), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White.copy(alpha = 0.25f), bounded = true)
            ) {
                triggerLightHaptic(context)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun triggerLightHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    } catch (_: Exception) {}
}

@Suppress("DEPRECATION")
private fun triggerWaveformHaptic(context: Context) {
    try {
        val timings = longArrayOf(0, 50, 60, 50, 60, 80)
        val amplitudes = intArrayOf(0, 200, 0, 255, 0, 180)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 50, 60, 50, 60, 80), -1)
        }
    } catch (_: Exception) {}
}
