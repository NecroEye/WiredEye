package com.muratcangzm.monitor.ui.dialogs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muratcangzm.monitor.model.StatKind

@Composable
fun MetricInfoDialog(
    kind: StatKind,
    valueProvider: () -> String,
    onDismiss: () -> Unit
) {

    val titleRes = when (kind) {
        StatKind.Total -> com.muratcangzm.resources.R.string.metric_total_title
        StatKind.Apps -> com.muratcangzm.resources.R.string.metric_apps_title
        StatKind.Pps -> com.muratcangzm.resources.R.string.metric_pps_title
        StatKind.Kbs -> com.muratcangzm.resources.R.string.metric_kbs_title
    }

    val descRes = when (kind) {
        StatKind.Total -> com.muratcangzm.resources.R.string.metric_total_desc
        StatKind.Apps -> com.muratcangzm.resources.R.string.metric_apps_desc
        StatKind.Pps -> com.muratcangzm.resources.R.string.metric_pps_desc
        StatKind.Kbs -> com.muratcangzm.resources.R.string.metric_kbs_desc
    }

    Surface(
        color = Color(0xF0141C2A),
        contentColor = Color(0xFFDBEAFE),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color(0xFF233355), Color(0xFF3A4D77), Color(0xFF233355)))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF7BD7FF),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current:", color = Color(0xFF9EB2C0))
                Text(
                    valueProvider(),
                    color = Color(0xFF30E3A2),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(stringResource(descRes), color = Color(0xFFB8C8D8))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}