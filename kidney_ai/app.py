"""
KIDNEY AI - Main Flask Application
Full-stack Medical AI Diagnosis System for Kidney Disease
"""

import os
import json
import uuid
from functools import wraps
import io
from datetime import datetime
from flask import (
    Flask, render_template, request, redirect, url_for,
    flash, session, jsonify, send_file, abort
)
from werkzeug.utils import secure_filename

from database.database import Database
from prediction.predictor import KidneyAIPredictor
from reports.report_generator import generate_pdf_report

app = Flask(__name__)
app.secret_key = os.environ.get("SECRET_KEY", "kidney-ai-academic-secret-key-2026")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_FOLDER = os.path.join(BASE_DIR, "uploads")
MODELS_DIR = os.path.join(BASE_DIR, "models")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(MODELS_DIR, exist_ok=True)

app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER
app.config["MAX_CONTENT_LENGTH"] = 16 * 1024 * 1024  # 16 MB max
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'bmp', 'tiff'}

# Initialize database & predictor
db = Database()
predictor = KidneyAIPredictor(models_dir=MODELS_DIR)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if "user_id" not in session:
            flash("Please sign in to access this page.", "warning")
            return redirect(url_for("login", next=request.url))
        return f(*args, **kwargs)
    return decorated_function

def admin_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if "user_id" not in session or not session.get("is_admin"):
            flash("Administrator privileges required.", "danger")
            return redirect(url_for("dashboard"))
        return f(*args, **kwargs)
    return decorated_function

@app.context_processor
def inject_global_vars():
    user = None
    if "user_id" in session:
        user = db.get_user_by_id(session["user_id"])
    return {
        "current_user": user,
        "model_loaded": predictor.is_loaded,
        "model_load_error": predictor.load_error_message,
        "current_year": datetime.now().year
    }

# --- Public & Auth Routes ---
@app.route("/")
def index():
    if "user_id" in session:
        return redirect(url_for("dashboard"))
    return render_template("index.html")

@app.route("/login", methods=["GET", "POST"])
def login():
    if "user_id" in session:
        return redirect(url_for("dashboard"))
    if request.method == "POST":
        email = request.form.get("email", "").strip()
        password = request.form.get("password", "")
        remember = bool(request.form.get("remember"))

        user, err = db.authenticate_user(email, password)
        if user:
            session["user_id"] = user["id"]
            session["user_name"] = user["name"]
            session["user_email"] = user["email"]
            session["is_admin"] = bool(user["is_admin"])
            session.permanent = remember
            flash(f"Welcome back, {user['name']}!", "success")
            next_url = request.args.get("next")
            return redirect(next_url or url_for("dashboard"))
        else:
            flash(err or "Authentication failed.", "danger")

    return render_template("login.html")

@app.route("/register", methods=["GET", "POST"])
def register():
    if "user_id" in session:
        return redirect(url_for("dashboard"))
    if request.method == "POST":
        name = request.form.get("name", "").strip()
        email = request.form.get("email", "").strip()
        password = request.form.get("password", "")
        confirm_password = request.form.get("confirm_password", "")

        if not name or not email or not password:
            flash("All fields are required.", "danger")
            return render_template("register.html")

        if password != confirm_password:
            flash("Passwords do not match.", "danger")
            return render_template("register.html")

        success, res = db.register_user(name, email, password)
        if success:
            flash("Account successfully created! You may now sign in.", "success")
            return redirect(url_for("login"))
        else:
            flash(f"Registration failed: {res}", "danger")

    return render_template("register.html")

@app.route("/logout")
def logout():
    session.clear()
    flash("You have been signed out.", "info")
    return redirect(url_for("login"))

# --- Dashboard & Core App ---
@app.route("/dashboard")
@login_required
def dashboard():
    stats = db.get_dashboard_stats(user_id=session["user_id"])
    recent_scans = db.get_user_history(user_id=session["user_id"], limit=5)
    return render_template("dashboard.html", stats=stats, recent_scans=recent_scans)

@app.route("/analyze", methods=["GET", "POST"])
@login_required
def analyze():
    if request.method == "POST":
        # Check model availability first
        if not predictor.is_loaded:
            flash(predictor.load_error_message or "Trained models not found. Please train models first.", "danger")
            return redirect(url_for("analyze"))

        if 'image' not in request.files:
            flash("No image file provided in upload request.", "danger")
            return redirect(request.url)

        file = request.files['image']
        if file.filename == '':
            flash("No selected file.", "danger")
            return redirect(request.url)

        if not allowed_file(file.filename):
            flash("Unsupported file format. Please upload PNG, JPG, JPEG, BMP, or TIFF images.", "danger")
            return redirect(request.url)

        scan_type = request.form.get("scan_type", "CT Scan")
        unique_name = f"{uuid.uuid4().hex}_{secure_filename(file.filename)}"
        save_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_name)
        file.save(save_path)

        try:
            # Execute authentic ML prediction pipeline
            pred_res = predictor.predict(save_path, scan_type=scan_type)
            
            # Save to SQLite database
            pred_id = db.save_prediction(
                user_id=session["user_id"],
                scan_type=scan_type,
                image_filename=unique_name,
                prediction_data=pred_res
            )
            return redirect(url_for("result", pred_id=pred_id))
        except Exception as e:
            flash(f"Analysis error: {str(e)}", "danger")
            return redirect(url_for("analyze"))

    return render_template("analyze.html")

@app.route("/result/<int:pred_id>")
@login_required
def result(pred_id):
    rec = db.get_prediction_by_id(pred_id, user_id=session["user_id"])
    if not rec:
        abort(404, "Prediction record not found.")

    key_features = {}
    if rec.get("key_features_json"):
        try:
            key_features = json.loads(rec["key_features_json"])
        except Exception:
            pass

    return render_template("result.html", record=rec, features=key_features)

@app.route("/history")
@login_required
def history():
    filter_class = request.args.get("class", "ALL")
    records = db.get_user_history(user_id=session["user_id"], limit=100, class_filter=filter_class)
    return render_template("history.html", records=records, active_filter=filter_class)

@app.route("/history/<int:pred_id>/delete", methods=["POST"])
@login_required
def delete_history_item(pred_id):
    success = db.delete_prediction(pred_id, user_id=session["user_id"])
    if success:
        flash("Record deleted successfully.", "success")
    else:
        flash("Could not delete record.", "danger")
    return redirect(url_for("history"))

@app.route("/report/<int:pred_id>/pdf")
@login_required
def download_pdf(pred_id):
    rec = db.get_prediction_by_id(pred_id, user_id=session["user_id"])
    if not rec:
        abort(404)
    user = db.get_user_by_id(session["user_id"])
    
    if rec.get("key_features_json"):
        try:
            rec["key_features"] = json.loads(rec["key_features_json"])
        except Exception:
            rec["key_features"] = {}

    pdf_bytes = generate_pdf_report(rec, user)
    return send_file(
        io.BytesIO(pdf_bytes),
        mimetype="application/pdf",
        as_attachment=True,
        download_name=f"KidneyAI_Report_{pred_id}_{rec['prediction']}.pdf"
    )

@app.route("/model-info")
def model_info():
    metadata = predictor.metadata or {}
    return render_template("model_info.html", metadata=metadata)

@app.route("/about")
def about():
    return render_template("about.html")

@app.route("/profile")
@login_required
def profile():
    user = db.get_user_by_id(session["user_id"])
    stats = db.get_dashboard_stats(user_id=session["user_id"])
    return render_template("profile.html", user=user, stats=stats)

# --- REST APIs ---
@app.route("/api/predict", methods=["POST"])
@login_required
def api_predict():
    if not predictor.is_loaded:
        return jsonify({"success": False, "error": predictor.load_error_message}), 503

    if 'image' not in request.files:
        return jsonify({"success": False, "error": "No image uploaded"}), 400

    file = request.files['image']
    scan_type = request.form.get("scan_type", "CT Scan")

    if not allowed_file(file.filename):
        return jsonify({"success": False, "error": "Invalid format"}), 400

    unique_name = f"{uuid.uuid4().hex}_{secure_filename(file.filename)}"
    save_path = os.path.join(app.config['UPLOAD_FOLDER'], unique_name)
    file.save(save_path)

    try:
        pred_res = predictor.predict(save_path, scan_type=scan_type)
        pred_id = db.save_prediction(session["user_id"], scan_type, unique_name, pred_res)
        pred_res["id"] = pred_id
        return jsonify(pred_res)
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/history", methods=["GET"])
@login_required
def api_history():
    records = db.get_user_history(session["user_id"], limit=50)
    return jsonify({"success": True, "records": records})

@app.route("/api/model-info", methods=["GET"])
def api_model_info():
    return jsonify({
        "success": True,
        "loaded": predictor.is_loaded,
        "metadata": predictor.metadata
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
