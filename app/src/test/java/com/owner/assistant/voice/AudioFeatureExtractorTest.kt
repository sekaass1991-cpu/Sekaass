package com.owner.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pure-JVM tests for the MFCC-style embedding pipeline that backs [VoiceLock] —
 * no Android framework needed, so these run in plain `testDebugUnitTest`
 * without a device or emulator.
 */
class AudioFeatureExtractorTest {

    private fun sineWave(frequencyHz: Double, sampleRate: Int = 16_000, durationMs: Int = 200): ShortArray {
        val sampleCount = sampleRate * durationMs / 1000
        return ShortArray(sampleCount) { i ->
            (sin(2 * PI * frequencyHz * i / sampleRate) * 20000).toInt().toShort()
        }
    }

    @Test
    fun `identical audio produces near-identical embeddings`() {
        val audio = sineWave(440.0)
        val embeddingA = AudioFeatureExtractor.extractEmbedding(audio)
        val embeddingB = AudioFeatureExtractor.extractEmbedding(audio)

        assertEquals(1.0f, AudioFeatureExtractor.cosineSimilarity(embeddingA, embeddingB), 1e-4f)
    }

    @Test
    fun `different pitched tones produce less similar embeddings than identical audio`() {
        val lowTone = sineWave(220.0)
        val highTone = sineWave(1760.0)

        val selfSimilarity = AudioFeatureExtractor.cosineSimilarity(
            AudioFeatureExtractor.extractEmbedding(lowTone),
            AudioFeatureExtractor.extractEmbedding(lowTone)
        )
        val crossSimilarity = AudioFeatureExtractor.cosineSimilarity(
            AudioFeatureExtractor.extractEmbedding(lowTone),
            AudioFeatureExtractor.extractEmbedding(highTone)
        )

        assertTrue(
            "Expected two different tones ($crossSimilarity) to be less self-similar than identical audio ($selfSimilarity)",
            crossSimilarity < selfSimilarity
        )
    }

    @Test
    fun `embedding is always 13 coefficients`() {
        assertEquals(13, AudioFeatureExtractor.extractEmbedding(sineWave(440.0)).size)
    }

    @Test
    fun `audio shorter than one frame returns a zeroed embedding instead of crashing`() {
        val tooShort = ShortArray(100)
        val embedding = AudioFeatureExtractor.extractEmbedding(tooShort)

        assertEquals(13, embedding.size)
        assertTrue(embedding.all { it == 0f })
    }

    @Test
    fun `cosine similarity handles mismatched or empty vectors without crashing`() {
        assertEquals(0f, AudioFeatureExtractor.cosineSimilarity(FloatArray(0), FloatArray(0)))
        assertEquals(0f, AudioFeatureExtractor.cosineSimilarity(FloatArray(3), FloatArray(5)))
        assertEquals(0f, AudioFeatureExtractor.cosineSimilarity(FloatArray(3), FloatArray(3)))
    }

    @Test
    fun `cosine similarity of identical non-zero vectors is 1`() {
        val vector = floatArrayOf(1f, 2f, 3f, -4f)
        assertEquals(1.0f, AudioFeatureExtractor.cosineSimilarity(vector, vector), 1e-6f)
    }
}
