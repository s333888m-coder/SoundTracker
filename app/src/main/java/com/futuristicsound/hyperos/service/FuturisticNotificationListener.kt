package com.futuristicsound.hyperos.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.futuristicsound.hyperos.engine.FuturisticSoundEngine
import com.futuristicsound.hyperos.engine.SoundEvent

class FuturisticNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val sound = when {
            pkg == "com.google.android.gm" -> SoundEvent.GMAIL_ALERT
            pkg.contains("messaging") || pkg.contains("mms") -> SoundEvent.SMS_ALERT
            pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" -> SoundEvent.WHATSAPP_ALERT
            else -> SoundEvent.GENERAL_NOTIF
        }
        FuturisticSoundEngine.play(this, sound)
    }
}