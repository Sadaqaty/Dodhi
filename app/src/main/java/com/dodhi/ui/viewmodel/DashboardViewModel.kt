package com.dodhi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.os.Environment
import com.dodhi.data.DodhiDatabase
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.BackupData
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.dodhi.util.PdfManager
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DodhiDatabase.getDatabase(application)
    private val dao = db.dodhiDao()

    private val prefs = application.getSharedPreferences("dodhi_prefs", Context.MODE_PRIVATE)
    
    private val _milkmanName = MutableStateFlow(prefs.getString("milkman_name", "Dodhi Village") ?: "Dodhi Village")
    val milkmanName: StateFlow<String> = _milkmanName.asStateFlow()

    private val _isMusicEnabled = MutableStateFlow(prefs.getBoolean("music_enabled", true))
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    fun updateMilkmanName(name: String) {
        _milkmanName.value = name
        prefs.edit().putString("milkman_name", name).apply()
    }

    fun toggleMusic(enabled: Boolean) {
        _isMusicEnabled.value = enabled
        prefs.edit().putBoolean("music_enabled", enabled).apply()
    }

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
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 86400000 // Exclusive end with DAO '< :end'
        dao.getRecordsInPeriod(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersByLocality: Flow<Map<String, List<Customer>>> = customers.map { list ->
        list.groupBy { it.locality }.toSortedMap()
    }

    fun getDailyProgress(): Flow<Progress> {
        val date = (_selectedDate.value.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        return combine(customers, dao.getRecordsInPeriod(date, date + 86400000)) { customers: List<Customer>, records: List<DeliveryRecord> ->
            val deliveredLiters = records.filter { it.type == "Delivered" || it.type == "Extra" }.sumOf { it.quantity }
            val expectedLiters = customers.sumOf { it.defaultQuantity }
            
            Progress(deliveredLiters, expectedLiters)
        }
    }

    data class Progress(val delivered: Double, val expected: Double)


    fun setSelectedDate(calendar: Calendar) {
        _selectedDate.value = calendar
    }

    fun refreshToToday() {
        _selectedDate.value = Calendar.getInstance()
    }

    val todayLitersNeeded: StateFlow<Double> = customers.map { list ->
        list.sumOf { it.defaultQuantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayTotalCollected: StateFlow<Double> = selectedDate.flatMapLatest { date ->
        val start = (date.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val end = start + 86400000
        dao.getRecordsInPeriod(start, end).map { records ->
            records.filter { it.type == "Delivered" || it.type == "Extra" }.sumOf { it.quantity }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayLitersDelivered: StateFlow<Double> = dailyRecords.map { list ->
        list.filter { it.type == "Delivered" || it.type == "Extra" }.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayDeliveriesCount: StateFlow<Int> = dailyRecords.map { list ->
        list.filter { it.type == "Delivered" || it.type == "Extra" }.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getCustomerBalance(customerId: Long): Flow<Double> {
        return combine(
            dao.getCustomerById(customerId),
            dao.getRecordsForCustomer(customerId),
            dao.getPaymentsForCustomer(customerId)
        ) { customer, records, payments ->
            if (customer == null) return@combine 0.0
            val recordSum = records.sumOf { it.amount }
            val paymentSum = payments.sumOf { it.amount }
            
            // Core Balance Logic: 
            // records.amount is the financial value of milk (Quantity * Rate at that time)
            // payments.amount is actual cash exchange
            
            if (customer.isProvider) {
                // SELLER: We buy milk. recordSum is what we owe them for milk.
                // paymentSum is what we already paid them.
                // Balance > 0 means we still owe them.
                recordSum - paymentSum
            } else {
                // BUYER: We sell milk. recordSum is what they owe us for milk.
                // paymentSum is what they already paid us.
                // Balance > 0 means they still owe us.
                recordSum - paymentSum
            }
        }
    }

    /** Total liters of milk delivered/received for this customer (all time, excludes Naga). */
    fun getTotalLiters(customerId: Long): Flow<Double> {
        return dao.getRecordsForCustomer(customerId).map { records ->
            records.filter { it.type != "Naga" }.sumOf { it.quantity }
        }
    }

    /** Total milk volume (Liters) for the current month. */
    fun getMonthlyLiters(customerId: Long): Flow<Double> {
        val (start, end) = MonthYear(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)).toCalendarRange()
        return dao.getRecordsInPeriod(start, end).map { list ->
            list.filter { it.customerId == customerId && it.type != "Naga" }.sumOf { it.quantity }
        }
    }

    /** Total financial bill (PKR) for the current month. */
    fun getMonthlyBill(customerId: Long): Flow<Double> {
        val (start, end) = MonthYear(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)).toCalendarRange()
        return dao.getRecordsInPeriod(start, end).map { list ->
            list.filter { it.customerId == customerId }.sumOf { it.amount }
        }
    }

    /** Data class representing a month/year key for reports */
    data class MonthYear(val year: Int, val month: Int) : Comparable<MonthYear> {
        override fun compareTo(other: MonthYear): Int {
            return if (year != other.year) year.compareTo(other.year)
            else month.compareTo(other.month)
        }
        fun toCalendarRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val end = cal.timeInMillis
            return Pair(start, end)
        }
    }

    /** Returns a sorted (newest first) list of all months that have delivery records. */
    fun getAvailableMonths(): Flow<List<MonthYear>> {
        return dao.getAllRecords().map { records ->
            records.map { record ->
                val cal = Calendar.getInstance().apply { timeInMillis = record.date }
                MonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            }.distinct().sortedDescending()
        }
    }

    data class MonthSummary(
        val monthYear: MonthYear,
        val totalLiters: Double,
        val totalAmount: Double,
        val totalPaid: Double,
        val balance: Double,
        val customerCount: Int,
        val nagaCount: Int
    )

    /** Summary of all deliveries and payments for a specific month */
    fun getMonthSummary(monthYear: MonthYear): Flow<MonthSummary> {
        val (start, end) = monthYear.toCalendarRange()
        return combine(
            dao.getRecordsInPeriod(start, end),
            dao.getPaymentsForCustomerInRange(start, end)
        ) { records, payments ->
            val delivered = records.filter { it.type != "Naga" }
            val nagaCount = records.count { it.type == "Naga" }
            val totalAmount = delivered.sumOf { it.amount }
            val totalPaid = payments.sumOf { it.amount }
            MonthSummary(
                monthYear = monthYear,
                totalLiters = delivered.sumOf { it.quantity },
                totalAmount = totalAmount,
                totalPaid = totalPaid,
                balance = totalAmount - totalPaid,
                customerCount = delivered.map { it.customerId }.distinct().size,
                nagaCount = nagaCount
            )
        }
    }

    /** All records for a customer within a specific month */
    fun getCustomerRecordsForMonth(customerId: Long, monthYear: MonthYear): Flow<List<DeliveryRecord>> {
        val (start, end) = monthYear.toCalendarRange()
        return dao.getRecordsInPeriod(start, end).map { records ->
            records.filter { it.customerId == customerId }
        }
    }

    fun generateParchiText(customerId: Long): Flow<String> {
        return combine(
            getCustomer(customerId),
            getMonthlyBill(customerId),
            getCustomerBalance(customerId)
        ) { customer, totalMonth, currentBalance ->
            if (customer == null) return@combine ""
            
            val totalMonthVal = totalMonth
            val balanceVal = currentBalance
            
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("ur"))
            val month = sdf.format(Date())
            
            """
            *حساب پرچی (Hisaab Parchi)*
            --------------------------
            *گاہک:* ${customer.name}
            *مہینہ:* $month
            
            *اس مہینے کا بل:* ${totalMonthVal.toInt()} روپے
            *پچھلا بقایا:* ${(balanceVal - totalMonthVal).toInt()} روپے
            *کل واجب الادا:* ${balanceVal.toInt()} روپے
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
            customers,
            dao.getRecordsInPeriod(start, end),
            dao.getPaymentsForCustomerInRange(start, end),
        ) { customerList, records, payments ->
            val providerIds = customerList.filter { it.isProvider }.map { it.id }.toSet()

            val consumerRecords = records.filter { it.customerId !in providerIds && it.type != "Naga" }
            val providerRecords = records.filter { it.customerId in providerIds && it.type != "Naga" }

            val sales = consumerRecords.sumOf { it.amount }
            val purchases = providerRecords.sumOf { it.amount }

            val collected = payments.filter { it.customerId !in providerIds }.sumOf { it.amount }
            val paid = payments.filter { it.customerId in providerIds }.sumOf { it.amount }

            val totalLiters = consumerRecords.sumOf { it.quantity }
            val nagaCount = records.count { it.type == "Naga" }
            val activeCustomerIds = records.filter { it.type != "Naga" }.map { it.customerId }.distinct()
            val activeCustomers = activeCustomerIds.size

            CollectionSummary(
                totalSales = sales,
                cashCollected = collected,
                toCollect = sales - collected,
                totalPurchases = purchases,
                cashPaid = paid,
                toPay = purchases - paid,
                netProfit = sales - purchases,
                totalLiters = totalLiters,
                activeCustomers = activeCustomers,
                nagaCount = nagaCount
            )
        }
    }

    data class CollectionSummary(
        val totalSales: Double,
        val cashCollected: Double,
        val toCollect: Double,
        val totalPurchases: Double,
        val cashPaid: Double,
        val toPay: Double,
        val netProfit: Double,
        val totalLiters: Double,
        val activeCustomers: Int,
        val nagaCount: Int
    )

    private fun getStartOfCurrentMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    fun getRecordsInPeriodAsFlow(start: Long, end: Long): Flow<List<DeliveryRecord>> {
        return dao.getRecordsInPeriod(start, end)
    }

    fun addPayment(customerId: Long, amount: Double, note: String = "") {
        viewModelScope.launch {
            dao.insertPayment(Payment(customerId = customerId, date = System.currentTimeMillis(), amount = amount, note = note))
        }
    }

    fun addMilkSource(name: String, quantity: Double, rate: Double) {
        viewModelScope.launch {
            dao.insertMilkSource(MilkSource(farmerName = name, date = System.currentTimeMillis(), quantity = quantity, rate = rate, totalAmount = quantity * rate))
        }
    }

    fun addCustomer(name: String, rate: Double, quantity: Double, unit: String = "Liter", locality: String = "", isProvider: Boolean = false) {
        viewModelScope.launch {
            dao.insertCustomer(Customer(
                name = name, 
                rate = rate, 
                defaultQuantity = quantity, 
                unit = unit,
                locality = locality,
                isProvider = isProvider
            ))
        }
    }

    fun updateCustomerSettings(customer: Customer, quantity: Double, rate: Double?) {
        viewModelScope.launch {
            // When updating customer settings, we only update the BASE rate.
            // Future records will use this new rate.
            dao.insertCustomer(customer.copy(defaultQuantity = quantity, customRate = rate))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            dao.deleteCustomer(customer)
        }
    }

    fun markDelivered(customer: Customer, type: String, quantity: Double) {
        markDeliveredWithDate(customer, type, quantity, _selectedDate.value)
    }

    fun markDeliveredWithDate(customer: Customer, type: String, quantity: Double, calendar: Calendar) {
        viewModelScope.launch {
            val dateCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val date = dateCal.timeInMillis
            
            // USE PRECISE SINGLE-RECORD QUERY
            val existing = dao.getRecordForCustomerOnDate(customer.id, date)
            
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

            // RATE HANDLING:
            // We capture the rate AT THE TIME of the record.
            // If the Dodhi changes his global rate tomorrow, old records remain unaffected
            // because their amount is already pre-calculated and stored.
            val targetRate = if (type == "Extra" && existing != null) {
                if (existing.rate > 0) existing.rate else (customer.customRate ?: customer.rate)
            } else {
                customer.customRate ?: customer.rate
            }

            val record = DeliveryRecord(
                id = existing?.id ?: 0,
                customerId = customer.id,
                date = date,
                quantity = targetQuantity,
                type = targetType,
                isExtra = isExtra,
                amount = targetQuantity * targetRate,
                rate = targetRate
            )
            dao.insertRecord(record)
        }
    }

    fun exportToCsv(context: Context) {
        // Removed — superseded by JSON backup/restore
    }

    fun exportDataBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.forceCheckpoint()
                val customersList = dao.getAllCustomersSync()
                val recordsList = dao.getAllRecordsSync()
                val paymentsList = dao.getAllPaymentsSync()
                val milkSourcesList = dao.getAllMilkSourcesSync()

                if (customersList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "No data to export", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val backupData = BackupData(
                    customers = customersList,
                    transactions = recordsList,
                    payments = paymentsList,
                    milkSources = milkSourcesList,
                    timestamp = System.currentTimeMillis(),
                    version = 1
                )
                val gson = GsonBuilder().setPrettyPrinting().create()
                val jsonString = gson.toJson(backupData)

                val backupDir = File(context.cacheDir, "backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                // Clean up old backup files (keep last 3)
                backupDir.listFiles()?.filter { it.name.startsWith("dodhi_backup_") && it.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(3)
                    ?.forEach { it.delete() }

                val backupFile = File(backupDir, "dodhi_backup_${System.currentTimeMillis()}.json")
                backupFile.writeText(jsonString)

                val uri = FileProvider.getUriForFile(
                    context.applicationContext,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Export Data Backup")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun previewImportBackup(context: Context, uri: Uri, onResult: (BackupData?, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) { onResult(null, "Unable to open backup file") }
                    return@launch
                }

                val jsonString = inputStream.bufferedReader().use { it.readText() }

                // Size guard: reject files over 10MB
                if (jsonString.length > 10 * 1024 * 1024) {
                    withContext(Dispatchers.Main) { onResult(null, "Backup file too large") }
                    return@launch
                }

                val gson = GsonBuilder().create()
                val backupData = try {
                    gson.fromJson(jsonString, BackupData::class.java)
                } catch (e: Exception) {
                    null
                }

                if (backupData == null) {
                    withContext(Dispatchers.Main) { onResult(null, "Unable to read backup file — it may be corrupted") }
                    return@launch
                }

                val error = backupData.validate()
                if (error != null) {
                    withContext(Dispatchers.Main) { onResult(null, error) }
                    return@launch
                }

                withContext(Dispatchers.Main) { onResult(backupData, null) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null, "Error reading backup: ${e.localizedMessage}") }
            }
        }
    }

    fun executeImportBackup(context: Context, backupData: BackupData, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Reset all IDs to 0 so Room auto-generates new ones — avoids ID collisions
                val cleanData = backupData.withResetIds()

                dao.importBackup(
                    customersList = cleanData.customers ?: emptyList(),
                    recordsList = cleanData.transactions ?: emptyList(),
                    paymentsList = cleanData.payments ?: emptyList(),
                    milkSourcesList = cleanData.milkSources ?: emptyList()
                )

                db.forceCheckpoint()

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("Import failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun sharePdfReport(context: Context, customer: Customer, isUrdu: Boolean) {
        viewModelScope.launch {
            val records = dao.getRecordsForCustomerSync(customer.id)
            val payments = dao.getPaymentsForCustomerSync(customer.id)
            val month = Calendar.getInstance()
            val recordsInMonth = records.filter { isSameMonth(it.date, month) }
            val paymentsInMonth = payments.filter { isSameMonth(it.date, month) }
            val currentNet = recordsInMonth.sumOf { it.amount } - paymentsInMonth.sumOf { it.amount }
            val totalBalance = records.sumOf { it.amount } - payments.sumOf { it.amount }
            val previousBalance = totalBalance - currentNet

            val pdfManager = PdfManager(context)
            val file = pdfManager.generateCustomerReport(
                milkmanName = milkmanName.value,
                customer = customer,
                records = records,
                payments = payments,
                month = month,
                isUrdu = isUrdu,
                previousBalance = previousBalance
            )
            
            if (file != null) {
                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        context.applicationContext,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, if (isUrdu) "رپورٹ شیئر کریں" else "Share Report")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun shareTextReport(context: Context, customer: Customer, isUrdu: Boolean) {
        viewModelScope.launch {
            val records = dao.getRecordsForCustomerSync(customer.id)
            val payments = dao.getPaymentsForCustomerSync(customer.id)
            val month = Calendar.getInstance()
            
            val reportTitle = if (isUrdu) "\n*ماہانہ ہساب - ${customer.name}*\n" else "\n*Monthly Ledger - ${customer.name}*\n"
            val sb = StringBuilder(reportTitle)
            
            val dateFormat = SimpleDateFormat("dd/MM")
            records.filter { isSameMonth(it.date, month) }.forEach {
                val label = when(it.type) {
                    "Delivered" -> if (isUrdu) "ڈیلیوری" else "Delivered"
                    "Extra" -> if (isUrdu) "اضافی" else "Extra"
                    else -> it.type
                }
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.quantity}L - ${it.amount} Rs.\n")
            }
            
            payments.filter { isSameMonth(it.date, month) }.forEach {
                val label = if (isUrdu) "رقم وصولی" else "Payment"
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.amount} Rs.\n")
            }
            
            val currentRecords = records.filter { isSameMonth(it.date, month) }
            val currentPayments = payments.filter { isSameMonth(it.date, month) }
            val totalBill = currentRecords.sumOf { it.amount }
            val totalPaid = currentPayments.sumOf { it.amount }
            val currentNet = totalBill - totalPaid
            
            val totalBalance = records.sumOf { it.amount } - payments.sumOf { it.amount }
            val previousBalance = totalBalance - currentNet
            
            sb.append("\n------------------\n")
            if (previousBalance != 0.0) {
                sb.append(if (isUrdu) "*پچھلا بقایا:* ${previousBalance.toInt()} Rs.\n" else "*Prev. Balance:* ${previousBalance.toInt()} Rs.\n")
            }
            sb.append(if (isUrdu) "*اس مہینے کا بل:* ${totalBill.toInt()} Rs.\n" else "*Monthly Bill:* ${totalBill.toInt()} Rs.\n")
            sb.append(if (isUrdu) "*ادائیگی:* ${totalPaid.toInt()} Rs.\n" else "*Monthly Paid:* ${totalPaid.toInt()} Rs.\n")
            sb.append(if (isUrdu) "*کل واجب الادا:* ${totalBalance.toInt()} Rs.\n" else "*Total Payable:* ${totalBalance.toInt()} Rs.\n")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(intent, if (isUrdu) "ہساب شیئر کریں" else "Share Ledger"))
        }
    }

    fun shareTextReportViaWhatsApp(context: Context, customer: Customer, isUrdu: Boolean) {
        viewModelScope.launch {
            val records = dao.getRecordsForCustomerSync(customer.id)
            val payments = dao.getPaymentsForCustomerSync(customer.id)
            val month = Calendar.getInstance()
            
            val reportTitle = if (isUrdu) "\n*ماہانہ ہساب - ${customer.name}*\n" else "\n*Monthly Ledger - ${customer.name}*\n"
            val sb = java.lang.StringBuilder(reportTitle)
            
            val dateFormat = SimpleDateFormat("dd/MM")
            records.filter { isSameMonth(it.date, month) }.forEach {
                val label = when(it.type) {
                    "Delivered" -> if (isUrdu) "ڈیلیوری" else "Delivered"
                    "Extra" -> if (isUrdu) "اضافی" else "Extra"
                    else -> it.type
                }
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.quantity}L - ${it.amount} Rs.\n")
            }
            
            payments.filter { isSameMonth(it.date, month) }.forEach {
                val label = if (isUrdu) "رقم وصولی" else "Payment"
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.amount} Rs.\n")
            }
            
            val currentRecords = records.filter { isSameMonth(it.date, month) }
            val currentPayments = payments.filter { isSameMonth(it.date, month) }
            val totalBill = currentRecords.sumOf { it.amount }
            val totalPaid = currentPayments.sumOf { it.amount }
            val currentNet = totalBill - totalPaid
            
            val totalBalance = records.sumOf { it.amount } - payments.sumOf { it.amount }
            val previousBalance = totalBalance - currentNet
            
            sb.append("\n------------------\n")
            if (previousBalance != 0.0) {
                sb.append(if (isUrdu) "*پچھلا بقایا:* ${previousBalance.toInt()} Rs.\n" else "*Prev. Balance:* ${previousBalance.toInt()} Rs.\n")
            }
            sb.append(if (isUrdu) "*اس مہینے کا بل:* ${totalBill.toInt()} Rs.\n" else "*Monthly Bill:* ${totalBill.toInt()} Rs.\n")
            sb.append(if (isUrdu) "*ادائیگی:* ${totalPaid.toInt()} Rs.\n" else "*Monthly Paid:* ${totalPaid.toInt()} Rs.\n")
            sb.append(if (isUrdu) "*کل واجب الادا:* ${totalBalance.toInt()} Rs.\n" else "*Total Payable:* ${totalBalance.toInt()} Rs.\n")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val generalIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                }
                context.startActivity(Intent.createChooser(generalIntent, if (isUrdu) "ہساب شیئر کریں" else "Share Ledger"))
            }
        }
    }

    private fun isSameMonth(date1: Long, cal2: Calendar): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    /** Generate and share a PDF report for a single customer for a specific month */
    fun sharePdfForMonth(context: Context, customer: Customer, monthYear: MonthYear, isUrdu: Boolean) {
        viewModelScope.launch {
            val (start, end) = monthYear.toCalendarRange()
            val records = dao.getRecordsInPeriod(start, end).first()
                .filter { it.customerId == customer.id }
            val payments = dao.getPaymentsForCustomerSync(customer.id)
                .filter { p -> val c = Calendar.getInstance().apply { timeInMillis = p.date }
                    c.get(Calendar.YEAR) == monthYear.year && c.get(Calendar.MONTH) == monthYear.month }
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, monthYear.year)
                set(Calendar.MONTH, monthYear.month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val currentNet = records.sumOf { it.amount } - payments.filter { p -> 
                val c = Calendar.getInstance().apply { timeInMillis = p.date }
                c.get(Calendar.YEAR) == monthYear.year && c.get(Calendar.MONTH) == monthYear.month 
            }.sumOf { it.amount }
            
            // For sharePdfForMonth, we need the total balance up to that month...
            // Actually, usually users want the "Grand Total to Date".
            // Let's stick to the user's request: "include previously remaining money".
            val grandRecords = dao.getRecordsForCustomerSync(customer.id)
            val grandPayments = dao.getPaymentsForCustomerSync(customer.id)
            val grandTotalBalance = grandRecords.sumOf { it.amount } - grandPayments.sumOf { it.amount }
            val previousBalance = grandTotalBalance - currentNet

            val pdfManager = PdfManager(context)
            val file = pdfManager.generateCustomerReport(
                milkmanName = milkmanName.value,
                customer = customer,
                records = records,
                payments = payments,
                month = cal,
                isUrdu = isUrdu,
                previousBalance = previousBalance
            )
            if (file != null) {
                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        context.applicationContext,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, if (isUrdu) "رپورٹ شیئر کریں" else "Share Report")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    /** Generate and share a text summary for all customers for a specific month */
    fun shareAllCustomersReportForMonth(context: Context, monthYear: MonthYear) {
        viewModelScope.launch {
            val (start, end) = monthYear.toCalendarRange()
            val allCustomers = dao.getAllCustomers().first()
            val records = dao.getRecordsInPeriod(start, end).first()
            val payments = dao.getPaymentsForCustomerInRange(start, end).first()
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, monthYear.year)
                set(Calendar.MONTH, monthYear.month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val monthLabel = sdf.format(cal.time)
            val sb = StringBuilder("*Dodhi Report — $monthLabel*\n")
            sb.append("=".repeat(30) + "\n\n")
            allCustomers.forEach { customer ->
                val custRecords = records.filter { it.customerId == customer.id && it.type != "Naga" }
                val custPayments = payments.filter { it.customerId == customer.id }
                if (custRecords.isEmpty()) return@forEach
                val bill = custRecords.sumOf { it.amount }
                val paid = custPayments.sumOf { it.amount }
                val liters = custRecords.sumOf { it.quantity }
                sb.append("👤 *${customer.name}*\n")
                sb.append("   ${liters} L  •  Bill: ${bill.toInt()} Rs.  •  Paid: ${paid.toInt()} Rs.  •  Due: ${(bill - paid).toInt()} Rs.\n\n")
            }
            val totalBill = records.filter { it.type != "Naga" }.sumOf { it.amount }
            val totalPaid = payments.sumOf { it.amount }
            sb.append("=".repeat(30) + "\n")
            sb.append("*Total Bill: ${totalBill.toInt()} Rs.*\n")
            sb.append("*Total Collected: ${totalPaid.toInt()} Rs.*\n")
            sb.append("*Outstanding: ${(totalBill - totalPaid).toInt()} Rs.*\n")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, "Share Monthly Report"))
        }
    }
}
