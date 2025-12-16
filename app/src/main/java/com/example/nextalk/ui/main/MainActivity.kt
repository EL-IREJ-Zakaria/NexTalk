package com.example.nextalk.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.preferences.PreferencesManager
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.ChatRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityMainBinding
import com.example.nextalk.ui.auth.LoginActivity
import com.example.nextalk.ui.chat.ChatActivity
import com.example.nextalk.ui.profile.ProfileActivity
import com.example.nextalk.ui.users.UsersActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var userRepository: UserRepository
    private val authRepository = AuthRepository()
    private lateinit var preferencesManager: PreferencesManager

    companion object {
        private const val TAG = "MainActivity"
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
