package com.onelock.app.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.fragment.app.FragmentActivity
import com.onelock.app.data.LockType
import com.onelock.app.ui.tabs.HomeTab
import com.onelock.app.ui.tabs.ProtectionShieldTab
import com.onelock.app.ui.tabs.SettingsTab
import com.onelock.app.ui.tabs.VaultLogsTab
import com.onelock.app.ui.theme.OneLockTheme
import com.onelock.app.ui.theme.OneUIAccent
import com.onelock.app.ui.theme.OneUIBackground
import com.onelock.app.ui.theme.OneUIBottomBar
import com.onelock.app.ui.theme.OneUIDanger
import com.onelock.app.ui.theme.OneUIDivider
import com.onelock.app.ui.theme.OneUIPinPadButton
import com.onelock.app.ui.theme.OneUITextPrimary
import com.onelock.app.ui.theme.OneUITextSecondary
import com.onelock.app.ui.theme.OneUITextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isBiometricPromptActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing so content flows behind transparent system bars
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Enable FLAG_SECURE on MainActivity to protect dashboard contents from recents screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.uiState.value
                if (state.selectedAppForDetail != null) {
                    viewModel.selectAppForDetail(null)
                } else if (state.currentTab != NavigationTab.HOME) {
                    viewModel.selectTab(NavigationTab.HOME)
                } else {
                    finish()
                }
            }
        })

        setContent {
            OneLockTheme {
                MainAppNavigationRoot(
                    viewModel = viewModel,
                    onTriggerBiometric = {
                        triggerSelfBiometricPrompt()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.onelock.app.engine.LockState.isLaunchingPermissionIntent) {
            com.onelock.app.engine.LockState.isLaunchingPermissionIntent = false
        }

        viewModel.refreshPermissions()
        viewModel.refreshLogs()

        val state = viewModel.uiState.value
        if (state.globalConfig.isOnboardingCompleted && !state.isSelfUnlocked && state.globalConfig.biometricEnabled) {
            triggerSelfBiometricPrompt()
        }
    }

    override fun onStop() {
        super.onStop()
        // If user is just opening system permission settings, don't self-lock mid-setup
        if (!com.onelock.app.engine.LockState.isLaunchingPermissionIntent) {
            viewModel.setSelfUnlocked(false)
        }
    }

    private fun triggerSelfBiometricPrompt() {
        if (isBiometricPromptActive) return
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptActive = false
                viewModel.setSelfUnlocked(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptActive = false
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock OneLock")
            .setSubtitle("Confirm your identity to access dashboard")
            .setNegativeButtonText("Use ${viewModel.uiState.value.globalConfig.defaultLockType.displayName}")
            .build()

        isBiometricPromptActive = true
        prompt.authenticate(promptInfo)
    }
}

@Composable
fun MainAppNavigationRoot(
    viewModel: MainViewModel,
    onTriggerBiometric: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInitialOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading, uiState.globalConfig.isOnboardingCompleted) {
        if (!uiState.isLoading && !uiState.globalConfig.isOnboardingCompleted) {
            showInitialOnboarding = true
        }
    }

    // Self-Protection Lock Screen overlay if locked and onboarding completed
    if (uiState.globalConfig.isOnboardingCompleted && !uiState.isSelfUnlocked) {
        SelfLockScreen(
            globalConfig = uiState.globalConfig,
            onUnlocked = { viewModel.setSelfUnlocked(true) },
            onTriggerBiometric = onTriggerBiometric
        )
        return
    }

    // Initial Onboarding Dialog
    if (showInitialOnboarding) {
        CredentialSetupDialog(
            lockType = uiState.globalConfig.defaultLockType,
            titlePrefix = "Create Master",
            onDismiss = {},
            onCredentialConfirmed = { newCredential ->
                viewModel.saveGlobalConfig(
                    uiState.globalConfig.copy(
                        baseCredential = newCredential,
                        isOnboardingCompleted = true
                    )
                )
                viewModel.setSelfUnlocked(true)
                showInitialOnboarding = false
            }
        )
    }

    // 4-Tab Container with Bottom Navigation Bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUIBackground)
    ) {
        // Tab Content with smooth crossfade transition
        Crossfade(
            targetState = uiState.currentTab,
            label = "tabCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                NavigationTab.HOME -> {
                    HomeTab(
                        uiState = uiState,
                        getAppIcon = { viewModel.getAppIcon(it) },
                        onAppRowClick = { app -> viewModel.selectAppForDetail(app) },
                        onToggleLock = { viewModel.toggleAppLock(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onFilterSelect = { viewModel.setFilter(it) }
                    )
                }
                NavigationTab.VAULT -> {
                    VaultLogsTab(
                        intruderLogs = uiState.intruderLogs,
                        maskedNotifications = uiState.maskedNotifications,
                        onDeleteIntruderLog = { viewModel.deleteIntruderLog(it) },
                        onClearIntruderLogs = { viewModel.clearIntruderLogs() },
                        onDeleteNotification = { viewModel.deleteNotification(it) },
                        onClearNotifications = { viewModel.clearNotifications() }
                    )
                }
                NavigationTab.SHIELD -> {
                    ProtectionShieldTab(
                        shieldConfig = uiState.shieldConfig,
                        isNotificationListenerGranted = uiState.isNotificationListenerGranted,
                        isCameraGranted = uiState.isCameraGranted,
                        onSaveShieldConfig = { viewModel.saveShieldConfig(it) },
                        onRefreshPermissions = { viewModel.refreshPermissions() }
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsTab(
                        globalConfig = uiState.globalConfig,
                        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                        isOverlayGranted = uiState.isOverlayGranted,
                        isNotificationListenerGranted = uiState.isNotificationListenerGranted,
                        isCameraGranted = uiState.isCameraGranted,
                        isBatteryOptimizationsIgnored = uiState.isBatteryOptimizationsIgnored,
                        onSaveGlobalConfig = { viewModel.saveGlobalConfig(it) }
                    )
                }
            }
        }

        // Persistent Samsung One UI Bottom Navigation Bar (#0B0B0D)
        OneUIBottomNavBar(
            currentTab = uiState.currentTab,
            onSelectTab = { viewModel.selectTab(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Per-App Detail Configuration Sheet
        uiState.selectedAppForDetail?.let { selectedApp ->
            val icon = viewModel.getAppIcon(selectedApp.packageName)
            val isLocked = uiState.lockedPackageNames.contains(selectedApp.packageName)
            val appConfig = viewModel.getAppConfig(selectedApp.packageName)

            AppDetailDialog(
                app = selectedApp,
                icon = icon,
                isLocked = isLocked,
                initialConfig = appConfig,
                onDismiss = { viewModel.selectAppForDetail(null) },
                onToggleLock = { locked ->
                    viewModel.setAppLocked(selectedApp.packageName, locked)
                },
                onSaveConfig = { updated ->
                    viewModel.saveAppConfig(updated)
                },
                onResetToGlobal = {
                    viewModel.resetAppConfigToDefault(selectedApp.packageName)
                }
            )
        }
    }
}

@Composable
fun OneUIBottomNavBar(
    currentTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationTab.HOME to Icons.Rounded.Home,
        NavigationTab.VAULT to Icons.Rounded.History,
        NavigationTab.SHIELD to Icons.Rounded.Security,
        NavigationTab.SETTINGS to Icons.Rounded.Settings
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OneUIBottomBar,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF18181E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for ((tab, icon) in items) {
                val isSelected = currentTab == tab

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) OneUIAccent.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) OneUIAccent else OneUITextTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) OneUIAccent else OneUITextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfLockScreen(
    globalConfig: com.onelock.app.data.GlobalLockConfig,
    onUnlocked: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    val context = LocalContext.current
    var enteredInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPatternError by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val triggerErrorAnimation: (String) -> Unit = { msg ->
        errorMessage = msg
        triggerHaptic(context)
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-25f) at 50
                    25f at 100
                    (-20f) at 150
                    20f at 200
                    (-10f) at 250
                    10f at 300
                    0f at 350
                }
            )
            delay(150)
            enteredInput = ""
            errorMessage = null
        }
    }

    val verifySelfInput: (String) -> Unit = { input ->
        if (input == globalConfig.baseCredential) {
            onUnlocked()
        } else {
            triggerErrorAnimation("Incorrect ${globalConfig.defaultLockType.displayName}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OneUIBackground)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF17171A))
                    .border(1.dp, OneUIDivider, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "OneLock",
                    tint = OneUIAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "OneLock Protected",
                color = OneUITextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = errorMessage ?: "Authenticate to access App Lock dashboard",
                color = if (errorMessage != null) OneUIDanger else OneUITextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            when (globalConfig.defaultLockType) {
                LockType.PIN_4, LockType.PIN_6 -> {
                    val maxDigits = if (globalConfig.defaultLockType == LockType.PIN_4) 4 else 6

                    Row(
                        modifier = Modifier
                            .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until maxDigits) {
                            val isFilled = i < enteredInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
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

                    Spacer(modifier = Modifier.height(36.dp))

                    SelfNumericPad(
                        showBio = globalConfig.biometricEnabled,
                        onDigitClick = { digit ->
                            if (enteredInput.length < maxDigits) {
                                val next = enteredInput + digit
                                enteredInput = next
                                if (next.length == maxDigits) {
                                    verifySelfInput(next)
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredInput.isNotEmpty()) {
                                enteredInput = enteredInput.dropLast(1)
                                errorMessage = null
                            }
                        },
                        onBiometricClick = onTriggerBiometric
                    )
                }

                LockType.PATTERN -> {
                    PatternLockView(
                        sizeDp = 290.dp,
                        isError = isPatternError,
                        hideTrail = globalConfig.hidePatternTrail,
                        onPatternStarted = {
                            isPatternError = false
                            errorMessage = null
                        },
                        onPatternCompleted = { pattern ->
                            if (pattern == globalConfig.baseCredential) {
                                onUnlocked()
                            } else {
                                isPatternError = true
                                errorMessage = "Incorrect pattern"
                                coroutineScope.launch {
                                    delay(500)
                                    isPatternError = false
                                    errorMessage = null
                                }
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
                            placeholder = { Text("Enter Master Password", color = OneUITextSecondary) },
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
                                onDone = { verifySelfInput(enteredInput) }
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { verifySelfInput(enteredInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUIAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Unlock Dashboard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            if (globalConfig.biometricEnabled && globalConfig.defaultLockType != LockType.PIN_4 && globalConfig.defaultLockType != LockType.PIN_6) {
                Spacer(modifier = Modifier.height(24.dp))
                IconButton(
                    onClick = onTriggerBiometric,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(OneUIPinPadButton)
                        .border(1.dp, OneUIDivider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = "Fingerprint",
                        tint = OneUIAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelfNumericPad(
    showBio: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBio) "BIO" else "", "0", "DEL")
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
                        "" -> Box(modifier = Modifier.size(74.dp))
                        "BIO" -> {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(OneUIPinPadButton)
                                    .border(1.dp, Color(0xFF252528), CircleShape)
                                    .clickable { onBiometricClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Fingerprint",
                                    tint = OneUIAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(OneUIPinPadButton)
                                    .border(1.dp, Color(0xFF252528), CircleShape)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Backspace",
                                    tint = OneUITextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(OneUIPinPadButton)
                                    .border(1.dp, Color(0xFF252528), CircleShape)
                                    .clickable { onDigitClick(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = OneUITextPrimary,
                                    fontSize = 26.sp,
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

private fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(35)
        }
    } catch (_: Exception) {}
}
