package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.view.SoundEffectConstants
import androidx.appcompat.app.AppCompatDelegate
import com.dodhi.ui.theme.ClayTerracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRunScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val customers by viewModel.customers.collectAsState(initial = emptyList())
    val progress by viewModel.getDailyProgress().collectAsState(initial = DashboardViewModel.Progress(0.0, 0.0))
    val selectedDate by viewModel.selectedDate.collectAsState()
    val context = LocalContext.current

    // Reset to today on entry
    LaunchedEffect(Unit) {
        viewModel.refreshToToday()
    }
    
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val dateString = dateFormatter.format(selectedDate.time)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            viewModel.setSelectedDate(cal)
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )
    
    val providers = customers.filter { it.isProvider }
    val consumers = customers.filter { !it.isProvider }

    // Sort: unmarked first, then marked (completed) at the bottom
    val dailyRecords by viewModel.dailyRecords.collectAsState()
    val sortedProviders = remember(providers, dailyRecords) {
        providers.sortedBy { customer -> dailyRecords.any { it.customerId == customer.id } }
    }
    val sortedConsumers = remember(consumers, dailyRecords) {
        consumers.sortedBy { customer -> dailyRecords.any { it.customerId == customer.id } }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.providers), stringResource(R.string.consumers))

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.delivery_report), fontWeight = FontWeight.Bold)
                            Text(
                                text = dateString,
                                fontSize = 12.sp,
                                color = GrassGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { datePickerDialog.show() }
                            )
                        }
                        Text("${progress.delivered} / ${progress.expected} L", color = EarthBrown, fontWeight = FontWeight.Bold)
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

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = GrassGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = GrassGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val currentList = if (selectedTabIndex == 0) sortedProviders else sortedConsumers
                
                items(currentList) { customer ->
                    RouteCustomerRow(customer, viewModel)
                }
            }
        }
    }
}

@Composable
fun RouteCustomerRow(customer: Customer, viewModel: DashboardViewModel) {
    val records by viewModel.dailyRecords.collectAsState()
    val record = records.find { it.customerId == customer.id }
    val isMarked = record != null
    val isNaga = record?.type == "Naga"
    val isDelivered = record?.type == "Delivered"

    var customQty by remember { mutableStateOf(customer.defaultQuantity.toString()) }
    var isEditing by remember { mutableStateOf(false) }

    // When entering edit mode pre-fill with recorded qty
    LaunchedEffect(isEditing) {
        if (isEditing && record != null) customQty = record.quantity.toString()
    }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    fun triggerFeedback() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    // Determine color coding
    val stripColor = when {
        isNaga -> Color(0xFFB71C1C)       // Red for Naga
        isDelivered -> Color(0xFF2E7D32)  // Green for Delivered
        else -> ClayTerracotta             // Terracotta/Ochre for Unmarked
    }

    val cardColor = when {
        isNaga -> Color(0xFFFFF3F3)
        isDelivered -> Color(0xFFE8F5E9)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Left color strip indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(stripColor)
            )
            
            Row(
                modifier = Modifier.padding(16.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EarthBrown)
                        if (isNaga) {
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
                    
                    if (isEditing) {
                        OutlinedTextField(
                            value = customQty,
                            onValueChange = { customQty = it },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.width(110.dp).height(55.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            label = { Text("Liters", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = EarthBrown
                            )
                        )
                    } else {
                        val recordQty = record?.quantity ?: customer.defaultQuantity
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMarked) {
                                    if (isNaga) "0 L (${stringResource(R.string.naga)})" else "$recordQty L"
                                } else {
                                    "Default: $recordQty L"
                                },
                                fontSize = 14.sp,
                                color = if (isMarked) {
                                    if (isNaga) Color(0xFFB71C1C) else Color(0xFF2E7D32)
                                } else {
                                    Color.Gray
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isMarked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit quantity",
                                    tint = Color.Gray.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { isEditing = true }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isEditing) {
                    // Actions during edit mode
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val qty = customQty.toDoubleOrNull() ?: 0.0
                                if (qty <= 0.0) {
                                    viewModel.markDelivered(customer, "Naga", 0.0)
                                } else {
                                    viewModel.markDelivered(customer, "Delivered", qty)
                                }
                                triggerFeedback()
                                isEditing = false
                            }
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = GrassGreen, modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (!isMarked) {
                    // Zero-Keyboard Quick Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Naga Button (Red)
                        Button(
                            onClick = {
                                viewModel.markDelivered(customer, "Naga", 0.0)
                                triggerFeedback()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCDD2)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            Text(stringResource(R.string.naga), color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Delivered Button (Green)
                        Button(
                            onClick = {
                                viewModel.markDelivered(customer, "Delivered", customer.defaultQuantity)
                                triggerFeedback()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            Text(
                                text = if (AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("ur")) "دودھ دیا" else "Delivered",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Marked state: Status Indicator + Pencil to change
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFB71C1C),
                            modifier = Modifier.size(32.dp)
                        )
                        IconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Entry",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
