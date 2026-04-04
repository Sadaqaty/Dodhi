package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.Payment
import com.dodhi.ui.theme.*
import com.dodhi.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTimeReportsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val availableMonths by viewModel.getAvailableMonths().collectAsState(initial = emptyList())
    val customers by viewModel.customers.collectAsState()

    // null = all customers, non-null = filtered to one customer
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    // null = show month list, non-null = show detail for that month
    var selectedMonth by remember { mutableStateOf<DashboardViewModel.MonthYear?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.khata_records), fontWeight = FontWeight.Bold, color = Color.White)
                        selectedCustomer?.let {
                            Text(it.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedMonth != null) selectedMonth = null
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EarthBrown)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(ParchiPaper.copy(alpha = 0.4f))) {

            if (selectedMonth == null) {
                // ─── Month List View ───
                // Customer filter bar
                CustomerFilterBar(
                    customers = customers,
                    selected = selectedCustomer,
                    onSelect = { selectedCustomer = it }
                )

                if (availableMonths.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_records_yet), fontSize = 18.sp, color = EarthBrown.copy(alpha = 0.6f))
                            Text(stringResource(R.string.no_records_hint), fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableMonths) { monthYear ->
                            MonthReportCard(
                                monthYear = monthYear,
                                viewModel = viewModel,
                                selectedCustomer = selectedCustomer,
                                onClick = { selectedMonth = monthYear }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            } else {
                // ─── Month Detail View ───
                MonthDetailView(
                    monthYear = selectedMonth!!,
                    viewModel = viewModel,
                    selectedCustomer = selectedCustomer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFilterBar(
    customers: List<Customer>,
    selected: Customer?,
    onSelect: (Customer?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.all_customers), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EarthBrown,
                    selectedLabelColor = Color.White
                )
            )
        }
        items(customers) { customer ->
            FilterChip(
                selected = selected?.id == customer.id,
                onClick = { onSelect(customer) },
                label = { Text(customer.name, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ClayTerracotta,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthReportCard(
    monthYear: DashboardViewModel.MonthYear,
    viewModel: DashboardViewModel,
    selectedCustomer: Customer?,
    onClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val monthLabel = remember(monthYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, monthYear.year)
            set(Calendar.MONTH, monthYear.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        sdf.format(cal.time)
    }

    if (selectedCustomer == null) {
        val summary by viewModel.getMonthSummary(monthYear).collectAsState(
            initial = DashboardViewModel.MonthSummary(monthYear, 0.0, 0.0, 0.0, 0.0, 0, 0)
        )

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(monthLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ClayTerracotta)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip("${summary.totalLiters.toInt()} L", HeritageOlive)
                        StatChip("${summary.customerCount} customers", EarthBrown)
                        if (summary.nagaCount > 0) StatChip("${summary.nagaCount} Naga", Color(0xFFB71C1C))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bill: ${summary.totalAmount.toInt()} PKR  •  Paid: ${summary.totalPaid.toInt()} PKR",
                        fontSize = 13.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val balance = summary.balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${kotlin.math.abs(balance).toInt()} PKR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (balance > 0) Color(0xFF2E7D32) else if (balance < 0) Color(0xFFD32F2F) else Color.Gray
                        )
                        Text(
                            if (balance > 0) stringResource(R.string.outstanding)
                            else if (balance < 0) stringResource(R.string.overpaid)
                            else stringResource(R.string.settled),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    } else {
        // Single customer card
        val records by viewModel.getCustomerRecordsForMonth(selectedCustomer.id, monthYear)
            .collectAsState(initial = emptyList())

        val delivered = records.filter { it.type != "Naga" }
        val nagas = records.count { it.type == "Naga" }
        val totalLiters = delivered.sumOf { it.quantity }
        val totalAmount = delivered.sumOf { it.amount }

        if (records.isEmpty()) return@MonthReportCard

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(monthLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ClayTerracotta)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip("${totalLiters.toInt()} L", HeritageOlive)
                        if (nagas > 0) StatChip("$nagas Naga", Color(0xFFB71C1C))
                    }
                    Text("Bill: ${totalAmount.toInt()} PKR", fontSize = 13.sp, color = Color.Gray)
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun StatChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MonthDetailView(
    monthYear: DashboardViewModel.MonthYear,
    viewModel: DashboardViewModel,
    selectedCustomer: Customer?
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val monthLabel = remember(monthYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, monthYear.year)
            set(Calendar.MONTH, monthYear.month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        sdf.format(cal.time)
    }

    if (selectedCustomer == null) {
        // All-customers month detail
        val summary by viewModel.getMonthSummary(monthYear).collectAsState(
            initial = DashboardViewModel.MonthSummary(monthYear, 0.0, 0.0, 0.0, 0.0, 0, 0)
        )
        val customers by viewModel.customers.collectAsState()
        val (start, end) = monthYear.toCalendarRange()
        val records by viewModel.getRecordsInPeriodAsFlow(start, end).collectAsState(initial = emptyList())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Summary banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EarthBrown)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(monthLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatColumn(stringResource(R.string.total_liters), "${summary.totalLiters.toInt()} L", GrassGreen)
                            SummaryStatColumn(stringResource(R.string.total_bill), "${summary.totalAmount.toInt()} PKR", Color.White)
                            SummaryStatColumn(stringResource(R.string.collected), "${summary.totalPaid.toInt()} PKR", Color(0xFF80CBC4))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val balance = summary.balance
                        Text(
                            text = if (balance > 0) "${stringResource(R.string.outstanding)}: ${balance.toInt()} PKR"
                                   else if (balance < 0) "${stringResource(R.string.overpaid)}: ${kotlin.math.abs(balance).toInt()} PKR"
                                   else stringResource(R.string.settled),
                            color = if (balance > 0) Color(0xFFFFCC80) else Color(0xFF80CBC4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Share all-customers report button
                        Button(
                            onClick = { viewModel.shareAllCustomersReportForMonth(context, monthYear) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.share_monthly_report), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.per_customer_breakdown), fontWeight = FontWeight.Bold, color = EarthBrown, fontSize = 16.sp)
            }

            // Show per-customer breakdown
            items(customers) { customer ->
                val customerRecords = records.filter { it.customerId == customer.id }
                if (customerRecords.isNotEmpty()) {
                    val delivered = customerRecords.filter { it.type != "Naga" }
                    val nagas = customerRecords.count { it.type == "Naga" }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(customer.name, fontWeight = FontWeight.Bold, color = ClayTerracotta)
                                Text("${delivered.sumOf { it.amount }.toInt()} PKR", fontWeight = FontWeight.Bold, color = HeritageOlive)
                            }
                            Text("${delivered.sumOf { it.quantity }.toInt()} L${if (nagas > 0) "  •  $nagas Naga" else ""}",
                                fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            // PDF share for individual customer
                            OutlinedButton(
                                onClick = { viewModel.sharePdfForMonth(context, customer, monthYear, false) },
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.pdf_report_btn), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    } else {
        // Single customer month detail
        val records by viewModel.getCustomerRecordsForMonth(selectedCustomer.id, monthYear)
            .collectAsState(initial = emptyList())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EarthBrown)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(selectedCustomer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(monthLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        val delivered = records.filter { it.type != "Naga" }
                        val nagas = records.count { it.type == "Naga" }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SummaryStatColumn(stringResource(R.string.total_liters), "${delivered.sumOf { it.quantity }.toInt()} L", GrassGreen)
                            SummaryStatColumn(stringResource(R.string.total_bill), "${delivered.sumOf { it.amount }.toInt()} PKR", Color.White)
                            SummaryStatColumn(stringResource(R.string.naga_days), "$nagas", Color(0xFFFFCC80))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.sharePdfForMonth(context, selectedCustomer, monthYear, false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.share_pdf_report), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.daily_records), fontWeight = FontWeight.Bold, color = EarthBrown, fontSize = 16.sp)
            }

            items(records.sortedByDescending { it.date }) { record ->
                val dateStr = dayFmt.format(Date(record.date))
                val isNaga = record.type == "Naga"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNaga) Color(0xFFFFF3F3) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dateStr, fontWeight = FontWeight.SemiBold, color = EarthBrown)
                            Text(
                                record.type,
                                fontSize = 12.sp,
                                color = if (isNaga) Color(0xFFB71C1C) else Color(0xFF2E7D32)
                            )
                        }
                        if (!isNaga) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${record.quantity} L", fontWeight = FontWeight.Bold, color = HeritageOlive)
                                Text("${record.amount.toInt()} PKR", fontSize = 13.sp, color = Color.Gray)
                            }
                        } else {
                            Text("0 L", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

/** Helper to make the period flow accessible from the screen */
fun DashboardViewModel.MonthYear.toCalendarRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
    return Pair(start, cal.timeInMillis)
}

@Composable
fun SummaryStatColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
