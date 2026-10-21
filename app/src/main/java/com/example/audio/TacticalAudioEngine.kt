package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class TacticalAudioEngine(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId: StateFlow<String?> = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    data class RecordedAudioResult(
        val file: File,
        val filePath: String,
        val durationSec: Int
    )

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        stopPlayback()

        return try {
            val audioDir = File(context.filesDir, "audio_notes").apply { mkdirs() }
            val file = File(audioDir, "tactical_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSec.value = 0
            true
        } catch (e: Exception) {
            Log.e("TacticalAudioEngine", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFile?.delete()
            currentOutputFile = null
            _isRecording.value = false
            false
        }
    }

    fun stopRecording(): RecordedAudioResult? {
        if (!_isRecording.value) return null
        val duration = ((System.currentTimeMillis() - recordStartTime) / 1000L).toInt().coerceAtLeast(1)
        val file = currentOutputFile

        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            _recordingDurationSec.value = 0

            if (file != null && file.exists() && file.length() > 0) {
                RecordedAudioResult(file = file, filePath = file.absolutePath, durationSec = duration)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("TacticalAudioEngine", "Error stopping recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecording.value = false
            null
        }
    }

    fun cancelRecording() {
        if (!_isRecording.value) return
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
        _isRecording.value = false
        _recordingDurationSec.value = 0
    }

    fun playAudio(messageId: String, filePathOrUri: String) {
        if (_currentlyPlayingId.value == messageId) {
            stopPlayback()
            return
        }

        stopPlayback()

        try {
            val player = MediaPlayer()
            val file = File(filePathOrUri)
            if (file.exists()) {
                player.setDataSource(file.absolutePath)
            } else {
                // Try URI or context asset
                player.setDataSource(filePathOrUri)
            }

            player.setOnCompletionListener {
                stopPlayback()
            }

            player.setOnErrorListener { _, _, _ ->
                stopPlayback()
                true
            }

            player.prepare()
            player.start()
            mediaPlayer = player
            _currentlyPlayingId.value = messageId
        } catch (e: Exception) {
            Log.e("TacticalAudioEngine", "Error playing audio file: $filePathOrUri", e)
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentlyPlayingId.value = null
        _playbackProgress.value = 0f
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
