package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MedicalDisclaimerCard

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
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
                text = "About KIDNEY AI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Final-Year B.Sc. Computer Science (AI & Data Science) Capstone Project",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Project Aim Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Project Objectives & Scope",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kidney AI is an academic computer-aided diagnosis (CAD) system designed to investigate " +
                            "classical machine learning techniques (Support Vector Machine & Decision Tree) for renal disease " +
                            "detection on medical CT and MRI scans.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }

        // Four Pathologies
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "The Four Target Renal Classes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DiseaseBullet("NORMAL:", "Healthy renal parenchyma without mass, cyst, or stone.")
                DiseaseBullet("CYST:", "Fluid-filled benign renal lesion with hypodense attenuation.")
                DiseaseBullet("TUMOR:", "Renal mass / RCC with heterogeneous tissue & irregular margins.")
                DiseaseBullet("STONE:", "Nephrolithiasis / hyperdense calcium concretion (>220).")
            }
        }

        // Engineering Principles
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Engineering & Academic Standards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrincipleItem("• Zero Fake AI:", "Predictions rely on real pixel features & trained classifiers.")
                PrincipleItem("• Dual Model Concordance:", "Flags disagreement whenever SVM and DT outputs diverge.")
                PrincipleItem("• Radiomic Explainability:", "Exposes exact variance, contrast, and density metrics.")
                PrincipleItem("• Local Data Privacy:", "SQLite and Room DB store records securely.")
            }
        }

        MedicalDisclaimerCard()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiseaseBullet(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrincipleItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
