package com.example.stay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val price: Double,
    val location: String,
    val type: String,
    val amenities: String,
    val availabilityDate: String,
    val depositAmount: Double,
    val imageUrl: String,
    val distanceToCampus: Double = 0.0, // Extension feature: Distance in km
    val status: String = "Available", // Available, Reserved
    val ownerEmail: String? = null,
    val reservedByEmail: String? = null
)
