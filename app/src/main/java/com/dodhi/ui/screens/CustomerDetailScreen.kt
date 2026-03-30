package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.dodhi.R
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.Payment
import com.dodhi.ui.theme.EarthBrown
import com.dodhi.ui.theme.GrassGreen
import com.dodhi.ui.theme.NatureGreen
import com.dodhi.ui.theme.PastelGreen
import com.dodhi.ui.theme.PastelBlue
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.components.PremiumTextField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(viewModel: DashboardViewModel, customerId: Long, onBack: () -> Unit) {
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.hisaab), stringResource(R.string.payment), stringResource(R.string.settings))

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(customer?.name ?: "", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        var showReportDialog by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Generate Report", tint = Color.Black)
                        }

                        if (showReportDialog) {
                            ReportGenerationDialog(
                                onDismiss = { showReportDialog = false },
                                onGenerate = { type, isUrdu ->
                                    customer?.let {
                                        if (type == "PDF") {
                                            viewModel.sharePdfReport(context, it, isUrdu)
                                        } else {
                                            viewModel.shareTextReport(context, it, isUrdu)
                                        }
                                    }
                                    showReportDialog = false
                                }
                            )
                        }

                        IconButton(onClick = { 
                            customer?.let { 
                                viewModel.deleteCustomer(it)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_customer), tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GrassGreen)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GrassGreen,
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = EarthBrown
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> HisaabTab(viewModel, customerId)
                1 -> PaymentsTab(viewModel, customerId)
                2 -> SettingsTab(viewModel, customerId)
            }
        }
    }
}

@Composable
fun HisaabTab(viewModel: DashboardViewModel, customerId: Long) {
    val records by viewModel.getRecordsForCustomer(customerId).collectAsState(initial = emptyList())
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    
    var showExtraDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedShift by remember { mutableStateOf("") }

    // Generate dates for current month
    val calendar = Calendar.getInstance()
    val monthStart = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val monthEnd = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) }
    
    val dates = remember {
        val list = mutableListOf<Calendar>()
        val curr = monthStart.clone() as Calendar
        while (curr.timeInMillis <= monthEnd.timeInMillis) {
            list.add(curr.clone() as Calendar)
            curr.add(Calendar.DAY_OF_MONTH, 1)
        }
        list.reversed()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            customer?.let { CustomerSummaryHeader(it, viewModel) }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 70.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dates) { date ->
                    customer?.let { cust ->
                        DailyLedgerRow(cust, date, records, viewModel, onExtraClick = { shift ->
                            selectedDate = date
                            selectedShift = shift
                            showExtraDialog = true
                        })
                    }
                }
            }
        }

        if (showExtraDialog && selectedDate != null) {
            ExtraMilkDialog(
                onDismiss = { showExtraDialog = false },
                onConfirm = { qty ->
                    customer?.let { 
                        viewModel.markDeliveredWithDate(it, "Extra", qty, selectedDate!!, selectedShift)
                    }
                    showExtraDialog = false
                }
            )
        }
        
        // Sticky Bottom Summary Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = EarthBrown,
            tonalElevation = 8.dp
        ) {
            val total by viewModel.getMonthlyTotal(customerId).collectAsState(initial = 0.0)
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.total_amount), color = Color.LightGray)
                Text("${total} ${stringResource(R.string.rupees)}", color = GrassGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DailyLedgerRow(customer: Customer, date: Calendar, allRecords: List<DeliveryRecord>, viewModel: DashboardViewModel, onExtraClick: (String) -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", java.util.Locale.getDefault())
    
    val morningRecord = allRecords.find { isSameDay(it.date, date) && it.shift == "Morning" }
    val eveningRecord = allRecords.find { isSameDay(it.date, date) && it.shift == "Evening" }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(dateFormat.format(date.time), fontWeight = FontWeight.Bold, color = EarthBrown)
                    Text(dayFormat.format(date.time), fontSize = 12.sp, color = Color.Gray)
                }
                
                // Naga Button
                Button(
                    onClick = { 
                        viewModel.markDeliveredWithDate(customer, "Naga", 0.0, date, "Morning")
                        viewModel.markDeliveredWithDate(customer, "Naga", 0.0, date, "Evening")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (morningRecord?.type == "Naga" && eveningRecord?.type == "Naga") Color.Red else Color.Red.copy(alpha = 0.1f),
                        contentColor = if (morningRecord?.type == "Naga" && eveningRecord?.type == "Naga") Color.White else Color.Red
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(stringResource(R.string.naga), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Morning Action
                ShiftAction(
                    label = stringResource(R.string.morning), 
                    record = morningRecord, 
                    onAction = { viewModel.markDeliveredWithDate(customer, "Delivered", customer.morningReq, date, "Morning") },
                    onExtra = { onExtraClick("Morning") },
                    modifier = Modifier.weight(1f)
                )
                
                // Evening Action
                ShiftAction(
                    label = stringResource(R.string.evening), 
                    record = eveningRecord, 
                    onAction = { viewModel.markDeliveredWithDate(customer, "Delivered", customer.eveningReq, date, "Evening") },
                    onExtra = { onExtraClick("Evening") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ShiftAction(label: String, record: DeliveryRecord?, onAction: () -> Unit, onExtra: () -> Unit, modifier: Modifier) {
    val isDelivered = record?.type == "Delivered"
    
    Surface(
        onClick = onAction,
        modifier = modifier,
        color = if (isDelivered) GrassGreen.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isDelivered) GrassGreen else Color.LightGray),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            if (isDelivered) {
                Text("${record?.quantity} L", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onExtra, modifier = Modifier.size(24.dp)) {
                    Text("+", fontWeight = FontWeight.Bold, color = EarthBrown)
                }
            } else {
                Text("-", color = Color.LightGray)
            }
        }
    }
}

fun isSameDay(timestamp: Long, calendar: Calendar): Boolean {
    val recordCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return recordCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
           recordCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
}


@Composable
fun PaymentsTab(viewModel: DashboardViewModel, customerId: Long) {
    val payments by viewModel.getPaymentsForCustomer(customerId).collectAsState(initial = emptyList())
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    
    var amount by remember { mutableStateOf("") }

    Column {
        customer?.let { cust ->
            val balance by viewModel.getCustomerBalance(cust.id).collectAsState(initial = 0.0)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = EarthBrown)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(if (cust.isProvider) R.string.you_owe else R.string.they_owe), 
                        color = Color.LightGray
                    )
                    Text("${balance} ${stringResource(R.string.rupees)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                }
            }
            
            // Payment Entry
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumTextField(value = amount, onValueChange = { amount = it }, label = stringResource(R.string.amount), isNumber = true)
                    }
                    Button(
                        onClick = {
                            if (amount.isNotEmpty()) {
                                viewModel.addPayment(cust.id, amount.toDoubleOrNull() ?: 0.0)
                                amount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
                    ) {
                        Text(
                            text = stringResource(if (cust.isProvider) R.string.payment_given else R.string.payment_received), 
                            color = Color.Black
                        )
                    }
                }
            }
        }
        
        Text(
            text = stringResource(R.string.payment_history),
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = EarthBrown
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payments.sortedByDescending { it.date }) { payment ->
                KhataRow(KhataItem.PaymentItem(payment), viewModel, customerId)
            }
        }
    }
}


@Composable
fun SettingsTab(viewModel: DashboardViewModel, customerId: Long) {
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    
    customer?.let { cust ->
        var morning by remember { mutableStateOf(cust.morningReq.toString()) }
        var evening by remember { mutableStateOf(cust.eveningReq.toString()) }
        var rate by remember { mutableStateOf((cust.customRate ?: cust.rate).toString()) }

        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.customer_settings), style = MaterialTheme.typography.headlineSmall, color = EarthBrown)
            
            PremiumTextField(value = morning, onValueChange = { morning = it }, label = stringResource(R.string.morning) + " (" + stringResource(R.string.liters) + ")", isNumber = true)
            PremiumTextField(value = evening, onValueChange = { evening = it }, label = stringResource(R.string.evening) + " (" + stringResource(R.string.liters) + ")", isNumber = true)
            PremiumTextField(value = rate, onValueChange = { rate = it }, label = stringResource(R.string.rate_per_liter), isNumber = true)
            
            Button(
                onClick = {
                    viewModel.updateCustomerSettings(
                        cust, 
                        morning.toDoubleOrNull() ?: 0.0, 
                        evening.toDoubleOrNull() ?: 0.0,
                        rate.toDoubleOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text(stringResource(R.string.save_changes), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun CustomerSummaryHeader(customer: Customer, viewModel: DashboardViewModel) {
    val total by viewModel.getMonthlyTotal(customer.id).collectAsState(initial = 0.0)
    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
    val rupees = stringResource(R.string.rupees)

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = EarthBrown),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.monthly_report), color = Color.LightGray, fontSize = 14.sp)
                Text("$total $rupees", color = GrassGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.outstanding), color = Color.LightGray, fontSize = 14.sp)
                Text("$balance $rupees", color = if (balance > 0) Color(0xFFF44336) else Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KhataRow(item: KhataItem, viewModel: DashboardViewModel, customerId: Long) {
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault())
    val rupees = stringResource(R.string.rupees)
    
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (item) {
                        is KhataItem.Delivery -> {
                            if (item.record.type == "Naga") stringResource(R.string.naga)
                            else if (customer?.isProvider == true) stringResource(R.string.buy_milk)
                            else stringResource(R.string.delivered)
                        }
                        is KhataItem.PaymentItem -> {
                            if (customer?.isProvider == true) stringResource(R.string.payment_given)
                            else stringResource(R.string.payment_received)
                        }
                    },
                    fontWeight = FontWeight.Bold,
                    color = when (item) {
                        is KhataItem.Delivery -> if (item.record.type == "Naga") Color.Red else Color(0xFF4CAF50)
                        is KhataItem.PaymentItem -> if (customer?.isProvider == true) Color.Red else Color(0xFF4CAF50)
                    }
                )
                Text(dateFormat.format(Date(item.date)), fontSize = 12.sp, color = Color.Gray)
            }
            
            Text(
                text = when (item) {
                    is KhataItem.Delivery -> "${item.record.quantity} ${stringResource(R.string.liters)} | ${item.record.amount} $rupees"
                    is KhataItem.PaymentItem -> "${item.payment.amount} $rupees"
                },
                fontWeight = FontWeight.Bold,
                color = if (item is KhataItem.PaymentItem) Color(0xFF4CAF50) else Color.Black
            )
        }
    }
}

sealed class KhataItem(val date: Long) {
    data class Delivery(val record: DeliveryRecord) : KhataItem(record.date)
    data class PaymentItem(val payment: Payment) : KhataItem(payment.date)
}

class SerratedEdgeShape(val serrationCount: Int = 20, val serrationHeight: Float = 15f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): androidx.compose.ui.graphics.Outline {
        val path = Path().apply {
            moveTo(0f, serrationHeight)
            val segmentWidth = size.width / serrationCount
            for (i in 0 until serrationCount) {
                val x = i * segmentWidth
                lineTo(x + segmentWidth / 2, 0f)
                lineTo(x + segmentWidth, serrationHeight)
            }
            lineTo(size.width, size.height - serrationHeight)
            for (i in serrationCount downTo 1) {
                val x = (i - 1) * segmentWidth
                lineTo(x + segmentWidth / 2, size.height)
                lineTo(x, size.height - serrationHeight)
            }
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun ReportGenerationDialog(onDismiss: () -> Unit, onGenerate: (String, Boolean) -> Unit) {
    var selectedType by remember { mutableStateOf("PDF") }
    var selectedLangIsUrdu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.generate_report), fontWeight = FontWeight.Bold, color = EarthBrown) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.report_format), fontWeight = FontWeight.Medium)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedType = "PDF" }) {
                        RadioButton(selected = selectedType == "PDF", onClick = { selectedType = "PDF" })
                        Text(stringResource(R.string.pdf_report))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedType = "Text" }) {
                        RadioButton(selected = selectedType == "Text", onClick = { selectedType = "Text" })
                        Text(stringResource(R.string.text_report))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                Text(stringResource(R.string.report_language), fontWeight = FontWeight.Medium)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedLangIsUrdu = false }) {
                        RadioButton(selected = !selectedLangIsUrdu, onClick = { selectedLangIsUrdu = false })
                        Text("English")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedLangIsUrdu = true }) {
                        RadioButton(selected = selectedLangIsUrdu, onClick = { selectedLangIsUrdu = true })
                        Text("اردو (Urdu)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedType, selectedLangIsUrdu) },
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text(stringResource(R.string.generate_report), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.Gray)
            }
        }
    )
}

@Composable
fun ExtraMilkDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var quantity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extra_milk_quantity), fontWeight = FontWeight.Bold, color = EarthBrown) },
        text = {
            Column {
                Text(stringResource(R.string.enter_quantity))
                Spacer(modifier = Modifier.height(8.dp))
                PremiumTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = "Liters",
                    isNumber = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { quantity.toDoubleOrNull()?.let { onConfirm(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = NatureGreen)
            ) {
                Text(stringResource(R.string.confirm), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.Gray)
            }
        }
    )
}

