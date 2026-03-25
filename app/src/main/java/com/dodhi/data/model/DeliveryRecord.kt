package com.dodhi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_records")
data class DeliveryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: Long, // timestamp
    val quantity: Double,
    val type: String, // Delivered, Extra, Naga
    val amount: Double
)
