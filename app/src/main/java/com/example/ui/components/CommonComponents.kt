package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveDeparture
import com.example.data.model.TransitStop
import com.example.data.model.VehicleType
import com.example.ui.theme.BusBadgeBg
import com.example.ui.theme.BusBadgeText
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.M3Border
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceElevated
import com.example.ui.theme.NightBadgeBg
import com.example.ui.theme.NightBadgeText
import com.example.ui.theme.PolishOnPrimaryContainer
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.PstBadgeBg
import com.example.ui.theme.PstBadgeText
import com.example.ui.theme.RealtimeGreen
import com.example.ui.theme.ScheduledGray
import com.example.ui.theme.SuburbanBadgeBg
import com.example.ui.theme.SuburbanBadgeText
import com.example.ui.theme.TextLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TramBadgeBg
import com.example.ui.theme.TramBadgeText
import com.example.ui.viewmodel.TransitFilterType

@Composable
fun RealtimePulseIndicator(
    modifier: Modifier = Modifier,
    text: String = "REAL-TIME"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PolishPrimaryContainer)
            .border(1.dp, M3Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .background(RealtimeGreen.copy(alpha = alpha), CircleShape)
        )
        Text(
            text = text.uppercase(),
            color = PolishPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun LineBadge(
    line: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val lineNum = line.toIntOrNull()
    val isPst = line in listOf("12", "14", "15", "16")
    
    val badgeBg = when {
        isPst -> PstBadgeBg
        lineNum != null && lineNum in 1..99 -> TramBadgeBg
        lineNum != null && lineNum in 200..299 -> NightBadgeBg
        lineNum != null && lineNum in 300..999 -> SuburbanBadgeBg
        else -> BusBadgeBg
    }
    
    val badgeTextColor = when {
        isPst -> PstBadgeText
        lineNum != null && lineNum in 1..99 -> TramBadgeText
        lineNum != null && lineNum in 200..299 -> NightBadgeText
        lineNum != null && lineNum in 300..999 -> SuburbanBadgeText
        else -> BusBadgeText
    }
    
    val borderColor = if (isSelected) PolishPrimary else if (isPst) PolishPrimary.copy(alpha = 0.5f) else Color.Transparent

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .testTag("line_badge_$line"),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PolishPrimary else badgeBg,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Text(
            text = line,
            color = if (isSelected) PolishOnPrimaryContainer else badgeTextColor,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopCard(
    stop: TransitStop,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stop_card_${stop.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = M3Surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Type Icon in Polish container
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (stop.hasPst) PolishPrimaryContainer else M3SurfaceElevated)
                            .border(1.dp, M3Border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stop.isTram) Icons.Default.Tram else Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = if (stop.hasPst) PolishPrimary else if (stop.isTram) PolishPrimary else PolishSecondaryContainer.let { Color(0xFFCCC2DC) },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stop.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (stop.code.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = M3SurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                                ) {
                                    Text(
                                        text = stop.code,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (stop.distanceMeters != null) {
                            val distText = if (stop.distanceMeters < 1000) {
                                "${stop.distanceMeters.toInt()} m"
                            } else {
                                String.format("%.1f km", stop.distanceMeters / 1000f)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PolishPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$distText od Ciebie",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (stop.description.isNotBlank()) {
                            Text(
                                text = stop.description,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Favorite Star Button inside rounded container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (stop.isFavorite) PolishPrimaryContainer else M3SurfaceElevated)
                        .border(1.dp, if (stop.isFavorite) PolishPrimary.copy(alpha = 0.5f) else M3Border, CircleShape)
                        .clickable(onClick = onToggleFavorite)
                        .testTag("favorite_button_${stop.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (stop.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Ulubiony przystanek",
                        tint = if (stop.isFavorite) PolishPrimary else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Lines List
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stop.lines.forEach { line ->
                    LineBadge(line = line)
                }
            }
        }
    }
}

@Composable
fun DepartureItemCard(
    departure: LiveDeparture,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("departure_item_${departure.id}"),
        shape = RoundedCornerShape(18.dp),
        color = M3Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (departure.minutesLeft <= 2) PolishPrimary.copy(alpha = 0.4f) else M3Border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Line Badge & Destination
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LineBadge(line = departure.line)

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = departure.direction.uppercase(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = departure.platform.ifBlank { "Plan: ${departure.departureTime}" },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        if (departure.isLowFloor) {
                            Icon(
                                imageVector = Icons.Default.Accessible,
                                contentDescription = "Niskopodłogowy",
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        if (departure.hasAirConditioning) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = "Klimatyzowany",
                                tint = PolishPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (departure.vehicleModel.isNotBlank()) {
                        Text(
                            text = departure.vehicleModel,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time & Realtime Countdown
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (departure.minutesLeft <= 0) {
                    RealtimePulseIndicator(text = "TERAZ")
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${departure.minutesLeft}",
                            color = PolishPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "min",
                            color = PolishPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (departure.isRealtime) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(RealtimeGreen, CircleShape)
                        )
                        Text(
                            text = if (departure.delayMinutes > 0) "+${departure.delayMinutes} min" else "REAL-TIME",
                            color = RealtimeGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "ROZKŁAD ${departure.departureTime}",
                            color = ScheduledGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: TransitFilterType,
    onFilterSelected: (TransitFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TransitFilterType.values(), key = { it.name }) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onFilterSelected(filter) }
                    .testTag("filter_chip_${filter.name}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) PolishPrimaryContainer else M3Surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) PolishPrimary else M3Border
                )
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected) PolishPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ExternalServicesCard(
    onOpenZtm: () -> Unit,
    onOpenJakdojade: () -> Unit,
    onOpenOpenData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = M3Surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsTransit,
                    contentDescription = null,
                    tint = PolishPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Oficjalne źródła i integracje online",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ZTM Poznań
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenZtm)
                        .testTag("link_ztm_poznan"),
                    shape = RoundedCornerShape(14.dp),
                    color = M3SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ZTM Poznań",
                            color = PolishPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Jakdojade
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenJakdojade)
                        .testTag("link_jakdojade"),
                    shape = RoundedCornerShape(14.dp),
                    color = M3SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jakdojade",
                            color = PolishPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Open Data Poznań
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenOpenData)
                        .testTag("link_opendata"),
                    shape = RoundedCornerShape(14.dp),
                    color = M3SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OpenData",
                            color = PolishPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClosestStopHeroCard(
    stop: TransitStop?,
    isLocating: Boolean,
    onStopClick: (TransitStop) -> Unit,
    onRefreshGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (stop != null) Modifier.clickable { onStopClick(stop) } else Modifier
            )
            .testTag("closest_stop_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = M3Surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PolishPrimary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PolishPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PolishPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "NAJBLIŻSZY PRZYSTANEK",
                            color = PolishPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (isLocating) "Wyszukiwanie GPS..." else "Wykryto automatycznie",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshGps,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("refresh_closest_gps_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Odśwież GPS",
                        tint = PolishPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (stop != null) {
                // Stop info
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stop.name,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (stop.code.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = M3SurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, M3Border)
                            ) {
                                Text(
                                    text = stop.code,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Distance & Walk time
                    if (stop.distanceMeters != null) {
                        val distanceM = stop.distanceMeters.toInt()
                        val walkMinutes = maxOf(1, (stop.distanceMeters / 80).toInt())
                        val distanceFormatted = if (distanceM < 1000) "$distanceM m" else String.format("%.1f km", stop.distanceMeters / 1000f)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PolishPrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsTransit,
                                        contentDescription = null,
                                        tint = PolishPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = distanceFormatted,
                                        color = PolishPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "•  ok. $walkMinutes min pieszo",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Lines
                if (stop.lines.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stop.lines.take(8).forEach { line ->
                            LineBadge(line = line)
                        }
                        if (stop.lines.size > 8) {
                            Text(
                                text = "+${stop.lines.size - 8}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                // CTA Button
                Button(
                    onClick = { onStopClick(stop) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_closest_departures_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PolishPrimary,
                        contentColor = PolishOnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Sprawdź odjazdy na żywo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "→", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            } else {
                Text(
                    text = if (isLocating) "Trwa ustalanie współrzędnych GPS i wyszukiwanie najbliższego przystanku..." else "Włącz GPS lub kliknij odśwież, aby automatycznie wykryć najbliższy przystanek.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
