package com.intervalpulse.timer.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.intervalpulse.timer.MainActivity
import com.intervalpulse.timer.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    companion object {
        fun show(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            context.startService(Intent(context, FloatingOverlayService::class.java))
        }

        fun hide(context: Context) {
            context.stopService(Intent(context, FloatingOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var phaseLabel: TextView
    private lateinit var timeLabel: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayView()

        scope.launch {
            TimerRepository.state.collect { state ->
                if (!state.isActiveSession) {
                    stopSelf()
                    return@collect
                }
                val accent = if (state.phase == TimerPhase.WORKOUT) 0xFF10B981.toInt() else 0xFF06B6D4.toInt()
                phaseLabel.text = if (state.isPaused) "${state.phase.label} • PAUSED" else state.phase.label
                phaseLabel.setTextColor(accent)

                val mins = state.remainingSeconds / 60
                val secs = state.remainingSeconds % 60
                timeLabel.text = String.format("%02d:%02d", mins, secs)

                progressBar.max = state.totalPhaseSeconds.coerceAtLeast(1)
                progressBar.progress = state.remainingSeconds

                val targetPx = dpToPx(state.config.overlayWidthDp)
                layoutParams?.let { params ->
                    if (params.width != targetPx && overlayView != null) {
                        params.width = targetPx
                        windowManager.updateViewLayout(overlayView, params)
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val widthPx = dpToPx(TimerRepository.state.value.config.overlayWidthDp)
        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dpToPx(16)
            y = dpToPx(80)
        }
        layoutParams = params

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(0xEE0F172A.toInt())
                cornerRadius = dpToPx(14).toFloat()
                setStroke(dpToPx(1), 0xFF334155.toInt())
            }
        }

        phaseLabel = TextView(this).apply {
            textSize = 11f
            setTextColor(0xFF10B981.toInt())
        }

        timeLabel = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.WHITE)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
        }

        container.addView(phaseLabel)
        container.addView(timeLabel)
        container.addView(progressBar)

        // Drag + tap-to-open-app
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        container.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val openIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(openIntent)
                        stopSelf()
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = container
        windowManager.addView(container, params)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        scope.cancel()
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
