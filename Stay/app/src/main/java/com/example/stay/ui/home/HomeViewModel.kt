package com.example.stay.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stay.data.local.AppDatabase
import com.example.stay.data.local.entity.Favorite
import com.example.stay.data.local.entity.Listing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListingItem(
    val listing: Listing,
    val isFavorite: Boolean
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val listingDao = AppDatabase.getDatabase(application).listingDao()
    private val sharedPrefs = application.getSharedPreferences("StayPrefs", Context.MODE_PRIVATE)

    private val _minPrice = MutableStateFlow(sharedPrefs.getFloat("filter_minPrice", 800.0f).toDouble())
    private val _maxPrice = MutableStateFlow(sharedPrefs.getFloat("filter_maxPrice", 2000.0f).toDouble())
    private val _location = MutableStateFlow(sharedPrefs.getString("filter_location", "") ?: "")
    private val _date = MutableStateFlow(sharedPrefs.getString("filter_date", "") ?: "")
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    enum class FilterMode {
        ALL, FAVORITES, MY_LISTINGS, MY_RESERVATIONS
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val listings: StateFlow<List<ListingItem>> = combine(
        _minPrice, _maxPrice, _location, _date, _filterMode, _refreshTrigger
    ) { args: Array<*> ->
        val min = args[0] as Double
        val max = args[1] as Double
        val loc = args[2] as String
        val date = args[3] as String
        val mode = args[4] as FilterMode

        val email = sharedPrefs.getString("userEmail", "") ?: ""
        
        val baseFlow: Flow<List<Listing>> = when (mode) {
            FilterMode.FAVORITES -> listingDao.getFavoriteListings(email)
            FilterMode.MY_LISTINGS -> listingDao.getListingsByOwner(email)
            FilterMode.MY_RESERVATIONS -> listingDao.getListingsByResident(email)
            FilterMode.ALL -> listingDao.getAllListings()
        }
        
        baseFlow.map { list ->
            list.filter { listing ->
                val matchesPrice = if (min == max) {
                    listing.price == min
                } else {
                    listing.price in min..max
                }
                val matchesLocation = loc.isEmpty() || 
                                     listing.location.lowercase().contains(loc.lowercase()) || 
                                     listing.title.lowercase().contains(loc.lowercase())
                val matchesDate = date.isEmpty() || listing.availabilityDate >= date
                
                // For Students (ALL mode), only show Available houses.
                // For Providers (MY_LISTINGS), show everything they own.
                val matchesStatus = if (mode == FilterMode.ALL) {
                    listing.status.equals("Available", ignoreCase = true)
                } else {
                    true
                }
                
                matchesPrice && matchesLocation && matchesDate && matchesStatus
            }
        }.flatMapLatest { filteredList ->
            listingDao.getFavoriteIds(email).map { favIds ->
                filteredList.map { ListingItem(it, favIds.contains(it.id)) }
            }
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateFilter(minPrice: Double, maxPrice: Double, location: String, date: String, savePrefs: Boolean = false) {
        _minPrice.value = minPrice
        _maxPrice.value = maxPrice
        _location.value = location
        _date.value = date

        if (savePrefs) {
            sharedPrefs.edit().apply {
                putFloat("filter_minPrice", minPrice.toFloat())
                putFloat("filter_maxPrice", maxPrice.toFloat())
                putString("filter_location", location)
                putString("filter_date", date)
                apply()
            }
        }
    }

    fun showFavorites() {
        _filterMode.value = FilterMode.FAVORITES
    }

    fun showMyListings() {
        _filterMode.value = FilterMode.MY_LISTINGS
    }

    fun showMyReservations() {
        _filterMode.value = FilterMode.MY_RESERVATIONS
    }

    private fun resetFilters() {
        _minPrice.value = 800.0
        _maxPrice.value = 2000.0
        _location.value = ""
        _date.value = ""
        
        sharedPrefs.edit().apply {
            remove("filter_minPrice")
            remove("filter_maxPrice")
            remove("filter_location")
            remove("filter_date")
            apply()
        }
    }

    fun toggleFavorite(listingId: Long) {
        val email = sharedPrefs.getString("userEmail", "") ?: ""
        viewModelScope.launch {
            val currentFavs = listingDao.getFavoriteIds(email).first()
            if (currentFavs.contains(listingId)) {
                listingDao.deleteFavorite(email, listingId)
            } else {
                listingDao.insertFavorite(Favorite(email, listingId))
            }
        }
    }
    
    fun refresh() {
        _filterMode.value = FilterMode.ALL
        resetFilters()
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
