package com.example.nextalk.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nextalk.R
import com.example.nextalk.data.model.Conversation
import com.example.nextalk.data.model.User
import com.example.nextalk.databinding.ItemConversationBinding
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val currentUserId: String,
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    private val usersMap = mutableMapOf<String, User>()

    fun updateUserInfo(userId: String, user: User) {
        usersMap[userId] = user
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(
        private val binding: ItemConversationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onConversationClick(getItem(position))
                }
            }
        }

        fun bind(conversation: Conversation) {
            val otherUserId = conversation.getOtherUserId(currentUserId)
            val otherUser = usersMap[otherUserId]

            binding.tvName.text = otherUser?.name ?: "Utilisateur"
            binding.tvLastMessage.text = conversation.lastMessage.ifEmpty { "Pas de messages" }
            binding.tvTime.text = formatTime(conversation.lastMessageTime)

            // Charger la photo de profil
            if (otherUser?.photoUrl?.isNotEmpty() == true) {
                Glide.with(binding.root.context)
                    .load(otherUser.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // Indicateur en ligne
            binding.viewOnlineIndicator.visibility = if (otherUser?.isOnline == true) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Badge de messages non lus
            if (conversation.unreadCount > 0 && conversation.lastMessageSenderId != currentUserId) {
                binding.tvUnreadCount.visibility = android.view.View.VISIBLE
                binding.tvUnreadCount.text = conversation.unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = android.view.View.GONE
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

            return when {
                // Aujourd'hui
                now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                // Hier
                now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
                    "Hier"
                }
                // Cette semaine
                now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) -> {
                    SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
                }
                // Plus ancien
                else -> {
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
