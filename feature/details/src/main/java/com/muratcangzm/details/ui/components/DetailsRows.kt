package com.muratcangzm.details.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.muratcangzm.resources.ui.theme.GhostColors

@Composable
internal fun KeyValueRow(
    key: String,
    value: String,
    onCopy: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val copyState = rememberUpdatedState(onCopy)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = key, color = GhostColors.TextDim, modifier = Modifier.width(92.dp))
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(text = value.ifBlank { "—" }, color = GhostColors.TextBright)
        }
        if (copyState.value != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GhostColors.Surface,
                border = BorderStroke(1.dp, GhostColors.Accent.copy(alpha = 0.16f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { copyState.value?.invoke() }
            ) {
                Text(text = "⧉", color = GhostColors.TextBright, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
        } else {
            Spacer(Modifier.width(0.dp))
        }
    }
}
