package com.dodhi.ui.screens

import androidx.compose.foundation.background
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
import com.dodhi.ui.theme.DeepBlue
import com.dodhi.ui.theme.GoldPrimary
import com.dodhi.ui.theme.GoldDark
import com.dodhi.ui.theme.CreamBase
import com.dodhi.ui.theme.SoftBlue
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.components.PremiumTextField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(viewModel: DashboardViewModel, customerId: Long, onBack: () -> Unit) {
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("حساب کتاب", "ادائیگی", "سیٹنگز") // Hisaab, Pay, Settings

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
                        var showParchi by remember { mutableStateOf(false) }
                        IconButton(onClick = { showParchi = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        if (showParchi) {
                            DigitalParchiDialog(viewModel, customerId) { showParchi = false }
                        }
                        IconButton(onClick = { 
                            customer?.let { 
                                viewModel.deleteCustomer(it)
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GoldPrimary)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = DeepBlue
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
                        DailyLedgerRow(cust, date, records, viewModel)
                    }
                }
            }
        }
        
        // Sticky Bottom Summary Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = DeepBlue,
            tonalElevation = 8.dp
        ) {
            val total by viewModel.getMonthlyTotal(customerId).collectAsState(initial = 0.0)
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("کل رقم (Total Month)", color = Color.LightGray)
                Text("${total} PKR", color = GoldPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DailyLedgerRow(customer: Customer, date: Calendar, allRecords: List<DeliveryRecord>, viewModel: DashboardViewModel) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale("ur"))
    val dayFormat = SimpleDateFormat("EEEE", Locale("ur"))
    
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
                    Text(dateFormat.format(date.time), fontWeight = FontWeight.Bold, color = DeepBlue)
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
                    Text("ناغہ (Naga)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Morning Action
                ShiftAction(
                    label = "صبح", 
                    record = morningRecord, 
                    onAction = { viewModel.markDeliveredWithDate(customer, "Delivered", customer.morningReq, date, "Morning") },
                    onExtra = { viewModel.markDeliveredWithDate(customer, "Extra", 1.0, date, "Morning") },
                    modifier = Modifier.weight(1f)
                )
                
                // Evening Action
                ShiftAction(
                    label = "شام", 
                    record = eveningRecord, 
                    onAction = { viewModel.markDeliveredWithDate(customer, "Delivered", customer.eveningReq, date, "Evening") },
                    onExtra = { viewModel.markDeliveredWithDate(customer, "Extra", 1.0, date, "Evening") },
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
        color = if (isDelivered) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isDelivered) GoldPrimary else Color.LightGray),
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
                    Text("+", fontWeight = FontWeight.Bold, color = DeepBlue)
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
                colors = CardDefaults.cardColors(containerColor = DeepBlue)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("کل بقایا (Net Balance)", color = Color.LightGray)
                    Text("${balance} PKR", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                }
            }
            
            // Peshgi / Payment Entry
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
                        PremiumTextField(value = amount, onValueChange = { amount = it }, label = "رقم (Amount)", isNumber = true)
                    }
                    Button(
                        onClick = {
                            if (amount.isNotEmpty()) {
                                viewModel.addPayment(cust.id, amount.toDoubleOrNull() ?: 0.0)
                                amount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("ادائیگی", color = Color.Black)
                    }
                }
            }
        }
        
        Text(
            text = "ادائیگی کی رپورٹ (Payment History)",
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
            color = DeepBlue
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(payments.sortedByDescending { it.date }) { payment ->
                KhataRow(KhataItem.PaymentItem(payment))
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
            Text("کسٹمر سیٹنگز", style = MaterialTheme.typography.headlineSmall, color = DeepBlue)
            
            PremiumTextField(value = morning, onValueChange = { morning = it }, label = "صبح کی مقدار (Morning Liters)", isNumber = true)
            PremiumTextField(value = evening, onValueChange = { evening = it }, label = "شام کی مقدار (Evening Liters)", isNumber = true)
            PremiumTextField(value = rate, onValueChange = { rate = it }, label = "فی لیٹر ریٹ (Custom Rate)", isNumber = true)
            
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
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("تبدیلی محفوظ کریں", color = Color.Black, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = DeepBlue),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.monthly_report), color = Color.LightGray, fontSize = 14.sp)
                Text("$total $rupees", color = GoldPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.outstanding), color = Color.LightGray, fontSize = 14.sp)
                Text("$balance $rupees", color = if (balance > 0) Color(0xFFF44336) else Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KhataRow(item: KhataItem) {
    val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale("ur"))
    val rupees = stringResource(R.string.rupees)
    
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
                        is KhataItem.Delivery -> stringResource(if (item.record.type == "Naga") R.string.naga else R.string.delivered)
                        is KhataItem.PaymentItem -> stringResource(R.string.collected)
                    },
                    fontWeight = FontWeight.Bold,
                    color = when (item) {
                        is KhataItem.Delivery -> if (item.record.type == "Naga") Color.Red else Color(0xFF4CAF50)
                        is KhataItem.PaymentItem -> DeepBlue
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
fun DigitalParchiDialog(viewModel: DashboardViewModel, customerId: Long, onDismiss: () -> Unit) {
    val parchiText by viewModel.generateParchiText(customerId).collectAsState(initial = "")
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, parchiText)
                    }
                    context.startActivity(Intent.createChooser(intent, "حساب شیئر کریں"))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Text("شیئر کریں (Share)", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بند کریں") }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(SerratedEdgeShape())
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Text(
                    text = parchiText,
                    lineHeight = 24.sp,
                    color = Color.DarkGray
                )
            }
        },
        containerColor = CreamBase
    )
}

