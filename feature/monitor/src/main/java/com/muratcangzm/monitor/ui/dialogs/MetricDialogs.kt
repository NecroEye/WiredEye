package com.muratcangzm.monitor.ui.dialogs

import com.muratcangzm.monitor.model.StatKind
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MetricInfoDialog(
    kind: StatKind,
    valueProvider: () -> String,
    onDismiss: () -> Unit
) {
    val title: String
    val desc: String
    when (kind) {
        StatKind.Total -> { title = "Total (All-time)"; desc = "The total amount of data observed since the app was opened. It resets when you press 'Clear'." }
        StatKind.Apps -> { title = "Apps (Unique)"; desc = "The number of unique apps producing traffic in the selected window, subject to the filter and minimum-bytes threshold." }
        StatKind.Pps -> { title = "PPS (Packets per Second)"; desc = "The instantaneous average: total packet count in the selected time window divided by the window duration." }
        StatKind.Kbs -> { title = "KB/s (Throughput)"; desc = "The total bytes within the selected window normalized per second; shows the instantaneous transfer rate in KB/s." }
    }
    Surface(
        color = Color(0xF0141C2A),
        contentColor = Color(0xFFDBEAFE),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF233355), Color(0xFF3A4D77), Color(0xFF233355)))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF7BD7FF))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current:", color = Color(0xFF9EB2C0))
                Text(valueProvider(), color = Color(0xFF30E3A2), style = MaterialTheme.typography.titleMedium)
            }
            Text(desc, color = Color(0xFFB8C8D8))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}