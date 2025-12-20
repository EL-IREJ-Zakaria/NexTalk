package com.example.nextalk.util

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Gestionnaire d'erreurs réseau avec retry logic
 */
object NetworkErrorHandler {

    private const val TAG = "NetworkErrorHandler"
    private const val DEFAULT_MAX_RETRIES = 3
    private const val DEFAULT_INITIAL_DELAY_MS = 1000L
    private const val DEFAULT_MAX_DELAY_MS = 10000L
    private const val DELAY_FACTOR = 2.0

    /**
     * Exécute une opération avec retry automatique en cas d'erreur réseau
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
        maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        factor: Double = DELAY_FACTOR,
        operation: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Attempt ${attempt + 1} of $maxRetries")
                return Result.success(operation())
            } catch (e: Exception) {
                lastException = e
                
                if (!isRetryableException(e)) {
                    Log.e(TAG, "Non-retryable exception encountered", e)
                    return Result.failure(e)
                }

                if (attempt < maxRetries - 1) {
                    Log.w(TAG, "Retryable exception on attempt ${attempt + 1}, retrying in ${currentDelay}ms", e)
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                } else {
                    Log.e(TAG, "Max retries reached, operation failed", e)
                }
            }
        }

        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries retries"))
    }

    /**
     * Détermine si une exception mérite un retry
     */
    private fun isRetryableException(exception: Exception): Boolean {
        return when (exception) {
            is IOException -> true
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            is FirebaseNetworkException -> true
            else -> false
        }
    }

    /**
     * Obtenir un message d'erreur user-friendly
     */
    fun getUserFriendlyMessage(exception: Exception): String {
        return when (exception) {
            is UnknownHostException -> "Pas de connexion Internet. Vérifiez votre réseau."
            is SocketTimeoutException -> "La connexion a expiré. Veuillez réessayer."
            is FirebaseNetworkException -> "Erreur de connexion au serveur. Vérifiez votre Internet."
            is IOException -> "Erreur de connexion. Vérifiez votre réseau."
            else -> exception.message ?: "Une erreur inattendue s'est produite."
        }
    }

    /**
     * Vérifie si l'erreur est due au réseau
     */
    fun isNetworkError(exception: Exception): Boolean {
        return isRetryableException(exception)
    }

    /**
     * Exécute une opération avec timeout
     */
    suspend fun <T> executeWithTimeout(
        timeoutMs: Long = 30000L,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                Result.success(operation())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Operation timed out or failed", e)
            Result.failure(e)
        }
    }
}

/**
 * Extension pour exécuter facilement avec retry
 */
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    operation: suspend () -> T
): Result<T> = NetworkErrorHandler.executeWithRetry(
    maxRetries = maxRetries,
    operation = operation
)

/**
 * Extension pour exécuter avec timeout
 */
suspend fun <T> withTimeout(
    timeoutMs: Long = 30000L,
    operation: suspend () -> T
): Result<T> = NetworkErrorHandler.executeWithTimeout(
    timeoutMs = timeoutMs,
    operation = operation
)
