package com.dodhi.data.model

data class BackupData(
    val customers: List<Customer>?,
    val transactions: List<DeliveryRecord>?,
    val payments: List<Payment>? = emptyList(),
    val milkSources: List<MilkSource>? = emptyList(),
    val timestamp: Long
)
