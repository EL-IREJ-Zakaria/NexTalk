package com.example.nextalk.ui.users

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.ChatRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityUsersBinding
import com.example.nextalk.ui.chat.ChatActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Écran pour sélectionner un utilisateur pour discuter
 * Interface moderne avec recherche, indicateurs en ligne, et design card-based
 */
class UsersActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "UsersActivity"
    }

    private lateinit var binding: ActivityUsersBinding
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var userRepository: UserRepository
    private lateinit var chatRepository: ChatRepository
    private val authRepository = AuthRepository()
    private var allUsers: List<com.example.nextalk.data.model.User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityUsersBinding.inflate(layoutInflater)
            setContentView(binding.root)

            initRepositories()
            setupToolbar()
            setupRecyclerView()
            setupSearch()
            loadUsers()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        userRepository = UserRepository(database.userDao())
        chatRepository = ChatRepository(database.conversationDao(), database.messageDao())
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter { user ->
            startChat(user)
        }

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@UsersActivity)
            adapter = usersAdapter
            itemAnimator?.changeDuration = 200
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Afficher le bouton "X" de nettoyage si du texte est présent
                binding.btnClearSearch.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                filterUsers(s.toString())
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        // Bouton pour effacer la recherche
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.requestFocus()
        }
    }

    private fun startChat(user: com.example.nextalk.data.model.User) {
        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: return@launch
                binding.progressBar.visibility = View.VISIBLE
                
                val conversationId = chatRepository.getOrCreateConversation(currentUserId, user.uid)

                binding.progressBar.visibility = View.GONE
                
                val intent = Intent(this@UsersActivity, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
                    putExtra(ChatActivity.EXTRA_OTHER_USER_ID, user.uid)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting conversation", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@UsersActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUsers() {
        val currentUserId = authRepository.getCurrentUserId()

        if (currentUserId == null) {
            Log.e(TAG, "No current user ID")
            showEmptyState()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                userRepository.getAllUsersExcept(currentUserId)
                    .catch { e ->
                        Log.e(TAG, "Error loading users", e)
                        binding.progressBar.visibility = View.GONE
                        showEmptyState()
                    }
                    .collectLatest { users ->
                        binding.progressBar.visibility = View.GONE
                        allUsers = users
                        
                        if (users.isEmpty()) {
                            showEmptyState()
                        } else {
                            showUsers()
                            usersAdapter.submitList(users)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadUsers", e)
                binding.progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        binding.tvNoUsers.visibility = View.VISIBLE
        binding.rvUsers.visibility = View.GONE
        binding.infoSection.visibility = View.GONE
    }

    private fun showUsers() {
        binding.tvNoUsers.visibility = View.GONE
        binding.rvUsers.visibility = View.VISIBLE
        binding.infoSection.visibility = View.VISIBLE
    }

    private fun filterUsers(query: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        if (query.isEmpty()) {
            if (allUsers.isEmpty()) {
                showEmptyState()
            } else {
                showUsers()
                usersAdapter.submitList(allUsers)
            }
            return
        }

        // Filtrage local pour une meilleure performance
        val filteredUsers = allUsers.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true)
        }

        if (filteredUsers.isEmpty()) {
            binding.tvNoUsers.visibility = View.VISIBLE
            binding.rvUsers.visibility = View.GONE
            binding.infoSection.visibility = View.GONE
        } else {
            showUsers()
            usersAdapter.submitList(filteredUsers)
        }
    }
}
