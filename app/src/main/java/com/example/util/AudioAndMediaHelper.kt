package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

object AudioAndMediaHelper {

    suspend fun uriToBase64(context: Context, uri: Uri, maxBytes: Int = 2 * 1024 * 1024): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (mimeType.startsWith("image/")) {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap != null) {
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        val bytes = out.toByteArray()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        return@withContext Pair(base64, "image/jpeg")
                    }
                }
                val rawStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val rawBytes = rawStream.use { it.readBytes() }
                val trimmed = if (rawBytes.size > maxBytes) rawBytes.copyOf(maxBytes) else rawBytes
                val base64 = Base64.encodeToString(trimmed, Base64.NO_WRAP)
                Pair(base64, mimeType)
            } catch (e: Exception) {
                null
            }
        }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // Record a short 2.5-second PCM WAV clip from the microphone for gemini-3.5-transcribe / gemini-3.8-live
    @Suppress("MissingPermission")
    suspend fun recordShortWavBase64(durationMs: Long = 2400L): String = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

        val pcmOutput = ByteArrayOutputStream()
        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuf
            )
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                val buffer = ByteArray(1024)
                recorder.startRecording()
                val endTime = System.currentTimeMillis() + durationMs
                while (System.currentTimeMillis() < endTime) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        pcmOutput.write(buffer, 0, read)
                    }
                }
                recorder.stop()
                recorder.release()
            }
        } catch (_: Exception) {
        }

        val pcmBytes = pcmOutput.toByteArray()
        val wavBytes = wrapPcmInWavHeader(pcmBytes, sampleRate, 1, 16)
        Base64.encodeToString(wavBytes, Base64.NO_WRAP)
    }

    // Play either returned PCM/WAV Base64 audio or a synthesized ambient chord progression for Lyria Music / TTS feedback
    suspend fun playAudioOrSynthesizedTrack(base64Audio: String?, isMusic: Boolean = true) = withContext(Dispatchers.IO) {
        val sampleRate = 24000
        val pcmBytes: ByteArray = if (!base64Audio.isNullOrBlank()) {
            try {
                Base64.decode(base64Audio, Base64.DEFAULT)
            } catch (_: Exception) {
                synthesizeAmbientMelodyPcm(sampleRate, if (isMusic) 3.0 else 1.2)
            }
        } else {
            synthesizeAmbientMelodyPcm(sampleRate, if (isMusic) 3.0 else 1.2)
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmBytes.size.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmBytes, 0, pcmBytes.size)
            track.play()
        } catch (_: Exception) {
        }
    }

    private fun synthesizeAmbientMelodyPcm(sampleRate: Int, seconds: Double): ByteArray {
        val totalSamples = (sampleRate * seconds).toInt()
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25) // C4, E4, G4, C5
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = ((t / (seconds / notes.size)).toInt()).coerceIn(0, notes.lastIndex)
            val freq = notes[noteIdx]
            val envelope = sin((i.toDouble() / totalSamples) * PI)
            val sampleVal = (sin(2.0 * PI * freq * t) * 0.35 + sin(2.0 * PI * (freq * 1.5) * t) * 0.15) * envelope
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }

    private fun wrapPcmInWavHeader(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmData.size + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)

        val out = ByteArrayOutputStream(44 + pcmData.size)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }
}

class AndroidTtsSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar")
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text.take(600), TextToSpeech.QUEUE_FLUSH, null, "novaforge_utt")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
