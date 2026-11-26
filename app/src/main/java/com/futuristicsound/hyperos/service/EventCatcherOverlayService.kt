package com.futuristicsound.hyperos.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.futuristicsound.hyperos.engine.FuturisticSoundEngine
import com.futuristicsound.hyperos.engine.SoundEvent

// Floating Event Catcher HUD — Works WITHOUT Developer Mode via SYSTEM_ALERT_WINDOW
class EventCatcherOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var infoText: TextView
    private var lastSig = ""

    private val liveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val type = intent?.getStringExtra("type") ?: return
            val pkg = intent.getStringExtra("pkg") ?: ""
            val cls = intent.getStringExtra("cls") ?: ""
            val matched = intent.getStringExtra("matched") ?: "UNMAPPED"
            lastSig = "$type|$pkg|$cls"
            infoText.text = "[$matched]\n$type\n$pkg\n$cls"
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E60D1428"))
            setPadding(24, 18, 24, 18)
        }
        infoText = TextView(this).apply {
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            text = "EVENT CATCHER HUD (No Dev Mode)\nPerform any gesture to catch..."
        }
        val bindBackBtn = Button(this).apply {
            text = "+ Bind to Swipe Back"
            setOnClickListener {
                if (lastSig.isNotEmpty()) {
                    getSharedPreferences("custom_event_rules", MODE_PRIVATE).edit()
                        .putString(lastSig, SoundEvent.SWIPE_BACK.name).apply()
                    FuturisticSoundEngine.play(this@EventCatcherOverlayService, SoundEvent.SWIPE_BACK)
                }
            }
        }
        overlayView.addView(infoText)
        overlayView.addView(bindBackBtn)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 30; y = 180 }

        windowManager.addView(overlayView, params)
        val filter = IntentFilter("com.futuristicsound.LIVE_EVENT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(liveReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(liveReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(liveReceiver)
        windowManager.removeView(overlayView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}