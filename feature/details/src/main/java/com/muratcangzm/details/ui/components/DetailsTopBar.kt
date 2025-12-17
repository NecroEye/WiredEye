package com.muratcangzm.details.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
internal fun DetailsTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val onBackState = rememberUpdatedState(onBack)
    Surface(
        color = GhostColors.Bg0.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GhostColors.Surface,
                border = BorderStroke(1.dp, GhostColors.Accent.copy(alpha = 0.16f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackState.value() }
            ) {
                Text(text = "←", color = GhostColors.TextBright, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, color = GhostColors.TextBright, maxLines = 1)
                Text(text = subtitle, color = GhostColors.TextDim, maxLines = 1)
            }
        }
    }
}
