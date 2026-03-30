package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit, onBack: () -> Unit) {
    val customers by viewModel.customers.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedShift by viewModel.selectedShift.collectAsState()
    val dailyRecords by viewModel.dailyRecords.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showMilkSourceDialog by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.locality.contains(searchQuery, ignoreCase = true) }
    }

    if (showMilkSourceDialog) {
        MilkSourceDialog(
            onDismiss = { showMilkSourceDialog = false },
            onConfirm = { name, quantity, rate ->
                viewModel.addMilkSource(name, quantity, rate)
                showMilkSourceDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.milk_collection), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GrassGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showMilkSourceDialog = true }, containerColor = GrassGreen) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item { HorizontalCalendarHeader(selectedDate) { viewModel.setSelectedDate(it) } }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedShift == "Morning",
                        onClick = { viewModel.setShift("Morning") },
                        label = { Text(stringResource(R.string.morning)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedShift == "Evening",
                        onClick = { viewModel.setShift("Evening") },
                        label = { Text(stringResource(R.string.evening)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { SummarySection(viewModel) }
            
            if (filteredCustomers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_customers), color = Color.Gray)
                    }
                }
            } else {
                items(filteredCustomers) { customer ->
                    val record = dailyRecords.find { it.customerId == customer.id }
                    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                    
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PremiumCustomerCard(
                            customer = customer,
                            balance = balance,
                            activeType = record?.type,
                            currentQuantity = record?.quantity ?: (if (selectedShift == "Morning") customer.morningReq else customer.eveningReq),
                            onClick = { onCustomerClick(customer.id) },
                            onAction = { type ->
                                val qty = if (selectedShift == "Morning") customer.morningReq else customer.eveningReq
                                viewModel.markDelivered(customer, type, qty)
                            }
                        )
                    }
                }
            }
        }
    }
}
