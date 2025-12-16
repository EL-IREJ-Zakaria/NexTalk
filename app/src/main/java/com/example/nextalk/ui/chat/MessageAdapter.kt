package com.example.nextalk.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nextalk.R
import com.example.nextalk.data.model.Message
import com.example.nextalk.data.model.MessageStatus
import com.example.nextalk.data.model.MessageType
import com.example.nextalk.databinding.ItemMessageReceivedBinding
import com.example.nextalk.databinding.ItemMessageSentBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptateur moderne pour les messages avec support de:
 * - Réactions emoji
 * - Réponses aux messages
 * - Messages vocaux
 * - Images
 * - Indicateur "modifié"
 * - Swipe-to-reply (géré dans ChatActivity)
 */
class MessageAdapter(
    private val currentUserId: String,
    private val onReplyClick: (Message) -> Unit = {},
    private val onReactionClick: (Message, String) -> Unit = { _, _ -> },
    private val onMessageLongClick: (Message, View) -> Unit = { _, _ -> },
    private val onVoicePlayClick: (Message) -> Unit = {},
    private val onImageClick: (String) -> Unit = {}
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position))
            is ReceivedMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            // Gestion des messages supprimés
            if (message.isDeleted) {
                showDeletedMessage()
                return
            }

            // Gestion de la réponse
            if (message.replyTo != null) {
                showReplyInfo(message.replyTo.senderName, message.replyTo.text)
            } else {
                binding.replyCard.visibility = View.GONE
            }

            // Gestion du contenu selon le type
            when (message.type) {
                MessageType.IMAGE -> showImage(message)
                MessageType.VOICE -> showVoiceMessage(message)
                else -> showTextMessage(message)
            }

            // Affichage des réactions
            showReactions(message)

            // Heure et statut
            binding.tvTime.text = formatTime(message.timestamp)
            binding.tvEdited.visibility = if (message.isEdited) View.VISIBLE else View.GONE

            // Statut du message
            binding.ivStatus.setImageResource(
                when (message.status) {
                    MessageStatus.PENDING -> R.drawable.ic_status_pending
                    MessageStatus.SENT -> R.drawable.ic_status_sent
                    MessageStatus.RECEIVED -> R.drawable.ic_status_received
                    MessageStatus.SEEN -> R.drawable.ic_status_seen
                }
            )

            // Gestion des clics longs pour les options
            binding.cardMessage.setOnLongClickListener {
                onMessageLongClick(message, it)
                true
            }
        }

        private fun showDeletedMessage() {
            binding.tvMessage.visibility = View.VISIBLE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.GONE
            binding.replyCard.visibility = View.GONE
            binding.reactionsGroup.visibility = View.GONE
            binding.tvMessage.text = binding.root.context.getString(R.string.deleted_message)
            binding.tvMessage.alpha = 0.6f
            binding.tvMessage.setTextIsSelectable(false)
        }

        private fun showReplyInfo(senderName: String, text: String) {
            binding.replyCard.visibility = View.VISIBLE
            binding.tvReplySenderName.text = if (senderName.isEmpty()) 
                binding.root.context.getString(R.string.you) else senderName
            binding.tvReplyText.text = text
        }

        private fun showTextMessage(message: Message) {
            binding.tvMessage.visibility = View.VISIBLE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.GONE
            binding.tvMessage.text = message.text
            binding.tvMessage.alpha = 1f
            binding.tvMessage.setTextIsSelectable(true)
        }

        private fun showImage(message: Message) {
            binding.tvMessage.visibility = View.GONE
            binding.ivImage.visibility = View.VISIBLE
            binding.voiceMessageLayout.visibility = View.GONE

            Glide.with(binding.root.context)
                .load(message.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .into(binding.ivImage)

            binding.ivImage.setOnClickListener {
                onImageClick(message.imageUrl)
            }
        }

        private fun showVoiceMessage(message: Message) {
            binding.tvMessage.visibility = View.GONE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.VISIBLE

            binding.tvVoiceDuration.text = formatVoiceDuration(message.voiceDuration)

            binding.btnPlayPause.setOnClickListener {
                onVoicePlayClick(message)
            }
        }

        private fun showReactions(message: Message) {
            val groupedReactions = message.getGroupedReactions()
            
            if (groupedReactions.isEmpty()) {
                binding.reactionsGroup.visibility = View.GONE
                return
            }

            binding.reactionsGroup.visibility = View.VISIBLE
            binding.reactionsGroup.removeAllViews()

            groupedReactions.forEach { (emoji, count) ->
                val chip = Chip(binding.root.context).apply {
                    text = "$emoji $count"
                    isClickable = true
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.white)
                    chipStrokeWidth = 2f
                    
                    // Highlight si l'utilisateur a réagi
                    if (message.hasUserReacted(currentUserId, emoji)) {
                        setChipStrokeColorResource(R.color.colorPrimary)
                    } else {
                        setChipStrokeColorResource(R.color.divider)
                    }

                    setOnClickListener {
                        onReactionClick(message, emoji)
                    }
                }
                binding.reactionsGroup.addView(chip)
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            // Gestion des messages supprimés
            if (message.isDeleted) {
                showDeletedMessage()
                return
            }

            // Gestion de la réponse
            if (message.replyTo != null) {
                showReplyInfo(message.replyTo.senderName, message.replyTo.text)
            } else {
                binding.replyCard.visibility = View.GONE
            }

            // Gestion du contenu selon le type
            when (message.type) {
                MessageType.IMAGE -> showImage(message)
                MessageType.VOICE -> showVoiceMessage(message)
                else -> showTextMessage(message)
            }

            // Affichage des réactions
            showReactions(message)

            // Heure
            binding.tvTime.text = formatTime(message.timestamp)
            binding.tvEdited.visibility = if (message.isEdited) View.VISIBLE else View.GONE

            // Gestion des clics longs
            binding.cardMessage.setOnLongClickListener {
                onMessageLongClick(message, it)
                true
            }
        }

        private fun showDeletedMessage() {
            binding.tvMessage.visibility = View.VISIBLE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.GONE
            binding.replyCard.visibility = View.GONE
            binding.reactionsGroup.visibility = View.GONE
            binding.tvMessage.text = binding.root.context.getString(R.string.deleted_message)
            binding.tvMessage.alpha = 0.6f
            binding.tvMessage.setTextIsSelectable(false)
        }

        private fun showReplyInfo(senderName: String, text: String) {
            binding.replyCard.visibility = View.VISIBLE
            binding.tvReplySenderName.text = senderName
            binding.tvReplyText.text = text
        }

        private fun showTextMessage(message: Message) {
            binding.tvMessage.visibility = View.VISIBLE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.GONE
            binding.tvMessage.text = message.text
            binding.tvMessage.alpha = 1f
            binding.tvMessage.setTextIsSelectable(true)
        }

        private fun showImage(message: Message) {
            binding.tvMessage.visibility = View.GONE
            binding.ivImage.visibility = View.VISIBLE
            binding.voiceMessageLayout.visibility = View.GONE

            Glide.with(binding.root.context)
                .load(message.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .into(binding.ivImage)

            binding.ivImage.setOnClickListener {
                onImageClick(message.imageUrl)
            }
        }

        private fun showVoiceMessage(message: Message) {
            binding.tvMessage.visibility = View.GONE
            binding.ivImage.visibility = View.GONE
            binding.voiceMessageLayout.visibility = View.VISIBLE

            binding.tvVoiceDuration.text = formatVoiceDuration(message.voiceDuration)

            binding.btnPlayPause.setOnClickListener {
                onVoicePlayClick(message)
            }
        }

        private fun showReactions(message: Message) {
            val groupedReactions = message.getGroupedReactions()
            
            if (groupedReactions.isEmpty()) {
                binding.reactionsGroup.visibility = View.GONE
                return
            }

            binding.reactionsGroup.visibility = View.VISIBLE
            binding.reactionsGroup.removeAllViews()

            groupedReactions.forEach { (emoji, count) ->
                val chip = Chip(binding.root.context).apply {
                    text = "$emoji $count"
                    isClickable = true
                    isCheckable = false
                    setChipBackgroundColorResource(R.color.white)
                    chipStrokeWidth = 2f
                    
                    // Highlight si l'utilisateur a réagi
                    if (message.hasUserReacted(currentUserId, emoji)) {
                        setChipStrokeColorResource(R.color.colorPrimary)
                    } else {
                        setChipStrokeColorResource(R.color.divider)
                    }

                    setOnClickListener {
                        onReactionClick(message, emoji)
                    }
                }
                binding.reactionsGroup.addView(chip)
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 24 * 60 * 60 * 1000 -> { // Moins de 24h
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            diff < 7 * 24 * 60 * 60 * 1000 -> { // Moins d'une semaine
                SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    private fun formatVoiceDuration(duration: Long): String {
        val seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
