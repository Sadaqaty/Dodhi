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

    fun updateMilkmanName(name: String) {
        _milkmanName.value = name
        prefs.edit().putString("milkman_name", name).apply()
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
        val end = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        dao.getRecordsInPeriod(start, end)  // live Flow — no .first()
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

    val todayLitersNeeded: StateFlow<Double> = customers.map { list ->
        list.sumOf { it.defaultQuantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayTotalCollected: StateFlow<Double> = customers.flatMapLatest { _ ->
        val date = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        dao.getRecordsInPeriod(date, date + 86400000).map { records ->
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
            if (customer.isProvider) {
                // For providers, record amount is what we OWE them, payment is what we PAID them.
                // Balance = We Owe - We Paid
                recordSum - paymentSum
            } else {
                // For consumers, record amount is what they OWE us, payment is what they PAID us.
                // Balance = They Owe - They Paid
                recordSum - paymentSum
            }
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

    /** Total liters of milk delivered/received for this customer (all time, excludes Naga). */
    fun getTotalLiters(customerId: Long): Flow<Double> {
        return dao.getRecordsForCustomer(customerId).map { records ->
            records.filter { it.type != "Naga" }.sumOf { it.quantity }
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
            
            val allRecords = dao.getRecordsInPeriod(date, date + 86400000).first()
            val existing = allRecords.find { it.customerId == customer.id }
            
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

    fun sharePdfReport(context: Context, customer: Customer, isUrdu: Boolean) {
        viewModelScope.launch {
            val records = dao.getRecordsForCustomerSync(customer.id)
            val payments = dao.getPaymentsForCustomerSync(customer.id)
            val month = Calendar.getInstance()
            
            val pdfManager = PdfManager(context)
            val file = pdfManager.generateCustomerReport(
                milkmanName = milkmanName.value,
                customer = customer,
                records = records,
                payments = payments,
                month = month,
                isUrdu = isUrdu
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
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.quantity}L - ${it.amount} PKR\n")
            }
            
            payments.filter { isSameMonth(it.date, month) }.forEach {
                val label = if (isUrdu) "رقم وصولی" else "Payment"
                sb.append("${dateFormat.format(Date(it.date))}: $label - ${it.amount} PKR\n")
            }
            
            val totalBill = records.filter { isSameMonth(it.date, month) }.sumOf { it.amount }
            val totalPaid = payments.filter { isSameMonth(it.date, month) }.sumOf { it.amount }
            val net = totalBill - totalPaid
            
            sb.append("\n------------------\n")
            sb.append(if (isUrdu) "*کل بل:* $totalBill PKR\n" else "*Total Bill:* $totalBill PKR\n")
            sb.append(if (isUrdu) "*کل ادائیگی:* $totalPaid PKR\n" else "*Total Paid:* $totalPaid PKR\n")
            sb.append(if (isUrdu) "*بقیہ بیلنس:* $net PKR\n" else "*Net Balance:* $net PKR\n")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(intent, if (isUrdu) "ہساب شیئر کریں" else "Share Ledger"))
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
            val pdfManager = PdfManager(context)
            val file = pdfManager.generateCustomerReport(
                milkmanName = milkmanName.value,
                customer = customer,
                records = records,
                payments = payments,
                month = cal,
                isUrdu = isUrdu
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
                sb.append("   ${liters.toInt()} L  •  Bill: ${bill.toInt()} PKR  •  Paid: ${paid.toInt()} PKR  •  Due: ${(bill - paid).toInt()} PKR\n\n")
            }
            val totalBill = records.filter { it.type != "Naga" }.sumOf { it.amount }
            val totalPaid = payments.sumOf { it.amount }
            sb.append("=".repeat(30) + "\n")
            sb.append("*Total Bill: ${totalBill.toInt()} PKR*\n")
            sb.append("*Total Collected: ${totalPaid.toInt()} PKR*\n")
            sb.append("*Outstanding: ${(totalBill - totalPaid).toInt()} PKR*\n")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, "Share Monthly Report"))
        }
    }
}
