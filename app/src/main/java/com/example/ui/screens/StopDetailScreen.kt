package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.StopDataSource
import com.example.ui.components.DepartureItemCard
import com.example.ui.components.LineBadge
import com.example.ui.components.RealtimePulseIndicator
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3Border
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceElevated
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PoznanAmber
import com.example.ui.theme.PoznanCyan
import com.example.ui.theme.PoznanGreen
import com.example.ui.theme.PoznanRed
import com.example.ui.theme.RealtimeGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TransitViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    viewModel: TransitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val stop = uiState.selectedStop
    val details = uiState.stopDetails

    if (stop == null) {
        Box(
            modifier = modifier.fillMaxSize().background(M3Background),
            contentAlignment = Alignment.Center
        ) {
            Text("Nie wybrano przystanku", color = TextSecondary)
        }
        return
    }

    val departures = details?.departures?.filter { dep ->
        uiState.selectedLineFilter == null || dep.line == uiState.selectedLineFilter
    } ?: emptyList()

    val sourceBadgeText = when (details?.dataSource) {
        StopDataSource.LIVE_API -> "PEKA LIVE"
        StopDataSource.ONLINE_NO_DEPARTURES -> "PEKA ONLINE"
        StopDataSource.FALLBACK_GENERATED -> "ROZKŁAD GTFS"
        null -> "ŁADOWANIE"
    }
    val sourceDescriptionText = when (details?.dataSource) {
        StopDataSource.LIVE_API -> "ZTM Poznań API"
        StopDataSource.ONLINE_NO_DEPARTURES -> "API online • Brak odjazdów teraz"
        StopDataSource.FALLBACK_GENERATED -> "Rozkład statyczny"
        null -> "Pobieranie danych"
    }

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val lastUpdatedTime = details?.lastUpdated?.let { timeFormatter.format(Date(it)) } ?: "Teraz"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(M3Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stop.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = if (stop.hasPst) "PST Poznański Szybki Tramwaj" else "Słupek ${stop.code} • ${stop.zone}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.toggleFavorite(stop) },
                    modifier = Modifier.testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (stop.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Dodaj do ulubionych",
                        tint = if (stop.isFavorite) PolishPrimary else TextTertiary
                    )
                }
                IconButton(
                    onClick = { viewModel.refreshDepartures() },
                    modifier = Modifier.testTag("refresh_departures_button")
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            color = PolishPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Odśwież",
                            tint = PolishPrimary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = M3Surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = M3Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RealtimePulseIndicator(text = sourceBadgeText)
                            Text(
                                text = sourceDescriptionText,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Aktualizacja: $lastUpdatedTime",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Line Filter selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filtruj linie na tym przystanku:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { viewModel.toggleLineFilter("") }
                                .testTag("line_filter_all"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.selectedLineFilter == null) PolishPrimaryContainer else M3Surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.selectedLineFilter == null) PolishPrimary else M3Border
                            )
                        ) {
                            Text(
                                text = "Wszystkie (${stop.lines.size})",
                                color = if (uiState.selectedLineFilter == null) PolishPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        stop.lines.forEach { line ->
                            LineBadge(
                                line = line,
                                isSelected = uiState.selectedLineFilter == line,
                                onClick = { viewModel.toggleLineFilter(line) }
                            )
                        }
                    }
                }
            }

            // Alerts
            if (details != null && details.alerts.isNotEmpty()) {
                items(details.alerts, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = M3Surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (alert.severity) {
                                AlertSeverity.CRITICAL -> PoznanRed
                                AlertSeverity.WARNING -> PoznanAmber
                                AlertSeverity.INFO -> PolishPrimary
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (alert.severity) {
                                    AlertSeverity.CRITICAL -> Icons.Default.Warning
                                    AlertSeverity.WARNING -> Icons.Default.Warning
                                    AlertSeverity.INFO -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (alert.severity) {
                                    AlertSeverity.CRITICAL -> PoznanRed
                                    AlertSeverity.WARNING -> PoznanAmber
                                    AlertSeverity.INFO -> PolishPrimary
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = alert.title,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = alert.message,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Departures list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Najbliższe odjazdy (${departures.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatyczne odświeżanie 15s",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Departures
            if (uiState.isDeparturesLoading && departures.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PolishPrimary)
                    }
                }
            } else if (departures.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = M3Surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Brak planowanych odjazdów",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (details?.dataSource == StopDataSource.ONLINE_NO_DEPARTURES) {
                                    "API online, ale w tej chwili brak aktywnych kursów dla tego przystanku."
                                } else {
                                    "W najbliższym czasie dla wybranego filtra nie znaleziono kursów."
                                },
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(departures, key = { it.id }) { departure ->
                    DepartureItemCard(departure = departure)
                }
            }

            // External integration shortcuts for this stop
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Zewnętrzne serwisy dla przystanku",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.openExternalUrl(
                                    context,
                                    "https://jakdojade.pl/poznan/trasa/"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("jakdojade_stop_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Surface,
                                contentColor = PolishPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jakdojade", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.openExternalUrl(
                                    context,
                                    "https://www.ztm.poznan.pl/rozklad-jazdy/"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ztm_schedule_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Surface,
                                contentColor = PolishPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rozkład ZTM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
