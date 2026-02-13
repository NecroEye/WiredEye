package com.muratcangzm.details

import android.content.Intent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.common.nav.Screens
import com.muratcangzm.details.ui.components.DetailsBackground
import com.muratcangzm.details.ui.components.DetailsBottomBar
import com.muratcangzm.details.ui.components.DetailsSectionCard
import com.muratcangzm.details.ui.components.DetailsTopBar
import com.muratcangzm.details.ui.components.KeyValueRow
import com.muratcangzm.details.utils.defaultTimeFormatter
import com.muratcangzm.details.utils.endpoint
import com.muratcangzm.details.utils.formatBytes
import com.muratcangzm.details.utils.formatTime
import com.muratcangzm.resources.ui.theme.GhostColors
import com.muratcangzm.ui.components.StatusBarStyle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.muratcangzm.resources.R as Res

@Suppress("ParamsComparedByRef")
@Composable
fun DetailsScreen(
    homeViewModel: HomeViewModel,
    detailsViewModel: DetailsViewModel = koinViewModel(),
    arguments: Screens.DetailsScreen,
) {
    val state by detailsViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(arguments) {
        detailsViewModel.onEvent(DetailsContract.Event.SetArguments(arguments))
    }

    DetailsScreenContent(
        state = state,
        onToggleRaw = { detailsViewModel.onEvent(DetailsContract.Event.ToggleRawExpanded) }
    )
}

@Composable
fun DetailsScreenContent(
    state: DetailsContract.State,
    onToggleRaw: () -> Unit
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val formatter = remember { defaultTimeFormatter() }

    val packet = state.uiPacket
    val raw = packet.raw

    val detailsTitle = stringResource(Res.string.details_title)
    val copyLabel = stringResource(Res.string.details_action_copy)
    val shareLabel = stringResource(Res.string.details_action_share)
    val shareChooserTitle = stringResource(Res.string.details_share_chooser)
    val sharingFailed = stringResource(Res.string.sharing_failed)

    val labelFrom = stringResource(Res.string.details_label_from)
    val labelTo = stringResource(Res.string.details_label_to)
    val labelBytes = stringResource(Res.string.details_label_bytes)
    val labelLocal = stringResource(Res.string.details_label_local)
    val labelRemote = stringResource(Res.string.details_label_remote)
    val labelTls = stringResource(Res.string.details_label_tls)
    val labelSni = stringResource(Res.string.details_label_sni)

    val labelKey = stringResource(Res.string.details_label_key)
    val labelTimestamp = stringResource(Res.string.details_label_timestamp)
    val labelUid = stringResource(Res.string.details_label_uid)
    val labelPackage = stringResource(Res.string.details_label_package)
    val labelProtocol = stringResource(Res.string.details_label_protocol)
    val labelDir = stringResource(Res.string.details_label_dir)

    val yes = stringResource(Res.string.details_yes)
    val no = stringResource(Res.string.details_no)

    val copiedDetails = stringResource(Res.string.copied_snackbar, detailsTitle)
    val copiedFrom = stringResource(Res.string.copied_snackbar, labelFrom)
    val copiedTo = stringResource(Res.string.copied_snackbar, labelTo)

    val shareHeader = stringResource(Res.string.details_share_header)
    val shareAppFmt = stringResource(Res.string.details_share_app, "%1\$s")
    val shareMetaFmt = stringResource(Res.string.details_share_meta, "%1\$s")
    val shareFromFmt = stringResource(Res.string.details_share_from, "%1\$s")
    val shareToFmt = stringResource(Res.string.details_share_to, "%1\$s")
    val shareBytesFmt = stringResource(Res.string.details_share_bytes, "%1\$s")
    val shareKeyFmt = stringResource(Res.string.details_share_key, "%1\$s")

    val title = remember(packet.app, raw.packageName, detailsTitle) {
        packet.app.ifBlank { raw.packageName.orEmpty().ifBlank { detailsTitle } }
    }
    val subtitle = remember(packet.proto, raw.protocol, raw.timestamp, formatter) {
        val p = packet.proto.ifBlank { raw.protocol.ifBlank { "?" } }
        val t = formatTime(raw.timestamp, formatter)
        "$p • $t"
    }
    val from = remember(packet.from, raw.localAddress, raw.localPort) {
        packet.from.ifBlank { endpoint(raw.localAddress, raw.localPort) }
    }
    val to = remember(packet.to, raw.remoteAddress, raw.remotePort) {
        packet.to.ifBlank { endpoint(raw.remoteAddress, raw.remotePort) }
    }
    val bytes = remember(packet.bytesLabel, raw.bytes) {
        packet.bytesLabel.ifBlank { formatBytes(raw.bytes) }
    }

    val shareText = remember(
        packet.key,
        title,
        subtitle,
        from,
        to,
        bytes,
        shareHeader,
        shareAppFmt,
        shareMetaFmt,
        shareFromFmt,
        shareToFmt,
        shareBytesFmt,
        shareKeyFmt
    ) {
        buildString {
            appendLine(shareHeader)
            appendLine(shareAppFmt.format(title))
            appendLine(shareMetaFmt.format(subtitle))
            appendLine(shareFromFmt.format(from))
            appendLine(shareToFmt.format(to))
            appendLine(shareBytesFmt.format(bytes))
            appendLine(shareKeyFmt.format(packet.key))
        }
    }

    StatusBarStyle(
        color = GhostColors.Bg0,
        darkIcons = false
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DetailsTopBar(
                title = title,
                subtitle = subtitle,
                onBack = { backDispatcher?.onBackPressed() }
            )
        },
        bottomBar = {
            DetailsBottomBar(
                copyLabel = copyLabel,
                shareLabel = shareLabel,
                onCopy = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(shareText))
                    scope.launch { snackbar.showSnackbar(copiedDetails) }
                },
                onShare = {
                    runCatching {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(i, shareChooserTitle))
                    }.onFailure {
                        scope.launch { snackbar.showSnackbar(sharingFailed) }
                    }
                }
            )
        }
    ) { padding ->
        DetailsBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 84.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DetailsSectionCard(title = stringResource(Res.string.details_section_packet)) {
                        KeyValueRow(labelFrom, from, onCopy = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(from))
                            scope.launch { snackbar.showSnackbar(copiedFrom) }
                        })
                        KeyValueRow(labelTo, to, onCopy = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(to))
                            scope.launch { snackbar.showSnackbar(copiedTo) }
                        })
                        KeyValueRow(labelBytes, bytes, onCopy = null)
                    }
                }

                item {
                    DetailsSectionCard(title = stringResource(Res.string.details_section_connection)) {
                        KeyValueRow(
                            labelLocal,
                            endpoint(raw.localAddress, raw.localPort),
                            onCopy = null
                        )
                        KeyValueRow(
                            labelRemote,
                            endpoint(raw.remoteAddress, raw.remotePort),
                            onCopy = null
                        )
                    }
                }

                item {
                    DetailsSectionCard(title = stringResource(Res.string.details_section_traffic)) {
                        KeyValueRow(labelTls, if (raw.tls) yes else no, onCopy = null)
                        if (!raw.sni.isNullOrBlank()) {
                            KeyValueRow(labelSni, raw.sni.orEmpty(), onCopy = null)
                        }
                    }
                }

                item {
                    val chevron = if (state.isRawExpanded) "▾" else "▸"
                    DetailsSectionCard(
                        title = stringResource(Res.string.details_section_raw_meta),
                        trailing = chevron,
                        onHeaderClick = onToggleRaw
                    ) {
                        AnimatedVisibility(
                            visible = state.isRawExpanded,
                            enter = fadeIn(tween(180)) + expandVertically(
                                tween(
                                    220,
                                    easing = FastOutSlowInEasing
                                )
                            ),
                            exit = fadeOut(tween(160)) + shrinkVertically(
                                tween(
                                    200,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                KeyValueRow(labelKey, packet.key, onCopy = null)
                                KeyValueRow(labelTimestamp, raw.timestamp.toString(), onCopy = null)
                                KeyValueRow(labelUid, raw.uid?.toString() ?: "null", onCopy = null)
                                KeyValueRow(labelPackage, raw.packageName.orEmpty(), onCopy = null)
                                KeyValueRow(labelProtocol, raw.protocol, onCopy = null)
                                KeyValueRow(labelDir, raw.dir.orEmpty(), onCopy = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DetailsScreenPreview() {
    DetailsScreenContent(
        state = DetailsContract.State(),
        onToggleRaw = {},
    )
}
