package com.example.nextalk.ui.status

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nextalk.data.model.Status
import com.example.nextalk.data.model.StatusType
import com.example.nextalk.data.repository.StatusRepository
import com.example.nextalk.util.NetworkErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * État de l'interface des statuts
 */
data class StatusUiState(
    val isLoading: Boolean = false,
    val statuses: List<Status> = emptyList(),
    val userStatuses: Map<String, List<Status>> = emptyMap(),
    val currentStatus: Status? = null,
    val error: String? = null,
    val uploadProgress: Int = 0,
    val isUploading: Boolean = false,
    val totalStatuses: Int = 0,
    val hasMoreStatuses: Boolean = true
)

/**
 * ViewModel pour la gestion des statuts
 */
class StatusViewModel(private val statusRepository: StatusRepository) : ViewModel() {

    companion object {
        private const val TAG = "StatusViewModel"
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private var lastLoadedTimestamp: Long? = null

    /**
     * Créer un statut texte
     */
    fun createTextStatus(
        userId: String,
        userName: String,
        userPhotoUrl: String,
        text: String,
        backgroundColor: String = "#075E54",
        textColor: String = "#FFFFFF"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isUploading = true)
            
            val result = statusRepository.createTextStatus(
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                text = text,
                backgroundColor = backgroundColor,
                textColor = textColor
            )
            
            result.onSuccess { status ->
                Log.d(TAG, "Text status created successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUploading = false,
                    currentStatus = status,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error creating text status", e)
                handleError(e as? Exception ?: Exception(e.message))
            }
        }
    }

    /**
     * Créer un statut média (image ou vidéo)
     */
    fun createMediaStatus(
        userId: String,
        userName: String,
        userPhotoUrl: String,
        mediaUri: Uri,
        type: StatusType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                isUploading = true,
                uploadProgress = 0
            )
            
            val result = statusRepository.createMediaStatus(
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                mediaUri = mediaUri,
                type = type
            )
            
            result.onSuccess { status ->
                Log.d(TAG, "Media status created successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isUploading = false,
                    uploadProgress = 100,
                    currentStatus = status,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error creating media status", e)
                handleError(e as? Exception ?: Exception(e.message))
            }
        }
    }

    /**
     * Charger les statuts d'un utilisateur
     */
    fun loadStatusesForUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            statusRepository.getStatusesByUser(userId)
                .catch { e ->
                    Log.e(TAG, "Error loading user statuses", e)
                    handleError(e as? Exception ?: Exception(e.message))
                }
                .collectLatest { statuses ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statuses = statuses,
                        error = null
                    )
                }
        }
    }

    /**
     * Charger les statuts récents
     */
    fun loadRecentStatuses(limit: Int = 50) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            statusRepository.getRecentStatuses(limit)
                .catch { e ->
                    Log.e(TAG, "Error loading recent statuses", e)
                    handleError(e as? Exception ?: Exception(e.message))
                }
                .collectLatest { statuses ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statuses = statuses,
                        totalStatuses = statuses.size,
                        error = null
                    )
                }
        }
    }

    /**
     * Charger les statuts paginés
     */
    fun loadStatusesPaginated() {
        viewModelScope.launch {
            if (_uiState.value.isLoading || !_uiState.value.hasMoreStatuses) {
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = statusRepository.getStatusesPaginated(
                limit = PAGE_SIZE,
                lastTimestamp = lastLoadedTimestamp
            )
            
            result.onSuccess { newStatuses ->
                if (newStatuses.isNotEmpty()) {
                    lastLoadedTimestamp = newStatuses.last().createdAt
                    val allStatuses = _uiState.value.statuses + newStatuses
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statuses = allStatuses,
                        totalStatuses = allStatuses.size,
                        hasMoreStatuses = newStatuses.size >= PAGE_SIZE,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasMoreStatuses = false
                    )
                }
            }.onFailure { e ->
                Log.e(TAG, "Error loading paginated statuses", e)
                handleError(e as? Exception ?: Exception(e.message))
            }
        }
    }

    /**
     * Synchroniser les statuts avec Firebase
     */
    fun syncStatuses(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = statusRepository.syncStatusesFromFirebase(userId)
            
            result.onSuccess {
                Log.d(TAG, "Statuses synced successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error syncing statuses", e)
                handleError(e as? Exception ?: Exception(e.message))
            }
        }
    }

    /**
     * Marquer un statut comme vu
     */
    fun markStatusAsViewed(statusId: String, userId: String) {
        viewModelScope.launch {
            try {
                statusRepository.markStatusAsViewed(statusId, userId)
                Log.d(TAG, "Status marked as viewed")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking status as viewed", e)
            }
        }
    }

    /**
     * Répondre à un statut
     */
    fun replyToStatus(
        statusId: String,
        userId: String,
        userName: String,
        message: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = statusRepository.replyToStatus(
                statusId = statusId,
                userId = userId,
                userName = userName,
                message = message
            )
            
            result.onSuccess {
                Log.d(TAG, "Reply sent successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                Log.e(TAG, "Error replying to status", e)
                handleError(e as? Exception ?: Exception(e.message))
            }
        }
    }

    /**
     * Supprimer un statut avec son média
     */
    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            try {
                statusRepository.deleteStatusWithMedia(statusId)
                Log.d(TAG, "Status deleted successfully")
                
                // Mettre à jour la liste locale
                _uiState.value = _uiState.value.copy(
                    statuses = _uiState.value.statuses.filter { it.id != statusId }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting status", e)
                handleError(e)
            }
        }
    }

    /**
     * Supprimer les statuts expirés
     */
    fun deleteExpiredStatuses() {
        viewModelScope.launch {
            try {
                statusRepository.deleteExpiredStatuses()
                Log.d(TAG, "Expired statuses deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting expired statuses", e)
            }
        }
    }

    /**
     * Charger le nombre de statuts d'un utilisateur
     */
    fun loadStatusCount(userId: String) {
        viewModelScope.launch {
            statusRepository.getStatusCountByUser(userId)
                .catch { e ->
                    Log.e(TAG, "Error loading status count", e)
                }
                .collectLatest { count ->
                    _uiState.value = _uiState.value.copy(totalStatuses = count)
                }
        }
    }

    /**
     * Effacer l'erreur
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Réinitialiser l'état
     */
    fun resetState() {
        _uiState.value = StatusUiState()
        lastLoadedTimestamp = null
    }

    /**
     * Gérer les erreurs avec messages user-friendly
     */
    private fun handleError(exception: Exception) {
        val message = NetworkErrorHandler.getUserFriendlyMessage(exception)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isUploading = false,
            error = message
        )
    }

    /**
     * Factory pour créer le ViewModel avec le repository
     */
    class Factory(private val statusRepository: StatusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatusViewModel::class.java)) {
                return StatusViewModel(statusRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
