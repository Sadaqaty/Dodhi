package com.dodhi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dodhi.R
import com.dodhi.data.model.Customer
import com.dodhi.ui.viewmodel.DashboardViewModel
import com.dodhi.ui.theme.*
import com.dodhi.ui.components.PremiumTextField
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAllTimeReportsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onDailyRunClick: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onAboutClick: () -> Unit
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(false) }

    val milkmanName by viewModel.milkmanName.collectAsState()
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("dodhi_prefs", android.content.Context.MODE_PRIVATE)

    // Check if it's first time
    LaunchedEffect(Unit) {
        val isFirstTime = prefs.getBoolean("is_first_time", true)
        if (isFirstTime) {
            showLanguagePicker = true
        }
    }

    // Refresh to today on entry to ensure totals/actions are correct
    LaunchedEffect(Unit) {
        viewModel.refreshToToday()
    }

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
        // 1. Scrollable Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                // Hero Header
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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onAboutClick) {
                            Icon(Icons.Default.Info, contentDescription = "About", tint = Color.White)
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ActionCard(
                            title = stringResource(R.string.khata_records),
                            subtitle = stringResource(R.string.khata_records_subtitle),
                            description = stringResource(R.string.khata_records_desc),
                            icon = R.drawable.ic_milk_collection_premium,
                            color = PastelGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onAllTimeReportsClick
                        )
                        ActionCard(
                            title = stringResource(R.string.reports),
                            subtitle = "Hisaab Reports",
                            description = stringResource(R.string.reports_desc),
                            icon = R.drawable.ic_reports_premium,
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
                            description = stringResource(R.string.add_customer_desc),
                            icon = R.drawable.ic_add_customer_premium,
                            color = PastelYellow,
                            modifier = Modifier.weight(1f),
                            onClick = onAddMemberClick
                        )
                        ActionCard(
                            title = stringResource(R.string.daily_run),
                            subtitle = "Start Delivery",
                            description = stringResource(R.string.daily_run_desc),
                            icon = R.drawable.cow_illustration,
                            color = PastelOrange,
                            modifier = Modifier.weight(1f),
                            onClick = onDailyRunClick
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
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
                    PremiumCustomerSummaryCard(customer, viewModel) {
                        onCustomerClick(customer.id)
                    }
                }
            }
        }

        if (showSettingsSheet) {
            SettingsSheet(viewModel = viewModel, onDismiss = { showSettingsSheet = false })
        }

        // Language picker shows first on first launch
        if (showLanguagePicker) {
            LanguagePickerDialog(
                onSelectLanguage = { languageTag ->
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                    showLanguagePicker = false
                    showTour = true
                }
            )
        }

        if (showTour) {
            AppTourDialog(
                onFinish = {
                    prefs.edit().putBoolean("is_first_time", false).apply()
                    showTour = false
                },
                onAddClick = onAddMemberClick,
                onRunClick = onDailyRunClick,
                onReportsClick = onReportsClick
            )
        }
    }
}

@Composable
fun LanguagePickerDialog(onSelectLanguage: (String) -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GrassGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.choose_language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EarthBrown,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.choose_language_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // English Button
                Button(
                    onClick = { onSelectLanguage("en") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("English", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Urdu Button
                OutlinedButton(
                    onClick = { onSelectLanguage("ur") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EarthBrown),
                    border = BorderStroke(2.dp, EarthBrown.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("اردو", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppTourDialog(
    onFinish: () -> Unit,
    onAddClick: () -> Unit,
    onRunClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        TourStep(R.string.tour_welcome_title, R.string.tour_welcome_desc, R.drawable.cow_illustration, GrassGreen),
        TourStep(R.string.tour_add_title, R.string.tour_add_desc, R.drawable.ic_add_customer_premium, Color(0xFF2196F3)),
        TourStep(R.string.tour_run_title, R.string.tour_run_desc, R.drawable.ic_milk_collection_premium, ClayTerracotta),
        TourStep(R.string.tour_reports_title, R.string.tour_reports_desc, R.drawable.ic_reports_premium, HeritageOlive),
        TourStep(R.string.tour_backup_title, R.string.tour_backup_desc, R.drawable.ic_reports_premium, EarthBrown)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scale"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Indicator
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.forEachIndexed { index, tourStep ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (index == step) 32.dp else 12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (index == step) tourStep.color else Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }

                // Icon with animated background
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(steps[step].color.copy(alpha = 0.08f))
                    )
                    Image(
                        painter = painterResource(id = steps[step].icon),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step number badge
                Surface(
                    color = steps[step].color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (step == 0) "✦" else "${stringResource(R.string.next)} $step/${steps.size - 1}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = steps[step].color
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(steps[step].title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = EarthBrown,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(steps[step].description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        TextButton(onClick = { step-- }) {
                            Text("← " + stringResource(R.string.next), color = Color.Gray)
                        }
                    } else {
                        TextButton(onClick = onFinish) {
                            Text(stringResource(R.string.skip), color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            if (step < steps.size - 1) {
                                step++
                            } else {
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = steps[step].color),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (step < steps.size - 1) stringResource(R.string.next) else stringResource(R.string.finish),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

data class TourStep(val title: Int, val description: Int, val icon: Int, val color: Color)

@Composable
fun PremiumCustomerSummaryCard(customer: Customer, viewModel: DashboardViewModel, onClick: () -> Unit) {
    val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
    
    // Semantics: 
    //   Consumer (we sell to them): balance > 0 means they OWE us → GREEN (good for us)
    //   Provider (we buy from them): balance > 0 means we OWE them → RED (we must pay)
    val balanceColor = when {
        balance == 0.0 -> EarthBrown.copy(alpha = 0.5f)
        customer.isProvider -> if (balance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
        else -> if (balance > 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    }
    
    val balanceLabel = when {
        balance == 0.0 -> "Clear"
        customer.isProvider -> if (balance > 0) "To Give" else "Overpaid"
        else -> if (balance > 0) "To Receive" else "Advance"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SerratedEdgeShape(serrationCount = 15))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 18.sp, 
                    color = ClayTerracotta,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(text = if (customer.isProvider) stringResource(R.string.providers) else stringResource(R.string.consumers), fontSize = 12.sp, color = EarthBrown.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${kotlin.math.abs(balance).toInt()} ${stringResource(R.string.rupees)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = balanceColor
                )
                Text(
                    text = balanceLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = balanceColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}


@Composable
fun ActionCard(
    title: String, 
    subtitle: String, 
    description: String,
    icon: Int, 
    color: Color, 
    modifier: Modifier, 
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(0.85f)
            .clickable(onClick = onClick)
            .shadow(4.dp, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EarthBrown,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = EarthBrown.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    val milkmanName by viewModel.milkmanName.collectAsState()
    val isMusicEnabled by viewModel.isMusicEnabled.collectAsState()
    var tempName by remember { mutableStateOf(milkmanName) }
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingBackupData by remember { mutableStateOf<com.dodhi.data.model.BackupData?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.previewImportBackup(context, uri) { backupData, error ->
                if (error != null) {
                    importError = error
                    showImportConfirm = true
                } else if (backupData != null) {
                    pendingBackupData = backupData
                    importError = null
                    showImportConfirm = true
                }
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingBackupData = null
                importError = null
            },
            title = {
                Text(
                    text = if (importError != null) "Import Error" else "Confirm Import",
                    fontWeight = FontWeight.Bold,
                    color = if (importError != null) Color.Red else EarthBrown
                )
            },
            text = {
                if (importError != null) {
                    Text(importError!!, color = Color.Gray)
                } else {
                    val data = pendingBackupData
                    Column {
                        Text(
                            text = "This will REPLACE all current data with the backup.",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        data?.let {
                            Text("Customers: ${it.customers?.size ?: 0}", fontSize = 14.sp)
                            Text("Records: ${it.transactions?.size ?: 0}", fontSize = 14.sp)
                            Text("Payments: ${it.payments?.size ?: 0}", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                .format(java.util.Date(it.timestamp))
                            Text("Backup from: $date", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                if (importError == null) {
                    Button(
                        onClick = {
                            pendingBackupData?.let { data ->
                                viewModel.executeImportBackup(context, data,
                                    onSuccess = {
                                        showImportConfirm = false
                                        pendingBackupData = null
                                        importError = null
                                        android.widget.Toast.makeText(context, "Data imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    onError = { error ->
                                        showImportConfirm = false
                                        pendingBackupData = null
                                        android.widget.Toast.makeText(context, "Import failed: $error", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Replace All Data", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingBackupData = null
                    importError = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = EarthBrown
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section 1: Profile
            SettingsSectionHeader(stringResource(R.string.profile_settings))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PremiumTextField(
                        value = tempName, 
                        onValueChange = { 
                            tempName = it
                            viewModel.updateMilkmanName(it)
                        }, 
                        label = stringResource(R.string.milkman_name)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section 2: App Preferences
            SettingsSectionHeader(stringResource(R.string.app_preferences))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column {
                    // Music Toggle
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.app_music), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(if (isMusicEnabled) stringResource(R.string.music_on) else stringResource(R.string.music_off)) },
                        leadingContent = { 
                            Icon(
                                imageVector = Icons.Default.Info, 
                                contentDescription = null,
                                tint = if (isMusicEnabled) GrassGreen else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        trailingContent = {
                            Switch(
                                checked = isMusicEnabled,
                                onCheckedChange = { viewModel.toggleMusic(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = GrassGreen, checkedTrackColor = GrassGreen.copy(alpha = 0.3f))
                            )
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                    
                    // Language Selection
                    LanguageSettingRow()
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section 3: Backup & Restore
            SettingsSectionHeader(stringResource(R.string.backup_restore))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
            ) {
                Column {
                    // Export Data
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.export_data), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(stringResource(R.string.export_data_desc)) },
                        leadingContent = { 
                            Icon(
                                imageVector = Icons.Default.Backup, 
                                contentDescription = null,
                                tint = GrassGreen,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.clickable {
                            viewModel.exportDataBackup(context)
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                    
                    // Import Data
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.import_data), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(stringResource(R.string.import_data_desc)) },
                        leadingContent = { 
                            Icon(
                                imageVector = Icons.Default.Restore, 
                                contentDescription = null,
                                tint = ClayTerracotta,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        modifier = Modifier.clickable {
                            importLauncher.launch("application/json")
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun LanguageSettingRow() {
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    
    Column {
        Text(
            text = stringResource(R.string.select_language),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        ListItem(
            headlineContent = { Text("English", fontWeight = FontWeight.Medium) },
            modifier = Modifier.clickable {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            },
            trailingContent = { if (currentLang != "ur") Icon(Icons.Default.Check, null, tint = GrassGreen) }
        )
        ListItem(
            headlineContent = { Text("اردو (Urdu)", fontWeight = FontWeight.Medium) },
            modifier = Modifier.clickable {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ur"))
            },
            trailingContent = { if (currentLang == "ur") Icon(Icons.Default.Check, null, tint = GrassGreen) }
        )
    }
}
