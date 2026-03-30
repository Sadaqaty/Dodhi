package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.data.model.Customer
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.theme.DeepBlue
import com.dodhi.ui.theme.SoftBlue
import com.dodhi.ui.theme.GoldPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit, onMorningRunClick: () -> Unit) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMilkSourceDialog = true },
                containerColor = GoldPrimary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_milk_source))
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    IconButton(onClick = {
                        val newLang = if (currentLang == "ur") "en" else "ur"
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(newLang)
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = stringResource(R.string.language),
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = onMorningRunClick) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = stringResource(R.string.morning_run),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GoldPrimary,
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
            contentPadding = PaddingValues(bottom = 100.dp) // Extra bottom padding for fab/nav
        ) {
            item {
                // Search Bar
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
                // Shift Toggle
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
                        Text(
                            text = stringResource(R.string.no_customers),
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
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

@Composable
fun HorizontalCalendarHeader(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
    val calendar = Calendar.getInstance()
    val days = (0..30).map { i ->
        (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
    }.reversed()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { date ->
            val isSelected = isSameDay(date, selectedDate)
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onDateSelected(date) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) GoldPrimary else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = SimpleDateFormat("E", java.util.Locale.getDefault()).format(date.time),
                        fontSize = 12.sp,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                    Text(
                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumCustomerCard(customer: Customer, balance: Double, activeType: String?, currentQuantity: Double, onClick: () -> Unit, onAction: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = customer.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                    if (customer.locality.isNotEmpty()) {
                        Text(text = customer.locality, fontSize = 14.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.baqaya), fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "$balance ${stringResource(R.string.rupees)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = SoftBlue,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "$currentQuantity ${if (customer.unit == "Liter") stringResource(R.string.liters) else stringResource(R.string.ser)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeliveryButton(
                    label = stringResource(R.string.delivered),
                    color = Color(0xFF4CAF50),
                    isActive = activeType == "Delivered",
                    modifier = Modifier.weight(1f)
                ) { onAction("Delivered") }
                
                DeliveryButton(
                    label = stringResource(R.string.extra),
                    color = Color(0xFFFFD700),
                    isActive = activeType == "Extra",
                    modifier = Modifier.weight(1f)
                ) { onAction("Extra") }
                
                DeliveryButton(
                    label = stringResource(R.string.naga),
                    color = Color(0xFFF44336),
                    isActive = activeType == "Naga",
                    modifier = Modifier.weight(1f)
                ) { onAction("Naga") }
            }
        }
    }
}

@Composable
fun DeliveryButton(label: String, color: Color, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) color else color.copy(alpha = 0.1f),
            contentColor = if (isActive) (if (color == Color(0xFFFFD700)) Color.Black else Color.White) else color
        ),
        border = if (!isActive) BorderStroke(1.dp, color) else null,
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isActive) 4.dp else 0.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SummarySection(viewModel: DashboardViewModel) {
    val litersNeeded by viewModel.todayLitersNeeded.collectAsState()
    val litersDelivered by viewModel.todayLitersDelivered.collectAsState()
    val deliveriesCount by viewModel.todayDeliveriesCount.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.today_summary), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$litersDelivered / $litersNeeded ${stringResource(R.string.liters)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (litersNeeded > 0) (litersDelivered / litersNeeded).toFloat().coerceIn(0f, 1f) else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = GoldPrimary,
                trackColor = SoftBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.deliveries_done)}: $deliveriesCount",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MilkSourceDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.milk_source)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.liters)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(name, quantity.toDoubleOrNull() ?: 0.0, rate.toDoubleOrNull() ?: 0.0) 
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
