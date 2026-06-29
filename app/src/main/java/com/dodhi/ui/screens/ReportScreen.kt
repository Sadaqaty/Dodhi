package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.ui.theme.*
import com.dodhi.ui.viewmodel.DashboardViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit) {
    val customers by viewModel.customers.collectAsState()
    val summary by viewModel.getCollectionSummary().collectAsState(initial = null)
    val dailyVolume by viewModel.getDailyVolumeInRange().collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reports), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GrassGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.exportDataBackup(context) }) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup Data",
                            tint = Color.White
                        )
                    }
                }
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
            // 1. Financial Summary Grid
            item {
                Text(
                    text = stringResource(R.string.monthly_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
                Spacer(modifier = Modifier.height(12.dp))
                BusinessSummaryGrid(summary)
            }
            
            // 2. Volume Chart
            item {
                Text(
                    text = stringResource(R.string.delivery_volume),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
                Spacer(modifier = Modifier.height(12.dp))
                VolumeBarChart(dailyVolume)
            }
            
            // 3. Customer Reports
            item {
                Text(
                    text = stringResource(R.string.all_customers),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
            }
            
            items(customers) { customer ->
                val total by viewModel.getMonthlyBill(customer.id).collectAsState(initial = 0.0)
                val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                PremiumReportCard(
                    customer = customer,
                    total = total,
                    balance = balance,
                    viewModel = viewModel,
                    onClick = { onCustomerClick(customer.id) }
                )
            }
        }
    }
}

@Composable
fun BusinessSummaryGrid(summary: DashboardViewModel.CollectionSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard(
                label = stringResource(R.string.total_sales),
                value = "${summary?.marketValue?.toInt() ?: 0} ${stringResource(R.string.rupees)}",
                color = GrassGreen,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = stringResource(R.string.collected),
                value = "${summary?.cashCollected?.toInt() ?: 0} ${stringResource(R.string.rupees)}",
                color = HeritageOlive,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard(
                label = stringResource(R.string.total_bought),
                value = "${summary?.totalPurchases?.toInt() ?: 0} ${stringResource(R.string.rupees)}",
                color = ClayTerracotta,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                label = stringResource(R.string.payment_given),
                value = "${summary?.cashPaid?.toInt() ?: 0} ${stringResource(R.string.rupees)}",
                color = EarthBrown,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun VolumeBarChart(data: List<DashboardViewModel.DayVolume>) {
    val maxVolume = (data.maxByOrNull { it.volume }?.volume ?: 10.0).coerceAtLeast(1.0)
    
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            data.takeLast(14).forEach { day ->
                val barHeight = (day.volume / maxVolume).toFloat()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barHeight.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(GrassGreen)
                )
            }
        }
    }
}

@Composable
fun PremiumReportCard(customer: com.dodhi.data.model.Customer, total: Double, balance: Double, viewModel: DashboardViewModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = customer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EarthBrown)
                    Spacer(modifier = Modifier.width(8.dp))
                    CustomerTypeTag(customer.isProvider)
                }
                Text(text = customer.locality.ifEmpty { stringResource(R.string.miscellaneous) }, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${total.toInt()} ${stringResource(R.string.rupees)}", 
                    fontWeight = FontWeight.ExtraBold, 
                    color = if (customer.isProvider) ClayTerracotta else GrassGreen
                )
                Text(
                    text = "${balance.toInt()} ${stringResource(R.string.rupees)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            val context = LocalContext.current
            IconButton(
                onClick = {
                    val isSystemUrdu = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("ur")
                    viewModel.shareTextReportViaWhatsApp(context, customer, isSystemUrdu)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_whatsapp),
                    contentDescription = "Share via WhatsApp",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerTypeTag(isProvider: Boolean) {
    val text = if (isProvider) stringResource(R.string.seller) else stringResource(R.string.buyer)
    val color = if (isProvider) EarthBrown else GrassGreen
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
