package com.tasker.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.service.voice.VoiceInteractionSession.ActivityId
import com.tasker.R

class MusicService {
    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var audioManager: AudioManager? = null
        private var focusRequest: AudioFocusRequest? = null
        private var isAudioFocusGranted = false

        // Play music when accepting a task
        fun playAcceptMusic(context: Context) {
            // Clean up any existing media player
            stopMusic(context)

            // Get audio manager if needed
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }

            // Request audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange, context)
                    }
                    .build()

                val result = audioManager?.requestAudioFocus(focusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager?.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange, context) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED

                isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            }

            // Only play if audio focus is granted
            if (isAudioFocusGranted) {
                try {
                    // Create and configure new media player for accept music
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(context.resources.openRawResourceFd(R.raw.task_accepted))
                        isLooping = true
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If there's an error, make sure we release audio focus
                    abandonAudioFocus(context)
                }
            }
        }

        // Play a sound when rejecting a task
        fun playRejectSound(context: Context) {
            // Clean up any existing media player
            stopMusic(context)

            // Get audio manager if needed
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            }

            // Request audio focus for a short duration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange, context)
                    }
                    .build()

                val result = audioManager?.requestAudioFocus(focusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
                isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager?.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange, context) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                ) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED

                isAudioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            }

            // Only play if audio focus is granted
            if (isAudioFocusGranted) {
                // Create and configure new media player for reject sound
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(context.resources.openRawResourceFd(R.raw.task_rejected))
                        isLooping = false
                        prepare()
                        start()

                        // Release media player when sound finishes playing
                        setOnCompletionListener {
                            it.release()
                            mediaPlayer = null
                            abandonAudioFocus(context)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If there's an error, make sure we release audio focus
                    abandonAudioFocus(context)
                }
            }
        }

        // Pause the music
        fun pauseMusic(context: Context) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }

        // Resume the music
        fun resumeMusic(context: Context) {
            mediaPlayer?.let {
                if (!it.isPlaying && isAudioFocusGranted) {
                    it.start()
                }
            }
        }

        // Stop all music
        fun stopMusic(context: Context) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                mediaPlayer = null
            }

            // Abandon audio focus
            abandonAudioFocus(context)
        }

        // Handle audio focus changes
        private fun handleAudioFocusChange(focusChange: Int, context: Context) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Another app took focus, pause our playback
                    pauseMusic(context)
                    isAudioFocusGranted = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Another app needs focus briefly, we can lower volume instead of pausing
                    mediaPlayer?.setVolume(0.3f, 0.3f)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // We got focus back, resume at full volume
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                    resumeMusic(context)
                    isAudioFocusGranted = true
                }
            }
        }

        // Abandon audio focus
        private fun abandonAudioFocus(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let {
                    audioManager?.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
            isAudioFocusGranted = false
            focusRequest = null
        }
    }
}