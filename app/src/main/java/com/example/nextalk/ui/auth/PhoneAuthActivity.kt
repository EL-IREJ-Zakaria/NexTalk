package com.example.nextalk.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.nextalk.R
import com.example.nextalk.databinding.ActivityPhoneAuthBinding
import com.example.nextalk.ui.main.MainActivity
import com.google.firebase.auth.PhoneAuthProvider

class PhoneAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSendCode.setOnClickListener {
            phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                // Ajouter le préfixe si nécessaire
                if (!phoneNumber.startsWith("+")) {
                    phoneNumber = "+212$phoneNumber" // Maroc par défaut, changez selon votre pays
                }
                viewModel.sendVerificationCode(phoneNumber, this)
            } else {
                Toast.makeText(this, "Entrez un numéro de téléphone", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            verificationId?.let { vId ->
                viewModel.verifyCode(vId, code)
            } ?: run {
                Toast.makeText(this, "Veuillez d'abord envoyer le code", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResendCode.setOnClickListener {
            resendToken?.let { token ->
                viewModel.resendCode(phoneNumber, this, token)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSendCode.isEnabled = !isLoading
            binding.btnVerify.isEnabled = !isLoading
        }

        viewModel.codeSent.observe(this) { pair ->
            pair?.let {
                verificationId = it.first
                resendToken = it.second

                // Afficher la section de vérification
                binding.layoutPhoneInput.visibility = View.GONE
                binding.layoutCodeInput.visibility = View.VISIBLE

                Toast.makeText(this, "Code envoyé !", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.phoneAuthResult.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this,
                        it.exceptionOrNull()?.message ?: getString(R.string.error_occurred),
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.clearResults()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
