package com.dodhi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val defaultQuantity: Double,
    val rate: Double,
    val unit: String = "Liter",
    val locality: String = "", // Street/Mohalla
    val morningReq: Double = 0.0, // Auto-fill quantity for morning
    val eveningReq: Double = 0.0, // Auto-fill quantity for evening
    val customRate: Double? = null, // Specific rate for this customer
    val peshgi: Double = 0.0,
    val udhaar: Double = 0.0,
    val isProvider: Boolean = false // true if Dodhi buys milk from this home
)
