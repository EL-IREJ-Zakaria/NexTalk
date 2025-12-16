package com.example.nextalk.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.nextalk.R
import com.example.nextalk.databinding.ActivityChangePhoneBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ChangePhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePhoneBinding
    private val viewModel: ChangePhoneViewModel by viewModels()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnSendCode.setOnClickListener {
            phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                if (!phoneNumber.startsWith("+")) {
                    phoneNumber = "+212$phoneNumber" // Maroc par défaut
                }
                sendVerificationCode()
            } else {
                Toast.makeText(this, R.string.phone_number_required, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.length == 6) {
                verificationId?.let { vId ->
                    viewModel.updatePhoneNumber(vId, code)
                }
            } else {
                Toast.makeText(this, R.string.invalid_code, Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendCode.setOnClickListener {
            resendToken?.let { token ->
                resendVerificationCode(token)
            }
        }
    }

    private fun sendVerificationCode() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendCode.isEnabled = false

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneAuthCallbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun resendVerificationCode(token: PhoneAuthProvider.ForceResendingToken) {
        binding.progressBar.visibility = View.VISIBLE

        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(phoneAuthCallbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val phoneAuthCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModel.linkPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                binding.progressBar.visibility = View.GONE
                binding.btnSendCode.isEnabled = true
                Toast.makeText(this@ChangePhoneActivity, e.message, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                vId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnSendCode.isEnabled = true

                verificationId = vId
                resendToken = token

                // Afficher la section de vérification
                binding.layoutPhoneInput.visibility = View.GONE
                binding.layoutCodeInput.visibility = View.VISIBLE

                Toast.makeText(this@ChangePhoneActivity, R.string.code_sent, Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnVerify.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(this, R.string.phone_updated, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        it.exceptionOrNull()?.message ?: getString(R.string.error_occurred),
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.clearResult()
            }
        }
    }
}

class ChangePhoneViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateResult = MutableLiveData<Result<Unit>?>()
    val updateResult: LiveData<Result<Unit>?> = _updateResult

    fun updatePhoneNumber(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        linkPhoneCredential(credential)
    }

    fun linkPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = auth.currentUser ?: throw Exception("Non connecté")

                // Mettre à jour le numéro de téléphone
                user.updatePhoneNumber(credential).await()

                _updateResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            }
            _isLoading.value = false
        }
    }

    fun clearResult() {
        _updateResult.value = null
    }
}
