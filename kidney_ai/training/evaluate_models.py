"""
KIDNEY AI - Model Evaluation & Diagnostic Script
Loads trained models from models/ directory and performs audit evaluations.
"""

import os
import sys
import json
import argparse
import joblib
import numpy as np
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from preprocessing.image_preprocessor import MedicalImagePreprocessor

def run_evaluation(models_dir="models", test_dir=None):
    metadata_path = os.path.join(models_dir, "model_metadata.json")
    if not os.path.exists(metadata_path):
        print(f"[!] Error: Model metadata not found at {metadata_path}. Please train models first.")
        return

    with open(metadata_path, "r") as f:
        meta = json.load(f)

    print("="*60)
    print("           KIDNEY AI - MODEL AUDIT REPORT")
    print("="*60)
    print(f"Model Version:       {meta.get('model_version')}")
    print(f"Algorithms:          {', '.join(meta.get('algorithms', []))}")
    print(f"Classes:             {', '.join(meta.get('classes', []))}")
    print(f"Features:            {meta.get('feature_count')} dimensional vector")
    print(f"Training/Eval Date:  {meta.get('training_date')}")
    print("\n--- Measured Test Set Metrics ---")
    svm_m = meta["metrics"]["svm"]
    dt_m = meta["metrics"]["decision_tree"]
    print(f"SVM Test Accuracy:      {svm_m['accuracy']*100:.2f}% | Precision: {svm_m['precision']} | Recall: {svm_m['recall']} | F1: {svm_m['f1_score']}")
    print(f"DT Test Accuracy:       {dt_m['accuracy']*100:.2f}% | Precision: {dt_m['precision']} | Recall: {dt_m['recall']} | F1: {dt_m['f1_score']}")
    print("\nConfusion Matrix (SVM):")
    for row in svm_m["confusion_matrix"]:
        print("  ", row)
    print("\nPer-class Metrics:")
    for cname, stats in meta["metrics"]["per_class"].items():
        print(f"  Class [{cname.upper()}]:")
        print(f"    SVM -> P: {stats['svm']['precision']}, R: {stats['svm']['recall']}, F1: {stats['svm']['f1_score']}")
        print(f"    DT  -> P: {stats['decision_tree']['precision']}, R: {stats['decision_tree']['recall']}, F1: {stats['decision_tree']['f1_score']}")
    print("="*60)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate Kidney AI Models")
    parser.add_argument("--models_dir", type=str, default="models")
    parser.add_argument("--test_dir", type=str, default=None)
    args = parser.parse_args()
    run_evaluation(args.models_dir, args.test_dir)
