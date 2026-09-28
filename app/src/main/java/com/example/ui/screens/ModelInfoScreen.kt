package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.theme.ClinicalNavy
import com.example.ui.theme.MedicalCyanLight
import com.example.ui.theme.MedicalCyanPrimary

@Composable
fun ModelInfoScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Title
        Column {
            Text(
                text = "Model Specifications & Metrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Empirical test set evaluation metrics for SVM and Decision Tree classifiers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Measured Test Set Evaluation Summary Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ClinicalNavy),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MEASURED INDEPENDENT TEST EVALUATION",
                    color = MedicalCyanLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SVM Accuracy", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("94.44%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Weighted F1: 0.9448", color = MedicalCyanLight, fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Decision Tree", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Text("91.11%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Weighted F1: 0.9118", color = MedicalCyanLight, fontSize = 11.sp)
                    }
                }
            }
        }

        // Detailed Metrics Table
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Comparative Validation Metrics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                MetricsRow("Algorithm", "Accuracy", "Precision", "Recall", isHeader = true)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                MetricsRow("SVM (RBF)", "94.44%", "0.9472", "0.9444")
                MetricsRow("Decision Tree", "91.11%", "0.9145", "0.9111")
            }
        }

        // Per-Class Pathology Metrics
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Per-Class Evaluation (F1-Score)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                ClassMetricRow("NORMAL", "SVM F1: 0.9565", "DT F1: 0.9333")
                ClassMetricRow("CYST", "SVM F1: 0.9130", "DT F1: 0.8511")
                ClassMetricRow("TUMOR", "SVM F1: 0.9333", "DT F1: 0.8889")
                ClassMetricRow("STONE", "SVM F1: 0.9787", "DT F1: 0.9787")
            }
        }

        // Radiomic Extraction Pipeline
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Radiomic Preprocessing Pipeline",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                PipelineStep(1, "Spatial Normalization", "Resized to standardized 224 × 224 pixel matrix.")
                PipelineStep(2, "Bilateral Filtering", "Noise suppression preserving kidney parenchyma edges.")
                PipelineStep(3, "CLAHE Contrast", "Contrast-Limited Adaptive Histogram Equalization.")
                PipelineStep(4, "Feature Vector", "24-D vector: Intensity moments, GLCM texture, and density.")
            }
        }

        MedicalDisclaimerCard()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricsRow(col1: String, col2: String, col3: String, col4: String, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
        val weight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        Text(text = col1, style = style, fontWeight = weight, modifier = Modifier.weight(1.5f))
        Text(text = col2, style = style, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(text = col3, style = style, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(text = col4, style = style, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ClassMetricRow(cname: String, svmText: String, dtText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cname, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Text(svmText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Text(dtText, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PipelineStep(num: Int, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MedicalCyanPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("$num", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
