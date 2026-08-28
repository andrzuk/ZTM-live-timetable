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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.TransitStop
import com.example.ui.components.ExternalServicesCard
import com.example.ui.components.FilterChipsRow
import com.example.ui.components.LineBadge
import com.example.ui.components.RealtimePulseIndicator
import com.example.ui.components.StopCard
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
import com.example.ui.theme.RealtimeGreen
import com.example.ui.theme.TextLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TransitFilterType
import com.example.ui.viewmodel.TransitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransitViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favoriteStops.collectAsState()
    val filteredStops by viewModel.filteredStopsState.collectAsState()
    val nearbyStops by viewModel.nearbyStops.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(M3Background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header - Professional Polish design
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "REAL-TIME GTFS",
                            color = PolishPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = "Poznań GO",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    RealtimePulseIndicator()
                }

                // GPS Location Status Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.refreshUserLocation() }
                        .testTag("location_status_bar"),
                    color = M3Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (uiState.userLat != null) PolishPrimary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = uiState.locationMessage ?: "Wykrywanie GPS...",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "Odśwież GPS",
                            color = PolishPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Search Input - Polish rounded input
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text("Szukaj przystanku (np. Fredry)...", color = TextSecondary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Szukaj",
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wyczyść",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = M3Surface,
                    unfocusedContainerColor = M3Surface,
                    focusedBorderColor = PolishPrimary,
                    unfocusedBorderColor = M3Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        // Filter Chips
        item {
            FilterChipsRow(
                selectedFilter = uiState.activeFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )
        }

        // Search active mode -> show matched stops directly
        if (uiState.searchQuery.isNotBlank() || uiState.activeFilter != TransitFilterType.ALL) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wyniki wyszukiwania (${filteredStops.size})",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(filteredStops, key = { it.id }) { stop ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StopCard(
                        stop = stop,
                        onClick = { viewModel.selectStop(stop) },
                        onToggleFavorite = { viewModel.toggleFavorite(stop) }
                    )
                }
            }
        } else {
            // Standard Home View

            // Favorites Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Ulubione przystanki",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${favorites.size} zapisanych",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (favorites.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = M3Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Brak ulubionych przystanków",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Kliknij gwiazdkę przy dowolnym przystanku, aby mieć szybki dostęp do jego rozkładu na żywo.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites, key = { it.id }) { favStop ->
                                Surface(
                                    modifier = Modifier
                                        .width(230.dp)
                                        .clickable { viewModel.selectStop(favStop) }
                                        .testTag("fav_chip_${favStop.id}"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = M3Surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PolishPrimary.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = favStop.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(PolishPrimaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = PolishPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            favStop.lines.take(4).forEach { line ->
                                                LineBadge(line = line)
                                            }
                                            if (favStop.lines.size > 4) {
                                                Text(
                                                    text = "+${favStop.lines.size - 4}",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Odjazdy na żywo →",
                                            color = PolishPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Nearby Stops Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = PolishPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "W Twojej okolicy (GPS)",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "Zobacz wszystkie →",
                            color = PolishPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.NEARBY) }
                        )
                    }
                }
            }

            // Top 4 nearest stops
            val topNearby = nearbyStops.take(4)
            items(topNearby, key = { it.id }) { stop ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StopCard(
                        stop = stop,
                        onClick = { viewModel.selectStop(stop) },
                        onToggleFavorite = { viewModel.toggleFavorite(stop) }
                    )
                }
            }

            // External Services Section
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExternalServicesCard(
                        onOpenZtm = {
                            viewModel.openExternalUrl(context, "https://www.ztm.poznan.pl/rozklad-jazdy/")
                        },
                        onOpenJakdojade = {
                            viewModel.openExternalUrl(context, "https://jakdojade.pl/poznan/trasa/")
                        },
                        onOpenOpenData = {
                            viewModel.openExternalUrl(context, "https://www.poznan.pl/opendata/data-item/e204a7f3-8cd7-4bb2-b42f-66317a7e0f09")
                        }
                    )
                }
            }
        }
    }
}

