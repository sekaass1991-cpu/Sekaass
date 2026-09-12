package com.owner.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream

/**
 * Thin wrapper around [AudioRecord] shared by wake-word capture and voice
 * enrollment so both use the exact same sample rate/format the feature
 * extractor expects (16kHz mono 16-bit PCM).
 */
object PcmRecorder {

    const val SAMPLE_RATE = 16_000

    @SuppressLint("MissingPermission") // caller is required to have checked RECORD_AUDIO
    fun recordFor(durationMillis: Long): ShortArray {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE) // at least 1s of headroom
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )

        val output = ByteArrayOutputStream()
        val chunk = ShortArray(1024)
        val targetSamples = (SAMPLE_RATE * durationMillis / 1000).toInt()
        var samplesRead = 0

        try {
            recorder.startRecording()
            while (samplesRead < targetSamples) {
                val read = recorder.read(chunk, 0, chunk.size)
                if (read <= 0) break
                for (i in 0 until read) {
                    output.write(chunk[i].toInt() and 0xFF)
                    output.write((chunk[i].toInt() shr 8) and 0xFF)
                }
                samplesRead += read
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        val bytes = output.toByteArray()
        val shorts = ShortArray(bytes.size / 2)
        for (i in shorts.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            shorts[i] = ((hi shl 8) or lo).toShort()
        }
        return shorts
    }
}
