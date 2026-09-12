package com.owner.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DndAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ENABLE -> DndScheduler(context).enableNow()
            ACTION_DISABLE -> DndScheduler(context).disableNow()
        }
    }

    companion object {
        const val ACTION_ENABLE = "com.owner.assistant.action.DND_ENABLE"
        const val ACTION_DISABLE = "com.owner.assistant.action.DND_DISABLE"
    }
}
