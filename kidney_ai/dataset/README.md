# KIDNEY AI Dataset Structure

This folder contains the dataset layout for training and evaluating the Support Vector Machine (SVM) and Decision Tree models for kidney disease classification.

## Classes
The system classifies images into exactly four clinical categories:
1. `normal`: Healthy kidney parenchyma without lesions, cysts, stones, or neoplastic growths.
2. `cyst`: Fluid-filled renal cyst with distinct round borders and low attenuation.
3. `tumor`: Solid renal mass / renal cell carcinoma (RCC) with irregular margins, tissue heterogeneity, or abnormal contrast enhancement.
4. `stone`: Nephrolithiasis / renal calculi showing high density (hyperattenuation) inside the collecting system or calyces.

## Directory Structure
Organize your dataset into standard splits:

```
dataset/
├── train/
│   ├── normal/
│   ├── cyst/
│   ├── tumor/
│   └── stone/
├── validation/
│   ├── normal/
│   ├── cyst/
│   ├── tumor/
│   └── stone/
└── test/
    ├── normal/
    ├── cyst/
    ├── tumor/
    └── stone/
```

## Recommended Open Dataset
You can use the widely recognized **CT KIDNEY DATASET: Normal-Cyst-Tumor and Stone** available on Kaggle (by Nazmul Islam).
- Contains 12,446 validated axial CT scan slices across Normal, Cyst, Tumor, and Stone classes.
- Download the dataset ZIP and extract images into `dataset/train/` and `dataset/test/` folders as shown above.

## Training the Models
Run the training script from the `kidney_ai` directory:
```bash
python training/train_models.py --dataset_dir dataset/
```
The script will validate class folders, extract calibrated texture/intensity/moments feature vectors, train the SVM and Decision Tree models, compute actual evaluation metrics, and save models to `models/`.
