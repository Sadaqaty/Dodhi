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

    val dailyRecords: StateFlow<List<DeliveryRecord>> = selectedDate.flatMapLatest { date ->
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
        dao.getRecordsInPeriod(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    suspend fun generateParchiText(customerId: Long): String {
        val customer = dao.getCustomerById(customerId).firstOrNull() ?: return ""
        val startOfMonth = getStartOfCurrentMonth()
        val endOfMonth = Calendar.getInstance().timeInMillis
        // We use first() here because we want a snapshot for the parchi
        val records = dao.getRecordsInPeriod(startOfMonth, endOfMonth).first()
            .filter { it.customerId == customerId }
        
        val total = records.filter { it.type != "Naga" }.sumOf { it.amount }
        val liters = records.filter { it.type != "Naga" }.sumOf { it.quantity }
        
        val sb = StringBuilder()
        sb.append("*-- ڈیجیٹل پرچی (Dodhi App) --*\n\n")
        sb.append("گاہک: ${customer.name}\n")
        sb.append("ریٹ: ${customer.rate}\n")
        sb.append("کل مقدار: $liters لیٹر\n")
        sb.append("کل رقم: $total روپے\n")
        sb.append("-----------------------------\n")
        sb.append("تاریخ: ${SimpleDateFormat("dd-MM-yyyy").format(Date())}")
        return sb.toString()
    }

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

    fun addCustomer(name: String, rate: Double, quantity: Double, unit: String = "Liter") {
        viewModelScope.launch {
            dao.insertCustomer(Customer(name = name, rate = rate, defaultQuantity = quantity, unit = unit))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            dao.deleteCustomer(customer)
        }
    }

    fun markDelivered(customer: Customer, type: String, quantity: Double) {
        viewModelScope.launch {
            val calendar = (_selectedDate.value.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val date = calendar.timeInMillis
            
            // For Delivered/Naga, we replace any existing record for that day
            // For Extra, we might want to handle it differently, but for now we'll also replace
            // to keep the UI predictable.
            val existing = dailyRecords.value.find { it.customerId == customer.id }
            
            var targetQuantity = quantity
            var targetType = type
            
            if (type == "Extra" && existing != null && existing.type != "Naga") {
                // If adding extra to an existing delivery, we add the quantities
                targetQuantity = existing.quantity + quantity
                targetType = "Delivered" // Still a delivery, just with extra
            } else if (type == "Naga") {
                targetQuantity = 0.0
            }

            val record = DeliveryRecord(
                id = existing?.id ?: 0,
                customerId = customer.id,
                date = date,
                quantity = targetQuantity,
                type = targetType,
                amount = targetQuantity * customer.rate
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
