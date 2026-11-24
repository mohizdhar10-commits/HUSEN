package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.signOut
import com.example.i18n.StudioStrings
import com.example.model.StudioProject
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberCredit
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    projectsState: UiState<List<StudioProject>>,
    lang: String,
    darkTheme: Boolean,
    strings: StudioStrings,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val projectCount = (projectsState as? UiState.Success)?.data?.size ?: 0
    val totalCreditsSpent = (projectsState as? UiState.Success)?.data?.sumOf { it.creditCost } ?: 0L

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "Creator",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ElectricViolet.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PLAN: ${profile?.planTier ?: "FREE"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = AmberCredit)
                            Text(
                                text = "${profile?.credits ?: 50L} ${strings.freeCreditsLeft}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${profile?.totalGenerations ?: 0L} gen",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }

                    Button(
                        onClick = { viewModel.claimDailyCredits() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_daily_credits_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text(strings.claimDailyCreditsBtn, color = Color.White)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = AmberCredit)
                        Text(strings.plansTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    PlanRow("FREE", "50 kredit awal + 10/hari • Watermark", "Rp 0")
                    PlanRow("PRO", "1.000 kredit/bln • Tanpa Watermark • 1080p", "Rp 149rb / $9")
                    PlanRow("UNLIMITED", "Tanpa batas • Batch & Priority • Midtrans / Xendit / Stripe", "Rp 399rb / $25")
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Analytics, contentDescription = null, tint = EmeraldSuccess)
                        Text(strings.analyticsTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == "id") "Total Proyek Tersimpan" else "Saved Studio Projects",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$projectCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == "id") "Total Kredit Terpakai" else "Total Credits Spent",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$totalCreditsSpent ⚡",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberCredit
                        )
                    }
                    Text(
                        text = if (lang == "id") {
                            "Estimasi biaya API: 360p (5⚡), 720p (10⚡), 1080p (15⚡), Gambar (3⚡), Script/Subtitle (1⚡)."
                        } else {
                            "API cost guide: 360p (5⚡), 720p (10⚡), 1080p (15⚡), Image (3⚡), Script/Subtitle (1⚡)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.preferencesTitle, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.darkModeLabel, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { viewModel.toggleTheme(it) },
                            modifier = Modifier.testTag("profile_dark_mode_switch")
                        )
                    }
                    Text(strings.languageLabel, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = lang == "id",
                            onClick = { viewModel.toggleLanguage("id") },
                            modifier = Modifier.testTag("lang_id_chip"),
                            label = { Text("Bahasa Indonesia") }
                        )
                        FilterChip(
                            selected = lang == "en",
                            onClick = { viewModel.toggleLanguage("en") },
                            modifier = Modifier.testTag("lang_en_chip"),
                            label = { Text("English") }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { signOut(credentialManager, scope) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.signOutBtn)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlanRow(tier: String, desc: String, price: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tier, style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(price, style = MaterialTheme.typography.labelMedium, color = AmberCredit)
        }
    }
}
