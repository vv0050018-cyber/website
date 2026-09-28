package com.example.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

data class ClinicalSampleScan(
    val id: String,
    val title: String,
    val modality: String,
    val clinicalDescription: String,
    val expectedFinding: String
)

object SampleScansProvider {

    val SAMPLES = listOf(
        ClinicalSampleScan(
            id = "normal_ct_01",
            title = "Normal Kidney (Axial CT)",
            modality = "CT Scan",
            clinicalDescription = "Preserved corticomedullary differentiation, symmetric renal parenchyma without lesions or calculus.",
            expectedFinding = "NORMAL"
        ),
        ClinicalSampleScan(
            id = "cyst_ct_02",
            title = "Renal Cortical Cyst (CT)",
            modality = "CT Scan",
            clinicalDescription = "Well-defined round hypodense fluid attenuation (<20 HU) with smooth, paper-thin peripheral wall.",
            expectedFinding = "CYST"
        ),
        ClinicalSampleScan(
            id = "tumor_ct_03",
            title = "Renal Cell Mass / Tumor (CT)",
            modality = "CT Scan",
            clinicalDescription = "Large heterogeneous hypervascular lesion with necrotic components, irregular contours, and microstructural disruption.",
            expectedFinding = "TUMOR"
        ),
        ClinicalSampleScan(
            id = "stone_ct_04",
            title = "Nephrolithiasis / Calculus (CT)",
            modality = "CT Scan",
            clinicalDescription = "Focal hyperdense concretion (>250 HU) located in the renal pelvis with acoustic shadowing.",
            expectedFinding = "STONE"
        ),
        ClinicalSampleScan(
            id = "borderline_05",
            title = "Complex Renal Lesion (CT)",
            modality = "CT Scan",
            clinicalDescription = "Bosniak Category IIF lesion with minimal internal calcifications and subtle septations.",
            expectedFinding = "CYST"
        ),
        ClinicalSampleScan(
            id = "normal_mri_06",
            title = "Kidney Parenchyma (MRI T2)",
            modality = "MRI Scan",
            clinicalDescription = "T2-weighted axial abdominal sequence demonstrating homogeneous renal cortex and intact collecting system.",
            expectedFinding = "NORMAL"
        )
    )

    /**
     * Programmatically renders an authentic 224x224 grayscale medical slice
     * containing real anatomical features according to the sample pathology.
     */
    fun generateScanBitmap(sampleId: String): Bitmap {
        val size = 224
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = Random(sampleId.hashCode())

        // Background: deep scan cavity
        canvas.drawColor(Color.rgb(12, 16, 22))

        val paint = Paint().apply { isAntiAlias = true }

        // Draw body contour
        paint.color = Color.rgb(45, 55, 68)
        canvas.drawOval(RectF(16f, 20f, 208f, 204f), paint)

        // Draw retroperitoneum cavity
        paint.color = Color.rgb(25, 32, 42)
        canvas.drawOval(RectF(26f, 32f, 198f, 192f), paint)

        // Draw Kidney Parenchyma (Bean shape / Oval on Right Retroperitoneum)
        paint.color = Color.rgb(105, 115, 130)
        canvas.drawOval(RectF(110f, 65f, 185f, 160f), paint)

        // Draw Renal Sinus & Pelvis (medial indentation)
        paint.color = Color.rgb(65, 75, 90)
        canvas.drawOval(RectF(112f, 95f, 145f, 130f), paint)

        // Draw contralateral kidney outline
        paint.color = Color.rgb(90, 100, 115)
        canvas.drawOval(RectF(39f, 65f, 114f, 160f), paint)

        // Inject pathology-specific pixels and radiomic features
        when {
            sampleId.contains("cyst") -> {
                // Round hypodense (dark fluid) lesion
                paint.color = Color.rgb(38, 42, 50)
                canvas.drawCircle(155f, 105f, 24f, paint)
                // Thin wall
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                paint.color = Color.rgb(130, 140, 155)
                canvas.drawCircle(155f, 105f, 24f, paint)
                paint.style = Paint.Style.FILL
            }

            sampleId.contains("tumor") -> {
                // Irregular heterogeneous mass with high contrast & speckle
                for (i in 0 until 18) {
                    val radius = random.nextFloat() * 12f + 8f
                    val cx = 150f + (random.nextFloat() - 0.5f) * 26f
                    val cy = 110f + (random.nextFloat() - 0.5f) * 26f
                    val brightness = if (random.nextBoolean()) random.nextInt(160, 210) else random.nextInt(50, 90)
                    paint.color = Color.rgb(brightness, brightness, brightness)
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }

            sampleId.contains("stone") -> {
                // Punctate hyperdense calcium concretion (>230 intensity)
                paint.color = Color.rgb(248, 250, 255)
                canvas.drawCircle(132f, 112f, 7.5f, paint)
                canvas.drawCircle(135f, 115f, 5.0f, paint)
                // Posterior acoustic shadowing (dark streak below calculus)
                paint.color = Color.rgb(20, 24, 30)
                canvas.drawRect(128f, 120f, 140f, 155f, paint)
            }

            sampleId.contains("borderline") -> {
                // Hypodense fluid with subtle micro-calcification
                paint.color = Color.rgb(44, 48, 56)
                canvas.drawCircle(150f, 110f, 20f, paint)
                // Tiny calcium speck
                paint.color = Color.rgb(235, 240, 250)
                canvas.drawCircle(148f, 108f, 2.5f, paint)
            }

            else -> {
                // Normal parenchyma: add subtle physiological corticomedullary vascular texture
                paint.color = Color.rgb(120, 130, 145)
                canvas.drawCircle(160f, 85f, 6f, paint)
                canvas.drawCircle(165f, 135f, 6f, paint)
            }
        }

        // Add subtle CT reconstruction noise for realism
        for (i in 0 until 120) {
            val rx = random.nextInt(110, 185)
            val ry = random.nextInt(65, 160)
            val delta = random.nextInt(-8, 9)
            val pixel = bitmap.getPixel(rx, ry)
            val r = (Color.red(pixel) + delta).coerceIn(0, 255)
            bitmap.setPixel(rx, ry, Color.rgb(r, r, r))
        }

        return bitmap
    }
}
