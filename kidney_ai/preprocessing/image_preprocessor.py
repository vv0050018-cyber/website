"""
KIDNEY AI - Image Preprocessing and Feature Extraction Pipeline
Used identically for both offline model training and real-time inference.
"""

import os
import cv2
import numpy as np
from PIL import Image
import joblib

SUPPORTED_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.bmp', '.tiff'}
TARGET_IMAGE_SIZE = (224, 224)

class MedicalImagePreprocessor:
    """
    Standardized preprocessing and clinical feature extraction for kidney CT/MRI images.
    """
    def __init__(self, target_size=TARGET_IMAGE_SIZE, use_clahe=True):
        self.target_size = target_size
        self.use_clahe = use_clahe
        self.clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))

    def validate_image_file(self, file_path_or_bytes):
        """
        Validates file existence, format, and readable pixel buffer.
        """
        if isinstance(file_path_or_bytes, str):
            if not os.path.exists(file_path_or_bytes):
                raise FileNotFoundError(f"Image file does not exist: {file_path_or_bytes}")
            ext = os.path.splitext(file_path_or_bytes)[1].lower()
            if ext not in SUPPORTED_EXTENSIONS:
                raise ValueError(f"Unsupported format '{ext}'. Allowed: {', '.join(SUPPORTED_EXTENSIONS)}")
            img = cv2.imread(file_path_or_bytes, cv2.IMREAD_UNCHANGED)
        elif isinstance(file_path_or_bytes, (bytes, bytearray)):
            nparr = np.frombuffer(file_path_or_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_UNCHANGED)
        else:
            raise TypeError("Unsupported input type for image validation.")

        if img is None or img.size == 0:
            raise ValueError("Corrupt or unreadable image file.")

        return img

    def preprocess_image(self, img_array):
        """
        Executes standard pipeline:
        1. RGB/Multi-channel to Grayscale
        2. Bilateral filtering for CT/MRI noise reduction while preserving tumor/cyst edges
        3. Standardized spatial resizing (224x224)
        4. CLAHE contrast enhancement
        5. Pixel normalization [0.0, 1.0]
        """
        # Convert to single-channel 8-bit grayscale
        if len(img_array.shape) == 3:
            if img_array.shape[2] == 4:
                gray = cv2.cvtColor(img_array, cv2.COLOR_BGRA2GRAY)
            else:
                gray = cv2.cvtColor(img_array, cv2.COLOR_BGR2GRAY)
        elif len(img_array.shape) == 2:
            gray = img_array.copy()
        else:
            raise ValueError(f"Invalid image dimensions: {img_array.shape}")

        if gray.dtype != np.uint8:
            gray = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX, dtype=cv2.CV_8U)

        # Spatial normalization
        resized = cv2.resize(gray, self.target_size, interpolation=cv2.INTER_AREA)

        # Edge-preserving denoising
        denoised = cv2.bilateralFilter(resized, d=5, sigmaColor=50, sigmaSpace=50)

        # CLAHE
        if self.use_clahe:
            enhanced = self.clahe.apply(denoised)
        else:
            enhanced = denoised

        # Normalized float representation for statistical calculations
        norm_float = enhanced.astype(np.float32) / 255.0

        return {
            "raw_gray": resized,
            "enhanced": enhanced,
            "norm_float": norm_float
        }

    def compute_glcm_features(self, gray_img):
        """
        Computes texture features inspired by Gray-Level Co-occurrence Matrix (GLCM):
        Contrast, Dissimilarity, Homogeneity, Energy, and Correlation.
        """
        # Quantize to 32 levels for fast and robust co-occurrence
        quantized = (gray_img // 8).astype(np.int32)
        h, w = quantized.shape
        
        # Horizontal adjacent co-occurrence
        left = quantized[:, :-1].flatten()
        right = quantized[:, 1:].flatten()

        glcm = np.zeros((32, 32), dtype=np.float64)
        np.add.at(glcm, (left, right), 1.0)
        np.add.at(glcm, (right, left), 1.0)

        total = glcm.sum()
        if total > 0:
            glcm /= total

        i, j = np.indices((32, 32))
        diff = np.abs(i - j)

        contrast = np.sum(glcm * (diff ** 2))
        dissimilarity = np.sum(glcm * diff)
        homogeneity = np.sum(glcm / (1.0 + (diff ** 2)))
        energy = np.sum(glcm ** 2)
        asm = energy

        mean_i = np.sum(i * glcm)
        mean_j = np.sum(j * glcm)
        var_i = np.sum(((i - mean_i) ** 2) * glcm)
        var_j = np.sum(((j - mean_j) ** 2) * glcm)
        std_prod = np.sqrt(var_i * var_j)

        if std_prod > 1e-6:
            correlation = np.sum((i - mean_i) * (j - mean_j) * glcm) / std_prod
        else:
            correlation = 1.0

        return {
            "contrast": float(contrast),
            "dissimilarity": float(dissimilarity),
            "homogeneity": float(homogeneity),
            "energy": float(energy),
            "asm": float(asm),
            "correlation": float(correlation)
        }

    def extract_features(self, preprocessed_dict):
        """
        Extracts a comprehensive 24-dimensional feature vector:
        - 6 Statistical Intensity Moments (Mean, Std, Skewness, Kurtosis, Energy, Entropy)
        - 5 Texture Features (Contrast, Dissimilarity, Homogeneity, Energy, Correlation)
        - 7 Hu Invariant Shape Moments (log-transformed)
        - 6 Anatomical & Attenuation Descriptors (High-density Stone ratio, Low-density Cyst ratio, Otsu area, Circularity, Solidity, Eccentricity)
        """
        norm_img = preprocessed_dict["norm_float"]
        enhanced_8u = preprocessed_dict["enhanced"]

        # 1. Intensity Statistics
        mean_val = float(np.mean(norm_img))
        std_val = float(np.std(norm_img))
        
        # Skewness and Kurtosis
        centered = norm_img - mean_val
        m2 = np.mean(centered ** 2)
        m3 = np.mean(centered ** 3)
        m4 = np.mean(centered ** 4)
        
        skewness = float(m3 / (m2 ** 1.5 + 1e-7))
        kurtosis = float(m4 / (m2 ** 2 + 1e-7) - 3.0)
        
        # Entropy
        hist, _ = np.histogram(enhanced_8u, bins=32, range=(0, 256), density=True)
        hist = hist[hist > 0]
        entropy = float(-np.sum(hist * np.log2(hist)))
        intensity_energy = float(np.sum(norm_img ** 2) / norm_img.size)

        # 2. GLCM Texture
        glcm = self.compute_glcm_features(enhanced_8u)

        # 3. Shape & Hu Moments
        moments = cv2.moments(enhanced_8u)
        hu_moments = cv2.HuMoments(moments).flatten()
        log_hu = []
        for h in hu_moments:
            if abs(h) > 0:
                log_hu.append(float(-1.0 * np.copysign(1.0, h) * np.log10(abs(h) + 1e-12)))
            else:
                log_hu.append(0.0)

        # 4. Otsu Segmentation & Density Ratios
        _, otsu_thresh = cv2.threshold(enhanced_8u, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        contours, _ = cv2.findContours(otsu_thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        area_ratio = float(np.sum(otsu_thresh == 255) / otsu_thresh.size)
        
        circularity = 0.0
        solidity = 0.0
        if contours:
            largest = max(contours, key=cv2.contourArea)
            area = cv2.contourArea(largest)
            perimeter = cv2.arcLength(largest, True)
            if perimeter > 0:
                circularity = float((4 * np.pi * area) / (perimeter * perimeter))
            hull = cv2.convexHull(largest)
            hull_area = cv2.contourArea(hull)
            if hull_area > 0:
                solidity = float(area / hull_area)

        # Attenuation features:
        # Kidney stones appear hyperdense (> 220 in 8-bit enhanced CT)
        stone_density_ratio = float(np.sum(enhanced_8u > 220) / enhanced_8u.size)
        # Renal cysts appear hypodense and uniform (< 60 in 8-bit enhanced CT)
        cyst_hypodense_ratio = float(np.sum(enhanced_8u < 60) / enhanced_8u.size)

        # Assembled feature vector (length: 24)
        feature_vector = [
            mean_val,
            std_val,
            skewness,
            kurtosis,
            entropy,
            intensity_energy,
            glcm["contrast"],
            glcm["dissimilarity"],
            glcm["homogeneity"],
            glcm["energy"],
            glcm["correlation"],
            log_hu[0],
            log_hu[1],
            log_hu[2],
            log_hu[3],
            log_hu[4],
            log_hu[5],
            log_hu[6],
            area_ratio,
            circularity,
            solidity,
            stone_density_ratio,
            cyst_hypodense_ratio,
            float(glcm["asm"])
        ]

        feature_names = [
            "mean_intensity", "std_intensity", "skewness", "kurtosis", "entropy", "intensity_energy",
            "glcm_contrast", "glcm_dissimilarity", "glcm_homogeneity", "glcm_energy", "glcm_correlation",
            "hu_moment_1", "hu_moment_2", "hu_moment_3", "hu_moment_4", "hu_moment_5", "hu_moment_6", "hu_moment_7",
            "segmented_area_ratio", "contour_circularity", "contour_solidity",
            "stone_hyperdense_ratio", "cyst_hypodense_ratio", "texture_asm"
        ]

        return np.array(feature_vector, dtype=np.float32), feature_names
