"""
KIDNEY AI - Database Layer (SQLite)
Manages user authentication, prediction records, model version history, and audit logging.
"""

import os
import sqlite3
import hashlib
import secrets
import json
from datetime import datetime

DEFAULT_DB_PATH = os.path.join(os.path.dirname(__file__), "kidney_ai.db")

class Database:
    def __init__(self, db_path=DEFAULT_DB_PATH):
        self.db_path = db_path
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        self.init_db()

    def get_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def init_db(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            
            # Users table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                is_admin INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """)

            # Prediction history table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS prediction_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                scan_type TEXT NOT NULL,
                image_filename TEXT NOT NULL,
                prediction TEXT NOT NULL,
                svm_prediction TEXT NOT NULL,
                decision_tree_prediction TEXT NOT NULL,
                confidence REAL,
                models_agree INTEGER NOT NULL,
                key_features_json TEXT,
                model_version TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
            )
            """)

            # Model versions table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS model_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                version TEXT UNIQUE NOT NULL,
                model_name TEXT NOT NULL,
                training_date TEXT NOT NULL,
                metrics_json TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """)

            # Audit logs
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT NOT NULL,
                details TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """)

            conn.commit()

            # Seed default demo researcher account if empty
            cursor.execute("SELECT COUNT(*) FROM users")
            if cursor.fetchone()[0] == 0:
                self._seed_demo_user(cursor)
                conn.commit()

    def _hash_password(self, password, salt=None):
        if not salt:
            salt = secrets.token_hex(16)
        pw_hash = hashlib.pbkdf2_hmac(
            'sha256',
            password.encode('utf-8'),
            salt.encode('utf-8'),
            iterations=100000
        ).hex()
        return pw_hash, salt

    def _seed_demo_user(self, cursor):
        pw_hash, salt = self._hash_password("Doctor123!")
        cursor.execute("""
            INSERT INTO users (name, email, password_hash, salt, is_admin)
            VALUES (?, ?, ?, ?, 1)
        """, ("Dr. Sarah Jenkins", "doctor@hospital.org", pw_hash, salt))

    # --- User Management ---
    def register_user(self, name, email, password):
        email_clean = email.strip().lower()
        if not email_clean or "@" not in email_clean:
            return False, "Invalid email address format."
        if len(password) < 6:
            return False, "Password must be at least 6 characters."

        pw_hash, salt = self._hash_password(password)

        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO users (name, email, password_hash, salt, is_admin)
                    VALUES (?, ?, ?, ?, 0)
                """, (name.strip(), email_clean, pw_hash, salt))
                conn.commit()
                user_id = cursor.lastrowid
                self.log_audit(user_id, "USER_REGISTER", f"Account created for {email_clean}")
                return True, user_id
        except sqlite3.IntegrityError:
            return False, "An account with this email already exists."
        except Exception as e:
            return False, str(e)

    def authenticate_user(self, email, password):
        email_clean = email.strip().lower()
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE email = ?", (email_clean,))
            user = cursor.fetchone()
            if not user:
                return None, "Invalid email or password."

            expected_hash, _ = self._hash_password(password, user["salt"])
            if secrets.compare_digest(expected_hash, user["password_hash"]):
                self.log_audit(user["id"], "USER_LOGIN", f"Successful login for {email_clean}")
                return dict(user), None
            else:
                return None, "Invalid email or password."

    def get_user_by_id(self, user_id):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id, name, email, is_admin, created_at FROM users WHERE id = ?", (user_id,))
            user = cursor.fetchone()
            return dict(user) if user else None

    # --- Prediction History ---
    def save_prediction(self, user_id, scan_type, image_filename, prediction_data):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("""
                INSERT INTO prediction_history (
                    user_id, scan_type, image_filename, prediction,
                    svm_prediction, decision_tree_prediction, confidence,
                    models_agree, key_features_json, model_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                user_id,
                scan_type,
                image_filename,
                prediction_data["prediction"],
                prediction_data["svm_prediction"],
                prediction_data["decision_tree_prediction"],
                prediction_data["confidence"],
                1 if prediction_data["models_agree"] else 0,
                json.dumps(prediction_data.get("key_features", {})),
                prediction_data.get("model_version", "v1.0")
            ))
            conn.commit()
            pred_id = cursor.lastrowid
            self.log_audit(user_id, "PREDICTION_SAVED", f"Prediction #{pred_id}: {prediction_data['prediction']}")
            return pred_id

    def get_user_history(self, user_id, limit=50, class_filter=None):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            if class_filter and class_filter.upper() != "ALL":
                cursor.execute("""
                    SELECT * FROM prediction_history 
                    WHERE user_id = ? AND UPPER(prediction) = ?
                    ORDER BY created_at DESC LIMIT ?
                """, (user_id, class_filter.upper(), limit))
            else:
                cursor.execute("""
                    SELECT * FROM prediction_history 
                    WHERE user_id = ? 
                    ORDER BY created_at DESC LIMIT ?
                """, (user_id, limit))
            rows = cursor.fetchall()
            return [dict(r) for r in rows]

    def get_prediction_by_id(self, pred_id, user_id=None):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            if user_id:
                cursor.execute("SELECT * FROM prediction_history WHERE id = ? AND user_id = ?", (pred_id, user_id))
            else:
                cursor.execute("SELECT * FROM prediction_history WHERE id = ?", (pred_id,))
            row = cursor.fetchone()
            return dict(row) if row else None

    def delete_prediction(self, pred_id, user_id):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM prediction_history WHERE id = ? AND user_id = ?", (pred_id, user_id))
            conn.commit()
            return cursor.rowcount > 0

    # --- Dashboard & Metrics ---
    def get_dashboard_stats(self, user_id=None):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            where_clause = "WHERE user_id = ?" if user_id else ""
            params = (user_id,) if user_id else ()

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause}", params)
            total_scans = cursor.fetchone()[0]

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause} {'AND' if user_id else 'WHERE'} UPPER(prediction) = 'NORMAL'", params)
            normal_cases = cursor.fetchone()[0]

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause} {'AND' if user_id else 'WHERE'} UPPER(prediction) = 'CYST'", params)
            cyst_cases = cursor.fetchone()[0]

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause} {'AND' if user_id else 'WHERE'} UPPER(prediction) = 'TUMOR'", params)
            tumor_cases = cursor.fetchone()[0]

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause} {'AND' if user_id else 'WHERE'} UPPER(prediction) = 'STONE'", params)
            stone_cases = cursor.fetchone()[0]

            cursor.execute(f"SELECT COUNT(*) FROM prediction_history {where_clause} {'AND' if user_id else 'WHERE'} models_agree = 1", params)
            concordant_cases = cursor.fetchone()[0]

            agreement_rate = (concordant_cases / total_scans * 100) if total_scans > 0 else 100.0

            return {
                "total_scans": total_scans,
                "normal_cases": normal_cases,
                "cyst_cases": cyst_cases,
                "tumor_cases": tumor_cases,
                "stone_cases": stone_cases,
                "concordant_cases": concordant_cases,
                "agreement_rate": round(agreement_rate, 1)
            }

    def log_audit(self, user_id, action, details):
        try:
            with self.get_connection() as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    INSERT INTO audit_logs (user_id, action, details)
                    VALUES (?, ?, ?)
                """, (user_id, action, details))
                conn.commit()
        except Exception:
            pass
