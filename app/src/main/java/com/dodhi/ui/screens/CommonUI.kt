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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType


@Composable
fun PremiumCustomerCard(
    customer: Customer, 
    balance: Double, 
    activeType: String?, 
    currentQuantity: Double, 
    onClick: () -> Unit, 
    onAction: (String, Double) -> Unit,
    onPayment: (Double) -> Unit
) {
    var rawQuantity by remember(currentQuantity) { mutableStateOf(currentQuantity.toString()) }
    var rawPayment by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(SerratedEdgeShape())
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = customer.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ClayTerracotta)
                    if (customer.locality.isNotEmpty()) {
                        Text(text = customer.locality, fontSize = 14.sp, color = EarthBrown.copy(alpha = 0.7f))
                    }
                    if (customer.isProvider) {
                        Surface(
                            color = OchreSand.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.buy_milk),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ClayTerracotta
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.baqaya), fontSize = 12.sp, color = EarthBrown.copy(alpha = 0.6f))
                    Text(
                        text = "$balance ${stringResource(R.string.rupees)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) Color(0xFFD32F2F) else HeritageOlive
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = rawQuantity,
                onValueChange = { rawQuantity = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.quantity), color = ClayTerracotta) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = ClayTerracotta,
                    unfocusedLabelColor = EarthBrown,
                    focusedBorderColor = ClayTerracotta,
                    unfocusedBorderColor = ClayTerracotta.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.5f),
                    unfocusedContainerColor = Color.Transparent
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeliveryButton(
                    label = stringResource(R.string.delivered),
                    color = HeritageOlive,
                    isActive = activeType == "Delivered",
                    modifier = Modifier.weight(1f)
                ) { onAction("Delivered", rawQuantity.toDoubleOrNull() ?: 0.0) }
                
                DeliveryButton(
                    label = stringResource(R.string.extra),
                    color = ClayTerracotta,
                    isActive = activeType == "Extra",
                    modifier = Modifier.weight(1f)
                ) { onAction("Extra", rawQuantity.toDoubleOrNull() ?: 0.0) }
                
                DeliveryButton(
                    label = stringResource(R.string.naga),
                    color = Color(0xFFD32F2F),
                    isActive = activeType == "Naga",
                    modifier = Modifier.weight(1f)
                ) { onAction("Naga", 0.0) }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = ClayTerracotta.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.payment_received),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ClayTerracotta,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rawPayment,
                    onValueChange = { rawPayment = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.0", color = EarthBrown.copy(alpha = 0.4f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeritageOlive,
                        unfocusedBorderColor = HeritageOlive.copy(alpha = 0.3f),
                        focusedContainerColor = HeritageOlive.copy(alpha = 0.05f)
                    )
                )
                
                Button(
                    onClick = { 
                        val amt = rawPayment.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onPayment(amt)
                            rawPayment = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeritageOlive),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
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
    val totalCollected by viewModel.todayTotalCollected.collectAsState(initial = 0.0)
    val totalRequired by viewModel.todayLitersNeeded.collectAsState(initial = 0.0)
    
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
                    text = "$totalCollected / $totalRequired",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (totalRequired > 0) (totalCollected / totalRequired).toFloat().coerceIn(0f, 1f) else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = GrassGreen,
                trackColor = PastelBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                    label = { Text(stringResource(R.string.quantity)) },
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

class SerratedEdgeShape(val serrationCount: Int = 25, val serrationHeight: Float = 12f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, serrationHeight)
            val segmentWidth = size.width / serrationCount
            for (i in 0 until serrationCount) {
                val x = i * segmentWidth
                lineTo(x + segmentWidth / 2, 0f)
                lineTo(x + segmentWidth, serrationHeight)
            }
            lineTo(size.width, size.height - serrationHeight)
            for (i in serrationCount downTo 1) {
                val x = (i - 1) * segmentWidth
                lineTo(x + segmentWidth / 2, size.height)
                lineTo(x, size.height - serrationHeight)
            }
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
