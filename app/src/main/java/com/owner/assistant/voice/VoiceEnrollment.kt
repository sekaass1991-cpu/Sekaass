package com.owner.assistant.voice

import android.content.Context
import com.owner.assistant.data.OwnerProfileStore

/**
 * Onboarding step: records a handful of short samples of the owner speaking
 * the wake phrase (or any phrase) and stores the resulting embeddings for
 * [VoiceLock] to compare against later. Call [recordOneSample] once per
 * enrollment step from the onboarding UI (each call blocks for
 * [SAMPLE_DURATION_MS], so run it off the main thread).
 */
class VoiceEnrollment(private val context: Context) {

    fun recordOneSample(): FloatArray {
        val pcm = PcmRecorder.recordFor(SAMPLE_DURATION_MS)
        val embedding = AudioFeatureExtractor.extractEmbedding(pcm)
        OwnerProfileStore.saveEnrollmentSample(context, embedding)
        return embedding
    }

    fun samplesRecorded(): Int = OwnerProfileStore.loadEnrolledEmbeddings(context).size

    fun isComplete(): Boolean = OwnerProfileStore.isVoiceEnrolled(context)

    fun reset() = OwnerProfileStore.clearVoiceProfile(context)

    companion object {
        const val SAMPLE_DURATION_MS = 2500L
        const val REQUIRED_SAMPLES = OwnerProfileStore.MIN_ENROLLMENT_SAMPLES
    }
}
