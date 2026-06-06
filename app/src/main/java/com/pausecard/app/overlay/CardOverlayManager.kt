package com.pausecard.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.pausecard.app.PauseCardApp
import com.pausecard.app.R
import com.pausecard.app.data.db.CardDao
import com.pausecard.app.data.db.DislikedCardDao
import com.pausecard.app.data.db.PauseCardDatabase
import com.pausecard.app.data.entity.CardEntity
import com.pausecard.app.data.repository.CardRepository
import com.pausecard.app.permissions.PermissionHelper
import com.pausecard.app.util.CrashReporting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.min

class CardOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentOverlay: View? = null
    private var countdownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isShowing = false

    private val cardRepository: CardRepository by lazy {
        val db = PauseCardDatabase.getInstance(context)
        CardRepository(db.cardDao(), db.dislikedCardDao())
    }

    fun showCard(packageName: String, appName: String) {
        if (isShowing || OverlayStateManager.isOverlayShowing()) {
            CrashReporting.logInfo(TAG, "Overlay already showing, skipping")
            return
        }

        if (!PermissionHelper.hasOverlayPermission(context)) {
            CrashReporting.logWarning(TAG, "No overlay permission")
            return
        }

        isShowing = true
        OverlayStateManager.markShowing(packageName)

        scope.launch {
            try {
                val card = cardRepository.getNextCard()
                val displayCard = card ?: getFallbackCard()
                handler.post {
                    showOverlay(displayCard, packageName, appName)
                }
            } catch (e: Exception) {
                CrashReporting.logError(TAG, "Error loading card", e)
                handler.post {
                    showOverlay(getFallbackCard(), packageName, appName)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay(card: CardEntity, packageName: String, appName: String) {
        val overlayView = createOverlayView(card, packageName, appName)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(overlayView, params)
            currentOverlay = overlayView
            startCountdown(overlayView, card)
        } catch (e: WindowManager.BadTokenException) {
            CrashReporting.logError(TAG, "BadTokenException, retrying", e)
            handler.postDelayed({
                try {
                    windowManager.addView(overlayView, params)
                    currentOverlay = overlayView
                    startCountdown(overlayView, card)
                } catch (e2: Exception) {
                    CrashReporting.logError(TAG, "Retry failed", e2)
                    dismiss()
                }
            }, 500)
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Failed to show overlay", e)
            dismiss()
        }
    }

    private fun createOverlayView(card: CardEntity, packageName: String, appName: String): View {
        val density = context.resources.displayMetrics.density

        val rootLayout = object : FrameLayout(context) {
            override fun dispatchDraw(canvas: Canvas) {
                super.dispatchDraw(canvas)
                drawScanlines(canvas)
            }
        }
        rootLayout.setBackgroundColor(Color.parseColor("#000000"))
        rootLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val startY = rootLayout.height * 0.6f
                if (event.y < startY) {
                    markNotInterested(card)
                }
            }
            true
        }

        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(
                    (32 * density).toInt(), (48 * density).toInt(),
                    (32 * density).toInt(), (32 * density).toInt()
                )
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        // Header: intercepted app info
        val headerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (32 * density).toInt()
            }
        }

        val appIconText = TextView(context).apply {
            text = getEmojiForPackage(packageName)
            textSize = 20f
        }
        headerContainer.addView(appIconText)

        val headerText = TextView(context).apply {
            text = context.getString(R.string.card_you_were_opening, appName)
            setTextColor(Color.parseColor("#B0B0B0"))
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding((12 * density).toInt(), 0, 0, 0)
        }
        headerContainer.addView(headerText)
        mainContainer.addView(headerContainer)

        // Title
        val titleView = TextView(context).apply {
            text = card.title
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 26f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = -0.02f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (24 * density).toInt()
            }
        }
        mainContainer.addView(titleView)

        // Body
        val bodyView = TextView(context).apply {
            text = card.body
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 16f
            typeface = try {
                Typeface.create("jettbrainsmono", Typeface.NORMAL)
            } catch (e: Exception) {
                Typeface.MONOSPACE
            }
            lineHeight = (28 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (48 * density).toInt()
                weight = 1f
            }
        }
        mainContainer.addView(bodyView)

        // Bottom row: category pill + countdown
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Category pill
        val categoryPill = TextView(context).apply {
            text = card.category
            setTextColor(Color.parseColor("#00E5CC"))
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(
                (16 * density).toInt(), (8 * density).toInt(),
                (16 * density).toInt(), (8 * density).toInt()
            )
            background = object : android.graphics.drawable.GradientDrawable() {
                init {
                    setColor(Color.parseColor("#1A00E5CC"))
                    cornerRadius = 20 * density
                }
            }
        }
        bottomRow.addView(categoryPill)

        // Spacer
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        bottomRow.addView(spacer)

        // Countdown + Continue button container
        val timerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val countdownText = TextView(context).apply {
            id = android.R.id.text1
            text = "15s"
            setTextColor(Color.parseColor("#00E5CC"))
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, (12 * density).toInt(), 0)
        }
        timerContainer.addView(countdownText)

        val continueButton = TextView(context).apply {
            id = android.R.id.button1
            text = "Continue"
            setTextColor(Color.parseColor("#000000"))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(
                (24 * density).toInt(), (12 * density).toInt(),
                (24 * density).toInt(), (12 * density).toInt()
            )
            background = object : android.graphics.drawable.GradientDrawable() {
                init {
                    setColor(Color.parseColor("#00E5CC"))
                    cornerRadius = 24 * density
                }
            }
            alpha = 0.3f
            isClickable = false
            setOnClickListener {
                dismiss()
            }
        }
        timerContainer.addView(continueButton)

        bottomRow.addView(timerContainer)
        mainContainer.addView(bottomRow)

        rootLayout.addView(mainContainer)
        return rootLayout
    }

    private fun drawScanlines(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#0AFFFFFF")
            strokeWidth = 1f
        }
        val height = canvas.height.toFloat()
        val width = canvas.width.toFloat()
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width, y, paint)
            y += 4f
        }
    }

    private fun startCountdown(overlayView: View, card: CardEntity) {
        val countdownText = overlayView.findViewById<TextView>(android.R.id.text1)
        val continueButton = overlayView.findViewById<TextView>(android.R.id.button1)

        val prefs = context.getSharedPreferences("pausecard_prefs", Context.MODE_PRIVATE)
        val isFocusHour = prefs.getBoolean("focus_hours_enabled", false)

        countdownTimer = object : CountDownTimer(TOTAL_COUNTDOWN_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                countdownText?.text = "${secondsLeft}s"

                if (millisUntilFinished <= CONTINUE_ENABLE_MS) {
                    if (!isFocusHour) {
                        continueButton?.alpha = 1.0f
                        continueButton?.isClickable = true
                    }
                }
            }

            override fun onFinish() {
                dismiss()
            }
        }.start()
    }

    private fun markNotInterested(card: CardEntity) {
        scope.launch {
            try {
                cardRepository.markDisliked(card.id)
                CrashReporting.logInfo(TAG, "Card ${card.id} marked as not interested")
            } catch (e: Exception) {
                CrashReporting.logError(TAG, "Error marking card as not interested", e)
            }
        }
    }

    private fun dismiss() {
        countdownTimer?.cancel()
        countdownTimer = null

        try {
            currentOverlay?.let { view ->
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Error removing overlay", e)
        }

        currentOverlay = null
        isShowing = false
        OverlayStateManager.markDismissed()
    }

    private fun getFallbackCard(): CardEntity {
        return CardEntity(
            title = "Take a Breath",
            body = "You were about to scroll mindlessly. Instead, take three deep breaths. " +
                    "Notice how your body feels right now. " +
                    "What were you planning to do on that app? Was it intentional? " +
                    "If yes, continue after the timer. If no, this pause just saved you time.",
            category = "General",
            isStatic = true
        )
    }

    private fun getEmojiForPackage(packageName: String): String {
        return when {
            packageName.contains("instagram") -> "📸"
            packageName.contains("youtube") -> "🎬"
            packageName.contains("tiktok") || packageName.contains("musically") -> "🎵"
            packageName.contains("twitter") -> "🐦"
            packageName.contains("facebook") -> "👤"
            packageName.contains("reddit") -> "🔥"
            packageName.contains("snapchat") -> "👻"
            packageName.contains("netflix") -> "📺"
            packageName.contains("spotify") -> "🎧"
            packageName.contains("discord") -> "💬"
            else -> "📱"
        }
    }

    fun cleanup() {
        scope.cancel()
        dismiss()
    }

    companion object {
        private const val TAG = "CardOverlayManager"
        private const val TOTAL_COUNTDOWN_MS = 15_000L
        private const val CONTINUE_ENABLE_MS = 10_000L
        const val ACTION_SHOW_CARD = "com.pausecard.app.SHOW_CARD"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
    }
}
