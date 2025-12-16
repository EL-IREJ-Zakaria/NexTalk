package com.example.nextalk.ui.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nextalk.R
import com.example.nextalk.data.model.User
import com.example.nextalk.databinding.ItemUserBinding

/**
 * Adaptateur pour afficher la liste des utilisateurs avec design moderne
 */
class UsersAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Clic sur la carte entière
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position))
                }
            }

            // Clic sur le bouton "Discuter"
            binding.btnStartChat.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position))
                }
            }
        }

        fun bind(user: User) {
            // Afficher le nom
            binding.tvName.text = user.name

            // Afficher l'email ou le statut
            if (user.isOnline) {
                binding.tvEmail.visibility = View.GONE
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = binding.root.context.getString(R.string.online)
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.colorOnline)
                )
            } else {
                binding.tvEmail.visibility = View.VISIBLE
                binding.tvStatus.visibility = View.GONE
                binding.tvEmail.text = user.email
            }

            // Charger la photo de profil avec animation
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // Indicateur en ligne avec animation
            binding.viewOnlineIndicator.visibility = if (user.isOnline) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
