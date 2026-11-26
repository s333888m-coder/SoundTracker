package com.futuristicsound.hyperos

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.futuristicsound.hyperos.engine.FuturisticSoundEngine
import com.futuristicsound.hyperos.engine.SoundEvent
import com.futuristicsound.hyperos.service.EventCatcherOverlayService

class MainActivity : AppCompatActivity() {
    private var pendingCustomEvent: SoundEvent? = null
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val ev = pendingCustomEvent ?: return@registerForActivityResult
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            FuturisticSoundEngine.setCustomUri(this, ev, uri.toString())
            FuturisticSoundEngine.play(this, ev)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050814"))
            setPadding(36, 48, 36, 48)
        }
        val title = TextView(this).apply {
            text = "FUTURISTIC SOUND • HyperOS 3.0.3.0"
            textSize = 20f
            setTextColor(Color.parseColor("#00E5FF"))
        }
        root.addView(title)

        val accBtn = Button(this).apply {
            text = "1. Enable Accessibility Service (Gestures)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val notifBtn = Button(this).apply {
            text = "2. Enable Notification Access (Gmail/SMS/WhatsApp)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        val hudBtn = Button(this).apply {
            text = "3. Launch Floating Event Catcher HUD (No Dev Mode)"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, EventCatcherOverlayService::class.java))
                }
            }
        }
        root.addView(accBtn)
        root.addView(notifBtn)
        root.addView(hudBtn)

        val prefs = getSharedPreferences("futuristic_sound_prefs", MODE_PRIVATE)
        SoundEvent.values().forEach { ev ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#0D1428"))
                setPadding(24, 18, 24, 18)
            }
            val lbl = TextView(this).apply {
                text = ev.label
                setTextColor(Color.WHITE)
            }
            val seek = SeekBar(this).apply {
                max = 100
                progress = prefs.getInt("vol_" + ev.name, 85)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                        prefs.edit().putInt("vol_" + ev.name, v).apply()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) { FuturisticSoundEngine.play(this@MainActivity, ev) }
                })
            }
            val customBtn = Button(this).apply {
                text = "Test / Custom Audio"
                setOnClickListener { FuturisticSoundEngine.play(this@MainActivity, ev) }
                setOnLongClickListener {
                    pendingCustomEvent = ev
                    pickAudioLauncher.launch(arrayOf("audio/*"))
                    true
                }
            }
            row.addView(lbl)
            row.addView(seek)
            row.addView(customBtn)
            root.addView(row)
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}