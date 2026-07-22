package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
    val records by viewModel.getRecordsForCustomer(customerId).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsForCustomer(customerId).collectAsState(initial = emptyList())

    val khataItems = remember(records, payments) {
        val list = mutableListOf<KhataItem>()
        list.addAll(records.map { KhataItem.Delivery(it) })
        list.addAll(payments.map { KhataItem.PaymentItem(it) })
        list.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showReportDialog by remember { mutableStateOf(false) }
                    var showSettingsDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Generate Report")
                    }
                    
                    IconButton(onClick = {
                        customer?.let {
                            val isSystemUrdu = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("ur")
                            viewModel.shareTextReportViaWhatsApp(context, it, isSystemUrdu)
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp),
                            contentDescription = "Share on WhatsApp",
                            tint = Color.Unspecified
                        )
                    }
                    
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }

                    if (showReportDialog) {
                        ReportGenerationDialog(
                            onDismiss = { showReportDialog = false },
                            onGenerate = { type, isUrdu ->
                                customer?.let {
                                    if (type == "PDF") viewModel.sharePdfReport(context, it, isUrdu)
                                    else viewModel.shareTextReport(context, it, isUrdu)
                                }
                                showReportDialog = false
                            }
                        )
                    }

                    if (showSettingsDialog && customer != null) {
                        CustomerSettingsDialog(
                            customer = customer!!,
                            onDismiss = { showSettingsDialog = false },
                            onSave = { qty, rate ->
                                viewModel.updateCustomerSettings(customer!!, qty, rate)
                                showSettingsDialog = false
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GrassGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                customer?.let { PremiumBillHeader(it, viewModel) }
            }

            item {
                customer?.let { QuickPaymentEntry(it, viewModel) }
            }

            item {
                Text(
                    stringResource(R.string.hisaab),
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (khataItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_records), color = Color.Gray)
                    }
                }
            } else {
                items(khataItems) { item ->
                    KhataRow(item, viewModel, customerId)
                }
            }
        }
    }
}

@Composable
fun PremiumBillHeader(customer: Customer, viewModel: DashboardViewModel) {
    val monthlyBill by viewModel.getMonthlyBill(customer.id).collectAsState(initial = 0.0)
    val monthlyLiters by viewModel.getMonthlyLiters(customer.id).collectAsState(initial = 0.0)
    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
    
    val rupees = stringResource(R.string.rupees)
    
    val balanceColor = when {
        balance == 0.0 -> Color.Gray
        customer.isProvider -> if (balance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
        else -> if (balance > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    }

    val balanceLabel = when {
        balance == 0.0 -> "Settled"
        customer.isProvider -> if (balance > 0) "To Give" else "Overpaid"
        else -> if (balance > 0) "To Receive" else "Advance"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, EarthBrown.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Main Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Column 1: Milk
                Column {
                    Text("Monthly Milk", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${monthlyLiters} L", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = EarthBrown)
                }
                // Column 2: Bill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Monthly Bill", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${monthlyBill.toInt()} $rupees", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = EarthBrown)
                }
                // Column 3: Balance
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${kotlin.math.abs(balance).toInt()} $rupees", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = balanceColor)
                    Text(balanceLabel.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = balanceColor)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sub-info: Rate
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rate per Liter:", color = Color.Gray, fontSize = 12.sp)
                Text("${(customer.customRate ?: customer.rate).toInt()} $rupees", fontWeight = FontWeight.Bold, color = EarthBrown, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuickPaymentEntry(customer: Customer, viewModel: DashboardViewModel) {
    var amount by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GrassGreen.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, GrassGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PremiumTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = "Add Payment (${stringResource(R.string.rupees)})",
                    isNumber = true
                )
            }
            Button(
                onClick = {
                    if (amount.isNotEmpty()) {
                        viewModel.addPayment(customer.id, amount.toDoubleOrNull() ?: 0.0)
                        amount = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
            ) {
                Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomerSettingsDialog(customer: Customer, onDismiss: () -> Unit, onSave: (Double, Double?) -> Unit) {
    var dailyQuantity by remember { mutableStateOf(customer.defaultQuantity.toString()) }
    var rate by remember { mutableStateOf((customer.customRate ?: customer.rate).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customer Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumTextField(value = dailyQuantity, onValueChange = { dailyQuantity = it }, label = "Daily Quantity (L)", isNumber = true)
                PremiumTextField(value = rate, onValueChange = { rate = it }, label = "Rate per Liter (${stringResource(R.string.rupees)})", isNumber = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(dailyQuantity.toDoubleOrNull() ?: 0.0, rate.toDoubleOrNull()) }, colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
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
                    is KhataItem.Delivery -> "${item.record.quantity} | ${item.record.amount} $rupees"
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

