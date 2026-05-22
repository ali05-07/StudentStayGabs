package com.example.stay.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stay.R
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.Listing
import com.example.stay.data.local.entity.User
import com.example.stay.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.round

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ListingAdapter
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val listingDao = db.listingDao()

        // Handle navigation mode (ALL, FAVORITES, MY_LISTINGS, MY_RESERVATIONS)
        val modeArg = arguments?.getString("mode") ?: "ALL"
        when (modeArg) {
            "FAVORITES" -> viewModel.showFavorites()
            "MY_LISTINGS" -> viewModel.showMyListings()
            "MY_RESERVATIONS" -> viewModel.showMyReservations()
            else -> viewModel.refresh()
        }

        adapter = ListingAdapter(
            onItemClick = { listing ->
                val bundle = bundleOf("listingId" to listing.id)
                findNavController().navigate(R.id.action_homeFragment_to_detailsFragment, bundle)
            },
            onFavoriteClick = { listing ->
                viewModel.toggleFavorite(listing.id)
            }
        )
        binding.rvListings.adapter = adapter

        // Observe listings from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listings.collectLatest { listings ->
                adapter.submitList(listings)
            }
        }

        // Observe filter updates from Navigation (Filter screen only)
        val navBackStackEntry = findNavController().currentBackStackEntry
        navBackStackEntry?.savedStateHandle?.getLiveData<Long>("filterTrigger")?.observe(viewLifecycleOwner) { _ ->
            val handle = navBackStackEntry.savedStateHandle
            val minPrice = handle.get<Double>("minPrice") ?: 800.0
            val maxPrice = handle.get<Double>("maxPrice") ?: 2000.0
            val location = handle.get<String>("location") ?: ""
            val date = handle.get<String>("date") ?: ""
            val savePrefs = handle.get<Boolean>("savePrefs") ?: false
            
            viewModel.updateFilter(minPrice, maxPrice, location, date, savePrefs)
            
            val priceMsg = if (minPrice == maxPrice) "BWP $minPrice" else "BWP $minPrice - $maxPrice"
            val statusMsg = "Strict Filter: $priceMsg in $location for $date"
            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
            
            handle.remove<Long>("filterTrigger")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (listingDao.getListingCount() == 0) {
                prePopulateData(db)
                showWelcomeNotification()
                viewModel.refresh()
            }
        }

        binding.fabFilter.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_filterFragment)
        }
    }

    private fun showWelcomeNotification() {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "welcome_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Welcome to Student Stay Gabs")
            .setContentText("Database initialized with 50 students and 80 listings.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(100, notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun prePopulateData(db: AppDatabase) {
        if (db.userDao().getUserCount() < 50) {
            val users = mutableListOf<User>()
            for (i in 1..50) {
                users.add(User(username = "Student $i", email = "student$i@example.com", password = "password$i"))
            }
            users.forEach { db.userDao().insert(it) }
        }

        val listings = mutableListOf<Listing>()
        val areas = listOf("Broadhurst", "Village", "Gaborone West", "Phakalane", "Block 6", "Tlokweng", "Block 8", "Maruapula")
        
        val houseData = listOf(
            Triple("One-room", 800.0..1000.0, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTqnZkYa3DMpzQ59huGrdD5i2i9pWtkYg6MlQ&s"),
            Triple("Single room", 1000.0..1200.0, "https://resources.pamgolding.co.za/content/properties/202506/2195447/h/2195447_H_18.jpg?w=1000"),
            Triple("Basic Sharing Room", 1200.0..1400.0, "https://a0.muscache.com/im/pictures/hosting/Hosting-1539863977654761174/original/0e7e565f-803b-4b16-bcd2-67a413f1dcac.jpeg?im_w=720"),
            Triple("Two Bedroom House", 1400.0..1600.0, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSp0corD2eAKOQVs72FSUS6gSyg__ERr8cgxg&s"),
            Triple("Three Bedroom House", 1600.0..1700.0, "https://images.prop24.com/377076181/Crop600x400"),
            Triple("Studio Pad", 1700.0..1800.0, "https://cf.bstatic.com/xdata/images/hotel/max1024x768/482940912.jpg?k=e10545f42ab012d22e0a8e2ab2587368edc88ec5c738cb7912c956c2717311c6&o="),
            Triple("Apartment", 1800.0..1900.0, "https://www.thegazette.news/wp-content/uploads/2022/07/MG_0073.jpeg"),
            Triple("Stand Alone House", 1900.0..2000.0, "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQaXa1SX19HkYJirpaed5vYADwXpjW5sKMdVA&s")
        )

        val baseDate = LocalDate.now()

        for (i in 0 until 80) {
            val data = houseData[i % houseData.size]
            val type = data.first
            val range = data.second
            val imageUrl = data.third
            
            val step = (range.endInclusive - range.start) / 5
            val rawPrice = range.start + (i % 5) * step
            val price = (round(rawPrice / 100.0) * 100.0).coerceIn(800.0, 2000.0)
            val location = areas[i % areas.size]
            
            val amenities = if (price in 800.0..1100.0) "Water, Electricity" else "WiFi, Water, Electricity"
            
            // Cycle dates every 10 items to ensure variety and density
            // i % 10 = 2 results in Today's date
            val generatedDate = baseDate.plusDays((i % 10 - 2).toLong())
            
            listings.add(
                Listing(
                    title = "$type in $location",
                    price = price,
                    location = location,
                    type = type,
                    amenities = amenities,
                    availabilityDate = generatedDate.toString(),
                    depositAmount = round((price * 0.5) / 100.0) * 100.0,
                    imageUrl = imageUrl,
                    distanceToCampus = (0.5 + (i % 5) * 0.7),
                    status = "Available"
                )
            )
        }
        db.listingDao().insertAll(listings)
    }
}
