package com.meditrack.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meditrack.app.domain.usecase.DailyAdherence
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AdherenceChart(
    dailyData: List<DailyAdherence>,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) {
        Text(
            text = "No data available for chart",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val takenColor = Color(0xFF22875A)
    val missedColor = Color(0xFFD32F2F)
    val maxValue = dailyData.maxOf { (it.taken + it.missed).coerceAtLeast(1) }.toFloat()
    val formatter = DateTimeFormatter.ofPattern("EEE")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Adherence bar chart" }
    ) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
                drawRect(takenColor)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Taken", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
                drawRect(missedColor)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Missed", style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val barWidth = (size.width - 48.dp.toPx()) / (dailyData.size * 3f)
            val chartHeight = size.height - 30.dp.toPx()
            val startX = 24.dp.toPx()

            dailyData.forEachIndexed { index, day ->
                val groupX = startX + index * (barWidth * 3)

                // Taken bar
                val takenHeight = if (maxValue > 0) (day.taken / maxValue) * chartHeight else 0f
                drawRect(
                    color = takenColor,
                    topLeft = Offset(groupX, chartHeight - takenHeight),
                    size = Size(barWidth, takenHeight)
                )

                // Missed bar
                val missedHeight = if (maxValue > 0) (day.missed / maxValue) * chartHeight else 0f
                drawRect(
                    color = missedColor,
                    topLeft = Offset(groupX + barWidth + 2.dp.toPx(), chartHeight - missedHeight),
                    size = Size(barWidth, missedHeight)
                )

                // Day label
                drawContext.canvas.nativeCanvas.drawText(
                    day.date.format(formatter),
                    groupX + barWidth,
                    size.height,
                    android.graphics.Paint().apply {
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = android.graphics.Color.GRAY
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdherenceChartPreview() {
    val data = listOf(
        DailyAdherence(LocalDate.now().minusDays(6), taken = 3, missed = 1),
        DailyAdherence(LocalDate.now().minusDays(5), taken = 4, missed = 0),
        DailyAdherence(LocalDate.now().minusDays(4), taken = 2, missed = 2),
        DailyAdherence(LocalDate.now().minusDays(3), taken = 4, missed = 0),
        DailyAdherence(LocalDate.now().minusDays(2), taken = 3, missed = 1),
        DailyAdherence(LocalDate.now().minusDays(1), taken = 1, missed = 3),
        DailyAdherence(LocalDate.now(), taken = 2, missed = 0)
    )
    AdherenceChart(dailyData = data, modifier = Modifier.padding(16.dp))
}
