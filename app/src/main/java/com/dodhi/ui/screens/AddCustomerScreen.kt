package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParchiPaper.copy(alpha = 0.5f))
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.add_customer),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = ClayTerracotta
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.customer_type), fontWeight = FontWeight.Bold, color = EarthBrown)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = !isProvider,
                onClick = { isProvider = false },
                label = { Text(stringResource(R.string.sell_milk)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HeritageOlive, 
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = isProvider,
                onClick = { isProvider = true },
                label = { Text(stringResource(R.string.buy_milk)) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ClayTerracotta, 
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
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
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = locality,
            onValueChange = { locality = it },
            label = stringResource(R.string.locality)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = rate,
            onValueChange = { 
                rate = it
                rateError = if (it.toDoubleOrNull() == null) "Invalid rate" else null
            },
            label = stringResource(R.string.rate),
            isNumber = true,
            isError = rateError != null,
            errorText = rateError
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = stringResource(R.string.default_quantity),
            isNumber = true
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                val isNameValid = name.isNotBlank()
                val isRateValid = rate.toDoubleOrNull() != null
                
                if (isNameValid && isRateValid) {
                    viewModel.addCustomer(name, rate.toDouble().coerceAtLeast(0.0), quantity.toDoubleOrNull() ?: 0.0, "Unit", locality, isProvider)
                    onDismiss()
                } else {
                    if (!isNameValid) nameError = "Name is required"
                    if (!isRateValid) rateError = "Valid rate is required"
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClayTerracotta),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
