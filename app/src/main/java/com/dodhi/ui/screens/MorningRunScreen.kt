package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import com.dodhi.R
import com.dodhi.ui.theme.EarthBrown
import com.dodhi.ui.theme.GrassGreen
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.data.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningRunScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val customersByLocality by viewModel.customersByLocality.collectAsState(initial = emptyMap())
    val progress by viewModel.getDailyProgress().collectAsState(initial = DashboardViewModel.Progress(0.0, 0.0))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_run), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GrassGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Live Progress Bar
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.delivery_report), fontWeight = FontWeight.Bold)
                        Text("${progress.delivered.toInt()} / ${progress.expected.toInt()} L", color = EarthBrown, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = (if (progress.expected > 0) progress.delivered / progress.expected else 0.0).toFloat(),
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = GrassGreen,
                        trackColor = Color.LightGray
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                customersByLocality.forEach { (locality, customers) ->
                    item {
                        Surface(
                            color = EarthBrown.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = locality.ifEmpty { stringResource(R.string.miscellaneous) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = EarthBrown,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    items(customers) { customer ->
                        RouteCustomerRow(customer, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCustomerRow(customer: Customer, viewModel: DashboardViewModel) {
    val records by viewModel.dailyRecords.collectAsState()
    val record = records.find { it.customerId == customer.id }
    val isDelivered = record?.type == "Delivered"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDelivered) Color(0xFFE8F5E9) else Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var customQty by remember { mutableStateOf(customer.defaultQuantity.toString()) }

            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = customQty,
                    onValueChange = { customQty = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.width(80.dp).height(50.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = EarthBrown
                    )
                )
            }
            
            IconButton(
                onClick = { 
                    if (!isDelivered) {
                        val qty = customQty.toDoubleOrNull() ?: customer.defaultQuantity
                        viewModel.markDelivered(customer, "Delivered", qty)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDelivered) Color(0xFF4CAF50) else Color.LightGray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
