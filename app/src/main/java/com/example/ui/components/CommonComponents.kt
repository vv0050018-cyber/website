package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidneyTopAppBar(
    currentScreen: Screen,
    userName: String?,
    onNavigateBack: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MedicalCyanPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Kidney AI Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "KIDNEY AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "CAD Renal Analysis Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back"
                    )
                }
            }
        },
        actions = {
            if (userName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = userName.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (onLogoutClick != null) {
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun KidneyBottomBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Dashboard,
            onClick = { onTabSelected(Screen.Dashboard) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            modifier = Modifier.testTag("nav_dashboard")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Analyze,
            onClick = { onTabSelected(Screen.Analyze) },
            icon = { Icon(Icons.Default.Biotech, contentDescription = "Scan Analysis") },
            label = { Text("Analyze") },
            modifier = Modifier.testTag("nav_analyze")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.History,
            onClick = { onTabSelected(Screen.History) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History") },
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.ModelInfo,
            onClick = { onTabSelected(Screen.ModelInfo) },
            icon = { Icon(Icons.Default.Psychology, contentDescription = "Model Info") },
            label = { Text("Models") },
            modifier = Modifier.testTag("nav_model_info")
        )
        NavigationBarItem(
            selected = currentScreen is Screen.About,
            onClick = { onTabSelected(Screen.About) },
            icon = { Icon(Icons.Default.Info, contentDescription = "About") },
            label = { Text("About") },
            modifier = Modifier.testTag("nav_about")
        )
    }
}

@Composable
fun DiseaseBadge(disease: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val (bgColor, textColor) = when (disease.uppercase()) {
        "NORMAL" -> Pair(DiseaseNormalBg, DiseaseNormal)
        "CYST" -> Pair(DiseaseCystBg, DiseaseCyst)
        "TUMOR" -> Pair(DiseaseTumorBg, DiseaseTumor)
        "STONE" -> Pair(DiseaseStoneBg, DiseaseStone)
        else -> Pair(Slate100, Slate600)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(if (large) 8.dp else 4.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = disease.uppercase(),
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (large) 14.sp else 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 6.dp else 2.dp)
        )
    }
}

@Composable
fun ConcordanceBadge(agreed: Boolean, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label, icon) = if (agreed) {
        listOf(DiseaseNormalBg, DiseaseNormal, "Models Agree", Icons.Default.CheckCircle)
    } else {
        listOf(DiseaseTumorBg, DiseaseTumor, "Model Disagreement", Icons.Default.Warning)
    }

    Surface(
        color = bgColor as Color,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, (textColor as Color).copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label as String,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MedicalDisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBEB)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Medical AI Notice",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Medical Research & CAD Disclaimer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "KIDNEY AI is developed for academic, research, and educational demonstration purposes. " +
                            "Predictions generated by SVM and Decision Tree models are not a replacement for a certified clinical diagnosis. " +
                            "Always consult a qualified radiologist or nephrologist for patient diagnosis and treatment decisions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF78350F),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
