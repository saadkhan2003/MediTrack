package com.meditrack.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meditrack.app.domain.model.DoseLog
import com.meditrack.app.domain.model.DoseStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoseLogItem(
    doseLog: DoseLog,
    modifier: Modifier = Modifier
) {
    val statusColor = when (doseLog.status) {
        DoseStatus.TAKEN -> Color(0xFF22875A)
        DoseStatus.MISSED -> Color(0xFFD32F2F)
        DoseStatus.PENDING -> Color(0xFF9E9E9E)
    }

    val statusIcon = when (doseLog.status) {
        DoseStatus.TAKEN -> Icons.Default.CheckCircle
        DoseStatus.MISSED -> Icons.Default.Close
        DoseStatus.PENDING -> Icons.Default.Schedule
    }

    val statusLabel = when (doseLog.status) {
        DoseStatus.TAKEN -> "Taken"
        DoseStatus.MISSED -> "Missed"
        DoseStatus.PENDING -> "Pending"
    }

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .semantics { contentDescription = "${doseLog.medicineName} dose log" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medicine name and time
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    contentDescription = statusLabel,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            androidx.compose.foundation.layout.Column {
                Text(
                    text = doseLog.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Scheduled: ${timeFormat.format(Date(doseLog.scheduledTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (doseLog.loggedTime != null) {
                    Text(
                        text = "Logged: ${timeFormat.format(Date(doseLog.loggedTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Status badge
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoseLogItemPreview() {
    DoseLogItem(
        doseLog = DoseLog(
            id = 1,
            medicineId = 1,
            medicineName = "Metformin 500mg",
            scheduledTime = System.currentTimeMillis(),
            loggedTime = System.currentTimeMillis(),
            status = DoseStatus.TAKEN
        )
    )
}
