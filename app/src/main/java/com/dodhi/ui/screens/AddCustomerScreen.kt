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
import com.dodhi.ui.theme.EarthBrown
import com.dodhi.ui.theme.GrassGreen
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.add_customer),
            style = MaterialTheme.typography.headlineLarge,
            color = EarthBrown
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
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = isProvider,
                onClick = { isProvider = true },
                label = { Text(stringResource(R.string.buy_milk)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.customer_name)
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
            onValueChange = { rate = it },
            label = stringResource(R.string.rate_per_liter),
            isNumber = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = stringResource(R.string.default_quantity),
            isNumber = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var selectedUnit by remember { mutableStateOf("Liter") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = selectedUnit == "Liter",
                onClick = { selectedUnit = "Liter" },
                label = { Text(stringResource(R.string.liters)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedUnit == "Ser",
                onClick = { selectedUnit = "Ser" },
                label = { Text(stringResource(R.string.ser)) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                viewModel.addCustomer(name, rate.toDoubleOrNull() ?: 0.0, quantity.toDoubleOrNull() ?: 0.0, selectedUnit, locality, isProvider)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
