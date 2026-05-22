package com.example.stay.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stay.R
import com.example.stay.data.local.entity.Listing
import com.example.stay.databinding.ItemListingBinding
import java.time.LocalDate
import java.util.Locale

class ListingAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onFavoriteClick: (Listing) -> Unit
) :
    ListAdapter<ListingItem, ListingAdapter.ListingViewHolder>(ListingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val binding = ItemListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ListingViewHolder(private val binding: ItemListingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListingItem) {
            val listing = item.listing
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvDate.text = String.format("Available: %s", listing.availabilityDate)
            binding.tvDate.setTextColor(ContextCompat.getColor(binding.tvDate.context, android.R.color.darker_gray))
            
            // Just round the money, no thebes (cents)
            binding.tvPrice.text = String.format(Locale.getDefault(), "BWP %,.0f", listing.price)

            binding.tvStatus.text = listing.status

            val favIcon = if (item.isFavorite) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
            binding.btnFavorite.setImageResource(favIcon)

            if (listing.imageUrl.isNotEmpty()) {
                Glide.with(binding.ivListing.context)
                    .load(listing.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivListing)
            } else {
                binding.ivListing.setImageResource(android.R.drawable.ic_menu_report_image)
            }
            
            binding.root.setOnClickListener {
                onItemClick(listing)
            }

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(listing)
            }
        }
    }

    class ListingDiffCallback : DiffUtil.ItemCallback<ListingItem>() {
        override fun areItemsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean {
            return oldItem.listing.id == newItem.listing.id
        }

        override fun areContentsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean {
            return oldItem == newItem
        }
    }
}
