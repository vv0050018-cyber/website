package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ml.KidneyPredictionResult
import com.example.ui.components.DiseaseBadge
import com.example.ui.theme.ClinicalNavy
import com.example.ui.theme.MedicalCyanPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportViewDialog(
    result: KidneyPredictionResult,
    modality: String,
    imageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KIDNEY AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ClinicalNavy
                        )
                        Text(
                            text = "Clinical CAD Analysis Report",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Patient / Examination Metadata
                ReportMetaRow("Examination Date:", currentDate)
                ReportMetaRow("Modality Sequence:", modality)
                ReportMetaRow("Evaluated Slice:", imageName.take(24))
                ReportMetaRow("Engine Architecture:", "SVM (RBF) + Decision Tree")
                ReportMetaRow("Model Release:", result.modelVersion)

                Spacer(modifier = Modifier.height(14.dp))

                // Diagnostic Findings Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "AI DIAGNOSTIC FINDINGS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Primary Classification:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            DiseaseBadge(disease = result.prediction, large = true)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ReportMetaRow("SVM Individual Class:", result.svmPrediction)
                        ReportMetaRow("Decision Tree Class:", result.dtPrediction)
                        ReportMetaRow("Calculated Confidence:", "${(result.confidence * 100).toInt()}%")
                        ReportMetaRow("Concordance Status:", if (result.modelsAgree) "Agreed" else "Disagreement Flagged")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Extracted Radiomic Numbers
                Text(
                    text = "Key Radiomic Indicators",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                ReportMetaRow("Mean Intensity (0-1):", "%.3f".format(result.features.meanIntensity))
                ReportMetaRow("Texture Contrast:", "%.2f".format(result.features.glcmContrast))
                ReportMetaRow("Texture Homogeneity:", "%.3f".format(result.features.glcmHomogeneity))
                ReportMetaRow("Stone Density Ratio (>220):", "%.4f".format(result.features.hyperdenseRatio))
                ReportMetaRow("Cyst Fluid Ratio (<60):", "%.4f".format(result.features.hypodenseRatio))

                Spacer(modifier = Modifier.height(14.dp))

                // Mandatory Clinical Disclaimer
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "IMPORTANT CLINICAL NOTICE:",
                            color = Color(0xFF991B1B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This report was generated for research, study, and educational demonstration. It is not an autonomous medical diagnosis. All treatment and clinical actions must be decided by a licensed physician.",
                            color = Color(0xFF7F1D1D),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Share / Export Button
                Button(
                    onClick = {
                        val shareText = """
                            KIDNEY AI - CLINICAL ANALYSIS REPORT
                            Date: $currentDate
                            Modality: $modality
                            File: $imageName
                            ------------------------------------
                            Primary Classification: ${result.prediction}
                            Confidence: ${(result.confidence * 100).toInt()}%
                            SVM Prediction: ${result.svmPrediction}
                            Decision Tree: ${result.dtPrediction}
                            Concordance: ${if (result.modelsAgree) "Agreed" else "Disagreement Flagged"}
                            ------------------------------------
                            Notice: Academic/Research Demonstration only.
                        """.trimIndent()

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Clinical Summary"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Export Clinical Summary")
                }
            }
        }
    }
}

@Composable
private fun ReportMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
