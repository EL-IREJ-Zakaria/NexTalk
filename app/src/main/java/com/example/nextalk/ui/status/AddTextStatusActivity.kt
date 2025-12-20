package com.example.nextalk.ui.status

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.StatusRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityAddTextStatusBinding
import kotlinx.coroutines.launch

class AddTextStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTextStatusBinding
    private lateinit var statusRepository: StatusRepository
    private lateinit var userRepository: UserRepository
    private val authRepository = AuthRepository()

    private var selectedBackgroundColor = "#075E54"
    private var selectedTextColor = "#FFFFFF"

    companion object {
        private const val TAG = "AddTextStatusActivity"
        private const val MAX_LENGTH = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTextStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRepositories()
        setupUI()
        setupColorPicker()
        setupTextInput()
        setupButtons()
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        statusRepository = StatusRepository(database.statusDao())
        userRepository = UserRepository(database.userDao())
    }

    private fun setupUI() {
        // Set initial background color
        binding.rootLayout.setBackgroundColor(Color.parseColor(selectedBackgroundColor))
        updateCharCount(0)
    }

    private fun setupColorPicker() {
        val colorViews = listOf(
            binding.colorContainer.findViewById<View>(R.id.colorGreen),
            binding.colorContainer.findViewById<View>(R.id.colorBlue),
            binding.colorContainer.findViewById<View>(R.id.colorPurple),
            binding.colorContainer.findViewById<View>(R.id.colorOrange),
            binding.colorContainer.findViewById<View>(R.id.colorRed),
            binding.colorContainer.findViewById<View>(R.id.colorPink),
            binding.colorContainer.findViewById<View>(R.id.colorTeal),
            binding.colorContainer.findViewById<View>(R.id.colorIndigo)
        )

        colorViews.forEach { colorView ->
            colorView.setOnClickListener {
                val colorHex = it.tag as? String ?: return@setOnClickListener
                selectColor(colorHex)
                
                // Visual feedback - add border to selected color
                colorViews.forEach { view ->
                    view.alpha = if (view == it) 1f else 0.6f
                    view.scaleX = if (view == it) 1.2f else 1f
                    view.scaleY = if (view == it) 1.2f else 1f
                }
            }
        }

        // Set initial selection
        colorViews.firstOrNull()?.let {
            it.alpha = 1f
            it.scaleX = 1.2f
            it.scaleY = 1.2f
        }
    }

    private fun selectColor(colorHex: String) {
        selectedBackgroundColor = colorHex
        
        // Animate background color change
        binding.rootLayout.animate()
            .alpha(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.rootLayout.setBackgroundColor(Color.parseColor(colorHex))
                binding.rootLayout.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setupTextInput() {
        binding.etStatusText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount(s?.length ?: 0)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Request focus and show keyboard
        binding.etStatusText.requestFocus()
    }

    private fun updateCharCount(count: Int) {
        binding.tvCharCount.text = "$count/$MAX_LENGTH"
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.fabSend.setOnClickListener {
            publishStatus()
        }
    }

    private fun publishStatus() {
        val text = binding.etStatusText.text?.toString()?.trim() ?: ""

        if (text.isEmpty()) {
            Toast.makeText(this, R.string.text_status_placeholder, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId == null) {
                    showLoading(false)
                    Toast.makeText(this@AddTextStatusActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val user = userRepository.getUserById(currentUserId)

                val result = statusRepository.createTextStatus(
                    userId = currentUserId,
                    userName = user?.name ?: "Utilisateur",
                    userPhotoUrl = user?.photoUrl ?: "",
                    text = text,
                    backgroundColor = selectedBackgroundColor,
                    textColor = selectedTextColor
                )

                showLoading(false)

                if (result.isSuccess) {
                    Toast.makeText(
                        this@AddTextStatusActivity,
                        "Statut publié avec succès!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@AddTextStatusActivity,
                        R.string.error_occurred,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing status", e)
                showLoading(false)
                Toast.makeText(this@AddTextStatusActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabSend.isEnabled = !show
    }
}
