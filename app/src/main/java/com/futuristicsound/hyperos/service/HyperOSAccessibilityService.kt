package com.futuristicsound.hyperos.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.futuristicsound.hyperos.engine.FuturisticSoundEngine
import com.futuristicsound.hyperos.engine.SoundEvent

class HyperOSAccessibilityService : AccessibilityService() {
    private var lastWindowPkg = ""
    private var lastBackTs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val typeName = AccessibilityEvent.eventTypeToString(event.eventType)
        val pkg = event.packageName?.toString() ?: "unknown"
        val cls = event.className?.toString() ?: "unknown"

        // 1. Check User-Bound Custom Catcher Rules first (No Developer Mode needed!)
        val customSound = lookupCustomRule(typeName, pkg, cls)
        var matchedEvent: SoundEvent? = customSound

        // 2. Built-in HyperOS 3.0.3.0 Detection Rules
        if (matchedEvent == null) {
            matchedEvent = when {
                cls.contains("ControlCenter", true) || cls.contains("HyperControl", true) -> SoundEvent.CONTROL_CENTER
                cls.contains("NotificationShade", true) || cls.contains("StatusBarWindow", true) -> SoundEvent.NOTIF_PANEL
                cls.contains("GestureStub", true) || cls.contains("BackIndicator", true) -> SoundEvent.SWIPE_BACK
                cls.contains("Recents", true) -> SoundEvent.SWIPE_RECENTS
                pkg == "com.miui.home" && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && lastWindowPkg != "com.miui.home" -> SoundEvent.SWIPE_HOME
                event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> SoundEvent.KEYBOARD_TAP
                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && pkg.contains("contacts") -> SoundEvent.DIALER_TAP
                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && pkg == "com.miui.home" -> SoundEvent.HOME_TAP
                event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED -> SoundEvent.APP_CLICK
                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED -> SoundEvent.LIST_SCROLL
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg != lastWindowPkg && pkg != "com.android.systemui" -> SoundEvent.APP_LAUNCH
                else -> null
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastWindowPkg = pkg
        }

        if (matchedEvent != null) {
            FuturisticSoundEngine.play(this, matchedEvent)
        }

        // Broadcast live metadata to Floating Event Catcher Popup HUD
        sendBroadcast(Intent("com.futuristicsound.LIVE_EVENT").apply {
            putExtra("type", typeName)
            putExtra("pkg", pkg)
            putExtra("cls", cls)
            putExtra("matched", matchedEvent?.name ?: "UNMAPPED")
        })
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                FuturisticSoundEngine.play(this, SoundEvent.VOLUME_KEY)
            } else if (event.keyCode == KeyEvent.KEYCODE_BACK && System.currentTimeMillis() - lastBackTs > 150) {
                lastBackTs = System.currentTimeMillis()
                FuturisticSoundEngine.play(this, SoundEvent.SWIPE_BACK)
            }
        }
        return super.onKeyEvent(event)
    }

    private fun lookupCustomRule(type: String, pkg: String, cls: String): SoundEvent? {
        val prefs = getSharedPreferences("custom_event_rules", Context.MODE_PRIVATE)
        val soundName = prefs.getString("$type|$pkg|$cls", null) ?: return null
        return runCatching { SoundEvent.valueOf(soundName) }.getOrNull()
    }

    override fun onInterrupt() {}
}