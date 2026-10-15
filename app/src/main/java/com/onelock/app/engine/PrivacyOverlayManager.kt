package com.onelock.app.engine

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Displays a frosted AMOLED privacy overlay on top of the recent apps switcher
 * or full-screen over Samsung pop-up windows to mask sensitive app contents.
 */
class PrivacyOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    @Volatile
    private var isShowing = false

    companion object {
        private const val TAG = "PrivacyOverlayManager"

        @Volatile
        var currentInstance: PrivacyOverlayManager? = null
            private set

        /**
         * Idempotent global dismissal of any attached privacy overlay across the app.
         */
        fun dismissActiveOverlay() {
            currentInstance?.hideOverlay()
        }
    }

    init {
        currentInstance = this
    }

    fun showOverlay(targetPackage: String) {
        if (!Settings.canDrawOverlays(context) || windowManager == null) {
            return
        }

        mainHandler.post {
            if (isShowing && overlayView != null) {
                return@post
            }

            try {
                val windowParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_SECURE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                val rootLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#F508080A"))

                    // Container for icon
                    val iconView = ImageView(context).apply {
                        val iconSize = (72 * context.resources.displayMetrics.density).toInt()
                        val imgParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                            bottomMargin = (16 * context.resources.displayMetrics.density).toInt()
                        }
                        this.layoutParams = imgParams
                        try {
                            val icon = context.packageManager.getApplicationIcon(targetPackage)
                            setImageDrawable(icon)
                        } catch (_: Exception) {}

                        val shape = GradientDrawable().apply {
                            cornerRadius = 22 * context.resources.displayMetrics.density
                            setColor(Color.parseColor("#17171A"))
                            setStroke((1 * context.resources.displayMetrics.density).toInt(), Color.parseColor("#26262A"))
                        }
                        background = shape
                        val pad = (12 * context.resources.displayMetrics.density).toInt()
                        setPadding(pad, pad, pad, pad)
                    }
                    addView(iconView)

                    val textView = TextView(context).apply {
                        text = "🔒 Privacy Shield Active"
                        setTextColor(Color.WHITE)
                        textSize = 15f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    addView(textView)

                    val subTextView = TextView(context).apply {
                        text = "Multitasking preview hidden by OneLock"
                        setTextColor(Color.parseColor("#8E8E93"))
                        textSize = 12f
                        setPadding(0, (4 * context.resources.displayMetrics.density).toInt(), 0, 0)
                    }
                    addView(subTextView)
                }

                // If a previous view was somehow still present, remove it safely first
                overlayView?.let { oldView ->
                    try {
                        windowManager.removeView(oldView)
                    } catch (_: Exception) {}
                }

                windowManager.addView(rootLayout, windowParams)
                overlayView = rootLayout
                isShowing = true
                Log.d(TAG, "Privacy shield overlay shown for $targetPackage")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying privacy overlay", e)
            }
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            val view = overlayView
            if (view != null && windowManager != null) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing privacy overlay view", e)
                } finally {
                    overlayView = null
                    isShowing = false
                    Log.d(TAG, "Privacy shield overlay dismissed")
                }
            } else {
                overlayView = null
                isShowing = false
            }
        }
    }
}

