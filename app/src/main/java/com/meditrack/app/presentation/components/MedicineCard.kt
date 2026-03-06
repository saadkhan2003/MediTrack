package com.meditrack.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meditrack.app.domain.model.DoseStatus
import com.meditrack.app.domain.model.Frequency
import com.meditrack.app.domain.model.Medicine
import com.meditrack.app.presentation.screens.home.DoseSlot
import com.meditrack.app.presentation.screens.home.TodayDoseItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicineCard(
    item: TodayDoseItem,
    onTaken: (DoseSlot) -> Unit,
    onMissed: (DoseSlot) -> Unit,
    onDelete: (Medicine) -> Unit,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val medicine = item.medicine
    val cardColor = Color(medicine.color)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val glassBg = if (isDark) Color(android.graphics.Color.parseColor("#1A1F2D")) else Color(android.graphics.Color.parseColor("#FFFFFF"))
    val glassBorder = if (isDark) Color(android.graphics.Color.parseColor("#FFFFFF")) else Color(android.graphics.Color.parseColor("#FFFFFF"))

    var expanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "cardScale")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isHighlighted) 12.dp else 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = cardColor.copy(alpha = 0.25f)
            )
            .then(
                if (isHighlighted) {
                    Modifier.border(2.dp, cardColor, RoundedCornerShape(24.dp))
                } else {
                    Modifier.border(1.dp, glassBorder, RoundedCornerShape(24.dp))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { expanded = !expanded }
            .semantics { contentDescription = "Medicine card for ${medicine.name}" }
            .animateContentSize(animationSpec = tween(400)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = glassBg
        )
    ) {
        Column {
            // Header with rich gradient accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(cardColor.copy(alpha = 0.7f), cardColor)
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Medicine name and details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Indicator subtle dot
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(cardColor)
                                .shadow(2.dp, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = medicine.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${medicine.dosage} • ${medicine.frequency.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDelete(medicine) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .semantics { contentDescription = "Delete ${medicine.name}" }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete medicine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stock info pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLowStock = medicine.remainingStock <= medicine.refillThreshold
                    val stockColor = if (medicine.remainingStock <= 0) {
                        MaterialTheme.colorScheme.error
                    } else if (isLowStock) {
                        Color(0xFFF59E0B) // Warning amber
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }

                    Box(
                        modifier = Modifier
                            .background(stockColor.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Stock: ${medicine.remainingStock}/${medicine.totalStock}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = stockColor
                        )
                    }

                    if (medicine.remainingStock <= 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Out of stock",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Empty",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Time slots logic, expanded visually or implicitly
                AnimatedVisibility(
                    visible = expanded || item.slots.any { it.status == DoseStatus.PENDING },
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        item.slots.forEach { slot ->
                            DoseSlotRow(
                                slot = slot,
                                onTaken = { onTaken(slot) },
                                onMissed = { onMissed(slot) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseSlotRow(
    slot: DoseSlot,
    onTaken: () -> Unit,
    onMissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (slot.status) {
        DoseStatus.TAKEN -> Color(0xFF10B981) // Vibrant emerald
        DoseStatus.MISSED -> Color(0xFFEF4444) // Vibrant red
        DoseStatus.PENDING -> MaterialTheme.colorScheme.primary
    }

    val statusIcon = when (slot.status) {
        DoseStatus.TAKEN -> Icons.Default.CheckCircle
        DoseStatus.MISSED -> Icons.Default.Close
        DoseStatus.PENDING -> Icons.Default.Schedule
    }

    val statusLabel = when (slot.status) {
        DoseStatus.TAKEN -> "Taken"
        DoseStatus.MISSED -> "Missed"
        DoseStatus.PENDING -> "Pending"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (slot.status == DoseStatus.PENDING) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time and pending indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (slot.status == DoseStatus.PENDING) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = slot.timeString.ifEmpty {
                        SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(Date(slot.scheduledTimeMillis))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions or Status badge
            if (slot.status == DoseStatus.PENDING) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Action Buttons (Glassmorphic style)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFFEF4444), CircleShape)
                            .clickable { onMissed() }
                            .semantics { contentDescription = "Mark as missed" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF34D399), Color(0xFF10B981))
                                ),
                                CircleShape
                            )
                            .clickable { onTaken() }
                            .semantics { contentDescription = "Mark as taken" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // Completed status badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            statusIcon,
                            contentDescription = statusLabel,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineCardPreview() {
    val item = TodayDoseItem(
        medicine = Medicine(
            id = 1,
            name = "Metformin 500mg",
            dosage = "1 tablet",
            frequency = Frequency.TWICE_DAILY,
            scheduledTimes = listOf("08:00", "20:00"),
            startDate = System.currentTimeMillis(),
            totalStock = 30,
            remainingStock = 25,
            color = 0xFF1954A3.toInt()
        ),
        slots = listOf(
            DoseSlot(scheduledTimeMillis = 0L, status = DoseStatus.TAKEN, timeString = "08:00 AM"),
            DoseSlot(scheduledTimeMillis = 0L, status = DoseStatus.PENDING, timeString = "08:00 PM")
        )
    )
    MedicineCard(item = item, onTaken = {}, onMissed = {}, onDelete = {})
}
