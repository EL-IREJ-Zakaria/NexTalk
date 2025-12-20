package com.example.nextalk.ui.call

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.CallStatus
import com.example.nextalk.data.model.CallType
import com.example.nextalk.data.repository.CallRepository
import com.example.nextalk.util.NetworkErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * État de l'interface d'appel
 */
data class CallUiState(
    val isLoading: Boolean = false,
    val calls: List<Call> = emptyList(),
    val currentCall: Call? = null,
    val callDuration: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = false,
    val isCallActive: Boolean = false,
    val error: String? = null,
    val callStats: CallStats = CallStats()
)

/**
 * Statistiques d'appels
 */
data class CallStats(
    val totalCalls: Int = 0,
    val totalDuration: Long = 0L,
    val missedCalls: Int = 0,
    val videoCalls: Int = 0
)

/**
 * ViewModel pour la gestion des appels
 */
class CallViewModel(private val callRepository: CallRepository) : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
    }

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    /**
     * Charger l'historique des appels pour un utilisateur
     */
    fun loadCallsForUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            callRepository.getCallsByUser(userId)
                .catch { e ->
                    Log.e(TAG, "Error loading calls", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collectLatest { calls ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calls = calls,
                        error = null
                    )
                }
        }
    }

    /**
     * Charger les appels récents
     */
    fun loadRecentCalls(limit: Int = 20) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            callRepository.getRecentCalls(limit)
                .catch { e ->
                    Log.e(TAG, "Error loading recent calls", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collectLatest { calls ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calls = calls,
                        error = null
                    )
                }
        }
    }

    /**
     * Charger les appels manqués
     */
    fun loadMissedCalls() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            callRepository.getMissedCalls()
                .catch { e ->
                    Log.e(TAG, "Error loading missed calls", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collectLatest { calls ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        calls = calls,
                        error = null
                    )
                }
        }
    }

    /**
     * Initier un appel
     */
    fun initiateCall(
        conversationId: String,
        callerId: String,
        callerName: String,
        callerPhotoUrl: String,
        receiverId: String,
        receiverName: String,
        receiverPhotoUrl: String,
        type: CallType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = callRepository.initiateCall(
                conversationId = conversationId,
                callerId = callerId,
                callerName = callerName,
                callerPhotoUrl = callerPhotoUrl,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverPhotoUrl = receiverPhotoUrl,
                type = type
            )
            
            result.onSuccess { call ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentCall = call,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error initiating call", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Accepter un appel entrant
     */
    fun acceptCall(callId: String) {
        viewModelScope.launch {
            try {
                callRepository.updateCallStatus(callId, CallStatus.CONNECTED)
                _uiState.value = _uiState.value.copy(
                    isCallActive = true,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting call", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Refuser un appel entrant
     */
    fun declineCall(callId: String) {
        viewModelScope.launch {
            try {
                callRepository.updateCallStatus(callId, CallStatus.DECLINED)
                _uiState.value = _uiState.value.copy(
                    currentCall = null,
                    isCallActive = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error declining call", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Terminer un appel
     */
    fun endCall(callId: String, duration: Long = 0L) {
        viewModelScope.launch {
            try {
                callRepository.updateCallStatus(callId, CallStatus.ENDED, duration)
                _uiState.value = _uiState.value.copy(
                    currentCall = null,
                    isCallActive = false,
                    callDuration = 0L,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Basculer le microphone (muet/démuté)
     */
    fun toggleMute() {
        _uiState.value = _uiState.value.copy(
            isMuted = !_uiState.value.isMuted
        )
    }

    /**
     * Basculer le haut-parleur
     */
    fun toggleSpeaker() {
        _uiState.value = _uiState.value.copy(
            isSpeakerOn = !_uiState.value.isSpeakerOn
        )
    }

    /**
     * Basculer la caméra (appel vidéo)
     */
    fun toggleCamera() {
        _uiState.value = _uiState.value.copy(
            isCameraOn = !_uiState.value.isCameraOn
        )
    }

    /**
     * Activer la vidéo durant un appel vocal
     */
    fun enableVideoInCall(callId: String) {
        viewModelScope.launch {
            try {
                callRepository.enableVideoInCall(callId)
                _uiState.value = _uiState.value.copy(isCameraOn = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error enabling video", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Supprimer un appel de l'historique
     */
    fun deleteCall(callId: String) {
        viewModelScope.launch {
            try {
                callRepository.deleteCall(callId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting call", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Charger les statistiques d'appels
     */
    fun loadCallStats(userId: String) {
        viewModelScope.launch {
            try {
                val stats = callRepository.getCallStats(userId)
                _uiState.value = _uiState.value.copy(
                    callStats = CallStats(
                        totalCalls = stats["totalCalls"] as? Int ?: 0,
                        totalDuration = stats["totalDuration"] as? Long ?: 0L,
                        missedCalls = stats["missedCalls"] as? Int ?: 0,
                        videoCalls = stats["videoCalls"] as? Int ?: 0
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading call stats", e)
            }
        }
    }

    /**
     * Mettre à jour la durée d'appel
     */
    fun updateCallDuration(duration: Long) {
        _uiState.value = _uiState.value.copy(callDuration = duration)
    }

    /**
     * Effacer l'erreur
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Synchroniser les appels avec Firebase
     */
    fun syncCalls(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = callRepository.syncCallsFromFirebase(userId)
            
            result.onSuccess {
                Log.d(TAG, "Calls synced successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error syncing calls", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = NetworkErrorHandler.getUserFriendlyMessage(e as? Exception ?: Exception(e.message))
                )
            }
        }
    }

    /**
     * Nettoyer les appels anciens
     */
    fun cleanOldCalls() {
        viewModelScope.launch {
            try {
                callRepository.cleanOldCalls()
                Log.d(TAG, "Old calls cleaned")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning old calls", e)
            }
        }
    }

    /**
     * Marquer les appels manqués comme vus
     */
    fun markMissedCallsAsSeen(userId: String) {
        viewModelScope.launch {
            try {
                callRepository.markMissedCallsAsSeen(userId)
                Log.d(TAG, "Missed calls marked as seen")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking missed calls as seen", e)
            }
        }
    }

    /**
     * Obtenir le nombre d'appels manqués
     */
    fun loadMissedCallsCount(userId: String) {
        viewModelScope.launch {
            try {
                val count = callRepository.getMissedCallsCount(userId)
                _uiState.value = _uiState.value.copy(
                    callStats = _uiState.value.callStats.copy(missedCalls = count)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading missed calls count", e)
            }
        }
    }

    /**
     * Supprimer tous les appels d'une conversation
     */
    fun deleteConversationCalls(conversationId: String) {
        viewModelScope.launch {
            try {
                callRepository.deleteCallsByConversation(conversationId)
                Log.d(TAG, "Conversation calls deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting conversation calls", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Gérer les erreurs avec messages user-friendly
     */
    private fun handleError(exception: Exception) {
        val message = NetworkErrorHandler.getUserFriendlyMessage(exception)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = message
        )
    }

    /**
     * Factory pour créer le ViewModel avec le repository
     */
    class Factory(private val callRepository: CallRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
                return CallViewModel(callRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
