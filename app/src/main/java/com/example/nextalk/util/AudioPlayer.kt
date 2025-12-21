package com.example.nextalk.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

/**
 * Gestionnaire audio pour la lecture et l'enregistrement de messages vocaux
 */
class AudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    // État de lecture
    sealed class PlaybackState {
        object Idle : PlaybackState()
        data class Playing(val messageId: String, val progress: Float) : PlaybackState()
        data class Paused(val messageId: String, val progress: Float) : PlaybackState()
        object Error : PlaybackState()
    }

    // État d'enregistrement
    sealed class RecordingState {
        object Idle : RecordingState()
        data class Recording(val duration: Long) : RecordingState()
        object Error : RecordingState()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentPlayingId: String? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0L

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState

    /**
     * Jouer un message vocal depuis une URL
     */
    fun play(messageId: String, audioUrl: String) {
        try {
            // Si on joue déjà ce message, pause
            if (currentPlayingId == messageId && mediaPlayer?.isPlaying == true) {
                pause()
                return
            }

            // Si on jouait un autre message, arrêter
            stop()

            currentPlayingId = messageId

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(audioUrl)

                setOnPreparedListener {
                    start()
                    _playbackState.value = PlaybackState.Playing(messageId, 0f)
                    Log.d(TAG, "Audio playback started for message: $messageId")
                }

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Idle
                    currentPlayingId = null
                    Log.d(TAG, "Audio playback completed")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = PlaybackState.Error
                    currentPlayingId = null
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            _playbackState.value = PlaybackState.Error
        }
    }

    /**
     * Jouer un fichier audio local
     */
    fun playLocal(messageId: String, filePath: String) {
        try {
            // Si on joue déjà ce message, pause
            if (currentPlayingId == messageId && mediaPlayer?.isPlaying == true) {
                pause()
                return
            }

            // Si on jouait un autre message, arrêter
            stop()

            currentPlayingId = messageId

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(filePath)

                setOnPreparedListener {
                    start()
                    _playbackState.value = PlaybackState.Playing(messageId, 0f)
                    Log.d(TAG, "Local audio playback started")
                }

                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Idle
                    currentPlayingId = null
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = PlaybackState.Error
                    true
                }

                prepare()
                start()
                _playbackState.value = PlaybackState.Playing(messageId, 0f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing local audio", e)
            _playbackState.value = PlaybackState.Error
        }
    }

    /**
     * Mettre en pause la lecture
     */
    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    val progress = if (it.duration > 0) {
                        it.currentPosition.toFloat() / it.duration.toFloat()
                    } else 0f
                    _playbackState.value = PlaybackState.Paused(currentPlayingId ?: "", progress)
                    Log.d(TAG, "Audio paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    /**
     * Reprendre la lecture
     */
    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _playbackState.value = PlaybackState.Playing(currentPlayingId ?: "", 0f)
                    Log.d(TAG, "Audio resumed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio", e)
        }
    }

    /**
     * Arrêter la lecture
     */
    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
            mediaPlayer = null
            currentPlayingId = null
            _playbackState.value = PlaybackState.Idle
            Log.d(TAG, "Audio stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    /**
     * Obtenir la position actuelle en millisecondes
     */
    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Obtenir la durée totale en millisecondes
     */
    fun getDuration(): Int {
        return try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Vérifier si l'audio est en cours de lecture
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifier si on joue un message spécifique
     */
    fun isPlayingMessage(messageId: String): Boolean {
        return currentPlayingId == messageId && isPlaying()
    }

    // ============= ENREGISTREMENT =============

    /**
     * Démarrer l'enregistrement vocal
     */
    fun startRecording(): File? {
        try {
            Log.d(TAG, "Starting recording setup...")
            
            // Arrêter tout enregistrement précédent
            try {
                mediaRecorder?.apply {
                    stop()
                    reset()
                    release()
                }
            } catch (e: Exception) {
                // Ignorer les erreurs si pas d'enregistrement en cours
            }
            mediaRecorder = null
            
            // Créer le répertoire d'enregistrement
            val audioDir = File(context.cacheDir, "voice_messages")
            if (!audioDir.exists()) {
                val created = audioDir.mkdirs()
                Log.d(TAG, "Audio directory created: $created, path: ${audioDir.absolutePath}")
            }
            
            // Créer le fichier
            recordingFile = File(audioDir, "voice_${System.currentTimeMillis()}.3gp")
            Log.d(TAG, "Recording file: ${recordingFile?.absolutePath}")
            
            // Configurer MediaRecorder avec des paramètres compatibles
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    Log.d(TAG, "Audio source set: MIC")
                    
                    // Utiliser 3GP qui est plus compatible
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    Log.d(TAG, "Output format set: THREE_GPP")
                    
                    // AMR_NB est très compatible
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    Log.d(TAG, "Audio encoder set: AMR_NB")
                    
                    setOutputFile(recordingFile?.absolutePath)
                    Log.d(TAG, "Output file set")
                    
                    prepare()
                    Log.d(TAG, "MediaRecorder prepared")
                    
                    start()
                    Log.d(TAG, "MediaRecorder started")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configuring MediaRecorder", e)
                    throw e
                }
            }
            
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(0L)
            Log.d(TAG, "Recording started successfully: ${recordingFile?.absolutePath}")
            
            return recordingFile
        } catch (e: IOException) {
            Log.e(TAG, "IOException starting recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error
            cleanupRecording()
            return null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException starting recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error
            cleanupRecording()
            return null
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - Permission denied: ${e.message}", e)
            _recordingState.value = RecordingState.Error
            cleanupRecording()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error
            cleanupRecording()
            return null
        }
    }
    
    private fun cleanupRecording() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignorer
        }
        mediaRecorder = null
        recordingFile?.delete()
        recordingFile = null
    }

    /**
     * Arrêter l'enregistrement et retourner le fichier
     */
    fun stopRecording(): Pair<File?, Long>? {
        Log.d(TAG, "Stopping recording...")
        
        if (mediaRecorder == null) {
            Log.w(TAG, "No active recording to stop")
            return null
        }
        
        return try {
            val duration = System.currentTimeMillis() - recordingStartTime
            Log.d(TAG, "Recording duration: ${duration}ms")
            
            mediaRecorder?.apply {
                try {
                    stop()
                    Log.d(TAG, "MediaRecorder stopped")
                } catch (e: RuntimeException) {
                    // stop() peut échouer si l'enregistrement était trop court
                    Log.e(TAG, "Error stopping MediaRecorder (recording may be too short)", e)
                }
                reset()
                release()
            }
            mediaRecorder = null
            recordingStartTime = 0L
            
            _recordingState.value = RecordingState.Idle
            
            // Vérifier que le fichier existe et a du contenu
            val file = recordingFile
            if (file != null && file.exists()) {
                Log.d(TAG, "Recording file exists: ${file.absolutePath}")
                Log.d(TAG, "Recording file size: ${file.length()} bytes")
                
                if (file.length() > 0) {
                    Pair(file, duration)
                } else {
                    Log.e(TAG, "Recording file is empty")
                    file.delete()
                    null
                }
            } else {
                Log.e(TAG, "Recording file does not exist")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error
            cleanupRecording()
            null
        }
    }

    /**
     * Annuler l'enregistrement
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling recording", e)
        } finally {
            mediaRecorder = null
            recordingFile?.delete()
            recordingFile = null
            _recordingState.value = RecordingState.Idle
        }
    }

    /**
     * Obtenir la durée d'enregistrement actuelle
     */
    fun getRecordingDuration(): Long {
        return if (recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }

    /**
     * Vérifier si on enregistre actuellement
     */
    fun isRecording(): Boolean {
        return _recordingState.value is RecordingState.Recording
    }

    /**
     * Libérer les ressources
     */
    fun release() {
        stop()
        cancelRecording()
        Log.d(TAG, "AudioPlayer released")
    }
}
