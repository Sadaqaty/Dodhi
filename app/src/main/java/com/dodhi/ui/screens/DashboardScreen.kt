package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Check
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material.icons.Icons
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
import com.dodhi.ui.components.PremiumTextField
import androidx.compose.foundation.layout.aspectRatio
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMilkCollectionClick: () -> Unit,
    onReportsClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onMorningRunClick: () -> Unit
) {
    var showLanguageSheet by remember { mutableStateOf(false) }

    var filterType by remember { mutableStateOf("All") }
    val customers by viewModel.customers.collectAsState(initial = emptyList())
    
    val filteredCustomers = remember(customers, filterType) {
        when(filterType) {
            "Bought" -> customers.filter { it.isProvider }
            "Sold" -> customers.filter { !it.isProvider }
            else -> customers
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Hero Header
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Image(
                painter = painterResource(id = R.drawable.village_heritage_header),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Header Overlay content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "DODHI",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "DAIRY MANAGEMENT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 2.sp
                )
            }
            
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showLanguageSheet = true }) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = Color.White)
                }
            }
        }

        // 2. Action Grid with Filter Tabs
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionCard(
                            title = stringResource(R.string.milk_collection),
                            subtitle = "Quick Entries",
                            icon = R.drawable.clay_pot_icon,
                            color = PastelGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onMilkCollectionClick
                        )
                        ActionCard(
                            title = stringResource(R.string.reports),
                            subtitle = "Hisaab Reports",
                            icon = R.drawable.reports_icon,
                            color = PastelBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onReportsClick
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionCard(
                            title = stringResource(R.string.add_customer),
                            subtitle = "Add New Member",
                            icon = R.drawable.add_customer_icon,
                            color = PastelYellow,
                            modifier = Modifier.weight(1f),
                            onClick = onAddMemberClick
                        )
                        ActionCard(
                            title = stringResource(R.string.daily_run),
                            subtitle = "Start Delivery",
                            icon = R.drawable.cow_illustration,
                            color = PastelOrange,
                            modifier = Modifier.weight(1f),
                            onClick = onMorningRunClick
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.hisaab),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EarthBrown
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == "All",
                            onClick = { filterType = "All" },
                            label = { Text(stringResource(R.string.all_customers)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NatureGreen)
                        )
                        FilterChip(
                            selected = filterType == "Bought",
                            onClick = { filterType = "Bought" },
                            label = { Text(stringResource(R.string.providers)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EarthBrown, selectedLabelColor = Color.White)
                        )
                        FilterChip(
                            selected = filterType == "Sold",
                            onClick = { filterType = "Sold" },
                            label = { Text(stringResource(R.string.consumers)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyBlue)
                        )
                    }
                }
            }
            
            items(filteredCustomers) { customer ->
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    PremiumCustomerSummaryCard(customer, viewModel)
                }
            }
        }

        if (showLanguageSheet) {
            SettingsSheet(viewModel = viewModel, onDismiss = { showLanguageSheet = false })
        }
    }
}

@Composable
fun PremiumCustomerSummaryCard(customer: Customer, viewModel: DashboardViewModel) {
    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SerratedEdgeShape(serrationCount = 15))
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = customer.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = ClayTerracotta)
                Text(text = if (customer.isProvider) stringResource(R.string.providers) else stringResource(R.string.consumers), fontSize = 12.sp, color = EarthBrown.copy(alpha = 0.6f))
            }
            Text(
                text = "${balance.toInt()} ${stringResource(R.string.rupees)}",
                fontWeight = FontWeight.ExtraBold,
                color = if (balance > 0) Color(0xFFD32F2F) else HeritageOlive
            )
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .aspectRatio(1.3f)
            .clickable(onClick = onClick)
            .shadow(2.dp, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EarthBrown,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = EarthBrown.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    val milkmanName by viewModel.milkmanName.collectAsState()
    var tempName by remember { mutableStateOf(milkmanName) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = EarthBrown)
            Spacer(modifier = Modifier.height(24.dp))
            
            PremiumTextField(
                value = tempName, 
                onValueChange = { 
                    tempName = it
                    viewModel.updateMilkmanName(it)
                }, 
                label = stringResource(R.string.milkman_name)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EarthBrown)
            Spacer(modifier = Modifier.height(16.dp))
            
            val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            
            ListItem(
                headlineContent = { Text("English", fontWeight = FontWeight.Medium) },
                modifier = Modifier.clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    onDismiss()
                },
                trailingContent = { if (currentLang != "ur") Icon(Icons.Default.Check, null, tint = GrassGreen) }
            )
            ListItem(
                headlineContent = { Text("اردو (Urdu)", fontWeight = FontWeight.Medium) },
                modifier = Modifier.clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ur"))
                    onDismiss()
                },
                trailingContent = { if (currentLang == "ur") Icon(Icons.Default.Check, null, tint = GrassGreen) }
            )
        }
    }
}

