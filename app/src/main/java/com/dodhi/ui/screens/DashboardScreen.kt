package com.dodhi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.foundation.layout.aspectRatio
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMilkCollectionClick: () -> Unit,
    onReportsClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onMorningRunClick: () -> Unit
) {
    var showLanguageSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Hero Header
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            Image(
                painter = painterResource(id = R.drawable.farm_header),
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

        // 2. Action Grid
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = stringResource(R.string.milk_collection),
                    icon = R.drawable.ic_milk_can,
                    color = PastelGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onMilkCollectionClick
                )
                ActionCard(
                    title = stringResource(R.string.reports),
                    icon = R.drawable.ic_reports,
                    color = PastelBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onReportsClick
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = stringResource(R.string.add_customer),
                    icon = R.drawable.ic_add_member,
                    color = PastelYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onAddMemberClick
                )
                ActionCard(
                    title = stringResource(R.string.morning_run),
                    icon = R.drawable.ic_cow,
                    color = PastelOrange,
                    modifier = Modifier.weight(1f),
                    onClick = onMorningRunClick
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = stringResource(R.string.settings),
                    icon = R.drawable.ic_settings_thematic,
                    color = PastelPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { showLanguageSheet = true }
                )
                // Placeholder or extra card
                Box(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showLanguageSheet) {
        LanguageSheet(onDismiss = { showLanguageSheet = false })
    }
}

@Composable
fun ActionCard(title: String, icon: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .shadow(4.dp, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.large
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = EarthBrown,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Select Language / زبان منتخب کریں", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            
            ListItem(
                headlineContent = { Text("English") },
                modifier = Modifier.clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    onDismiss()
                },
                trailingContent = { if (currentLang != "ur") Icon(Icons.Default.Add, null) }
            )
            ListItem(
                headlineContent = { Text("اردو (Urdu)") },
                modifier = Modifier.clickable {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ur"))
                    onDismiss()
                },
                trailingContent = { if (currentLang == "ur") Icon(Icons.Default.Add, null) }
            )
        }
    }
}

