package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.KidneyPredictionResult
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.ConcordanceBadge
import com.example.ui.components.DiseaseBadge
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    result: KidneyPredictionResult,
    bitmap: Bitmap?,
    modality: String,
    imageName: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(Screen.Dashboard)
    }

    var showReportDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KIDNEY AI ANALYSIS RESULT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Dual-Model Radiomic Analysis",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { showReportDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_view_report")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clinical Report", fontSize = 12.sp)
            }
        }

        // Primary Classification Banner
        val (verdictBg, verdictColor) = when (result.prediction.uppercase()) {
            "NORMAL" -> Pair(DiseaseNormalBg, DiseaseNormal)
            "CYST" -> Pair(DiseaseCystBg, DiseaseCyst)
            "TUMOR" -> Pair(DiseaseTumorBg, DiseaseTumor)
            "STONE" -> Pair(DiseaseStoneBg, DiseaseStone)
            else -> Pair(Slate100, Slate800)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = verdictBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PRIMARY AI CLASSIFICATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = verdictColor.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.prediction.uppercase(),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = verdictColor,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Calculated Statistical Confidence: ${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = verdictColor
                )
            }
        }

        // Model Concordance Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (result.modelsAgree) DiseaseNormalBg else DiseaseTumorBg
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (result.modelsAgree) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.modelsAgree) DiseaseNormal else DiseaseTumor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = result.statusTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.modelsAgree) DiseaseNormal else DiseaseTumor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = result.statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.modelsAgree) Color(0xFF065F46) else Color(0xFF991B1B),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Dual-Algorithm Breakdown
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Individual Algorithm Predictions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Support Vector Machine", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            DiseaseBadge(disease = result.svmPrediction)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RBF Decision Boundary", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Decision Tree", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            DiseaseBadge(disease = result.dtPrediction)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("CART Gini Leaf Splits", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Scan Preview & Radiomic Features Inspector
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Extracted Radiomic Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (bitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Evaluated Slice",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                FeatureRow("Mean Parenchyma Intensity:", "%.3f".format(result.features.meanIntensity))
                FeatureRow("Intensity Variance:", "%.4f".format(result.features.intensityVariance))
                FeatureRow("GLCM Texture Contrast:", "%.2f".format(result.features.glcmContrast))
                FeatureRow("GLCM Homogeneity:", "%.3f".format(result.features.glcmHomogeneity))
                FeatureRow("Hyperdense Ratio (>220):", "%.4f".format(result.features.hyperdenseRatio))
                FeatureRow("Hypodense Ratio (<60):", "%.4f".format(result.features.hypodenseRatio))
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.Analyze) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze Another")
            }

            Button(
                onClick = { viewModel.navigateTo(Screen.History) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View History")
            }
        }

        MedicalDisclaimerCard()
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showReportDialog) {
        ReportViewDialog(
            result = result,
            modality = modality,
            imageName = imageName,
            onDismiss = { showReportDialog = false }
        )
    }
}

@Composable
private fun FeatureRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
