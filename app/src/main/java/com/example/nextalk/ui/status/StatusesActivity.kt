package com.example.nextalk.ui.status

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.model.StatusType
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.StatusRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityStatusesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatusesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatusesBinding
    private lateinit var statusRepository: StatusRepository
    private lateinit var userRepository: UserRepository
    private val authRepository = AuthRepository()

    private var selectedMediaType: StatusType? = null

    companion object {
        private const val TAG = "StatusesActivity"
    }

    // Image/Video picker launcher
    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleMediaSelected(it)
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (selectedMediaType) {
                StatusType.IMAGE -> launchImagePicker()
                StatusType.VIDEO -> launchVideoPicker()
                else -> {}
            }
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatusesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRepositories()
        setupToolbar()
        setupMyStatusCard()
        setupFab()
        setupRecyclerView()
        loadCurrentUserPhoto()
        observeStatuses()
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        statusRepository = StatusRepository(database.statusDao())
        userRepository = UserRepository(database.userDao())
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupMyStatusCard() {
        binding.cardMyStatus.setOnClickListener {
            showAddStatusBottomSheet()
        }
    }

    private fun setupFab() {
        binding.fabAddStatus.setOnClickListener {
            showAddStatusBottomSheet()
        }
    }

    private fun setupRecyclerView() {
        binding.rvStatuses.layoutManager = LinearLayoutManager(this)
        // TODO: Add StatusAdapter when implementing status list
    }

    private fun loadCurrentUserPhoto() {
        lifecycleScope.launch {
            try {
                authRepository.getCurrentUserId()?.let { userId ->
                    val user = userRepository.getUserById(userId)
                    user?.photoUrl?.let { photoUrl ->
                        if (photoUrl.isNotEmpty()) {
                            Glide.with(this@StatusesActivity)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .into(binding.ivMyPhoto)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user photo", e)
            }
        }
    }

    private fun observeStatuses() {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        lifecycleScope.launch {
            statusRepository.getRecentStatuses()
                .catch { e ->
                    Log.e(TAG, "Error loading statuses", e)
                }
                .collectLatest { statuses ->
                    val otherStatuses = statuses.filter { it.userId != currentUserId }
                    
                    if (otherStatuses.isEmpty()) {
                        binding.layoutNoStatuses.visibility = View.VISIBLE
                        binding.rvStatuses.visibility = View.GONE
                    } else {
                        binding.layoutNoStatuses.visibility = View.GONE
                        binding.rvStatuses.visibility = View.VISIBLE
                        // TODO: Update adapter with statuses
                    }
                }
        }
    }

    private fun showAddStatusBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.Theme_NexTalk_BottomSheet)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_status, null)
        bottomSheetDialog.setContentView(view)

        // Text status option
        view.findViewById<MaterialCardView>(R.id.cardTextStatus).setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(this, AddTextStatusActivity::class.java))
        }

        // Photo status option
        view.findViewById<MaterialCardView>(R.id.cardPhotoStatus).setOnClickListener {
            bottomSheetDialog.dismiss()
            selectedMediaType = StatusType.IMAGE
            checkPermissionAndPickMedia(StatusType.IMAGE)
        }

        // Video status option
        view.findViewById<MaterialCardView>(R.id.cardVideoStatus).setOnClickListener {
            bottomSheetDialog.dismiss()
            selectedMediaType = StatusType.VIDEO
            checkPermissionAndPickMedia(StatusType.VIDEO)
        }

        bottomSheetDialog.show()
    }

    private fun checkPermissionAndPickMedia(type: StatusType) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (type == StatusType.IMAGE) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_MEDIA_VIDEO
            }
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                when (type) {
                    StatusType.IMAGE -> launchImagePicker()
                    StatusType.VIDEO -> launchVideoPicker()
                    else -> {}
                }
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun launchImagePicker() {
        mediaPickerLauncher.launch("image/*")
    }

    private fun launchVideoPicker() {
        mediaPickerLauncher.launch("video/*")
    }

    private fun handleMediaSelected(uri: Uri) {
        val type = selectedMediaType ?: return
        
        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: return@launch
                val user = userRepository.getUserById(currentUserId)
                
                val result = statusRepository.createMediaStatus(
                    userId = currentUserId,
                    userName = user?.name ?: "Utilisateur",
                    userPhotoUrl = user?.photoUrl ?: "",
                    mediaUri = uri,
                    type = type
                )

                if (result.isSuccess) {
                    Toast.makeText(
                        this@StatusesActivity,
                        "Statut publié avec succès!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@StatusesActivity,
                        R.string.error_occurred,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating media status", e)
                Toast.makeText(this@StatusesActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
