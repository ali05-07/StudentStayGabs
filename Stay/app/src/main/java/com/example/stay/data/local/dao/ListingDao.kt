package com.example.stay.data.local.dao

import androidx.room.*
import com.example.stay.data.local.entity.Favorite
import com.example.stay.data.local.entity.Listing
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listings: List<Listing>)

    @Update
    suspend fun update(listing: Listing)

    @Query("SELECT * FROM listings")
    fun getAllListings(): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE id = :id")
    suspend fun getListingById(id: Long): Listing?

    @Query("""
        SELECT * FROM listings 
        WHERE price = :maxPrice 
        AND (LOWER(title) LIKE '%' || LOWER(:location) || '%' OR LOWER(location) LIKE '%' || LOWER(:location) || '%')
        AND availabilityDate = :date
        AND status = 'Available'
    """)
    fun filterListings(maxPrice: Double, location: String, date: String): Flow<List<Listing>>

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun getListingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Query("DELETE FROM user_favorites WHERE userEmail = :userEmail AND listingId = :listingId")
    suspend fun deleteFavorite(userEmail: String, listingId: Long)

    @Query("SELECT listingId FROM user_favorites WHERE userEmail = :userEmail")
    fun getFavoriteIds(userEmail: String): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE userEmail = :userEmail AND listingId = :listingId)")
    suspend fun isFavoriteExists(userEmail: String, listingId: Long): Boolean

    @Query("""
        SELECT listings.* FROM listings 
        INNER JOIN user_favorites ON listings.id = user_favorites.listingId 
        WHERE user_favorites.userEmail = :userEmail
    """)
    fun getFavoriteListings(userEmail: String): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE ownerEmail = :email")
    fun getListingsByOwner(email: String): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE reservedByEmail = :email")
    fun getListingsByResident(email: String): Flow<List<Listing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listing: Listing): Long
}
