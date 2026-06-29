package com.dodhi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.view.SoundEffectConstants
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.ui.theme.*
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.components.PremiumTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }

    var isProvider by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var rateError by remember { mutableStateOf<String?>(null) }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    fun triggerFeedback() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_customer), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EarthBrown)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ParchiPaper.copy(alpha = 0.3f))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Type selector
                    Column {
                        Text(
                            text = stringResource(R.string.customer_type), 
                            fontWeight = FontWeight.Bold, 
                            color = EarthBrown,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (!isProvider) GrassGreen else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .clickable { isProvider = false }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.sell_milk), 
                                    color = if (!isProvider) Color.White else Color.Gray, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isProvider) ClayTerracotta else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .clickable { isProvider = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.buy_milk), 
                                    color = if (isProvider) Color.White else Color.Gray, 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (!isProvider) 
                                stringResource(R.string.sell_mode_desc)
                            else 
                                stringResource(R.string.buy_mode_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.4f))

                    // Name
                    Column {
                        PremiumTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                nameError = if (it.isBlank()) "Name cannot be empty" else null
                            },
                            label = stringResource(R.string.customer_name),
                            isError = nameError != null,
                            errorText = nameError
                        )
                        Text(
                            text = stringResource(R.string.customer_name_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            lineHeight = 15.sp
                        )
                    }

                    // Locality
                    Column {
                        PremiumTextField(
                            value = locality,
                            onValueChange = { locality = it },
                            label = stringResource(R.string.locality)
                        )
                        Text(
                            text = stringResource(R.string.locality_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            lineHeight = 15.sp
                        )
                    }

                    // Rate
                    Column {
                        PremiumTextField(
                            value = rate,
                            onValueChange = { 
                                rate = it
                                rateError = if (it.toDoubleOrNull() == null) "Invalid rate" else null
                            },
                            label = "${stringResource(R.string.rate)} (${stringResource(R.string.rupees)} / Liter)",
                            isNumber = true,
                            isError = rateError != null,
                            errorText = rateError
                        )
                        Text(
                            text = stringResource(R.string.rate_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            lineHeight = 15.sp
                        )
                    }

                    // Default Quantity
                    Column {
                        PremiumTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = "${stringResource(R.string.default_quantity)} (Liters)",
                            isNumber = true
                        )
                        Text(
                            text = stringResource(R.string.quantity_desc),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val isNameValid = name.isNotBlank()
                    val isRateValid = rate.toDoubleOrNull() != null
                    
                    if (isNameValid && isRateValid) {
                        viewModel.addCustomer(name, rate.toDouble().coerceAtLeast(0.0), quantity.toDoubleOrNull() ?: 0.0, "Unit", locality, isProvider)
                        triggerFeedback()
                        onDismiss()
                    } else {
                        if (!isNameValid) nameError = "Name is required"
                        if (!isRateValid) rateError = "Valid rate is required"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClayTerracotta),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
