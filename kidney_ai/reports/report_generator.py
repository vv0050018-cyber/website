"""
KIDNEY AI - PDF Clinical Report Generator
Generates clinical-grade AI analysis PDF reports with full model audit trails and medical disclaimers.
"""

import os
import io
from datetime import datetime
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

def generate_pdf_report(prediction_record, user_info, image_path=None):
    """
    Creates a formatted PDF in-memory buffer using ReportLab.
    """
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=letter,
        rightMargin=40,
        leftMargin=40,
        topMargin=40,
        bottomMargin=40
    )

    styles = getSampleStyleSheet()
    
    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=20,
        textColor=colors.HexColor('#0B1E36'),
        spaceAfter=4
    )
    subtitle_style = ParagraphStyle(
        'DocSub',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        textColor=colors.HexColor('#4A5568'),
        spaceAfter=15
    )
    section_heading = ParagraphStyle(
        'SecHead',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=12,
        textColor=colors.HexColor('#0B1E36'),
        spaceBefore=12,
        spaceAfter=6
    )
    disclaimer_style = ParagraphStyle(
        'DiscStyle',
        parent=styles['Italic'],
        fontName='Helvetica-Oblique',
        fontSize=8.5,
        textColor=colors.HexColor('#C53030'),
        leading=11
    )

    story = []

    # 1. Header
    story.append(Paragraph("KIDNEY AI", title_style))
    story.append(Paragraph("AI-Powered Medical Image Diagnosis System for Kidney Disease (Research Platform)", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor('#3182CE'), spaceAfter=15))

    # 2. Patient / User Info & Examination Metadata
    created_at = prediction_record.get("created_at", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    exam_data = [
        [Paragraph("<b>Clinician / Investigator:</b>", styles['Normal']), Paragraph(user_info.get("name", "N/A"), styles['Normal'])],
        [Paragraph("<b>User Email:</b>", styles['Normal']), Paragraph(user_info.get("email", "N/A"), styles['Normal'])],
        [Paragraph("<b>Examination Modality:</b>", styles['Normal']), Paragraph(f"{prediction_record.get('scan_type', 'CT')} Scan", styles['Normal'])],
        [Paragraph("<b>Image File:</b>", styles['Normal']), Paragraph(prediction_record.get("image_filename", "scan.png"), styles['Normal'])],
        [Paragraph("<b>Analysis Timestamp:</b>", styles['Normal']), Paragraph(str(created_at), styles['Normal'])],
        [Paragraph("<b>AI System Engine:</b>", styles['Normal']), Paragraph(f"SVM + Decision Tree ({prediction_record.get('model_version', 'v1.0')})", styles['Normal'])]
    ]
    t_exam = Table(exam_data, colWidths=[160, 360])
    t_exam.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F7FAFC')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('TOPPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_exam)
    story.append(Spacer(1, 15))

    # 3. Diagnostic Findings
    story.append(Paragraph("Diagnostic Classification Findings", section_heading))
    
    pred_label = prediction_record.get("prediction", "UNKNOWN").upper()
    svm_label = prediction_record.get("svm_prediction", "UNKNOWN").upper()
    dt_label = prediction_record.get("decision_tree_prediction", "UNKNOWN").upper()
    models_agree = bool(prediction_record.get("models_agree", 1))
    
    status_text = "Concordant (Both Models Agree)" if models_agree else "Model Disagreement Detected"
    status_color = colors.HexColor('#22543D') if models_agree else colors.HexColor('#742A2A')
    status_bg = colors.HexColor('#C6F6D5') if models_agree else colors.HexColor('#FED7D7')

    conf_str = f"{float(prediction_record['confidence']) * 100:.1f}%" if prediction_record.get('confidence') else "Not Calibrated"

    result_data = [
        [Paragraph("<b>Primary AI Finding:</b>", styles['Normal']), Paragraph(f"<b><font size=14 color='#1A365D'>{pred_label}</font></b>", styles['Normal'])],
        [Paragraph("<b>SVM Prediction:</b>", styles['Normal']), Paragraph(svm_label, styles['Normal'])],
        [Paragraph("<b>Decision Tree Prediction:</b>", styles['Normal']), Paragraph(dt_label, styles['Normal'])],
        [Paragraph("<b>Calculated Confidence:</b>", styles['Normal']), Paragraph(conf_str, styles['Normal'])],
        [Paragraph("<b>Concordance Status:</b>", styles['Normal']), Paragraph(f"<b>{status_text}</b>", styles['Normal'])]
    ]
    t_res = Table(result_data, colWidths=[160, 360])
    t_res.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#EBF8FF')),
        ('BACKGROUND', (0,1), (-1,-1), colors.HexColor('#FFFFFF')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E0')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('TOPPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(t_res)
    story.append(Spacer(1, 15))

    # 4. Extracted Medical Features (if available)
    if "key_features" in prediction_record and prediction_record["key_features"]:
        story.append(Paragraph("Extracted Radiomic & Texture Indicators", section_heading))
        kf = prediction_record["key_features"]
        feat_data = [
            ["Metric", "Measured Value", "Clinical Relevance"],
            ["Mean Parenchyma Intensity", str(kf.get("mean_intensity", "N/A")), "Baseline renal soft-tissue attenuation"],
            ["Intensity Variance", str(kf.get("intensity_variance", "N/A")), "Heterogeneity / parenchymal disruption"],
            ["GLCM Texture Contrast", str(kf.get("glcm_contrast", "N/A")), "Micro-structure sharpness & borders"],
            ["GLCM Homogeneity", str(kf.get("glcm_homogeneity", "N/A")), "Internal lesion uniformity (high in cysts)"],
            ["Hyperdense Ratio (>220)", str(kf.get("stone_density_ratio", "N/A")), "Punctate calcification indicator (Stones)"],
            ["Hypodense Ratio (<60)", str(kf.get("cyst_hypodense_ratio", "N/A")), "Fluid-filled renal cystic attenuation"]
        ]
        t_feat = Table(feat_data, colWidths=[150, 100, 270])
        t_feat.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#2D3748')),
            ('TEXTCOLOR', (0,0), (-1,0), colors.white),
            ('FONTNAME', (0,0), (-1,0), 'Helvetica-Bold'),
            ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E0')),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 4),
            ('TOPPADDING', (0,0), (-1,-1), 4),
        ]))
        story.append(t_feat)
        story.append(Spacer(1, 20))

    # 5. Mandatory Medical AI Disclaimer
    story.append(Paragraph("MANDATORY CLINICAL & ACADEMIC DISCLAIMER", section_heading))
    disclaimer_text = (
        "IMPORTANT NOTICE: This document was automatically generated by the KIDNEY AI system, an experimental "
        "academic and research machine learning project. The predictions, probabilities, and radiomic features "
        "provided are intended solely for research, study, and educational exploration. They do NOT constitute "
        "a definitive medical diagnosis or clinical treatment plan. Medical decisions must always be made by a "
        "licensed physician, radiologist, or nephrologist following standard clinical guidelines and diagnostic protocols."
    )
    story.append(Paragraph(disclaimer_text, disclaimer_style))
    story.append(Spacer(1, 15))

    # 6. Verification Footer
    footer_text = f"Report Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')} | KIDNEY AI Final-Year Project"
    story.append(Paragraph(f"<font size=8 color='#718096'>{footer_text}</font>", styles['Normal']))

    doc.build(story)
    buffer.seek(0)
    return buffer.getvalue()
