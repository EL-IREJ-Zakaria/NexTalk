package com.example.nextalk.util

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Extension pour wrapper les opérations de repository avec gestion d'erreurs centralisée
 */
inline fun <T> safeCall(
    tag: String,
    crossinline block: suspend () -> T
): suspend () -> Result<T> = {
    try {
        Result.success(block())
    } catch (e: Exception) {
        Log.e(tag, "Error in safeCall", e)
        Result.failure(e)
    }
}

/**
 * Extension Flow pour gérer les erreurs et logging centralisé
 */
fun <T> Flow<T>.handleErrors(tag: String, fallbackValue: T): Flow<T> =
    this.catch { e ->
        Log.e(tag, "Flow error", e)
        emit(fallbackValue)
    }

/**
 * Extension Result pour traiter les success/failure de façon fluide
 */
inline fun <T, R> Result<T>.mapResult(transform: (T) -> R): Result<R> =
    try {
        when {
            isSuccess -> Result.success(transform(getOrNull()!!))
            else -> Result.failure(exceptionOrNull()!!)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

/**
 * Extension pour récupérer le message d'erreur ou valeur par défaut
 */
fun <T> Result<T>.getErrorMessage(): String =
    exceptionOrNull()?.message ?: "Unknown error occurred"

/**
 * Extension pour logging automatique des résultats
 */
inline fun <T> Result<T>.logResult(tag: String, operation: String): Result<T> {
    if (isSuccess) {
        Log.d(tag, "$operation succeeded")
    } else {
        Log.e(tag, "$operation failed: ${getErrorMessage()}")
    }
    return this
}
