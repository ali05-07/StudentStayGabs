package com.example.stay.data.local.entity

import androidx.room.Entity

@Entity(tableName = "user_favorites", primaryKeys = ["userEmail", "listingId"])
data class Favorite(
    val userEmail: String,
    val listingId: Long
)
