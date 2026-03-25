package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(viewModel: DashboardViewModel, customerId: Long, onBack: () -> Unit) {
    val customer by viewModel.getCustomer(customerId).collectAsState(initial = null)
    val records by viewModel.getRecordsForCustomer(customerId).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsForCustomer(customerId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val khataItems = remember(records, payments) {
        (records.map { KhataItem.Delivery(it) } + payments.map { KhataItem.PaymentItem(it) })
            .sortedByDescending { it.date }
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
                    IconButton(onClick = { 
                        scope.launch {
                            val text = viewModel.generateParchiText(customerId)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Header
            customer?.let { cust ->
                CustomerSummaryHeader(cust, viewModel)
            }
            
            Text(
                text = stringResource(R.string.daily_entry),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
                color = DeepBlue
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(khataItems) { item ->
                    KhataRow(item)
                }
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
