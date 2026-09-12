package com.owner.assistant.voice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * Turns a raw 16-bit PCM mono clip into a fixed-length MFCC-style embedding
 * so [VoiceLock] can compare "does this sound like the owner" without any
 * ML model or network call — everything here is plain signal processing.
 *
 * This is intentionally lightweight (mel-filterbank + DCT, no deltas, no
 * neural speaker-embedding model) so it runs comfortably on-device in pure
 * Kotlin. It is good enough to reject an unrelated voice with a different
 * pitch/timbre, but it is NOT the same guarantee a trained speaker-verification
 * model gives. Swap this out for a proper on-device model (e.g. a distilled
 * d-vector/x-vector network) if the false-accept rate matters more than
 * keeping the dependency-free implementation.
 */
object AudioFeatureExtractor {

    private const val SAMPLE_RATE = 16_000
    private const val FRAME_SIZE = 512 // 32ms @ 16kHz, power of two for FFT
    private const val FRAME_STRIDE = 256 // 50% overlap
    private const val MEL_FILTERS = 26
    private const val MFCC_COEFFICIENTS = 13

    /** @param pcm16 mono, 16kHz, 16-bit little-endian PCM samples. */
    fun extractEmbedding(pcm16: ShortArray): FloatArray {
        if (pcm16.size < FRAME_SIZE) return FloatArray(MFCC_COEFFICIENTS)

        val melFilterBank = buildMelFilterBank(MEL_FILTERS, FRAME_SIZE, SAMPLE_RATE)
        val accumulated = DoubleArray(MFCC_COEFFICIENTS)
        var frameCount = 0

        var offset = 0
        while (offset + FRAME_SIZE <= pcm16.size) {
            val frame = DoubleArray(FRAME_SIZE) { i ->
                pcm16[offset + i] / 32768.0 * hammingWindow(i, FRAME_SIZE)
            }

            val (real, imag) = fft(frame)
            val powerSpectrum = DoubleArray(FRAME_SIZE / 2) { i ->
                (real[i] * real[i] + imag[i] * imag[i]) / FRAME_SIZE
            }

            val melEnergies = DoubleArray(MEL_FILTERS) { m ->
                var sum = 0.0
                for (k in powerSpectrum.indices) sum += powerSpectrum[k] * melFilterBank[m][k]
                ln(max(sum, 1e-10))
            }

            val mfcc = dct(melEnergies, MFCC_COEFFICIENTS)
            for (i in mfcc.indices) accumulated[i] += mfcc[i]
            frameCount++
            offset += FRAME_STRIDE
        }

        if (frameCount == 0) return FloatArray(MFCC_COEFFICIENTS)
        return FloatArray(MFCC_COEFFICIENTS) { (accumulated[it] / frameCount).toFloat() }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))).toFloat()
    }

    private fun hammingWindow(n: Int, size: Int): Double =
        0.54 - 0.46 * cos(2 * PI * n / (size - 1))

    /** Iterative radix-2 Cooley-Tukey FFT. `input.size` must be a power of two. */
    private fun fft(input: DoubleArray): Pair<DoubleArray, DoubleArray> {
        val n = input.size
        val real = input.copyOf()
        val imag = DoubleArray(n)

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wReal = cos(angle)
            val wImag = sin(angle)
            var i = 0
            while (i < n) {
                var curReal = 1.0
                var curImag = 0.0
                for (k in 0 until len / 2) {
                    val evenIdx = i + k
                    val oddIdx = i + k + len / 2
                    val oddReal = real[oddIdx] * curReal - imag[oddIdx] * curImag
                    val oddImag = real[oddIdx] * curImag + imag[oddIdx] * curReal

                    real[oddIdx] = real[evenIdx] - oddReal
                    imag[oddIdx] = imag[evenIdx] - oddImag
                    real[evenIdx] += oddReal
                    imag[evenIdx] += oddImag

                    val nextReal = curReal * wReal - curImag * wImag
                    val nextImag = curReal * wImag + curImag * wReal
                    curReal = nextReal
                    curImag = nextImag
                }
                i += len
            }
            len = len shl 1
        }
        return real to imag
    }

    private fun hzToMel(hz: Double) = 2595.0 * kotlin.math.log10(1.0 + hz / 700.0)

    private fun melToHz(mel: Double) = 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0)

    private fun buildMelFilterBank(numFilters: Int, fftSize: Int, sampleRate: Int): Array<DoubleArray> {
        val lowMel = hzToMel(0.0)
        val highMel = hzToMel(sampleRate / 2.0)
        val melPoints = DoubleArray(numFilters + 2) { lowMel + (highMel - lowMel) * it / (numFilters + 1) }
        val hzPoints = melPoints.map { melToHz(it) }
        val bins = hzPoints.map { ((fftSize + 1) * it / sampleRate).toInt() }

        return Array(numFilters) { m ->
            DoubleArray(fftSize / 2) { k ->
                val left = bins[m]
                val center = bins[m + 1]
                val right = bins[m + 2]
                when {
                    k < left || k > right -> 0.0
                    k <= center -> if (center == left) 0.0 else (k - left).toDouble() / (center - left)
                    else -> if (right == center) 0.0 else (right - k).toDouble() / (right - center)
                }
            }
        }
    }

    private fun dct(input: DoubleArray, numCoefficients: Int): DoubleArray {
        val n = input.size
        return DoubleArray(numCoefficients) { k ->
            var sum = 0.0
            for (i in 0 until n) sum += input[i] * cos(PI / n * (i + 0.5) * k)
            sum
        }
    }
}
