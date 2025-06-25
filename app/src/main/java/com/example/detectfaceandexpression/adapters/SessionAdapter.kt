package com.example.detectfaceandexpression.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.detectfaceandexpression.databinding.ItemSessionCardBinding
import com.example.detectfaceandexpression.roomDB.SessionEntity


class SessionAdapter(private val onItemClick: (SessionEntity) -> Unit,
                     private val onItemLongClick: (SessionEntity) -> Unit
                     ) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    val diffUtil = object : DiffUtil.ItemCallback<SessionEntity>(){ // pasing a dataclass

        override fun areItemsTheSame(oldItem: SessionEntity, newItem: SessionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionEntity, newItem: SessionEntity): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    inner class SessionViewHolder(val binding: ItemSessionCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(session: SessionEntity) {
            binding.tvSessionName.text = session.title
            binding.tvSessionDate.text = session.dateTime
            binding.tvSessionDuration.text = session.duration

            // Long press to delete
            binding.root.setOnLongClickListener {
                onItemLongClick(session)
                true
            }

            // Short click → open detail
            binding.root.setOnClickListener {
                onItemClick(session)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int = differ.currentList.size
}
