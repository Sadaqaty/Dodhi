package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.ui.theme.EarthBrown
import com.dodhi.ui.theme.NatureGreen
import com.dodhi.ui.theme.PastelGreen
import com.dodhi.ui.theme.GrassGreen
import com.dodhi.ui.viewmodel.DashboardViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit) {
    val customers by viewModel.customers.collectAsState()
    val summary by viewModel.getCollectionSummary().collectAsState(initial = null)
    val dailyVolume by viewModel.getDailyVolumeInRange().collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reports), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GrassGreen,
                    titleContentColor = Color.Black
                )
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
            // 1. Financial Summary Cards
            item {
                BusinessSummaryCards(summary)
            }
            
            // 2. Volume Chart
            item {
                Text(stringResource(R.string.delivery_volume), style = MaterialTheme.typography.titleMedium, color = EarthBrown)
                Spacer(modifier = Modifier.height(8.dp))
                VolumeBarChart(dailyVolume)
            }
            
            // 3. Customer Reports
            item {
                Text(stringResource(R.string.reports), style = MaterialTheme.typography.titleMedium, color = EarthBrown)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(customers) { customer ->
                val total by viewModel.getMonthlyTotal(customer.id).collectAsState(initial = 0.0)
                val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                PremiumReportCard(
                    customer = customer,
                    total = total,
                    balance = balance,
                    onClick = { onCustomerClick(customer.id) }
                )
            }
        }
    }
}

@Composable
fun BusinessSummaryCards(summary: DashboardViewModel.CollectionSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.market_value), "${summary?.marketValue ?: 0.0} ${stringResource(R.string.rupees)}", NatureGreen, Modifier.weight(1f))
            SummaryCard(stringResource(R.string.collected), "${summary?.cashCollected ?: 0.0} ${stringResource(R.string.rupees)}", Color(0xFF4CAF50), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(stringResource(R.string.outstanding), "${summary?.outstanding ?: 0.0} ${stringResource(R.string.rupees)}", Color(0xFFF44336), Modifier.weight(1f))
            SummaryCard(stringResource(R.string.waste), "${summary?.waste ?: 0.0}", Color(0xFF9C27B0), Modifier.weight(1f))
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = color)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun VolumeBarChart(data: List<DashboardViewModel.DayVolume>) {
    val maxVolume = (data.maxByOrNull { it.volume }?.volume ?: 10.0).coerceAtLeast(1.0)
    
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.takeLast(10).forEach { day ->
                val barHeight = (day.volume / maxVolume).toFloat()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(barHeight)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(GrassGreen)
                )
            }
        }
    }
}

@Composable
fun PremiumReportCard(customer: com.dodhi.data.model.Customer, total: Double, balance: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = customer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EarthBrown)
                Text(text = customer.locality, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${total} ${stringResource(R.string.rupees)}", fontWeight = FontWeight.Bold, color = NatureGreen)
                Text(
                    text = "${balance} ${stringResource(R.string.rupees)}",
                    fontSize = 12.sp,
                    color = if (balance > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }
        }
    }
}
