package com.example.stay.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.ChatMessage
import com.example.stay.databinding.FragmentChatBinding
import com.example.stay.databinding.ItemChatMessageBinding
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatAdapter

    private var listingId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listingId = arguments?.getLong("listingId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (listingId == -1L) {
            // If we don't have a listingId, we can't show a specific chat
            return
        }

        val db = AppDatabase.getDatabase(requireContext())
        val chatDao = db.chatDao()

        // Set title to include listing info if possible
        viewLifecycleOwner.lifecycleScope.launch {
            val listing = db.listingDao().getListingById(listingId)
            listing?.let {
                // Assuming you have a toolbar or textview for the chat title
                // For now, let's try to update a TextView if it exists or just use a toast
                // binding.tvChatTitle.text = "Chat about ${it.title}"
            }
        }

        adapter = ChatAdapter()
        binding.rvChat.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            chatDao.getMessagesForListing(listingId).collect { messages ->
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val message = ChatMessage(
                        listingId = listingId,
                        senderName = "Student",
                        message = text,
                        isFromMe = true
                    )
                    chatDao.insert(message)
                    binding.etMessage.setText("")
                    
                    // Simulate landlord response based on what was said
                    simulateLandlordResponse(db, text)
                }
            }
        }
    }

    private suspend fun simulateLandlordResponse(db: AppDatabase, studentText: String) {
        kotlinx.coroutines.delay(1500)
        
        val responseText = when {
            studentText.contains("price", ignoreCase = true) || studentText.contains("cost", ignoreCase = true) -> 
                "The monthly rent for this property is fixed. Let me know if you want to book a viewing!"
            studentText.contains("view", ignoreCase = true) || studentText.contains("see", ignoreCase = true) -> 
                "Sure! I can show you the place tomorrow. Does that work for you?"
            studentText.contains("hello", ignoreCase = true) || studentText.contains("hi", ignoreCase = true) -> {
                val listing = db.listingDao().getListingById(listingId)
                "Hello! Thanks for reaching out about ${listing?.title ?: "my property"}. How can I help you today?"
            }
            else -> "That sounds good. Do you have any other questions about this house?"
        }

        val response = ChatMessage(
            listingId = listingId,
            senderName = "Landlord",
            message = responseText,
            isFromMe = false
        )
        db.chatDao().insert(response)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder>(DiffCallback) {

    class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvSender.text = message.senderName
            binding.tvMessage.text = message.message
            
            val params = binding.cardMessage.layoutParams as ViewGroup.MarginLayoutParams
            if (message.isFromMe) {
                binding.cardMessage.setCardBackgroundColor(0xFF800020.toInt()) // Burgundy
                binding.tvMessage.setTextColor(0xFFFFFFFF.toInt()) // White text
                binding.tvSender.setTextColor(0xFFFFFFFF.toInt())
                (binding.root as android.widget.LinearLayout).gravity = android.view.Gravity.END
            } else {
                binding.cardMessage.setCardBackgroundColor(0xFFFFFDD0.toInt()) // Cream White
                binding.tvMessage.setTextColor(0xFF2C1B1B.toInt()) // Dark text
                binding.tvSender.setTextColor(0xFF800020.toInt()) // Burgundy sender name
                (binding.root as android.widget.LinearLayout).gravity = android.view.Gravity.START
            }
            binding.cardMessage.layoutParams = params
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean = oldItem == newItem
    }
}
