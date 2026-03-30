package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dodhi.R
import com.dodhi.ui.theme.EarthBrown
import com.dodhi.ui.theme.GrassGreen
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.data.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRunScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
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
    val isMarked = record != null // Any mark (delivered or naga)
    val isNaga = record?.type == "Naga"
    val isDelivered = record?.type == "Delivered"

    var customQty by remember { mutableStateOf(customer.defaultQuantity.toString()) }
    var isEditing by remember { mutableStateOf(false) }

    // When entering edit mode pre-fill with recorded qty
    LaunchedEffect(isEditing) {
        if (isEditing && record != null) customQty = record.quantity.toString()
    }

    val showInputMode = !isMarked || isEditing

    // Determine card background color
    val cardColor = when {
        isNaga -> Color(0xFFFFF3F3)       // light red for Naga
        isDelivered -> Color(0xFFE8F5E9)  // light green for Delivered
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (isNaga && !isEditing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.naga),
                            fontSize = 11.sp,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFFFCDD2), MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (showInputMode) {
                    OutlinedTextField(
                        value = customQty,
                        onValueChange = { customQty = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.width(100.dp).height(55.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        label = { Text("Liters", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = EarthBrown
                        )
                    )
                } else {
                    // Show what was recorded
                    val recordQty = record?.quantity ?: 0.0
                    Text(
                        text = if (isNaga) "0 L (Naga)" else "${recordQty} L",
                        fontSize = 13.sp,
                        color = if (isNaga) Color(0xFFB71C1C) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            if (showInputMode) {
                // Mark as Delivered button
                IconButton(
                    onClick = {
                        val qty = customQty.toDoubleOrNull() ?: 0.0
                        if (qty <= 0.0) {
                            viewModel.markDelivered(customer, "Naga", 0.0)
                        } else {
                            viewModel.markDelivered(customer, "Delivered", qty)
                        }
                        isEditing = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Mark as done",
                        tint = Color.LightGray,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                // Status icon + small pencil to edit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                        modifier = Modifier.size(36.dp)
                    )
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit entry",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
