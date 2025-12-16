package com.example.nextalk.ui.auth

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextalk.data.model.User
import com.example.nextalk.data.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.Calendar

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<FirebaseUser>?>()
    val loginResult: LiveData<Result<FirebaseUser>?> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>?>()
    val registerResult: LiveData<Result<User>?> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Result<Unit>?>()
    val resetPasswordResult: LiveData<Result<Unit>?> = _resetPasswordResult

    private val _googleSignInResult = MutableLiveData<Result<FirebaseUser>?>()
    val googleSignInResult: LiveData<Result<FirebaseUser>?> = _googleSignInResult

    private val _phoneAuthResult = MutableLiveData<Result<FirebaseUser>?>()
    val phoneAuthResult: LiveData<Result<FirebaseUser>?> = _phoneAuthResult

    private val _codeSent = MutableLiveData<Pair<String, PhoneAuthProvider.ForceResendingToken>?>()
    val codeSent: LiveData<Pair<String, PhoneAuthProvider.ForceResendingToken>?> = _codeSent

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    // ==================== EMAIL/PASSWORD AUTH ====================

    fun login(email: String, password: String) {
        if (!validateEmail(email)) {
            _errorMessage.value = "Email invalide"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Le mot de passe doit contenir au moins 6 caractères"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = authRepository.login(email, password)
            _isLoading.value = false
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        birthDate: Long
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Le nom est requis"
            return
        }
        if (!validateEmail(email)) {
            _errorMessage.value = "Email invalide"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Le mot de passe doit contenir au moins 6 caractères"
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Les mots de passe ne correspondent pas"
            return
        }
        if (!isAdult(birthDate)) {
            _errorMessage.value = "Vous devez avoir au moins 18 ans"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _registerResult.value = authRepository.register(email, password, name, birthDate)
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String) {
        if (!validateEmail(email)) {
            _errorMessage.value = "Email invalide"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _resetPasswordResult.value = authRepository.resetPassword(email)
            _isLoading.value = false
        }
    }

    // ==================== GOOGLE AUTH ====================

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _googleSignInResult.value = authRepository.signInWithGoogle(idToken)
            _isLoading.value = false
        }
    }

    // ==================== PHONE AUTH ====================

    fun sendVerificationCode(phoneNumber: String, activity: Activity) {
        if (phoneNumber.isBlank()) {
            _errorMessage.value = "Numéro de téléphone requis"
            return
        }

        _isLoading.value = true

        authRepository.sendVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,
            callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-vérification (SMS Retriever API)
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "Erreur d'envoi du code"
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    _isLoading.value = false
                    _codeSent.value = Pair(verificationId, token)
                }
            }
        )
    }

    fun verifyCode(verificationId: String, code: String) {
        if (code.length != 6) {
            _errorMessage.value = "Le code doit contenir 6 chiffres"
            return
        }

        val credential = authRepository.getPhoneCredential(verificationId, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _isLoading.value = true
            _phoneAuthResult.value = authRepository.signInWithPhoneCredential(credential)
            _isLoading.value = false
        }
    }

    fun resendCode(
        phoneNumber: String,
        activity: Activity,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        _isLoading.value = true

        authRepository.resendVerificationCode(
            phoneNumber = phoneNumber,
            activity = activity,
            token = token,
            callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "Erreur de renvoi du code"
                }

                override fun onCodeSent(
                    verificationId: String,
                    newToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    _isLoading.value = false
                    _codeSent.value = Pair(verificationId, newToken)
                }
            }
        )
    }

    // ==================== HELPERS ====================

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearResults() {
        _loginResult.value = null
        _registerResult.value = null
        _resetPasswordResult.value = null
        _googleSignInResult.value = null
        _phoneAuthResult.value = null
        _codeSent.value = null
    }

    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isAdult(birthDate: Long): Boolean {
        val birthCalendar = Calendar.getInstance().apply { timeInMillis = birthDate }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age >= 18
    }
}
