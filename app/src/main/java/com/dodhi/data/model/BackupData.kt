package com.dodhi.data.model

data class BackupData(
    val customers: List<Customer>?,
    val transactions: List<DeliveryRecord>?,
    val payments: List<Payment>? = emptyList(),
    val milkSources: List<MilkSource>? = emptyList(),
    val timestamp: Long,
    val version: Int = 1
) {
    fun validate(): String? {
        if (customers.isNullOrEmpty()) return "No customers found in backup"
        if (customers.any { it.name.isBlank() }) return "Found customer with empty name"
        if (customers.any { it.rate < 0 }) return "Found negative rate for customer: ${customers.first { it.rate < 0 }.name}"
        if (customers.any { it.defaultQuantity < 0 }) return "Found negative quantity for customer: ${customers.first { it.defaultQuantity < 0 }.name}"

        val customerIds = customers.map { it.id }.toSet()
        transactions?.forEach { record ->
            if (record.customerId !in customerIds) return "Record references unknown customer ID: ${record.customerId}"
            if (record.quantity < 0) return "Found negative quantity in record for customer ID: ${record.customerId}"
            if (record.amount < 0) return "Found negative amount in record for customer ID: ${record.customerId}"
        }
        payments?.forEach { payment ->
            if (payment.customerId !in customerIds) return "Payment references unknown customer ID: ${payment.customerId}"
            if (payment.amount < 0) return "Found negative payment amount for customer ID: ${payment.customerId}"
        }
        return null
    }

    fun withResetIds(): BackupData {
        return BackupData(
            customers = customers?.map { it.copy(id = 0) },
            transactions = transactions?.map { it.copy(id = 0, customerId = 0) },
            payments = payments?.map { it.copy(id = 0, customerId = 0) },
            milkSources = milkSources?.map { it.copy(id = 0) },
            timestamp = timestamp,
            version = version
        )
    }
}
