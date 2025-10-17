package com.muratcangzm.monitor.ui.menus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.resources.R

@Composable
fun GhostViewModeAnchor(
    mode: ViewMode,
    onChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val accent = Color(0xFF7BD7FF)
    Box(modifier) {
        Surface(
            shape = shape,
            color = Color(0x22101826),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF233355).copy(alpha = 0.55f), accent.copy(alpha = 0.45f), Color(0xFF233355).copy(alpha = 0.55f)))),
            modifier = Modifier
                .clip(shape)
                .clickable { expanded = !expanded }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("▦", color = accent.copy(alpha = 0.9f))
                Text(if (mode == ViewMode.RAW) stringResource(R.string.view_mode_raw_short) else stringResource(R.string.view_mode_agg_short), color = Color(0xFF9EB2C0))
            }
        }
        DropdownMenu(containerColor = Color.Transparent, expanded = expanded, onDismissRequest = { expanded = false }) {
            Surface(
                color = Color(0xFF0F1726),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45))))
            ) {
                Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                    Text(stringResource(R.string.view_mode_menu_title), color = Color(0xFF9EB2C0), modifier = Modifier.padding(6.dp))
                    GhostMenuRow(
                        icon = "≋",
                        title = stringResource(R.string.view_mode_raw_title),
                        subtitle = stringResource(R.string.view_mode_raw_subtitle),
                        selected = mode == ViewMode.RAW
                    ) { onChange(ViewMode.RAW); expanded = false }
                    GhostMenuRow(
                        icon = "⧉",
                        title = stringResource(R.string.view_mode_agg_title),
                        subtitle = stringResource(R.string.view_mode_agg_subtitle),
                        selected = mode == ViewMode.AGGREGATED
                    ) { onChange(ViewMode.AGGREGATED); expanded = false }
                }
            }
        }
    }
}

@Composable
fun GhostMenuRow(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF7BD7FF)
    val textCol = if (selected) accent else Color(0xFFDBEAFE)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, color = accent)
        Column(Modifier.weight(1f)) {
            Text(title, color = textCol)
            Text(subtitle, color = Color(0xFF8EA0B5), style = MaterialTheme.typography.labelSmall)
        }
        if (selected) Text("✓", color = accent)
    }
}