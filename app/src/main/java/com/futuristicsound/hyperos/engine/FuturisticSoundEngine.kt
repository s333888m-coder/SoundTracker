package com.futuristicsound.hyperos.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.PI
import kotlin.math.sin

enum class SoundEvent(val label: String, val startHz: Double, val endHz: Double, val durationMs: Int) {
    SWIPE_BACK("Swipe Back (Edge)", 960.0, 390.0, 140),
    SWIPE_RECENTS("Swipe Up (Recents)", 340.0, 780.0, 160),
    SWIPE_HOME("Swipe Up (Home)", 520.0, 1040.0, 120),
    NOTIF_PANEL("Top-Left Notification Panel", 1180.0, 620.0, 160),
    CONTROL_CENTER("Top-Right Control Center", 480.0, 1320.0, 180),
    APP_CLICK("App & Settings Click", 1280.0, 640.0, 55),
    HOME_TAP("Home Icon Tap", 620.0, 1240.0, 75),
    KEYBOARD_TAP("Keyboard Typing", 1750.0, 780.0, 32),
    DIALER_TAP("Dialer Pad Touch", 880.0, 1140.0, 100),
    LIST_SCROLL("List Scroll Tick", 1900.0, 1200.0, 22),
    GMAIL_ALERT("Gmail Alert", 880.0, 1760.0, 220),
    SMS_ALERT("SMS / Messages", 1046.0, 1568.0, 180),
    WHATSAPP_ALERT("WhatsApp / Chat", 587.0, 1480.0, 170),
    GENERAL_NOTIF("General Notification", 920.0, 1220.0, 160),
    INCOMING_CALL("Incoming Call Ringtone", 523.0, 1046.0, 350),
    CALL_CONNECTED("Call Connected", 440.0, 1100.0, 180),
    CALL_ENDED("Call Ended", 880.0, 290.0, 210),
    APP_LAUNCH("App Launch", 420.0, 1180.0, 120),
    SCREEN_UNLOCK("Screen Unlock", 523.0, 1568.0, 220),
    SCREEN_LOCK("Screen Lock", 840.0, 240.0, 95),
    CHARGER_PLUG("HyperCharge Connected", 240.0, 1600.0, 300),
    CHARGER_UNPLUG("Charger Disconnected", 960.0, 320.0, 190),
    VOLUME_KEY("Volume Keys", 1100.0, 1320.0, 50),
    BT_CONNECTED("Bluetooth Connected", 740.0, 1480.0, 180)
}

object FuturisticSoundEngine {
    private const val PREFS = "futuristic_sound_prefs"

    fun getVolume(context: Context, event: SoundEvent): Float {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val master = p.getInt("master_vol", 85) / 100f
        val perSound = p.getInt("vol_" + event.name, 85) / 100f
        return (master * perSound).coerceIn(0f, 1f)
    }

    fun isEnabled(context: Context, event: SoundEvent): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getBoolean("master_on", true) && p.getBoolean("on_" + event.name, true)
    }

    fun setCustomUri(context: Context, event: SoundEvent, uriString: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("uri_" + event.name, uriString).apply()
    }

    fun play(context: Context, event: SoundEvent) {
        if (!isEnabled(context, event)) return
        val vol = getVolume(context, event)
        val customUri = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("uri_" + event.name, null)

        if (!customUri.isNullOrBlank()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(customUri))
                    setVolume(vol, vol)
                    prepare()
                    start()
                    setOnCompletionListener { release() }
                }
                return
            } catch (_: Exception) {}
        }
        Thread { synthesizeSciFiTone(event.startHz, event.endHz, event.durationMs, vol) }.start()
    }

    private fun synthesizeSciFiTone(startHz: Double, endHz: Double, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val count = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * progress
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(progress * PI) * volume
            val wave = (sin(phase) * 0.72 + sin(phase * 2.0) * 0.28) * env
            samples[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep(durationMs.toLong() + 40)
        track.release()
    }
}