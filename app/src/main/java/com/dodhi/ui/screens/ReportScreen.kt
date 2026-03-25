package com.dodhi.ui.screens

import androidx.compose.foundation.background
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
import com.dodhi.ui.theme.DeepBlue
import androidx.compose.foundation.clickable
import com.dodhi.ui.theme.GoldDark
import com.dodhi.ui.theme.CreamBase
import com.dodhi.ui.theme.GoldPrimary
import com.dodhi.ui.viewmodel.DashboardViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit) {
    val customers by viewModel.customers.collectAsState()
    var showPaymentDialog by remember { mutableStateOf<com.dodhi.data.model.Customer?>(null) }
    
    if (showPaymentDialog != null) {
        PaymentDialog(
            customer = showPaymentDialog!!,
            onDismiss = { showPaymentDialog = null },
            onConfirm = { amount ->
                viewModel.addPayment(showPaymentDialog!!.id, amount)
                showPaymentDialog = null
            }
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.monthly_report), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GoldPrimary,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.monthly_report),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(customers) { customer ->
                    val total by viewModel.getMonthlyTotal(customer.id).collectAsState(initial = 0.0)
                    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                    PremiumReportCard(
                        customer = customer,
                        total = total,
                        balance = balance,
                        onClick = { onCustomerClick(customer.id) },
                        onCollectPayment = { showPaymentDialog = customer }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumReportCard(customer: com.dodhi.data.model.Customer, total: Double, balance: Double, onClick: () -> Unit, onCollectPayment: () -> Unit) {
    val rupees = stringResource(R.string.rupees)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = customer.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CreamBase, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.total_amount), color = Color.Gray)
                Text(text = "$total $rupees", fontWeight = FontWeight.Bold, color = GoldDark)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(R.string.outstanding), color = Color.Gray)
                Text(
                    text = "$balance $rupees",
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCollectPayment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                contentPadding = PaddingValues(12.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = stringResource(R.string.collected), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentDialog(customer: com.dodhi.data.model.Customer, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "${customer.name} - ${stringResource(R.string.collected)}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.total_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }) {
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
