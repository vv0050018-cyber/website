"""
KIDNEY AI - Inference and Dual Model Agreement Engine
Loads real trained models and runs authenticated clinical predictions.
"""

import os
import sys
import json
import joblib
import numpy as np

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from preprocessing.image_preprocessor import MedicalImagePreprocessor

class KidneyAIPredictor:
    def __init__(self, models_dir="models"):
        self.models_dir = models_dir
        self.preprocessor = MedicalImagePreprocessor()
        self.svm_model = None
        self.dt_model = None
        self.scaler = None
        self.label_encoder = None
        self.metadata = None
        self.is_loaded = False
        self.load_error_message = None

        self._load_models()

    def _load_models(self):
        svm_path = os.path.join(self.models_dir, "svm_model.pkl")
        dt_path = os.path.join(self.models_dir, "decision_tree_model.pkl")
        scaler_path = os.path.join(self.models_dir, "preprocessor.pkl")
        le_path = os.path.join(self.models_dir, "label_encoder.pkl")
        meta_path = os.path.join(self.models_dir, "model_metadata.json")

        missing = []
        for path in [svm_path, dt_path, scaler_path, le_path, meta_path]:
            if not os.path.exists(path):
                missing.append(os.path.basename(path))

        if missing:
            self.load_error_message = (
                f"Trained model artifacts missing: {', '.join(missing)}. "
                "Please run 'python training/train_models.py' before performing inference."
            )
            self.is_loaded = False
            return

        try:
            self.svm_model = joblib.load(svm_path)
            self.dt_model = joblib.load(dt_path)
            self.scaler = joblib.load(scaler_path)
            self.label_encoder = joblib.load(le_path)
            with open(meta_path, "r") as f:
                self.metadata = json.load(f)
            self.is_loaded = True
            self.load_error_message = None
        except Exception as e:
            self.load_error_message = f"Failed to load model binaries: {str(e)}"
            self.is_loaded = False

    def predict(self, image_source, scan_type="CT"):
        """
        Executes end-to-end inference:
        Image -> Validation -> Preprocessing -> Feature Extraction -> SVM & Decision Tree -> Agreement Check
        """
        if not self.is_loaded:
            raise RuntimeError(self.load_error_message or "Machine learning models not loaded.")

        # 1. Validation & Preprocessing
        raw_img = self.preprocessor.validate_image_file(image_source)
        preproc_dict = self.preprocessor.preprocess_image(raw_img)

        # 2. Extract 24-dimensional feature vector
        features, feature_names = self.preprocessor.extract_features(preproc_dict)
        features_2d = features.reshape(1, -1)

        # 3. Scaler for SVM
        features_scaled = self.scaler.transform(features_2d)

        # 4. SVM Prediction & Probabilities
        svm_pred_idx = self.svm_model.predict(features_scaled)[0]
        svm_pred_label = str(self.label_encoder.inverse_transform([svm_pred_idx])[0]).upper()
        
        svm_probs = {}
        svm_conf = None
        if hasattr(self.svm_model, "predict_proba"):
            probs = self.svm_model.predict_proba(features_scaled)[0]
            classes = self.label_encoder.classes_
            for c, p in zip(classes, probs):
                svm_probs[c.upper()] = round(float(p), 4)
            svm_conf = round(float(np.max(probs)), 4)

        # 5. Decision Tree Prediction & Probabilities
        dt_pred_idx = self.dt_model.predict(features_2d)[0]
        dt_pred_label = str(self.label_encoder.inverse_transform([dt_pred_idx])[0]).upper()

        dt_probs = {}
        if hasattr(self.dt_model, "predict_proba"):
            probs_dt = self.dt_model.predict_proba(features_2d)[0]
            classes = self.label_encoder.classes_
            for c, p in zip(classes, probs_dt):
                dt_probs[c.upper()] = round(float(p), 4)

        # 6. Model Agreement Analysis
        models_agree = (svm_pred_label == dt_pred_label)

        if models_agree:
            final_pred = svm_pred_label
            agreement_status = "Models Agree"
            status_description = "Concordance between Support Vector Machine and Decision Tree classifiers."
            final_conf = svm_conf
        else:
            agreement_status = "Model Disagreement Detected"
            status_description = (
                f"Model divergence: SVM predicted {svm_pred_label} while Decision Tree predicted {dt_pred_label}. "
                "Higher uncertainty is present; secondary clinical radiologist consultation is strongly advised."
            )
            # When models disagree, prioritize SVM's calibrated boundary probability or mark conservative
            final_pred = svm_pred_label if (svm_conf and svm_conf > 0.6) else dt_pred_label
            final_conf = svm_conf

        # Selected key features for medical transparency
        key_features = {
            "mean_intensity": round(float(features[0]), 4),
            "intensity_variance": round(float(features[1] ** 2), 4),
            "glcm_contrast": round(float(features[6]), 4),
            "glcm_homogeneity": round(float(features[8]), 4),
            "hu_moment_1": round(float(features[11]), 4),
            "stone_density_ratio": round(float(features[21]), 4),
            "cyst_hypodense_ratio": round(float(features[22]), 4)
        }

        return {
            "success": True,
            "prediction": final_pred,
            "svm_prediction": svm_pred_label,
            "decision_tree_prediction": dt_pred_label,
            "confidence": final_conf,
            "svm_probabilities": svm_probs,
            "decision_tree_probabilities": dt_probs,
            "models_agree": models_agree,
            "agreement_status": agreement_status,
            "status_description": status_description,
            "scan_type": scan_type,
            "model_version": self.metadata.get("model_version", "v1.0"),
            "key_features": key_features,
            "disclaimer": (
                "This result is generated by an AI-assisted research and educational system. "
                "It is not a confirmed medical diagnosis and must not replace professional clinical evaluation."
            )
        }
