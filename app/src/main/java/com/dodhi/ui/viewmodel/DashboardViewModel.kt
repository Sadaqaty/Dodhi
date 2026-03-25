package com.dodhi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.os.Environment
import com.dodhi.data.DodhiDatabase
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.text.SimpleDateFormat

import com.dodhi.data.model.Payment
import com.dodhi.data.model.MilkSource

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DodhiDatabase.getDatabase(application)
    private val dao = db.dodhiDao()

    val customers: StateFlow<List<Customer>> = dao.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCustomer(id: Long): Flow<Customer?> = dao.getCustomerById(id)
    
    fun getRecordsForCustomer(id: Long): Flow<List<DeliveryRecord>> = dao.getRecordsForCustomer(id)
    
    fun getPaymentsForCustomer(id: Long): Flow<List<Payment>> = dao.getPaymentsForCustomer(id)

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _selectedShift = MutableStateFlow("Morning") // Morning or Evening
    val selectedShift: StateFlow<String> = _selectedShift.asStateFlow()

    fun setShift(shift: String) {
        _selectedShift.value = shift
    }

    val dailyRecords: StateFlow<List<DeliveryRecord>> = combine(selectedDate, selectedShift) { date, shift ->
        val start = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val end = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        // We filter by shift in the ViewModel for now, or update DAO to filter by shift
        dao.getRecordsInPeriod(start, end).first().filter { it.shift == shift }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersByLocality: Flow<Map<String, List<Customer>>> = customers.map { list ->
        list.groupBy { it.locality }.toSortedMap()
    }

    fun getShiftProgress(shift: String): Flow<ShiftProgress> {
        val date = (_selectedDate.value.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return combine(customers, dao.getRecordsInPeriod(date, date + 86400000)) { customers: List<Customer>, records: List<DeliveryRecord> ->
            val shiftRecords = records.filter { it.shift == shift }
            val deliveredLiters = shiftRecords.filter { it.type == "Delivered" }.sumOf { it.quantity }
            val expectedLiters = customers.sumOf { c: Customer -> if (shift == "Morning") c.morningReq else c.eveningReq }
            
            ShiftProgress(deliveredLiters, expectedLiters)
        }
    }

    data class ShiftProgress(val delivered: Double, val expected: Double)


    fun setSelectedDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    val todayLitersNeeded: StateFlow<Double> = customers.map { list ->
        list.sumOf { it.defaultQuantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayLitersDelivered: StateFlow<Double> = dailyRecords.map { list ->
        list.filter { it.type == "Delivered" || it.type == "Extra" }.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDeliveriesCount: StateFlow<Int> = dailyRecords.map { list ->
        list.filter { it.type == "Delivered" || it.type == "Extra" }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getCustomerBalance(customerId: Long): Flow<Double> {
        return combine(
            dao.getRecordsForCustomer(customerId),
            dao.getPaymentsForCustomer(customerId)
        ) { records, payments ->
            records.sumOf { it.amount } - payments.sumOf { it.amount }
        }
    }

    fun getMonthlyTotal(customerId: Long): Flow<Double> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        val end = calendar.timeInMillis
        
        return dao.getRecordsInPeriod(start, end).map { list ->
            list.filter { it.customerId == customerId }.sumOf { it.amount }
        }
    }

    fun generateParchiText(customerId: Long): Flow<String> {
        return combine(
            getCustomer(customerId),
            getMonthlyTotal(customerId),
            getCustomerBalance(customerId)
        ) { customer, totalMonth, balance ->
            if (customer == null) return@combine ""
            
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("ur"))
            val month = sdf.format(Date())
            
            """
            *حساب پرچی (Hisaab Parchi)*
            --------------------------
            *گاہک:* ${customer.name}
            *مہینہ:* $month
            
            *اس مہینے کا بل:* $totalMonth PKR
            *پچھلا بقایا:* ${balance - totalMonth} PKR
            *کل واجب الادا:* $balance PKR
            --------------------------
            *دودهی ایپ (Dodhi App)*
            """.trimIndent()
        }
    }

    fun getDailyVolumeInRange(): Flow<List<DayVolume>> {
        val start = getStartOfCurrentMonth()
        val end = Calendar.getInstance().timeInMillis
        return dao.getRecordsInPeriod(start, end).map { records ->
            records.groupBy { it.date }
                .map { (date, dailyRecs) ->
                    DayVolume(date, dailyRecs.filter { it.type != "Naga" }.sumOf { it.quantity })
                }.sortedBy { it.date }
        }
    }

    data class DayVolume(val date: Long, val volume: Double)

    fun getCollectionSummary(): Flow<CollectionSummary> {
        val start = getStartOfCurrentMonth()
        val end = Calendar.getInstance().timeInMillis
        
        return combine(
            dao.getRecordsInPeriod(start, end),
            dao.getPaymentsForCustomerInRange(start, end), // Need to add this to DAO
            dao.getMilkSourcesInPeriod(start, end)
        ) { records, payments, sources ->
            val marketValue = records.filter { it.type != "Naga" }.sumOf { it.amount }
            val cashCollected = payments.sumOf { it.amount }
            val totalSourced = sources.sumOf { it.quantity }
            val totalSold = records.filter { it.type != "Naga" }.sumOf { it.quantity }
            
            CollectionSummary(
                marketValue = marketValue,
                cashCollected = cashCollected,
                outstanding = marketValue - cashCollected, // This is simplified for the month
                waste = totalSourced - totalSold
            )
        }
    }

    data class CollectionSummary(
        val marketValue: Double,
        val cashCollected: Double,
        val outstanding: Double,
        val waste: Double
    )

    private fun getStartOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    fun addPayment(customerId: Long, amount: Double) {
        viewModelScope.launch {
            dao.insertPayment(Payment(customerId = customerId, date = System.currentTimeMillis(), amount = amount))
        }
    }

    fun addMilkSource(name: String, quantity: Double, rate: Double) {
        viewModelScope.launch {
            dao.insertMilkSource(MilkSource(farmerName = name, date = System.currentTimeMillis(), quantity = quantity, rate = rate, totalAmount = quantity * rate))
        }
    }

    fun addCustomer(name: String, rate: Double, quantity: Double, unit: String = "Liter", locality: String = "") {
        viewModelScope.launch {
            dao.insertCustomer(Customer(
                name = name, 
                rate = rate, 
                defaultQuantity = quantity, 
                unit = unit,
                locality = locality,
                morningReq = quantity // Default to morning
            ))
        }
    }

    fun updateCustomerSettings(customer: Customer, morning: Double, evening: Double, rate: Double?) {
        viewModelScope.launch {
            dao.insertCustomer(customer.copy(morningReq = morning, eveningReq = evening, customRate = rate))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            dao.deleteCustomer(customer)
        }
    }

    fun markDelivered(customer: Customer, type: String, quantity: Double) {
        markDeliveredWithDate(customer, type, quantity, _selectedDate.value, _selectedShift.value)
    }

    fun markDeliveredWithDate(customer: Customer, type: String, quantity: Double, calendar: Calendar, shift: String) {
        viewModelScope.launch {
            val dateCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val date = dateCal.timeInMillis
            
            val allRecords = dao.getRecordsInPeriod(date, date + 86400000).first()
            val existing = allRecords.find { it.customerId == customer.id && it.shift == shift }
            
            var targetQuantity = quantity
            var targetType = type
            var isExtra = false
            
            if (type == "Extra" && existing != null && existing.type != "Naga") {
                targetQuantity = existing.quantity + quantity
                targetType = "Delivered"
                isExtra = true
            } else if (type == "Naga") {
                targetQuantity = 0.0
            }

            val record = DeliveryRecord(
                id = existing?.id ?: 0,
                customerId = customer.id,
                date = date,
                quantity = targetQuantity,
                type = targetType,
                shift = shift,
                isExtra = isExtra,
                amount = targetQuantity * (customer.customRate ?: customer.rate)
            )
            dao.insertRecord(record)
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val customers = dao.getAllCustomers().first()
            val csvBuilder = StringBuilder()
            csvBuilder.append("Name,Rate,DefaultQuantity\n")
            customers.forEach {
                csvBuilder.append("${it.name},${it.rate},${it.defaultQuantity}\n")
            }
            
            val fileName = "dodhi_khata_backup_${System.currentTimeMillis()}.csv"
            try {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                file.writeText(csvBuilder.toString())
                // In a real app, you'd use FileProvider to share this via Intent
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
