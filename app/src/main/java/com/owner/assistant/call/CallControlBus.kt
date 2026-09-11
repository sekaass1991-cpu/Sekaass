package com.owner.assistant.call

import android.telecom.Call
import android.telecom.VideoProfile
import java.lang.ref.WeakReference

/**
 * Bridges voice commands (handled in [com.owner.assistant.service.CommandRouter],
 * a plain class) and [InCallActivity]'s UI buttons to the live
 * [android.telecom.InCallService] instance and its current [Call] — Android
 * only ever instantiates one InCallService for the app, so a single mutable
 * reference here is sufficient.
 */
object CallControlBus {

    fun interface CallChangedListener {
        fun onCallChanged(call: Call?)
    }

    private var currentCall: Call? = null
    private var serviceRef: WeakReference<AssistantInCallService>? = null
    private var listener: CallChangedListener? = null

    fun bindService(service: AssistantInCallService) {
        serviceRef = WeakReference(service)
    }

    fun unbindService(service: AssistantInCallService) {
        if (serviceRef?.get() === service) serviceRef = null
    }

    fun setCurrentCall(call: Call?) {
        currentCall = call
        listener?.onCallChanged(call)
    }

    fun clearIfCurrent(call: Call) {
        if (currentCall === call) {
            currentCall = null
            listener?.onCallChanged(null)
        }
    }

    fun getCurrentCall(): Call? = currentCall

    /** [InCallActivity] uses this to react to the call ringing, connecting, or ending. Only one screen needs to observe this at a time. */
    fun setListener(listener: CallChangedListener?) {
        this.listener = listener
    }

    fun answer() {
        currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun decline() {
        currentCall?.let {
            if (it.state == Call.STATE_RINGING) it.reject(false, null) else it.disconnect()
        }
    }

    fun setMuted(muted: Boolean) {
        serviceRef?.get()?.setMuted(muted)
    }

    fun isMuted(): Boolean = serviceRef?.get()?.callAudioState?.isMuted ?: false
}
