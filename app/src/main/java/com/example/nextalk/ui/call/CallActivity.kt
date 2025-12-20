package com.example.nextalk.ui.call

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.CallStatus
import com.example.nextalk.data.model.CallType
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.CallRepository
import com.example.nextalk.databinding.ActivityCallBinding
import kotlinx.coroutines.launch

/**
 * Activité pour les appels vocaux et vidéo
 * Gère l'interface utilisateur durant un appel
 */
class CallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_CALL_TYPE = "call_type"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_USER_NAME = "user_name"
        const val EXTRA_USER_PHOTO = "user_photo"
        const val EXTRA_IS_INCOMING = "is_incoming"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val TAG = "CallActivity"
    }

    private lateinit var binding: ActivityCallBinding
    private lateinit var callRepository: CallRepository
    private val authRepository = AuthRepository()

    private var callId: String = ""
    private var callType: CallType = CallType.VOICE
    private var userId: String = ""
    private var userName: String = ""
    private var userPhoto: String = ""
    private var isIncoming: Boolean = false
    private var isCallActive: Boolean = false
    private var isMicMuted: Boolean = false
    private var isSpeakerOn: Boolean = false
    private var isCameraOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityCallBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Récupérer les données de l'intent
            callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
            callType = CallType.valueOf(intent.getStringExtra(EXTRA_CALL_TYPE) ?: "VOICE")
            userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
            userName = intent.getStringExtra(EXTRA_USER_NAME) ?: ""
            userPhoto = intent.getStringExtra(EXTRA_USER_PHOTO) ?: ""
            isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)

            initRepositories()
            setupUI()
            setupCallButtons()

            if (isIncoming) {
                showIncomingCallUI()
            } else {
                showOutgoingCallUI()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        callRepository = CallRepository(database.callDao())
    }

    private fun setupUI() {
        // Charger l'avatar de l'utilisateur
        if (userPhoto.isNotEmpty()) {
            Glide.with(this)
                .load(userPhoto)
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivUserAvatar)

            Glide.with(this)
                .load(userPhoto)
                .placeholder(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivIncomingAvatar)
        }

        // Afficher le nom de l'utilisateur
        binding.tvUserName.text = userName
        binding.tvIncomingName.text = userName

        // Afficher le type d'appel
        if (callType == CallType.VIDEO) {
            binding.btnToggleCamera.visibility = View.VISIBLE
            binding.tvIncomingCallType.text = getString(R.string.incoming_video_call)
        } else {
            binding.btnToggleCamera.visibility = View.GONE
            binding.tvIncomingCallType.text = getString(R.string.incoming_voice_call)
        }
    }

    private fun setupCallButtons() {
        // Bouton Micro (muet/démuté)
        binding.btnMuteMic.setOnClickListener {
            isMicMuted = !isMicMuted
            binding.btnMuteMic.setImageResource(
                if (isMicMuted) R.drawable.ic_mic_off else R.drawable.ic_mic
            )
            updateMicrophoneState(isMicMuted)
        }

        // Bouton Haut-parleur
        binding.btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            binding.btnSpeaker.setImageResource(
                if (isSpeakerOn) R.drawable.ic_speaker else R.drawable.ic_speaker
            )
            updateSpeakerState(isSpeakerOn)
        }

        // Bouton Caméra (vidéo)
        binding.btnToggleCamera.setOnClickListener {
            isCameraOn = !isCameraOn
            binding.btnToggleCamera.setImageResource(
                if (isCameraOn) R.drawable.ic_camera else R.drawable.ic_camera_off
            )
            updateCameraState(isCameraOn)
        }

        // Bouton Quitter l'appel
        binding.btnEndCall.setOnClickListener {
            endCall()
        }

        // Appel entrant - Refuser
        binding.btnDeclineCall.setOnClickListener {
            declineCall()
        }

        // Appel entrant - Accepter
        binding.btnAcceptCall.setOnClickListener {
            acceptCall()
        }
    }

    private fun showIncomingCallUI() {
        binding.incomingCallLayout.visibility = View.VISIBLE
        binding.tvCallStatus.text = getString(R.string.incoming_call)
        binding.ivUserAvatar.visibility = View.GONE
        binding.chronometer.visibility = View.GONE
        binding.callControlsLayout.visibility = View.GONE
    }

    private fun showOutgoingCallUI() {
        binding.incomingCallLayout.visibility = View.GONE
        binding.ivUserAvatar.visibility = View.VISIBLE
        binding.tvCallStatus.text = getString(R.string.calling)
        // Afficher les contrôles pour permettre de raccrocher
        binding.callControlsLayout.visibility = View.VISIBLE

        // Simuler que l'appel est connecté après 3 secondes (pour démo)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            if (!isFinishing && !isCallActive) {
                // Simuler la connexion de l'appel
                isCallActive = true
                binding.tvCallStatus.text = getString(R.string.call_connected)
                binding.chronometer.visibility = View.VISIBLE
                binding.chronometer.base = SystemClock.elapsedRealtime()
                binding.chronometer.start()
            }
        }
        
        // Timeout après 30 secondes si l'appel n'est pas actif
        lifecycleScope.launch {
            kotlinx.coroutines.delay(30000)
            if (!isCallActive && !isFinishing) {
                Toast.makeText(this@CallActivity, R.string.call_missed, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun acceptCall() {
        isCallActive = true
        binding.incomingCallLayout.visibility = View.GONE
        binding.ivUserAvatar.visibility = View.VISIBLE
        binding.callControlsLayout.visibility = View.VISIBLE
        binding.tvCallStatus.text = getString(R.string.call_connected)
        binding.chronometer.visibility = View.VISIBLE

        // Démarrer le chronomètre
        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()

        // Mettre à jour le statut de l'appel en base de données
        lifecycleScope.launch {
            try {
                callRepository.updateCallStatus(callId, CallStatus.CONNECTED)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating call status", e)
            }
        }
    }

    private fun declineCall() {
        lifecycleScope.launch {
            try {
                callRepository.updateCallStatus(callId, CallStatus.DECLINED)
                Toast.makeText(this@CallActivity, R.string.call_declined, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error declining call", e)
                Toast.makeText(this@CallActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun endCall() {
        binding.chronometer.stop()
        
        // Calculer la durée seulement si l'appel était actif
        val duration = if (isCallActive && binding.chronometer.base > 0) {
            (SystemClock.elapsedRealtime() - binding.chronometer.base) / 1000
        } else {
            0L
        }
        
        val finalStatus = if (isCallActive) CallStatus.ENDED else CallStatus.MISSED

        lifecycleScope.launch {
            try {
                if (callId.isNotEmpty()) {
                    callRepository.updateCallStatus(callId, finalStatus, duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
            }
        }
        
        Toast.makeText(this@CallActivity, R.string.call_ended, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateMicrophoneState(muted: Boolean) {
        // TODO: Implémenter l'activation/désactivation du microphone
        Log.d(TAG, "Microphone muted: $muted")
    }

    private fun updateSpeakerState(enabled: Boolean) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isSpeakerphoneOn = enabled
        }
        Log.d(TAG, "Speaker enabled: $enabled")
    }

    private fun updateCameraState(enabled: Boolean) {
        // TODO: Implémenter l'activation/désactivation de la caméra
        Log.d(TAG, "Camera enabled: $enabled")
    }

    override fun onResume() {
        super.onResume()
        // Garder l'écran allumé durant l'appel
        window.attributes.screenBrightness = 1.0f
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.chronometer.stop()
    }
}
