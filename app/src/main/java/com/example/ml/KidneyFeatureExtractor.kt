package com.example.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class KidneyRadiomicFeatures(
    val meanIntensity: Float,
    val intensityVariance: Float,
    val skewness: Float,
    val kurtosis: Float,
    val entropy: Float,
    val glcmContrast: Float,
    val glcmHomogeneity: Float,
    val hyperdenseRatio: Float,    // Indicator for Kidney Stones (>220)
    val hypodenseRatio: Float,     // Indicator for Renal Cysts (<60)
    val contourCircularity: Float, // Higher in spherical cysts, lower in irregular tumors
    val rawVector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KidneyRadiomicFeatures
        return rawVector.contentEquals(other.rawVector)
    }

    override fun hashCode(): Int = rawVector.contentHashCode()
}

object KidneyFeatureExtractor {

    private const val TARGET_WIDTH = 224
    private const val TARGET_HEIGHT = 224

    /**
     * Extracts radiomic feature vector from a kidney CT/MRI scan Bitmap.
     */
    fun extract(sourceBitmap: Bitmap): KidneyRadiomicFeatures {
        val scaled = Bitmap.createScaledBitmap(sourceBitmap, TARGET_WIDTH, TARGET_HEIGHT, true)
        val pixelCount = TARGET_WIDTH * TARGET_HEIGHT
        val pixels = IntArray(pixelCount)
        scaled.getPixels(pixels, 0, TARGET_WIDTH, 0, 0, TARGET_WIDTH, TARGET_HEIGHT)

        val gray = FloatArray(pixelCount)
        var sum = 0.0
        var hyperdenseCount = 0
        var hypodenseCount = 0

        val histogram = IntArray(32) // 32 quantized bins for GLCM and entropy

        for (i in 0 until pixelCount) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            // Luminance formula
            val intensity = (0.299f * r + 0.587f * g + 0.114f * b)
            val norm = intensity / 255.0f
            gray[i] = norm
            sum += norm

            if (intensity > 220f) hyperdenseCount++
            if (intensity < 60f) hypodenseCount++

            val bin = (norm * 31.999f).toInt().coerceIn(0, 31)
            histogram[bin]++
        }

        val mean = (sum / pixelCount).toFloat()

        // Higher order moments
        var m2 = 0.0
        var m3 = 0.0
        var m4 = 0.0
        for (i in 0 until pixelCount) {
            val diff = gray[i] - mean
            val diff2 = diff * diff
            m2 += diff2
            m3 += diff2 * diff
            m4 += diff2 * diff2
        }

        val variance = (m2 / pixelCount).toFloat()
        val stdDev = sqrt(variance)
        val skewness = if (stdDev > 1e-5f) (m3 / pixelCount / stdDev.toDouble().pow(3.0)).toFloat() else 0f
        val kurtosis = if (stdDev > 1e-5f) (m4 / pixelCount / stdDev.toDouble().pow(4.0) - 3.0).toFloat() else 0f

        // Shannon Entropy
        var entropy = 0.0
        for (count in histogram) {
            if (count > 0) {
                val p = count.toDouble() / pixelCount
                entropy -= p * (ln(p) / ln(2.0))
            }
        }

        // Texture GLCM approximation (Horizontal Adjacent Co-occurrence)
        var glcmContrast = 0.0
        var glcmHomogeneity = 0.0
        var pairCount = 0

        for (y in 0 until TARGET_HEIGHT) {
            for (x in 0 until TARGET_WIDTH - 1) {
                val idx1 = y * TARGET_WIDTH + x
                val idx2 = idx1 + 1
                val diff = abs(gray[idx1] - gray[idx2])
                glcmContrast += (diff * diff) * 10.0
                glcmHomogeneity += 1.0 / (1.0 + diff * 10.0)
                pairCount++
            }
        }

        if (pairCount > 0) {
            glcmContrast /= pairCount
            glcmHomogeneity /= pairCount
        }

        val hyperdenseRatio = (hyperdenseCount.toFloat() / pixelCount)
        val hypodenseRatio = (hypodenseCount.toFloat() / pixelCount)

        // Geometric contour circularity approximation based on variance of mass distribution
        val circularity = (1.0f / (1.0f + abs(variance - 0.035f) * 15f)).coerceIn(0.2f, 0.95f)

        val raw = floatArrayOf(
            mean, variance, skewness, kurtosis, entropy.toFloat(),
            glcmContrast.toFloat(), glcmHomogeneity.toFloat(),
            hyperdenseRatio, hypodenseRatio, circularity
        )

        return KidneyRadiomicFeatures(
            meanIntensity = mean,
            intensityVariance = variance,
            skewness = skewness,
            kurtosis = kurtosis,
            entropy = entropy.toFloat(),
            glcmContrast = glcmContrast.toFloat(),
            glcmHomogeneity = glcmHomogeneity.toFloat(),
            hyperdenseRatio = hyperdenseRatio,
            hypodenseRatio = hypodenseRatio,
            contourCircularity = circularity,
            rawVector = raw
        )
    }
}
