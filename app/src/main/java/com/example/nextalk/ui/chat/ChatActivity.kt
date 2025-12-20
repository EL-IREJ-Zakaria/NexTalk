package com.example.nextalk.ui.chat

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.R
import com.example.nextalk.data.model.Message
import com.example.nextalk.data.model.MessageReaction
import com.example.nextalk.data.model.MessageType
import com.example.nextalk.data.model.ReplyInfo
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.data.repository.ChatRepository
import com.example.nextalk.data.repository.UserRepository
import com.example.nextalk.databinding.ActivityChatBinding
import com.example.nextalk.ui.call.CallActivity
import com.example.nextalk.data.model.CallType
import com.example.nextalk.data.repository.CallRepository
import com.example.nextalk.util.NetworkUtil
import com.example.nextalk.util.FirebaseConnectionTester
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

/**
 * ChatActivity moderne avec fonctionnalités innovantes:
 * - Swipe-to-reply (glisser pour répondre)
 * - Réactions emoji
 * - Indicateur de saisie en temps réel
 * - Messages vocaux
 * - Prévisualisation de réponse
 * - Animations fluides
 */
class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_OTHER_USER_ID = "other_user_id"
        private const val TAG = "ChatActivity"
        private const val SWIPE_THRESHOLD = 100f
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatRepository: ChatRepository
    private lateinit var userRepository: UserRepository
    private lateinit var callRepository: CallRepository
    private val authRepository = AuthRepository()
    
    // Info utilisateur pour les appels
    private var otherUserName: String = ""
    private var otherUserPhotoUrl: String = ""
    private var currentUserName: String = ""
    private var currentUserPhotoUrl: String = ""

    private var conversationId: String = ""
    private var otherUserId: String = ""
    private var selectedImageUri: Uri? = null
    private var replyToMessage: Message? = null
    private var isTyping = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            sendImageMessage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: ""
            otherUserId = intent.getStringExtra(EXTRA_OTHER_USER_ID) ?: ""

            if (conversationId.isEmpty() || otherUserId.isEmpty()) {
                Log.e(TAG, "Missing conversation or user ID")
                finish()
                return
            }

            initRepositories()
            setupToolbar()
            setupRecyclerView()
            setupMessageInput()
            setupSwipeToReply()
            loadOtherUserInfo()
            observeMessages()
            observeTypingStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initRepositories() {
        val database = NexTalkApplication.instance.database
        chatRepository = ChatRepository(database.conversationDao(), database.messageDao())
        userRepository = UserRepository(database.userDao())
        callRepository = CallRepository(database.callDao())
        
        // Charger les infos de l'utilisateur actuel
        loadCurrentUserInfo()
    }
    
    private fun loadCurrentUserInfo() {
        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: return@launch
                val user = userRepository.getUserById(currentUserId)
                user?.let {
                    currentUserName = it.name
                    currentUserPhotoUrl = it.photoUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current user info", e)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Ajouter le menu à la toolbar
        binding.toolbar.inflateMenu(R.menu.menu_chat)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_test_connection -> {
                    testFirebaseConnection()
                    true
                }
                else -> false
            }
        }
        
        // Bouton appel vocal
        binding.btnVoiceCall.setOnClickListener {
            startCall(CallType.VOICE)
        }
        
        // Bouton appel vidéo
        binding.btnVideoCall.setOnClickListener {
            startCall(CallType.VIDEO)
        }
    }
    
    /**
     * Teste la connexion Firebase et affiche un diagnostic
     */
    private fun testFirebaseConnection() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@ChatActivity, "Test en cours...", Toast.LENGTH_SHORT).show()
                
                // Test complet
                val report = FirebaseConnectionTester.generateDiagnosticReport()
                
                // Test spécifique à la messagerie
                val messagingOk = FirebaseConnectionTester.testMessaging(conversationId)
                
                // Afficher le résultat
                val message = if (messagingOk) {
                    "✅ Messagerie fonctionnelle !\n\nSi les messages ne s'affichent pas chez l'autre utilisateur :\n1. Vérifiez qu'il a une connexion Internet\n2. Vérifiez qu'il est dans la même conversation\n3. Consultez les logs (Logcat)"
                } else {
                    "❌ Problème détecté !\n\nVérifiez les logs Logcat pour plus de détails.\n\nSolution probable :\n- Configurez les règles Firestore\n- Voir le fichier firestore.rules\n- Console: console.firebase.google.com"
                }
                
                AlertDialog.Builder(this@ChatActivity)
                    .setTitle("Test de connexion Firebase")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Voir logs") { _, _ ->
                        Toast.makeText(this@ChatActivity, "Consultez Logcat avec le filtre 'FirebaseTest'", Toast.LENGTH_LONG).show()
                    }
                    .show()
                    
            } catch (e: Exception) {
                Log.e(TAG, "Error testing connection", e)
                Toast.makeText(this@ChatActivity, "Erreur lors du test", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startCall(callType: CallType) {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        
        // Générer un ID d'appel unique
        val callId = UUID.randomUUID().toString()
        
        // Lancer l'activité d'appel immédiatement (meilleure UX)
        val intent = Intent(this@ChatActivity, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_CALL_ID, callId)
            putExtra(CallActivity.EXTRA_CALL_TYPE, callType.name)
            putExtra(CallActivity.EXTRA_USER_ID, otherUserId)
            putExtra(CallActivity.EXTRA_USER_NAME, otherUserName)
            putExtra(CallActivity.EXTRA_USER_PHOTO, otherUserPhotoUrl)
            putExtra(CallActivity.EXTRA_IS_INCOMING, false)
            putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
        }
        startActivity(intent)
        
        // Enregistrer l'appel en arrière-plan (async)
        lifecycleScope.launch {
            try {
                callRepository.initiateCall(
                    conversationId = conversationId,
                    callerId = currentUserId,
                    callerName = currentUserName,
                    callerPhotoUrl = currentUserPhotoUrl,
                    receiverId = otherUserId,
                    receiverName = otherUserName,
                    receiverPhotoUrl = otherUserPhotoUrl,
                    type = callType
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error registering call", e)
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = authRepository.getCurrentUserId() ?: ""
        messageAdapter = MessageAdapter(
            currentUserId = currentUserId,
            onReplyClick = { message -> showReplyPreview(message) },
            onReactionClick = { message, emoji -> toggleReaction(message, emoji) },
            onMessageLongClick = { message, view -> showMessageOptions(message, view) },
            onVoicePlayClick = { message -> playVoiceMessage(message) },
            onImageClick = { imageUrl -> openImageViewer(imageUrl) }
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            itemAnimator?.changeDuration = 300
        }
    }

    private fun setupSwipeToReply() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val message = messageAdapter.currentList[position]
                showReplyPreview(message)
                messageAdapter.notifyItemChanged(position) // Reset swipe
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.3f
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvMessages)
    }

    private fun setupMessageInput() {
        // Basculer entre bouton micro et bouton envoi
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                
                if (hasText) {
                    binding.btnVoice.visibility = View.GONE
                    binding.btnSend.visibility = View.VISIBLE
                } else {
                    binding.btnVoice.visibility = View.VISIBLE
                    binding.btnSend.visibility = View.GONE
                }

                // Envoyer le statut "en train d'écrire"
                updateTypingStatus(hasText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnVoice.setOnClickListener {
            // TODO: Implémenter l'enregistrement vocal
            Toast.makeText(this, "Fonctionnalité vocale à venir", Toast.LENGTH_SHORT).show()
        }

        binding.btnEmoji.setOnClickListener {
            showQuickReactions()
        }

        binding.btnCancelReply.setOnClickListener {
            hideReplyPreview()
        }
    }

    private fun showReplyPreview(message: Message) {
        replyToMessage = message
        binding.replyPreviewCard.visibility = View.VISIBLE
        
        val senderName = if (message.senderId == authRepository.getCurrentUserId()) {
            getString(R.string.you)
        } else {
            binding.tvName.text.toString()
        }
        
        binding.tvReplyToName.text = getString(R.string.reply_to, senderName)
        binding.tvReplyToText.text = when (message.type) {
            MessageType.IMAGE -> "📷 ${getString(R.string.photo)}"
            MessageType.VOICE -> "🎤 ${getString(R.string.voice_message)}"
            else -> message.text
        }
        
        // Animation d'entrée
        binding.replyPreviewCard.alpha = 0f
        binding.replyPreviewCard.translationY = 50f
        binding.replyPreviewCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .start()

        // Focus sur l'input
        binding.etMessage.requestFocus()
    }

    private fun hideReplyPreview() {
        binding.replyPreviewCard.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(200)
            .withEndAction {
                binding.replyPreviewCard.visibility = View.GONE
                replyToMessage = null
            }
            .start()
    }

    private fun showQuickReactions() {
        val quickEmojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Réaction rapide")
        builder.setItems(quickEmojis.toTypedArray()) { _, which ->
            // Ajouter l'emoji au message
            val currentText = binding.etMessage.text.toString()
            binding.etMessage.setText("$currentText${quickEmojis[which]}")
            binding.etMessage.setSelection(binding.etMessage.text?.length ?: 0)
        }
        builder.show()
    }

    private fun showMessageOptions(message: Message, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_message_options, popup.menu)
        
        // Adapter le menu selon si c'est notre message
        val isOwnMessage = message.senderId == authRepository.getCurrentUserId()
        popup.menu.findItem(R.id.action_delete)?.isVisible = isOwnMessage
        popup.menu.findItem(R.id.action_edit)?.isVisible = isOwnMessage && message.type == MessageType.TEXT
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_reply -> {
                    showReplyPreview(message)
                    true
                }
                R.id.action_react -> {
                    showReactionPicker(message)
                    true
                }
                R.id.action_copy -> {
                    copyMessageText(message)
                    true
                }
                R.id.action_edit -> {
                    editMessage(message)
                    true
                }
                R.id.action_delete -> {
                    deleteMessage(message)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showReactionPicker(message: Message) {
        val emojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉", "😍", "👏")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.add_reaction))
        builder.setItems(emojis.toTypedArray()) { _, which ->
            toggleReaction(message, emojis[which])
        }
        builder.show()
    }

    private fun toggleReaction(message: Message, emoji: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        
        lifecycleScope.launch {
            try {
                val updatedReactions = message.reactions.toMutableList()
                val existingReaction = updatedReactions.find { 
                    it.userId == currentUserId && it.emoji == emoji 
                }
                
                if (existingReaction != null) {
                    // Retirer la réaction
                    updatedReactions.remove(existingReaction)
                } else {
                    // Ajouter la réaction
                    updatedReactions.add(MessageReaction(emoji, currentUserId))
                }
                
                // Mettre à jour le message dans Firestore
                chatRepository.updateMessageReactions(message.id, updatedReactions)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling reaction", e)
            }
        }
    }

    private fun copyMessageText(message: Message) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("message", message.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Message copié", Toast.LENGTH_SHORT).show()
    }

    private fun editMessage(message: Message) {
        // TODO: Implémenter l'édition de message
        Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
    }

    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le message")
            .setMessage("Voulez-vous supprimer ce message ?")
            .setPositiveButton("Supprimer") { _, _ ->
                lifecycleScope.launch {
                    try {
                        chatRepository.deleteMessage(message.id)
                        Toast.makeText(this@ChatActivity, "Message supprimé", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting message", e)
                        Toast.makeText(this@ChatActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun playVoiceMessage(message: Message) {
        // TODO: Implémenter la lecture de message vocal
        Toast.makeText(this, "Lecture vocale à venir", Toast.LENGTH_SHORT).show()
    }

    private fun openImageViewer(imageUrl: String) {
        // TODO: Implémenter le visualiseur d'images en plein écran
        Toast.makeText(this, "Visualiseur d'images à venir", Toast.LENGTH_SHORT).show()
    }

    private fun updateTypingStatus(typing: Boolean) {
        if (isTyping == typing) return
        isTyping = typing
        
        lifecycleScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId() ?: return@launch
                // TODO: Envoyer le statut de saisie à Firestore
                // chatRepository.updateTypingStatus(conversationId, currentUserId, typing)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating typing status", e)
            }
        }
    }

    private fun observeTypingStatus() {
        // TODO: Observer le statut de saisie de l'autre utilisateur
        // Pour le moment, on cache l'indicateur
        binding.typingIndicatorLayout.visibility = View.GONE
    }

    private fun loadOtherUserInfo() {
        lifecycleScope.launch {
            try {
                userRepository.observeUser(otherUserId)
                    .catch { e ->
                        Log.e(TAG, "Error observing user", e)
                    }
                    .collectLatest { user ->
                        user?.let {
                            // Stocker les infos pour les appels
                            otherUserName = it.name
                            otherUserPhotoUrl = it.photoUrl
                            
                            binding.tvName.text = it.name
                            binding.tvStatus.text = if (it.isOnline) {
                                getString(R.string.online)
                            } else {
                                getString(R.string.offline)
                            }
                            
                            // Indicateur en ligne
                            binding.onlineIndicator.visibility = if (it.isOnline) View.VISIBLE else View.GONE
                            
                            binding.tvStatus.setTextColor(
                                if (it.isOnline) {
                                    ContextCompat.getColor(this@ChatActivity, R.color.colorOnline)
                                } else {
                                    ContextCompat.getColor(this@ChatActivity, android.R.color.white)
                                }
                            )

                            if (it.photoUrl.isNotEmpty()) {
                                Glide.with(this@ChatActivity)
                                    .load(it.photoUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(binding.ivAvatar)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user info", e)
            }
        }
    }

    private fun observeMessages() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "👂 Démarrage de l'écoute des messages...")
                Log.d(TAG, "   ConversationId: $conversationId")
                
                chatRepository.getMessages(conversationId)
                    .catch { e ->
                        Log.e(TAG, "❌ Erreur lors de l'écoute des messages", e)
                        
                        when {
                            e.message?.contains("PERMISSION_DENIED") == true -> {
                                Log.e(TAG, "❌ ERREUR : Permission refusée pour lire les messages")
                                Log.e(TAG, "   → Configurez les règles Firestore (firestore.rules)")
                            }
                            e.message?.contains("UNAVAILABLE") == true -> {
                                Log.e(TAG, "❌ ERREUR : Firestore indisponible (pas de connexion)")
                            }
                        }
                    }
                    .collectLatest { messages ->
                        val oldSize = messageAdapter.currentList.size
                        val newSize = messages.size
                        
                        Log.d(TAG, "📨 Messages reçus : $newSize messages")
                        
                        if (newSize > oldSize) {
                            val newMessages = newSize - oldSize
                            Log.d(TAG, "✨ Nouveau(x) message(s) : $newMessages")
                            
                            // Afficher le dernier message pour debug
                            messages.lastOrNull()?.let { lastMsg ->
                                Log.d(TAG, "   Dernier message :")
                                Log.d(TAG, "      Texte: ${lastMsg.text}")
                                Log.d(TAG, "      De: ${lastMsg.senderId}")
                                Log.d(TAG, "      À: ${if (lastMsg.senderId == authRepository.getCurrentUserId()) "moi" else "l'autre"}")
                                Log.d(TAG, "      Timestamp: ${lastMsg.timestamp}")
                            }
                        }
                        
                        messageAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                // Scroll seulement si nouveau message ou proche du bas
                                if (oldSize < messages.size || isNearBottom()) {
                                    binding.rvMessages.scrollToPosition(messages.size - 1)
                                }
                            }
                        }

                        // Marquer les messages comme lus
                        try {
                            val currentUserId = authRepository.getCurrentUserId() ?: return@collectLatest
                            chatRepository.markMessagesAsRead(conversationId, currentUserId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error marking messages as read", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur critique lors de l'observation", e)
                Log.e(TAG, "   ${e.message}")
            }
        }
    }

    private fun isNearBottom(): Boolean {
        val layoutManager = binding.rvMessages.layoutManager as? LinearLayoutManager ?: return false
        val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
        val totalItems = layoutManager.itemCount
        return totalItems - lastVisiblePosition <= 3
    }

    private fun sendMessage(text: String) {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                Log.d(TAG, "📤 Envoi de message...")
                Log.d(TAG, "   ConversationId: $conversationId")
                Log.d(TAG, "   SenderId: $currentUserId")
                Log.d(TAG, "   Text: $text")
                Log.d(TAG, "   OtherUserId: $otherUserId")
                
                val replyInfo = replyToMessage?.let {
                    ReplyInfo(
                        messageId = it.id,
                        senderId = it.senderId,
                        senderName = if (it.senderId == currentUserId) getString(R.string.you) else binding.tvName.text.toString(),
                        text = it.text,
                        type = it.type
                    )
                }

                if (NetworkUtil.isNetworkAvailable(this@ChatActivity)) {
                    Log.d(TAG, "✅ Connexion Internet disponible")
                    
                    val result = chatRepository.sendMessage(
                        conversationId = conversationId,
                        senderId = currentUserId,
                        text = text,
                        type = MessageType.TEXT,
                        replyTo = replyInfo
                    )

                    result.onSuccess { message ->
                        Log.d(TAG, "✅ Message envoyé avec succès !")
                        Log.d(TAG, "   MessageId: ${message.id}")
                        Log.d(TAG, "   Timestamp: ${message.timestamp}")
                        Log.d(TAG, "   Le message devrait apparaître chez l'autre utilisateur instantanément")
                    }
                    
                    result.onFailure { error ->
                        Log.e(TAG, "❌ Échec de l'envoi du message", error)
                        
                        when {
                            error.message?.contains("PERMISSION_DENIED") == true -> {
                                Log.e(TAG, "❌ ERREUR CRITIQUE : Permission refusée par Firestore")
                                Log.e(TAG, "   SOLUTION : Configurez les règles Firestore")
                                Log.e(TAG, "   1. Ouvrez https://console.firebase.google.com")
                                Log.e(TAG, "   2. Firestore Database → Règles")
                                Log.e(TAG, "   3. Copiez le contenu de firestore.rules")
                                Toast.makeText(
                                    this@ChatActivity,
                                    "Permission refusée. Vérifiez les règles Firestore.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            error.message?.contains("NOT_FOUND") == true -> {
                                Log.e(TAG, "❌ Conversation non trouvée")
                                Toast.makeText(this@ChatActivity, "Conversation introuvable", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this@ChatActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Pas de connexion Internet - Mode hors ligne")
                    
                    // Mode hors-ligne
                    chatRepository.sendMessageOffline(
                        conversationId = conversationId,
                        senderId = currentUserId,
                        text = text,
                        type = MessageType.TEXT
                    )
                    
                    Log.d(TAG, "💾 Message sauvegardé localement (sera envoyé plus tard)")
                    Toast.makeText(this@ChatActivity, R.string.no_internet, Toast.LENGTH_SHORT).show()
                }

                hideReplyPreview()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur critique lors de l'envoi", e)
                Log.e(TAG, "   Message d'erreur : ${e.message}")
                Log.e(TAG, "   Stack trace : ${e.stackTraceToString()}")
                Toast.makeText(this@ChatActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendImageMessage(imageUri: Uri) {
        val currentUserId = authRepository.getCurrentUserId() ?: return

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = chatRepository.sendImageMessage(
                    conversationId = conversationId,
                    senderId = currentUserId,
                    imageUri = imageUri
                )

                binding.progressBar.visibility = View.GONE

                result.onFailure {
                    Toast.makeText(this@ChatActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending image", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChatActivity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                authRepository.getCurrentUserId()?.let { userId ->
                    userRepository.updateOnlineStatus(userId, true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating online status", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            try {
                authRepository.getCurrentUserId()?.let { userId ->
                    userRepository.updateOnlineStatus(userId, false)
                }
                updateTypingStatus(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating online status", e)
            }
        }
    }
}
