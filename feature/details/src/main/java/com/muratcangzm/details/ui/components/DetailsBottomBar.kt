package com.muratcangzm.details.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.muratcangzm.resources.ui.theme.GhostColors

@Composable
internal fun DetailsBottomBar(
    copyLabel: String,
    shareLabel: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val onCopyState = rememberUpdatedState(onCopy)
    val onShareState = rememberUpdatedState(onShare)

    Surface(
        color = GhostColors.Bg0.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionPill(text = copyLabel, modifier = Modifier.weight(1f)) { onCopyState.value() }
            ActionPill(text = shareLabel, modifier = Modifier.weight(1f)) { onShareState.value() }
        }
    }
}

@Composable
private fun ActionPill(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = GhostColors.Surface,
        border = BorderStroke(1.dp, GhostColors.Accent.copy(alpha = 0.28f)),
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = GhostColors.TextBright)
        }
    }
}
