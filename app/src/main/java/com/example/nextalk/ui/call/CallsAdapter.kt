package com.example.nextalk.ui.call

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nextalk.R
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.CallStatus
import com.example.nextalk.data.model.CallType
import com.example.nextalk.databinding.ItemCallBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adaptateur pour afficher l'historique des appels
 */
class CallsAdapter(
    private val currentUserId: String,
    private val onCallClick: (Call) -> Unit = {},
    private val onCallAgainClick: (Call) -> Unit = {},
    private val onDeleteClick: (Call) -> Unit = {}
) : ListAdapter<Call, CallsAdapter.CallViewHolder>(CallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CallViewHolder(
        private val binding: ItemCallBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCallClick(getItem(position))
                }
            }
        }

        fun bind(call: Call) {
            val context = binding.root.context

            // Déterminer qui appeler (émetteur ou récepteur)
            val otherUserName = if (call.callerId == currentUserId) {
                call.receiverName
            } else {
                call.callerName
            }

            val otherUserPhoto = if (call.callerId == currentUserId) {
                call.receiverPhotoUrl
            } else {
                call.callerPhotoUrl
            }

            // Afficher le nom
            binding.tvName.text = otherUserName

            // Charger l'avatar
            if (otherUserPhoto.isNotEmpty()) {
                Glide.with(context)
                    .load(otherUserPhoto)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // Type d'appel (icône et texte)
            val isIncoming = call.receiverId == currentUserId
            when (call.type) {
                CallType.VOICE -> {
                    binding.ivCallType.setImageResource(R.drawable.ic_mic)
                    binding.tvStatus.text = if (isIncoming) {
                        context.getString(R.string.incoming_voice_call)
                    } else {
                        context.getString(R.string.outgoing, context.getString(R.string.voice_call))
                    }
                }
                CallType.VIDEO -> {
                    binding.ivCallType.setImageResource(R.drawable.ic_camera)
                    binding.tvStatus.text = if (isIncoming) {
                        context.getString(R.string.incoming_video_call)
                    } else {
                        context.getString(R.string.outgoing, context.getString(R.string.video_call))
                    }
                }
            }

            // Statut avec icône
            when (call.status) {
                CallStatus.MISSED -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_call_end)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorError))
                }
                CallStatus.DECLINED -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_call_end)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorError))
                }
                CallStatus.ENDED -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_status_received)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorSuccess))
                }
                else -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_call)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
                }
            }

            // Durée de l'appel
            binding.tvDuration.text = call.getFormattedDuration()
            if (call.duration == 0L) {
                binding.tvDuration.text = "--:--"
            }

            // Date et heure
            binding.tvDate.text = formatDateTime(call.timestamp)

            // Menu contextuel (swipe pour actions)
            setupContextMenu(call)
        }

        private fun setupContextMenu(call: Call) {
            // Bouton rappeler (contextuel)
            binding.root.setOnLongClickListener {
                showContextMenu(call)
                true
            }
        }

        private fun showContextMenu(call: Call) {
            val context = binding.root.context
            val menu = android.widget.PopupMenu(context, binding.root)
            menu.menuInflater.inflate(R.menu.menu_call_options, menu.menu)

            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_call_again -> {
                        onCallAgainClick(call)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(call)
                        true
                    }
                    else -> false
                }
            }

            menu.show()
        }

        private fun formatDateTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val oneDayMs = 24 * 60 * 60 * 1000

            return when {
                diff < oneDayMs -> {
                    // Aujourd'hui
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                diff < 2 * oneDayMs -> {
                    // Hier
                    "${binding.root.context.getString(R.string.yesterday)} " +
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                diff < 7 * oneDayMs -> {
                    // Cette semaine
                    SimpleDateFormat("EEEE HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
                else -> {
                    // Plus ancien
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }

    class CallDiffCallback : DiffUtil.ItemCallback<Call>() {
        override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean {
            return oldItem == newItem
        }
    }
}
