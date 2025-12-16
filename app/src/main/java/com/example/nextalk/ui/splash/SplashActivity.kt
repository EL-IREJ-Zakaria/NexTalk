package com.example.nextalk.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.nextalk.data.preferences.PreferencesManager
import com.example.nextalk.databinding.ActivitySplashBinding
import com.example.nextalk.ui.auth.LoginActivity
import com.example.nextalk.ui.main.MainActivity
import com.example.nextalk.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferencesManager: PreferencesManager
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        lifecycleScope.launch {
            delay(2000) // Afficher le splash pendant 2 secondes
            navigateToNextScreen()
        }
    }

    private suspend fun navigateToNextScreen() {
        val onboardingCompleted = preferencesManager.onboardingCompleted.first()
        val currentUser = auth.currentUser

        val intent = when {
            !onboardingCompleted -> Intent(this, OnboardingActivity::class.java)
            currentUser != null -> Intent(this, MainActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
