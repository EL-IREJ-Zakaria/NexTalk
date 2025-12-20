package com.example.nextalk.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.preferences.PreferencesManager
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.ChatRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.data.repository.CallRepository
import com.example.nextalk.databinding.ActivityMainBinding
import com.example.nextalk.ui.auth.LoginActivity
import com.example.nextalk.ui.call.CallsHistoryActivity
import com.example.nextalk.ui.chat.ChatActivity
import com.example.nextalk.ui.profile.ProfileActivity
import com.example.nextalk.ui.users.UsersActivity
import com.google.android.material.badge.BadgeDrawable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var userRepository: UserRepository
    private lateinit var callRepository: CallRepository
    private val authRepository = AuthRepository()
    private lateinit var preferencesManager: PreferencesManager
    
    // Nouvelles fonctionnalités
    private var searchJob: Job? = null
    private var allConversations: List<com.example.nextalk.data.model.Conversation> = emptyList()
    private var unreadCount = 0
    private var missedCallsCount = 0

    companion object {
        private const val TAG = "MainActivity"
        private const val SEARCH_DELAY_MS = 300L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Vérifier si l'utilisateur est connecté
            if (!authRepository.isLoggedIn) {
                navigateToLogin()
                return
            }

            initRepositories()
            setupToolbar()
            setupBottomNavigation()
            setupRecyclerView()
            setupFab()
            observeConversations()
            updateOnlineStatus(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // En cas d'erreur, essayer de rediriger vers le login
            navigateToLogin()
        }
    }

    private fun initRepositories() {
        try {
            val database = NexTalkApplication.instance.database
            chatRepository = ChatRepository(database.conversationDao(), database.messageDao())
            userRepository = UserRepository(database.userDao())
            preferencesManager = PreferencesManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing repositories", e)
            throw e
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_conversations -> {
                    // Déjà sur l'écran des conversations
                    binding.rvConversations.visibility = View.VISIBLE
                    binding.fabNewChat.visibility = View.VISIBLE
                    binding.toolbar.title = getString(R.string.app_name)
                    true
                }
                R.id.nav_calls -> {
                    // Naviguer vers l'historique des appels
                    startActivity(Intent(this, CallsHistoryActivity::class.java))
                    // Garder l'onglet conversations sélectionné pour éviter la confusion
                    binding.bottomNavigation.selectedItemId = R.id.nav_conversations
                    true
                }
                R.id.nav_statuses -> {
                    // Naviguer vers les statuts (si implémenté)
                    try {
                        val statusIntent = Intent(this, Class.forName("com.example.nextalk.ui.status.StatusesActivity"))
                        startActivity(statusIntent)
                    } catch (e: ClassNotFoundException) {
                        Log.d(TAG, "StatusesActivity not found")
                    }
                    // Garder l'onglet conversations sélectionné
                    binding.bottomNavigation.selectedItemId = R.id.nav_conversations
                    true
                }
                else -> false
            }
        }
        
        // Sélectionner l'onglet conversations par défaut
        binding.bottomNavigation.selectedItemId = R.id.nav_conversations
    }

    private fun setupRecyclerView() {
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        conversationAdapter = ConversationAdapter(
            currentUserId = currentUserId,
            onConversationClick = { conversation ->
                try {
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id)
                        putExtra(
                            ChatActivity.EXTRA_OTHER_USER_ID,
                            conversation.getOtherUserId(currentUserId)
                        )
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to chat", e)
                }
            }
        )

        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    private fun setupFab() {
        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(this, UsersActivity::class.java))
        }
    }

    private fun observeConversations() {
        val currentUserId = authRepository.getCurrentUserId()

        if (currentUserId == null) {
            Log.e(TAG, "No current user ID")
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            try {
                chatRepository.getConversationsForUser(currentUserId)
                    .catch { e ->
                        Log.e(TAG, "Error in conversations flow", e)
                        showEmptyState()
                    }
                    .collectLatest { conversations ->
                        if (conversations.isEmpty()) {
                            showEmptyState()
                        } else {
                            showConversations()

                            // Charger les infos utilisateurs pour chaque conversation
                            conversations.forEach { conversation ->
                                val otherUserId = conversation.getOtherUserId(currentUserId)
                                launch {
                                    try {
                                        val user = userRepository.getUserById(otherUserId)
                                        user?.let {
                                            conversationAdapter.updateUserInfo(
                                                otherUserId,
                                                it
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error loading user info for $otherUserId", e)
                                    }
                                }
                            }

                            conversationAdapter.submitList(conversations)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing conversations", e)
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        binding.tvNoConversations.visibility = View.VISIBLE
        binding.rvConversations.visibility = View.GONE
    }

    private fun showConversations() {
        binding.tvNoConversations.visibility = View.GONE
        binding.rvConversations.visibility = View.VISIBLE
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        lifecycleScope.launch {
            try {
                authRepository.getCurrentUserId()?.let { userId ->
                    userRepository.updateOnlineStatus(userId, isOnline)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating online status", e)
            }
        }
    }

    private fun navigateToLogin() {
        try {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (authRepository.isLoggedIn) {
            updateOnlineStatus(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (authRepository.isLoggedIn) {
            updateOnlineStatus(false)
        }
    }
}
