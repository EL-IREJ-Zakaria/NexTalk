package com.example.nextalk.ui.call

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.model.CallType
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.CallRepository
import com.example.nextalk.databinding.ActivityCallsHistoryBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Écran d'historique des appels
 * Affiche tous les appels avec filtrage par type et statut
 */
class CallsHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallsHistoryBinding
    private lateinit var callRepository: CallRepository
    private lateinit var callAdapter: CallsAdapter
    private val authRepository = AuthRepository()

    companion object {
        private const val TAG = "CallsHistoryActivity"
    }

    private var currentFilter = CallFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityCallsHistoryBinding.inflate(layoutInflater)
            setContentView(binding.root)

            initRepositories()
            setupToolbar()
            setupRecyclerView()
            setupTabs()
            loadCalls()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        callRepository = CallRepository(database.callDao())
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        callAdapter = CallsAdapter(
            currentUserId = currentUserId,
            onCallClick = { call ->
                // Voir les détails de l'appel
                showCallDetails(call)
            },
            onCallAgainClick = { call ->
                // Rappeler
                // TODO: Implémenter le rappel
                Toast.makeText(this, "Rappel à venir", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { call ->
                // Supprimer l'appel
                deleteCall(call.id)
            }
        )

        binding.rvCalls.apply {
            layoutManager = LinearLayoutManager(this@CallsHistoryActivity)
            adapter = callAdapter
            itemAnimator?.changeDuration = 200
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                currentFilter = when (tab.position) {
                    0 -> CallFilter.ALL
                    1 -> CallFilter.VOICE
                    2 -> CallFilter.VIDEO
                    3 -> CallFilter.MISSED
                    else -> CallFilter.ALL
                }
                loadCalls()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun loadCalls() {
        val currentUserId = authRepository.getCurrentUserId()

        if (currentUserId == null) {
            Log.e(TAG, "No current user ID")
            showEmptyState()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                callRepository.getCallsByUser(currentUserId)
                    .catch { e ->
                        Log.e(TAG, "Error loading calls", e)
                        binding.progressBar.visibility = View.GONE
                        showEmptyState()
                    }
                    .collectLatest { calls ->
                        binding.progressBar.visibility = View.GONE

                        val filteredCalls = when (currentFilter) {
                            CallFilter.ALL -> calls
                            CallFilter.VOICE -> calls.filter { it.type == CallType.VOICE }
                            CallFilter.VIDEO -> calls.filter { it.type == CallType.VIDEO }
                            CallFilter.MISSED -> calls.filter { it.status.name == "MISSED" }
                        }

                        if (filteredCalls.isEmpty()) {
                            showEmptyState()
                        } else {
                            showCalls()
                            callAdapter.submitList(filteredCalls)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadCalls", e)
                binding.progressBar.visibility = View.GONE
                showEmptyState()
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.rvCalls.visibility = View.GONE
    }

    private fun showCalls() {
        binding.emptyState.visibility = View.GONE
        binding.rvCalls.visibility = View.VISIBLE
    }

    private fun showCallDetails(call: com.example.nextalk.data.model.Call) {
        // Créer un Intent pour afficher les détails de l'appel
        val details = StringBuilder()
        details.append("Appel ${call.type.name}\n")
        details.append("Statut: ${call.status.name}\n")
        details.append("Durée: ${call.getFormattedDuration()}\n")
        details.append("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(call.timestamp))}\n")

        Toast.makeText(this, details.toString(), Toast.LENGTH_LONG).show()
    }

    private fun deleteCall(callId: String) {
        lifecycleScope.launch {
            try {
                callRepository.deleteCall(callId)
                Toast.makeText(this@CallsHistoryActivity, "Appel supprimé", Toast.LENGTH_SHORT).show()
                loadCalls()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting call", e)
                Toast.makeText(this@CallsHistoryActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Enum pour les filtres d'appel
 */
enum class CallFilter {
    ALL,     // Tous les appels
    VOICE,   // Appels vocaux
    VIDEO,   // Appels vidéo
    MISSED   // Appels manqués
}
