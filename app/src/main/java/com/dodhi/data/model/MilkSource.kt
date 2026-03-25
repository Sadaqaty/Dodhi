package com.dodhi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_sources")
data class MilkSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmerName: String,
    val date: Long,
    val quantity: Double,
    val rate: Double,
    val totalAmount: Double
)
