package com.example.nextalk.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.model.User
import com.example.nextalk.data.preferences.PreferencesManager
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityProfileBinding
import com.example.nextalk.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var userRepository: UserRepository
    private lateinit var preferencesManager: PreferencesManager
    private val authRepository = AuthRepository()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var currentUser: User? = null

    // Sélection d'image depuis la galerie
    private val pickImageFromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            loadImagePreview(it)
        }
    }

    // Prise de photo avec la caméra
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                selectedImageUri = it
                loadImagePreview(it)
            }
        }
    }

    // Permission caméra
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRepositories()
        setupToolbar()
        setupViews()
        loadUserData()
        loadPreferences()
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        userRepository = UserRepository(database.userDao())
        preferencesManager = PreferencesManager(this)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        // Changement de photo de profil
        binding.ivAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        // Sauvegarde du profil
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        // Changement de numéro de téléphone
        binding.layoutChangePhone.setOnClickListener {
            showChangePhoneDialog()
        }

        // Mode sombre
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        // Notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setNotificationsEnabled(isChecked)
            }
        }

        // Déconnexion
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Supprimer le compte
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.change_photo)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> pickImageFromGallery.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val imageFile = createImageFile()
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        takePhoto.launch(uri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun loadImagePreview(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.ivAvatar)
    }

    private fun loadUserData() {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        lifecycleScope.launch {
            userRepository.observeUser(currentUserId).collectLatest { user ->
                user?.let {
                    currentUser = it
                    binding.etName.setText(it.name)
                    binding.tvEmail.text = it.email.ifEmpty { getString(R.string.not_set) }

                    // Afficher le numéro de téléphone si disponible
                    val phoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber
                    binding.tvPhoneNumber.text = phoneNumber ?: getString(R.string.not_set)

                    if (it.photoUrl.isNotEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(it.photoUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(binding.ivAvatar)
                    }
                }
            }
        }
    }

    private fun loadPreferences() {
        lifecycleScope.launch {
            preferencesManager.darkModeEnabled.collectLatest { enabled ->
                binding.switchDarkMode.isChecked = enabled
            }
        }

        lifecycleScope.launch {
            preferencesManager.notificationsEnabled.collectLatest { enabled ->
                binding.switchNotifications.isChecked = enabled
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.name_required)
            return
        }
        binding.tilName.error = null

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                var photoUrl = currentUser?.photoUrl ?: ""

                // Upload de la nouvelle photo si sélectionnée
                selectedImageUri?.let { uri ->
                    val imageRef =
                        storage.reference.child("profile_images/${UUID.randomUUID()}.jpg")
                    imageRef.putFile(uri).await()
                    photoUrl = imageRef.downloadUrl.await().toString()
                }

                // Mettre à jour dans Firestore
                val userId = authRepository.getCurrentUserId() ?: throw Exception("Non connecté")
                val updates = mutableMapOf<String, Any>(
                    "name" to name
                )
                if (photoUrl.isNotEmpty()) {
                    updates["photoUrl"] = photoUrl
                }

                firestore.collection("users").document(userId).update(updates).await()

                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true

                Toast.makeText(this@ProfileActivity, R.string.profile_updated, Toast.LENGTH_SHORT)
                    .show()
                selectedImageUri = null

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(
                    this@ProfileActivity,
                    e.message ?: getString(R.string.error_occurred),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showChangePhoneDialog() {
        val intent = Intent(this, ChangePhoneActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            authRepository.logout()
            preferencesManager.clearAll()

            startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun deleteAccount() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: throw Exception("Non connecté")

                // Supprimer les données utilisateur de Firestore
                firestore.collection("users").document(userId).delete().await()

                // Supprimer le compte Firebase Auth
                FirebaseAuth.getInstance().currentUser?.delete()?.await()

                preferencesManager.clearAll()

                Toast.makeText(this@ProfileActivity, R.string.account_deleted, Toast.LENGTH_SHORT)
                    .show()

                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ProfileActivity,
                    e.message ?: getString(R.string.error_occurred),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
