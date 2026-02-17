package com.muratcangzm.settings.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.settings.ui.components.SettingsActionCard
import com.muratcangzm.settings.ui.components.SettingsHeroCard
import com.muratcangzm.settings.ui.components.SettingsSection
import com.muratcangzm.settings.ui.components.SettingsSurface
import com.muratcangzm.settings.ui.components.SettingsUpgradeCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateAccount: () -> Unit,
    onNavigatePremium: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsContract.Effect.OpenUrl -> uriHandler.openUri(effect.url)
                is SettingsContract.Effect.Snackbar -> snackbarHostState.showSnackbar(effect.message)
                is SettingsContract.Effect.Toast -> snackbarHostState.showSnackbar(effect.message)
                SettingsContract.Effect.NavigateAccount -> onNavigateAccount()
                SettingsContract.Effect.NavigatePremium -> onNavigatePremium()
            }
        }
    }

    SettingsSurface {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Settings") },
                    navigationIcon = {
                        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { inner ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .consumeWindowInsets(inner)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                item {
                    SettingsSection(
                        title = "WiredEye",
                        subtitle = "On-device analysis and privacy-first telemetry."
                    ) {
                        SettingsHeroCard(
                            title = "Security & Privacy",
                            subtitle = "No account required for core features.",
                            badgeText = "v${state.versionName}"
                        )
                    }
                }

                item {
                    SettingsSection(
                        title = "Premium",
                        subtitle = "Unlock advanced protection models and automation."
                    ) {
                        SettingsUpgradeCard(
                            title = "Upgrade",
                            subtitle = if (state.isPremium) {
                                "Premium active"
                            } else {
                                "Plans and paywall will be added in a later update"
                            },
                            icon = Icons.Outlined.Star,
                            trailingText = if (state.isPremium) "Active" else "Soon",
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenPremium) }
                        )
                    }
                }

                item {
                    SettingsSection(
                        title = "Account",
                        subtitle = "Not configured yet. You can add Google sign-in later."
                    ) {
                        SettingsActionCard(
                            title = "Account",
                            subtitle = state.userEmail ?: "Not signed in",
                            icon = Icons.Outlined.Info,
                            trailingText = "Soon",
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenAccount) }
                        )
                    }
                }

                item {
                    SettingsSection(
                        title = "Legal",
                        subtitle = "Required policies and licenses."
                    ) {
                        SettingsActionCard(
                            title = "Privacy Policy",
                            subtitle = "Data handling and device processing",
                            icon = Icons.Outlined.PrivacyTip,
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenPrivacyPolicy) }
                        )

                        SettingsActionCard(
                            title = "Terms",
                            subtitle = "Usage terms and limitations",
                            icon = Icons.Outlined.Gavel,
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenTerms) }
                        )

                        SettingsActionCard(
                            title = "Open source licenses",
                            subtitle = "Libraries and acknowledgements",
                            icon = Icons.Outlined.Description,
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenOpenSourceLicenses) }
                        )
                    }
                }

                item {
                    SettingsSection(
                        title = "Support",
                        subtitle = "Contact and troubleshooting."
                    ) {
                        SettingsActionCard(
                            title = "Contact support",
                            subtitle = "Send an email for help",
                            icon = Icons.Outlined.Email,
                            onClick = { viewModel.onEvent(SettingsContract.Event.OpenSupport) }
                        )

                        SettingsActionCard(
                            title = "Copy support email",
                            subtitle = "Copy to clipboard",
                            icon = Icons.Outlined.Email,
                            trailingText = "Copy",
                            onClick = {
                                val supportEmail =
                                    state.supportEmail.ifBlank { "support@yourdomain.com" }
                                clipboard.setText(AnnotatedString(supportEmail))
                                viewModel.onEvent(SettingsContract.Event.CopySupportEmail)
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "Build ${state.buildType}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

        }
    }
}
