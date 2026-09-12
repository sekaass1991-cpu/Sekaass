package com.owner.assistant.voice

import android.content.Context
import com.owner.assistant.data.OwnerProfileStore

/**
 * Gatekeeper: every command must pass through here before the CommandRouter
 * acts on it. Ensures ONLY the owner's voice can trigger anything.
 *
 * Compares a live utterance's MFCC-style embedding (see [AudioFeatureExtractor])
 * against every sample recorded during onboarding enrollment and accepts the
 * command if the best match clears the configured similarity threshold.
 */
class VoiceLock(private val context: Context) {

    fun isOwnerVoice(pcm16: ShortArray): Boolean {
        val enrolled = OwnerProfileStore.loadEnrolledEmbeddings(context)
        if (enrolled.isEmpty()) {
            // No enrollment yet: fail closed. Nothing should act on anyone's
            // voice until the owner has completed onboarding.
            return false
        }

        val liveEmbedding = AudioFeatureExtractor.extractEmbedding(pcm16)
        val bestSimilarity = enrolled.maxOf { AudioFeatureExtractor.cosineSimilarity(it, liveEmbedding) }
        val threshold = OwnerProfileStore.getVoiceSimilarityThreshold(context)
        return bestSimilarity >= threshold
    }
}
