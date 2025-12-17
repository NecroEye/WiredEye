package com.muratcangzm.details.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.muratcangzm.resources.ui.theme.GhostColors

private val Shape = RoundedCornerShape(18.dp)

@Composable
internal fun DetailsSectionCard(
    title: String,
    trailing: String? = null,
    onHeaderClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = remember {
        Brush.linearGradient(
            listOf(
                GhostColors.StrokeBase.copy(alpha = 0.55f),
                GhostColors.Accent.copy(alpha = 0.35f),
                GhostColors.StrokeBase.copy(alpha = 0.55f)
            )
        )
    }
    Surface(
        shape = Shape,
        color = GhostColors.Surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = onHeaderClick != null) { onHeaderClick?.invoke() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = GhostColors.TextBright, modifier = Modifier.weight(1f))
                if (!trailing.isNullOrBlank()) Text(text = trailing, color = GhostColors.TextDim)
            }
            content()
        }
    }
}
