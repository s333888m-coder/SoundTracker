package com.futuristicsound.hyperos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.futuristicsound.hyperos.engine.FuturisticSoundEngine
import com.futuristicsound.hyperos.engine.SoundEvent

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> FuturisticSoundEngine.play(context, SoundEvent.CHARGER_PLUG)
            Intent.ACTION_POWER_DISCONNECTED -> FuturisticSoundEngine.play(context, SoundEvent.CHARGER_UNPLUG)
            Intent.ACTION_USER_PRESENT -> FuturisticSoundEngine.play(context, SoundEvent.SCREEN_UNLOCK)
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_STATE_RINGING -> FuturisticSoundEngine.play(context, SoundEvent.INCOMING_CALL)
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> FuturisticSoundEngine.play(context, SoundEvent.CALL_CONNECTED)
                    TelephonyManager.EXTRA_STATE_IDLE -> FuturisticSoundEngine.play(context, SoundEvent.CALL_ENDED)
                }
            }
        }
    }
}