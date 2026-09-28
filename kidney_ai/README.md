# KIDNEY AI: AI-Powered Medical Image Diagnosis System for Kidney Disease

**Final-Year B.Sc. Computer Science – Artificial Intelligence & Data Science Project**

---

## 📌 Executive Summary
**KIDNEY AI** is a clinical computer-aided diagnosis (CAD) system developed to assist in the classification of kidney medical images (CT and MRI scans) into four distinct pathology classes:
1. **NORMAL** — Healthy renal parenchyma
2. **CYST** — Fluid-filled benign renal lesion
3. **TUMOR** — Solid renal mass / Renal Cell Carcinoma (RCC)
4. **STONE** — Nephrolithiasis / Calcified renal calculi

The system uses dual machine learning architectures:
- **Support Vector Machine (SVM)** with Radial Basis Function (RBF) kernel and Platt probability calibration.
- **Decision Tree Classifier** (CART with Gini impurity metric) for clinical interpretability.
- **Dual-Model Concordance Engine:** Evaluates whether both models agree. If they diverge, a clinical disagreement warning is explicitly issued.

---

## 🔬 Image Preprocessing & Feature Extraction Pipeline
Every uploaded kidney scan goes through a standardized 24-dimensional radiomic feature extraction pipeline:
1. **Spatial Normalization**: Resized to 224 × 224 pixels.
2. **Noise Reduction**: Bilateral edge-preserving filtering to eliminate CT/MRI reconstruction noise without blurring pathological boundaries.
3. **Contrast Enhancement**: Contrast Limited Adaptive Histogram Equalization (CLAHE).
4. **Radiomic Extraction (24 Dimensions)**:
   - **Intensity Statistics (6-D)**: Mean attenuation, variance, skewness, kurtosis, Shannon entropy, total intensity energy.
   - **GLCM Texture (5-D)**: Contrast, dissimilarity, inverse difference moment (homogeneity), energy/ASM, correlation.
   - **Hu Invariant Moments (7-D)**: Log-transformed shape moments 1–7.
   - **Attenuation Ratios & Geometry (6-D)**: Hyperdense stone ratio (>220), hypodense cyst ratio (<60), segmented area ratio, circularity, solidity.

---

## 🚀 How to Run Locally in VS Code on Windows

### Prerequisites
- Python 3.10, 3.11, or 3.12 installed on Windows.
- VS Code installed with the Python extension.

### Step 1: Open the Project in VS Code
1. Open VS Code.
2. Click **File > Open Folder...** and select the `kidney_ai` directory.

### Step 2: Create and Activate Virtual Environment
Open the VS Code Terminal (`Ctrl + ~`) and run:

```powershell
# Create virtual environment
python -m venv venv

# Activate on Windows PowerShell
.\venv\Scripts\Activate.ps1
# (Or on Command Prompt: .\venv\Scripts\activate.bat)
```

### Step 3: Install Required Dependencies
```powershell
pip install -r requirements.txt
```

### Step 4: Train the ML Models
To train the SVM and Decision Tree models and serialize them into `models/`:

```powershell
python training/train_models.py
```
*Note: If you have downloaded the Kaggle CT Kidney dataset, pass `--dataset_dir dataset/`.*
*The script will fit the StandardScaler, train the SVC and Decision Tree, compute real test set accuracy/precision/recall/F1 metrics, and save `models/svm_model.pkl`, `models/decision_tree_model.pkl`, `models/preprocessor.pkl`, `models/label_encoder.pkl`, and `models/model_metadata.json`.*

### Step 5: Run the Flask Web Application
```powershell
python app.py
```
Open your browser and navigate to:
```
http://127.0.0.1:5000
```

### Step 6: Sign In with Demo Researcher Credentials
- **Email:** `doctor@hospital.org`
- **Password:** `Doctor123!`
*(Or click "Create Account" to register a new user).*

---

## 📂 Project Architecture
```
kidney_ai/
├── app.py                      # Flask Application Controller & REST Endpoints
├── requirements.txt            # Python Dependencies
├── README.md                   # Full System Documentation
├── .env.example                # Environment Variable Template
├── .gitignore                  # Git Ignore Rules
├── dataset/                    # Dataset Directory (train, val, test)
│   └── README.md               # Dataset Guidelines & Kaggle Link
├── models/                     # Serialized Model Binaries & Metadata
│   ├── svm_model.pkl           # Trained Support Vector Machine
│   ├── decision_tree_model.pkl # Trained Decision Tree
│   ├── preprocessor.pkl        # Fitted Feature Scaler
│   ├── label_encoder.pkl       # Class Label Encoder
│   └── model_metadata.json     # Measured Evaluation Metrics
├── training/
│   ├── train_models.py         # End-to-End Training & Evaluation Pipeline
│   └── evaluate_models.py      # Independent Model Audit Script
├── preprocessing/
│   └── image_preprocessor.py   # OpenCV & CLAHE Preprocessing Pipeline
├── prediction/
│   └── predictor.py            # Dual-Model Inference & Concordance Engine
├── database/
│   ├── database.py             # SQLite Persistence (Users, History, Audits)
│   └── kidney_ai.db            # Auto-generated SQLite Database
├── reports/
│   └── report_generator.py     # ReportLab Clinical PDF Generator
├── templates/                  # Modern Medical AI HTML Templates
│   ├── base.html
│   ├── index.html
│   ├── login.html
│   ├── register.html
│   ├── dashboard.html
│   ├── analyze.html
│   ├── result.html
│   ├── history.html
│   ├── model_info.html
│   ├── about.html
│   └── profile.html
└── static/
    ├── css/
    │   └── styles.css          # Clinical Medical AI Stylesheet
    ├── js/
    │   └── main.js             # Client-side Validation & Interactions
    └── images/
```

---

## ⚖️ Ethical & Medical Disclaimer
This software is developed strictly for **academic, research, and educational demonstration purposes**. 
AI-generated classifications are not a definitive medical diagnosis and must never supersede clinical 
judgement by a qualified radiologist, nephrologist, or physician.
