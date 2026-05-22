package com.example.stay.ui.notifications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.databinding.FragmentNotificationsBinding
import com.example.stay.ui.home.ListingAdapter
import com.example.stay.ui.home.ListingItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ListingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val listingDao = db.listingDao()
        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
        val userEmail = sharedPrefs.getString("userEmail", "") ?: ""

        adapter = ListingAdapter(
            onItemClick = { listing ->
                val bundle = bundleOf("listingId" to listing.id)
                findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
            },
            onFavoriteClick = { listing ->
                // Toggle favorite in Alerts
                viewLifecycleOwner.lifecycleScope.launch {
                    val favIds = listingDao.getFavoriteIds(userEmail).first()
                    if (favIds.contains(listing.id)) {
                        listingDao.deleteFavorite(userEmail, listing.id)
                    } else {
                        listingDao.insertFavorite(com.example.stay.data.local.entity.Favorite(userEmail, listing.id))
                    }
                }
            }
        )
        binding.rvNotifications.adapter = adapter

        loadAlerts()
    }

    private fun loadAlerts() {
        val sharedPrefs = requireContext().getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)
        val prefMaxPrice = sharedPrefs.getFloat("prefMaxPrice", -1f).toDouble()
        val prefLocation = sharedPrefs.getString("prefLocation", "") ?: ""
        val prefDate = sharedPrefs.getString("prefDate", "") ?: ""
        val userEmail = sharedPrefs.getString("userEmail", "") ?: ""

        if (prefMaxPrice == -1.0) {
            binding.tvNoNotifications.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            // Show listings that match the user's SAVED preferences
            db.listingDao().filterListings(prefMaxPrice, prefLocation, prefDate).collectLatest { listings ->
                if (listings.isEmpty()) {
                    binding.tvNoNotifications.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.tvNoNotifications.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                    
                    val favIds = db.listingDao().getFavoriteIds(userEmail).first()
                    val items = listings.map { ListingItem(it, favIds.contains(it.id)) }
                    adapter.submitList(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
