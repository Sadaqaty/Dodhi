package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: DashboardViewModel, onCustomerClick: (Long) -> Unit) {
    val customers by viewModel.customers.collectAsState()
    val summary by viewModel.getCollectionSummary().collectAsState(initial = null)
    val dailyVolume by viewModel.getDailyVolumeInRange().collectAsState(initial = emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reports), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GrassGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.exportDataBackup(context) }) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup Data",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profit Summary Banner
            item {
                ProfitBanner(summary)
            }

            // 2. Quick Stats Row
            item {
                QuickStatsRow(summary)
            }

            // 3. Cash Flow Section
            item {
                Text(
                    text = "Cash Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
                Spacer(modifier = Modifier.height(8.dp))
                CashFlowSection(summary)
            }

            // 4. Volume Chart
            item {
                Text(
                    text = stringResource(R.string.delivery_volume),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
                Spacer(modifier = Modifier.height(8.dp))
                VolumeBarChart(dailyVolume)
            }

            // 5. Customer Reports
            item {
                Text(
                    text = stringResource(R.string.all_customers),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EarthBrown
                )
            }

            items(customers) { customer ->
                val total by viewModel.getMonthlyBill(customer.id).collectAsState(initial = 0.0)
                val balance by viewModel.getCustomerBalance(customer.id).collectAsState(initial = 0.0)
                PremiumReportCard(
                    customer = customer,
                    total = total,
                    balance = balance,
                    viewModel = viewModel,
                    onClick = { onCustomerClick(customer.id) }
                )
            }
        }
    }
}

@Composable
fun ProfitBanner(summary: DashboardViewModel.CollectionSummary?) {
    val netProfit = summary?.netProfit ?: 0.0
    val isProfit = netProfit >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProfit) GrassGreen else Color(0xFFD32F2F)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(if (isProfit) R.string.profit_banner_title else R.string.loss_banner_title),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${netProfit.toInt()} ${stringResource(R.string.rupees)}",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.sales_label)}: ${(summary?.totalSales ?: 0.0).toInt()}  •  ${stringResource(R.string.purchases_label)}: ${(summary?.totalPurchases ?: 0.0).toInt()}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun QuickStatsRow(summary: DashboardViewModel.CollectionSummary?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatCard(
            label = stringResource(R.string.milk_sold),
            value = "${summary?.totalLiters ?: 0.0} L",
            color = GrassGreen,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            label = stringResource(R.string.active_customers),
            value = "${summary?.activeCustomers ?: 0}",
            color = HeritageOlive,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            label = stringResource(R.string.off_days),
            value = "${summary?.nagaCount ?: 0}",
            color = Color(0xFFD32F2F),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun CashFlowSection(summary: DashboardViewModel.CollectionSummary?) {
    val toCollect = summary?.toCollect ?: 0.0
    val toPay = summary?.toPay ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // To Collect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.to_collect), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(stringResource(R.string.from_customers), fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))
                }
                Text(
                    text = "${toCollect.toInt()} ${stringResource(R.string.rupees)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (toCollect > 0) Color(0xFFD32F2F) else GrassGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // To Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.to_pay), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(stringResource(R.string.to_providers), fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))
                }
                Text(
                    text = "${toPay.toInt()} ${stringResource(R.string.rupees)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (toPay > 0) ClayTerracotta else GrassGreen
                )
            }
        }
    }
}

@Composable
fun VolumeBarChart(data: List<DashboardViewModel.DayVolume>) {
    val maxVolume = (data.maxByOrNull { it.volume }?.volume ?: 10.0).coerceAtLeast(1.0)
    val dayFmt = remember { SimpleDateFormat("dd", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalVolume = data.sumOf { it.volume }
                Text("${totalVolume} L ${stringResource(R.string.total_volume)}", fontSize = 13.sp, color = EarthBrown, fontWeight = FontWeight.Bold)
                Text("${data.size} ${stringResource(R.string.days_label)}", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                data.takeLast(21).forEach { day ->
                    val barHeight = (day.volume / maxVolume).toFloat()
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeight.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(GrassGreen)
                        )
                        Text(
                            text = dayFmt.format(Date(day.date)),
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumReportCard(customer: com.dodhi.data.model.Customer, total: Double, balance: Double, viewModel: DashboardViewModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ParchiPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ClayTerracotta.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                 Row(
                     verticalAlignment = Alignment.CenterVertically,
                     modifier = Modifier.fillMaxWidth()
                 ) {
                     Text(
                         text = customer.name,
                         fontSize = 18.sp,
                         fontWeight = FontWeight.Bold,
                         color = EarthBrown,
                         modifier = Modifier.weight(1f, fill = false),
                         maxLines = 1,
                         overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                     )
                     Spacer(modifier = Modifier.width(8.dp))
                     CustomerTypeTag(customer.isProvider)
                 }
                Text(text = customer.locality.ifEmpty { stringResource(R.string.miscellaneous) }, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${total.toInt()} ${stringResource(R.string.rupees)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (customer.isProvider) ClayTerracotta else GrassGreen
                )
                Text(
                    text = "${balance.toInt()} ${stringResource(R.string.rupees)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (balance > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            val context = LocalContext.current
            IconButton(
                onClick = {
                    val isSystemUrdu = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("ur")
                    viewModel.shareTextReportViaWhatsApp(context, customer, isSystemUrdu)
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_whatsapp),
                    contentDescription = "Share via WhatsApp",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerTypeTag(isProvider: Boolean) {
    val text = if (isProvider) stringResource(R.string.seller) else stringResource(R.string.buyer)
    val color = if (isProvider) EarthBrown else GrassGreen

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
