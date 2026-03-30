package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.dodhi.data.model.Customer
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

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
                    containerColor = if (isSelected) GrassGreen else Color.White
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
                        color = if (isSelected) Color.White else Color.Gray
                    )
                    Text(
                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumCustomerCard(
    customer: Customer, 
    balance: Double, 
    activeType: String?, 
    currentQuantity: Double, 
    onClick: () -> Unit, 
    onAction: (String, Double) -> Unit
) {
    var rawQuantity by remember(currentQuantity) { mutableStateOf(currentQuantity.toString()) }
    
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customer.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = EarthBrown)
                    if (customer.locality.isNotEmpty()) {
                        Text(text = customer.locality, fontSize = 14.sp, color = Color.Gray)
                    }
                    if (customer.isProvider) {
                        Surface(
                            color = PastelOrange.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.buy_milk),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EarthBrown
                            )
                        }
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
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = rawQuantity,
                    onValueChange = { rawQuantity = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.liters)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = PastelBlue.copy(alpha = 0.1f)
                    )
                )
                
                Text(
                    text = if (customer.unit == "Liter") stringResource(R.string.liters) else stringResource(R.string.ser),
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeliveryButton(
                    label = stringResource(R.string.delivered),
                    color = Color(0xFF4CAF50),
                    isActive = activeType == "Delivered",
                    modifier = Modifier.weight(1f)
                ) { onAction("Delivered", rawQuantity.toDoubleOrNull() ?: 0.0) }
                
                DeliveryButton(
                    label = stringResource(R.string.extra),
                    color = Color(0xFFFFD700),
                    isActive = activeType == "Extra",
                    modifier = Modifier.weight(1f)
                ) { onAction("Extra", rawQuantity.toDoubleOrNull() ?: 0.0) }
                
                DeliveryButton(
                    label = stringResource(R.string.naga),
                    color = Color(0xFFF44336),
                    isActive = activeType == "Naga",
                    modifier = Modifier.weight(1f)
                ) { onAction("Naga", 0.0) }
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
                    color = EarthBrown
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (litersNeeded > 0) (litersDelivered / litersNeeded).toFloat().coerceIn(0f, 1f) else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = GrassGreen,
                trackColor = PastelBlue
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
