from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File
from typing import Optional
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from datetime import timedelta
import datetime
import os
import uuid
from fpdf import FPDF
import json

import database
from database import engine, get_db
from fastapi.staticfiles import StaticFiles
import models, schemas, auth
import otp_service
from groq import Groq
from io import BytesIO
from fastapi.responses import Response, JSONResponse
from ml_service import ml_predictor
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()

# Initialize Google Gemini Client (General-Purpose AI Advisor)
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('models/gemini-flash-latest', 
    system_instruction="You are NailVital AI, a versatile and intelligent assistant. While you have expertise in nail health and dermatology, you are capable of answering any general-purpose questions from the user across any topic. Provide clear, concise, and helpful responses.")

# Initialize Groq Client for high-speed chat
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
groq_client = Groq(api_key=GROQ_API_KEY)

# Detailed mapping for 22 nail conditions
DISEASE_DETAILS = {
    "aloperia_areata": {
        "name": "Alopecia Areata (Nail Changes)",
        "description": "Nail changes in alopecia areata often appear as small pits, horizontal ridges, or rough texture. This is an autoimmune condition where the immune system attacks hair follicles, sometimes affecting the nail matrix.",
        "recommendation": "Consult a dermatologist to evaluate the underlying autoimmune activity. Corticosteroids or other immune-modulating treatments may be discussed."
    },
    "beaus_lines": {
        "name": "Beau's Lines",
        "description": "Deep grooved lines that run across the nail. They form when growth at the area under the cuticle is interrupted by injury or severe illness (like a high fever or infection).",
        "recommendation": "Identify the cause of the systemic stress. Ensure proper nutrition and hydration as the nail grows out."
    },
    "bluish_nail": {
        "name": "Bluish Nails (Cyanosis)",
        "description": "A bluish tint to the nails indicates that the blood isn't getting enough oxygen. This can be caused by cold temperatures or underlying cardiovascular or respiratory issues.",
        "recommendation": "Monitor your oxygen levels. If persistent, seek medical attention to rule out circulation or lung problems."
    },
    "clubbing": {
        "name": "Nail Clubbing",
        "description": "Nails that curve around the fingertips, which become enlarged. This is often associated with long-term low blood oxygen levels related to heart or lung disease.",
        "recommendation": "Urgent medical consultation is recommended to evaluate heart and lung health."
    },
    "dariers_disease": {
        "name": "Darier's Disease",
        "description": "Nails may show longitudinal red or white streaks, or V-shaped nicks at the edge. It is a rare genetic skin disorder.",
        "recommendation": "Consult a specialist for topical retinoids or other management strategies."
    },
    "eczema": {
        "name": "Nail Eczema",
        "description": "Causes pitting, thickening, and irregular ridges. The surrounding skin is often red, itchy, and inflamed.",
        "recommendation": "Use hypoallergenic moisturizers and avoid harsh chemicals. Topical steroids may be needed for severe flare-ups."
    },
    "half_and_half_nails": {
        "name": "Half-and-Half Nails (Lindsay's Nails)",
        "description": "The bottom half is white while the top half is pink or brown. This can be a sign of chronic kidney disease.",
        "recommendation": "A medical check-up focusing on kidney function is highly advised."
    },
    "koilonychia": {
        "name": "Koilonychia (Spoon Nails)",
        "description": "Soft nails that look scooped out, forming a concave shape. This is a common indicator of iron deficiency anemia.",
        "recommendation": "A blood test for iron levels is recommended. Increase iron-rich foods in your diet."
    },
    "leukonychia": {
        "name": "Leukonychia (White Spots)",
        "description": "White spots or lines on the nail. Usually caused by minor trauma during nail formation or mild vitamin deficiencies.",
        "recommendation": "Usually harmless. Ensure a balanced diet (Zinc/Calcium) and protect nails from physical trauma."
    },
    "melanoma": {
        "name": "Subungual Melanoma",
        "description": "A dark, vertical streak typically on one nail that may expand or change color. This is a serious form of skin cancer.",
        "recommendation": "IMMEDIATE dermatological evaluation is required. Do not delay."
    },
    "muehrckes_lines": {
        "name": "Muehrcke's Lines",
        "description": "Pairs of transverse white lines extending across the nail. Often linked to low levels of albumin in the blood.",
        "recommendation": "Consult a doctor for liver or kidney function tests."
    },
    "onychogryphosis": {
        "name": "Onychogryphosis (Ram's Horn Nails)",
        "description": "Hypertrophy of the nail plate causing it to thicken and curve like a horn. Often caused by trauma or poor circulation.",
        "recommendation": "Professional podiatry care is recommended for safe trimming and management."
    },
    "onycholycis": {
        "name": "Onycholysis",
        "description": "Detachment of the nail from the nail bed. Can be caused by injury, fungal infection, or certain medications.",
        "recommendation": "Keep the nail trimmed short. Avoid moisture trapped under the nail. Consult a doctor if infection is suspected."
    },
    "onychomycosis": {
        "name": "Onychomycosis (Nail Fungus)",
        "description": "A common fungal infection causing thickened, brittle, and discolored nails (yellow/brown).",
        "recommendation": "Keep feet/hands dry. Over-the-counter or prescription antifungal treatments are usually required."
    },
    "pale_nail": {
        "name": "Pale Nails",
        "description": "Nails that appear very white or washed out. Can be a sign of anemia, liver disease, or malnutrition.",
        "recommendation": "Review your nutritional intake. Consult a doctor for blood work if accompanied by fatigue."
    },
    "pitting": {
        "name": "Nail Pitting",
        "description": "Small dents or pits on the nail surface. Frequently seen in people with psoriasis or connective tissue disorders.",
        "recommendation": "Consult a dermatologist to evaluate for psoriasis or underlying inflammatory conditions."
    },
    "psoriasis": {
        "name": "Nail Psoriasis",
        "description": "Causes pitting, crumbly texture, and discoloration (oil spots). It is linked to the chronic skin condition psoriasis.",
        "recommendation": "Specialized nail treatments or systemic therapies may be discussed with a dermatologist."
    },
    "red_lunula": {
        "name": "Red Lunula",
        "description": "The half-moon area at the base of the nail appears red. Can be associated with heart failure or autoimmune diseases.",
        "recommendation": "Detailed medical evaluation of cardiovascular and immune health is recommended."
    },
    "splinter_hemorrhage": {
        "name": "Splinter Hemorrhage",
        "description": "Tiny blood clots that look like splinters under the nail. Usually due to trauma, but can occasionally indicate heart infection (endocarditis).",
        "recommendation": "Often grows out. However, if multiple nails are affected without trauma, see a doctor."
    },
    "terrys_nail": {
        "name": "Terry's Nails",
        "description": "Most of the nail appears white with a narrow pink band at the tip. Frequently associated with liver aging or liver disease.",
        "recommendation": "Consult a doctor for liver health screening."
    },
    "yellow_nails": {
        "name": "Yellow Nail Syndrome",
        "description": "Thick yellow nails that grow slowly. Often associated with respiratory issues or chronic lymphedema.",
        "recommendation": "Evaluate for lung health and lymphatic drainage issues with a medical professional."
    },
    "healthy": {
        "name": "Healthy Nails",
        "description": "The nails appear smooth, consistent in color, and free of spots or severe ridges. This is a normal and healthy scan result.",
        "recommendation": "Continue maintaining good nail hygiene and a balanced diet."
    }
}

# Create Database tables on startup
try:
    models.Base.metadata.create_all(bind=engine)
    print("Database tables initialized.")
except Exception as e:
    print(f"WARNING: Could not initialize DB tables: {e}")

app = FastAPI(title="NailVital AI Backend")

from fastapi.middleware.cors import CORSMiddleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins for mobile app flexibility
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)
from fastapi.responses import FileResponse

@app.get("/uploads/{filename}")
def serve_upload(filename: str):
    file_path = os.path.join(UPLOAD_DIR, filename)
    if os.path.exists(file_path):
        return FileResponse(file_path)
    raise HTTPException(status_code=404, detail="Image not found")

from fastapi import BackgroundTasks

@app.post("/register", response_model=schemas.UserResponse)
def register(user: schemas.UserCreate, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    import re

    # ── Validate Full Name (letters, spaces and dots only, min 2 characters) ──
    name_clean = user.name.strip()
    if not re.match(r'^[A-Za-z .]{2,}$', name_clean):
        if re.search(r'[^A-Za-z .]', name_clean):
            raise HTTPException(
                status_code=422,
                detail="Full name must not contain numbers or symbols"
            )
        raise HTTPException(
            status_code=422,
            detail="Full name must be at least 2 characters"
        )

    # ── Validate Phone Number (exactly 10 digits, starts with 6–9) ──
    if user.phone:
        phone_digits = re.sub(r'\D', '', user.phone)
        if len(phone_digits) != 10:
            raise HTTPException(
                status_code=422,
                detail=f"Phone number must be exactly 10 digits (you provided {len(phone_digits)})"
            )
        if not re.match(r'^[6-9]', phone_digits):
            raise HTTPException(
                status_code=422,
                detail="Phone number must start with 6, 7, 8, or 9"
            )
        # Normalise stored phone to digits-only
        user.phone = phone_digits

    clean_email = user.email.strip().lower()
    db_user = db.query(models.User).filter(models.User.email == clean_email).first()
    
    # If user exists and is already verified, return 400
    if db_user and db_user.is_verified:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    hashed_password = auth.get_password_hash(user.password)
    otp_code = otp_service.generate_otp()
    
    if db_user:
        # User exists but is unverified - allow updating details and resending OTP
        db_user.name = name_clean
        db_user.phone = user.phone
        db_user.age = user.age
        db_user.gender = user.gender
        db_user.height = user.height
        db_user.hashed_password = hashed_password
        db_user.otp = otp_code
        db_user.is_verified = False # Ensure it stays False until verified
        db.commit()
        db.refresh(db_user)
        new_user = db_user
    else:
        # Create new user
        new_user = models.User(
            name=name_clean, 
            email=clean_email, 
            phone=user.phone,
            age=user.age,
            gender=user.gender,
            height=user.height,
            hashed_password=hashed_password, 
            otp=otp_code
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
    
    # Send OTP in background to prevent API timeout
    background_tasks.add_task(otp_service.send_otp, clean_email, otp_code)
    
    return new_user

@app.get("/users/me/export-data")
def export_data(current_user: models.User = Depends(auth.get_current_user)):
    data = {
        "user": {
            "name": current_user.name,
            "email": current_user.email,
            "phone": current_user.phone,
            "age": current_user.age,
            "gender": current_user.gender,
            "height": current_user.height
        },
        "scans": [
            {
                "date": s.created_at.strftime('%Y-%m-%d'),
                "result": DISEASE_DETAILS.get(s.result_class, {}).get("name", s.result_class.replace("_", " ").title()),
                "confidence": s.confidence
            } for s in current_user.scans
        ]
    }
    return JSONResponse(content=data)

@app.get("/users/me", response_model=schemas.UserResponse)
def get_user_me(current_user: models.User = Depends(auth.get_current_user)):
    return current_user

@app.put("/users/me", response_model=schemas.UserResponse)
def update_user_me(
    user_update: schemas.UserUpdate, 
    db: Session = Depends(get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    if user_update.name is not None:
        current_user.name = user_update.name
    if user_update.phone is not None:
        current_user.phone = user_update.phone
    if user_update.age is not None:
        current_user.age = user_update.age
    if user_update.gender is not None:
        current_user.gender = user_update.gender
    if user_update.height is not None:
        current_user.height = user_update.height
    if user_update.password is not None:
        current_user.hashed_password = auth.get_password_hash(user_update.password)
    
    db.commit()
    db.refresh(current_user)
    return current_user

@app.delete("/users/me")
def delete_user_me(
    request: schemas.DeleteAccountRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(auth.get_current_user)
):
    if not auth.verify_password(request.password, current_user.hashed_password):
        raise HTTPException(status_code=400, detail="Incorrect password")

    # Delete all associated scans to prevent foreign key IntegrityError
    db.query(models.Scan).filter(models.Scan.user_id == current_user.id).delete()
    
    db.delete(current_user)
    db.commit()
    return {"message": "Account deleted successfully"}

@app.post("/verify-otp", response_model=schemas.Token)
def verify_otp(email: str, otp: str, db: Session = Depends(get_db)):
    email_clean = email.strip().lower()
    otp_clean = otp.strip()
    user = db.query(models.User).filter(models.User.email == email_clean).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    if user.otp == otp_clean:
        user.is_verified = True
        user.otp = None # Clear OTP after success
        db.commit()
        db.refresh(user)
        
        # Generate token so user is logged in automatically after verification
        access_token_expires = timedelta(minutes=auth.ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = auth.create_access_token(
            data={"sub": user.email}, expires_delta=access_token_expires
        )
        return {"access_token": access_token, "token_type": "bearer", "user": user}
    else:
        raise HTTPException(status_code=400, detail="Invalid OTP")

@app.post("/resend-otp")
def resend_otp(email: str, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    email_clean = email.strip().lower()
    user = db.query(models.User).filter(models.User.email == email_clean).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    new_otp = otp_service.generate_otp()
    user.otp = new_otp
    user.is_verified = False
    db.commit()
    
    background_tasks.add_task(otp_service.send_otp, email_clean, new_otp)
    return {"message": "OTP resent successfully"}

@app.post("/forgot-password")
def forgot_password(email: str, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    email_clean = email.strip().lower()
    user = db.query(models.User).filter(models.User.email == email_clean).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    otp = otp_service.generate_otp()
    user.otp = otp
    db.commit()
    
    background_tasks.add_task(otp_service.send_otp, email_clean, otp)
    return {"message": "Reset OTP sent"}

@app.post("/reset-password")
def reset_password(email: str, otp: str, new_password: str, db: Session = Depends(get_db)):
    email_clean = email.strip().lower()
    otp_clean = otp.strip()
    user = db.query(models.User).filter(models.User.email == email_clean).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    if user.otp == otp_clean:
        user.hashed_password = auth.get_password_hash(new_password)
        user.otp = None
        db.commit()
        return {"message": "Password reset successful"}
    else:
        raise HTTPException(status_code=400, detail="Invalid OTP")

@app.post("/login", response_model=schemas.Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    try:
        clean_email = form_data.username.strip().lower()
        user = db.query(models.User).filter(models.User.email == clean_email).first()
        if not user or not auth.verify_password(form_data.password, user.hashed_password):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect email or password",
                headers={"WWW-Authenticate": "Bearer"},
            )
        access_token_expires = timedelta(minutes=auth.ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = auth.create_access_token(
            data={"sub": user.email}, expires_delta=access_token_expires
        )
        return {"access_token": access_token, "token_type": "bearer", "user": user}
    except HTTPException:
        raise
    except Exception as e:
        if "timeout" in str(e).lower() or "connection" in str(e).lower():
            raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Database unreachable. Please check your network or VPN.")
        raise e

@app.post("/scan", response_model=schemas.ScanResponse)
def analyze_nail(
    file: UploadFile = File(...), 
    finger: str = "Unknown",
    current_user: models.User = Depends(auth.get_current_user),
    db: Session = Depends(get_db)
):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image")

    contents = file.file.read()
    
    # Run ML Prediction
    try:
        prediction = ml_predictor.predict(contents)
        if "error" in prediction:
            raise HTTPException(
                status_code=400, 
                detail=f"NOT_A_NAIL:{prediction.get('reason', 'UNKNOWN')}"
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")

    # Save File with unique UUID to prevent caching issues
    unique_filename = f"{uuid.uuid4()}_{file.filename}"
    file_path = f"{UPLOAD_DIR}/{unique_filename}"
    with open(file_path, "wb") as f:
        f.write(contents)

    # Handle multiple findings
    findings = prediction.get("findings", [])
    new_scan = models.Scan(
        user_id=current_user.id,
        image_path=file_path,
        finger=finger,
        result_class=prediction["result_class"],
        confidence=prediction["confidence"],
        findings_json=json.dumps(findings)
    )

    # Save Scan to Database (always attempt, error-resilient)
    try:
        db.add(new_scan)
        db.commit()
        db.refresh(new_scan)
        print(f"Scan saved to DB with id={new_scan.id}")
    except Exception as e:
        print(f"WARNING: Could not save scan to database: {e}")
        db.rollback()
        new_scan.id = 0

    # Enrich response
    response_findings = []
    for f in findings:
        details = DISEASE_DETAILS.get(f["result_class"], {})
        response_findings.append(schemas.Finding(
            result_class=f["result_class"],
            display_name=details.get("name"),
            description=details.get("description"),
            recommendation=details.get("recommendation"),
            confidence=f["confidence"]
        ))
    
    # Enrich primary result for backward compatibility
    primary_details = DISEASE_DETAILS.get(new_scan.result_class, {})
    new_scan.display_name = primary_details.get("name")
    new_scan.description = primary_details.get("description")
    new_scan.recommendation = primary_details.get("recommendation")
    new_scan.findings = response_findings

    return new_scan

@app.get("/history", response_model=list[schemas.ScanResponse])
def get_scan_history(
    limit: Optional[int] = None,
    current_user: models.User = Depends(auth.get_current_user), 
    db: Session = Depends(get_db)
):
    query = db.query(models.Scan).filter(models.Scan.user_id == current_user.id).order_by(models.Scan.created_at.desc())
    if limit:
        query = query.limit(limit)
    scans = query.all()
    
    # Enrich all scans with professional metadata and multiple findings
    for scan in scans:
        findings = []
        if scan.findings_json:
            try:
                findings_data = json.loads(scan.findings_json)
                for f in findings_data:
                    details = DISEASE_DETAILS.get(f["result_class"], {})
                    findings.append(schemas.Finding(
                        result_class=f["result_class"],
                        display_name=details.get("name"),
                        description=details.get("description"),
                        recommendation=details.get("recommendation"),
                        confidence=f["confidence"]
                    ))
            except:
                pass
        
        # Primary result enrichment
        details = DISEASE_DETAILS.get(scan.result_class, {})
        scan.display_name = details.get("name")
        scan.description = details.get("description")
        scan.recommendation = details.get("recommendation")
        scan.findings = findings
        
    return scans

@app.delete("/scans/{scan_id}")
def delete_scan(scan_id: int, current_user: models.User = Depends(auth.get_current_user), db: Session = Depends(get_db)):
    scan = db.query(models.Scan).filter(models.Scan.id == scan_id, models.Scan.user_id == current_user.id).first()
    if not scan:
        raise HTTPException(status_code=404, detail="Scan not found")
    
    # Delete the physical file
    if os.path.exists(scan.image_path):
        try:
            os.remove(scan.image_path)
        except:
            pass
            
    db.delete(scan)
    db.commit()
    return {"message": "Scan deleted successfully"}

@app.get("/scans/{scan_id}/export-pdf")
def export_scan_pdf(scan_id: int, current_user: models.User = Depends(auth.get_current_user), db: Session = Depends(get_db)):
    scan = db.query(models.Scan).filter(models.Scan.id == scan_id, models.Scan.user_id == current_user.id).first()
    if not scan:
        raise HTTPException(status_code=404, detail="Scan not found")
    
    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()
    
    # Header
    pdf.set_font("helvetica", 'B', 20)
    pdf.set_text_color(0, 51, 102) # Dark Blue
    pdf.cell(0, 15, txt="NailVital AI Health Report", ln=True, align='C')
    pdf.set_draw_color(0, 51, 102)
    pdf.line(10, pdf.get_y(), 200, pdf.get_y())
    pdf.ln(10)
    
    # Patient Info Header (Enhanced Layout)
    pdf.set_font("helvetica", 'B', 12)
    pdf.set_text_color(0, 51, 102)
    pdf.cell(0, 10, "PATIENT INFORMATION", ln=True)
    pdf.set_draw_color(220, 220, 220)
    pdf.line(10, pdf.get_y(), 200, pdf.get_y())
    pdf.ln(2)

    pdf.set_font("helvetica", 'B', 10)
    pdf.set_text_color(50, 50, 50)
    
    # Column 1
    curr_y = pdf.get_y()
    pdf.cell(30, 7, "Name:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(60, 7, f"{current_user.name}", 0)
    
    # Column 2
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Age:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 7, f"{current_user.age or 'N/A'}", ln=True)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Gender:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(60, 7, f"{current_user.gender or 'N/A'}", 0)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Phone:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 7, f"{current_user.phone or 'N/A'}", ln=True)

    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Height:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(60, 7, f"{current_user.height or 'N/A'}", 0)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Scan Date:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 7, f"{scan.created_at.strftime('%Y-%m-%d')}", ln=True)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 7, "Nail Location:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 7, f"{scan.finger or 'Not Specified'}", ln=True)
    pdf.ln(5)
    
    # Result Box
    pdf.set_fill_color(240, 248, 255) # Light Blue
    pdf.rect(10, pdf.get_y(), 190, 30, 'F')
    pdf.set_y(pdf.get_y() + 5)
    
    pdf.set_font("helvetica", 'B', 14)
    display_result = DISEASE_DETAILS.get(scan.result_class, {}).get("name", scan.result_class.replace("_", " ").title())
    pdf.cell(0, 10, txt=f"DIAGNOSIS: {display_result}", ln=True, align='C')
    pdf.set_font("helvetica", '', 12)
    pdf.cell(0, 10, txt=f"AI Confidence: {scan.confidence:.2f}%", ln=True, align='C')
    pdf.ln(10)
    
    # Image Section
    abs_image_path = os.path.abspath(scan.image_path)
    if os.path.exists(abs_image_path):
        try:
            # Centering image
            pdf.image(abs_image_path, x=45, y=None, w=120)
        except Exception as e:
            pdf.set_font("helvetica", 'I', 10)
            pdf.set_text_color(200, 0, 0)
            pdf.cell(0, 10, txt=f"[Note: Image could not be embedded: {str(e)}]", ln=True, align='C')
    else:
         pdf.cell(0, 10, txt="[Image file not found on server]", ln=True)

    # Condition Deep Dive
    details = DISEASE_DETAILS.get(scan.result_class, {"name": display_result, "description": "No detailed information available.", "recommendation": "Consult a doctor."})
    
    pdf.set_font("helvetica", 'B', 14)
    pdf.set_text_color(0, 51, 102)
    pdf.cell(0, 10, txt="CONDITION DEEP DIVE", ln=True)
    pdf.set_draw_color(0, 51, 102)
    pdf.line(10, pdf.get_y(), 100, pdf.get_y())
    pdf.ln(3)
    
    pdf.set_font("helvetica", 'B', 12)
    pdf.set_text_color(0, 0, 0)
    pdf.cell(0, 8, txt=f"Condition: {details['name']}", ln=True)
    
    pdf.set_font("helvetica", '', 11)
    pdf.multi_cell(0, 6, txt=f"About: {details['description']}")
    pdf.ln(3)
    
    pdf.set_font("helvetica", 'B', 11)
    pdf.set_text_color(0, 102, 51) # Dark Green
    pdf.multi_cell(0, 6, txt=f"Recommendation: {details['recommendation']}")
    
    pdf.ln(10)
    pdf.set_font("helvetica", 'I', 10)
    pdf.set_text_color(128, 128, 128)
    pdf.multi_cell(0, 5, txt="Disclaimer: This report is generated by an advanced AI model. It is intended for informational and tracking purposes only. Please consult a qualified health professional for clinical diagnosis or treatment.", align='C')

    # Return as bytes to prevent blank file/corruption
    try:
        pdf_bytes = bytes(pdf.output())
    except Exception as e:
        return JSONResponse(status_code=500, content={"detail": f"PDF generation failed: {str(e)}"})
        
    return Response(content=pdf_bytes, media_type="application/pdf", headers={
        "Content-Disposition": f"attachment; filename=NailVital_Report_{scan_id}.pdf"
    })

@app.get("/history/export-pdf")
def export_history_pdf(current_user: models.User = Depends(auth.get_current_user), db: Session = Depends(get_db)):
    scans = db.query(models.Scan).filter(models.Scan.user_id == current_user.id).order_by(models.Scan.created_at.desc()).all()
    
    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()
    
    pdf.set_font("helvetica", 'B', 20)
    pdf.set_text_color(0, 51, 102)
    pdf.cell(0, 15, txt="NailVital - Complete History Report", ln=True, align='C')
    pdf.set_draw_color(0, 51, 102)
    pdf.line(10, pdf.get_y(), 200, pdf.get_y())
    pdf.ln(10)
    
    pdf.set_font("helvetica", 'B', 12)
    pdf.set_text_color(0, 51, 102)
    pdf.cell(0, 10, "PATIENT PROFILE", ln=True)
    pdf.set_draw_color(220, 220, 220)
    pdf.line(10, pdf.get_y(), 200, pdf.get_y())
    pdf.ln(2)

    pdf.set_font("helvetica", 'B', 10)
    pdf.set_text_color(50, 50, 50)
    
    # Header Info in columns
    pdf.cell(30, 8, "Name:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(60, 8, f"{current_user.name}", 0)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 8, "Age/Gender:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 8, f"{current_user.age or 'N/A'} / {current_user.gender or 'N/A'}", ln=True)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 8, "Email:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(60, 8, f"{current_user.email}", 0)
    
    pdf.set_font("helvetica", 'B', 10)
    pdf.cell(30, 8, "Report Date:", 0)
    pdf.set_font("helvetica", '', 10)
    pdf.cell(0, 8, f"{datetime.datetime.now().strftime('%Y-%m-%d %H:%M')}", ln=True)
    
    for i, scan in enumerate(scans):
        # Add a page break if we're not on the first scan to ensure each diagnosis starts fresh if space is tight
        # or at least has enough room for the box + some info
        if i > 0 and pdf.get_y() > 150:
            pdf.add_page()
        elif i > 0:
            pdf.ln(10)
            pdf.set_draw_color(0, 51, 102)
            pdf.line(10, pdf.get_y(), 200, pdf.get_y())
            pdf.ln(10)

        # Header for the specific scan
        pdf.set_font("helvetica", 'B', 14)
        pdf.set_text_color(0, 51, 102)
        scan_date_str = scan.created_at.strftime('%Y-%m-%d')
        finger_info = f" ({scan.finger})" if scan.finger else ""
        pdf.cell(0, 10, txt=f"RECORD #{len(scans)-i}: {scan_date_str}{finger_info}", ln=True)
        pdf.ln(2)

        # Result Box (Matching single report)
        pdf.set_fill_color(240, 248, 255) # Light Blue
        pdf.rect(10, pdf.get_y(), 190, 25, 'F')
        pdf.set_y(pdf.get_y() + 3)
        
        pdf.set_font("helvetica", 'B', 13)
        pdf.set_text_color(0, 0, 0)
        display_result = DISEASE_DETAILS.get(scan.result_class, {}).get("name", scan.result_class.replace("_", " ").title())
        pdf.cell(0, 8, txt=f"DIAGNOSIS: {display_result}", ln=True, align='C')
        pdf.set_font("helvetica", '', 11)
        pdf.cell(0, 8, txt=f"AI Confidence: {scan.confidence:.2f}%", ln=True, align='C')
        pdf.ln(8)
        
        # Image Section (Centered, matching single report)
        abs_image_path = os.path.abspath(scan.image_path)
        if os.path.exists(abs_image_path):
            try:
                # Centering image - slightly smaller than single report (w=100) to keep multi-report cohesive
                pdf.image(abs_image_path, x=55, y=None, w=100)
                pdf.ln(5)
            except Exception as e:
                pdf.set_font("helvetica", 'I', 9)
                pdf.set_text_color(200, 0, 0)
                pdf.cell(0, 10, txt=f"[Note: Image could not be embedded: {str(e)}]", ln=True, align='C')
        else:
             pdf.set_font("helvetica", 'I', 9)
             pdf.set_text_color(150, 150, 150)
             pdf.cell(0, 10, txt="[Image file not found on server]", ln=True, align='C')

        # Condition Deep Dive (Matching single report)
        details = DISEASE_DETAILS.get(scan.result_class, {"name": display_result, "description": "No detailed information available.", "recommendation": "Consult a doctor."})
        
        pdf.set_font("helvetica", 'B', 12)
        pdf.set_text_color(0, 51, 102)
        pdf.cell(0, 8, txt="CONDITION DEEP DIVE", ln=True)
        pdf.set_draw_color(0, 51, 102)
        pdf.line(10, pdf.get_y(), 80, pdf.get_y())
        pdf.ln(2)
        
        pdf.set_font("helvetica", 'B', 11)
        pdf.set_text_color(0, 0, 0)
        pdf.cell(0, 7, txt=f"Condition: {details['name']}", ln=True)
        
        pdf.set_font("helvetica", '', 10)
        pdf.multi_cell(0, 5, txt=f"About: {details['description']}")
        pdf.ln(2)
        
        pdf.set_font("helvetica", 'B', 10)
        pdf.set_text_color(0, 102, 51) # Dark Green
        pdf.multi_cell(0, 5, txt=f"Recommendation: {details['recommendation']}")
        pdf.set_text_color(0, 0, 0) # Reset to black
        
        # Disclaimer at the bottom of the section (lighter)
        pdf.ln(5)
        pdf.set_font("helvetica", 'I', 8)
        pdf.set_text_color(150, 150, 150)
        pdf.multi_cell(0, 4, txt="This report section is AI-generated for informational purposes. Consult a professional for clinical diagnosis.", align='C')
        pdf.set_text_color(0, 0, 0)

    try:
        pdf_bytes = bytes(pdf.output())
    except Exception as e:
        return JSONResponse(status_code=500, content={"detail": f"PDF generation failed: {str(e)}"})
        
    return Response(content=pdf_bytes, media_type="application/pdf", headers={
        "Content-Disposition": "attachment; filename=NailVital_History_Report.pdf"
    })

@app.post("/chat", response_model=schemas.ChatResponse)
def get_ai_advice(request: schemas.ChatRequest, current_user: models.User = Depends(auth.get_current_user)):
    try:
        # Inject current date into context to keep the AI up to date
        current_date = datetime.datetime.now().strftime("%A, %B %d, %Y")
        
        # System prompt for context awareness
        system_prompt = (
            f"You are NailVital AI, an intelligent assistant. Today is {current_date}. "
            "You have expertise in nail health and dermatology, but can answer any general-purpose questions. "
            "Provide clear, concise, and helpful responses."
        )
        
        # Using Groq (Llama 3.3 70B) for lightning fast chat
        chat_completion = groq_client.chat.completions.create(
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": request.message}
            ],
            model="llama-3.3-70b-versatile",
            temperature=0.7,
            max_tokens=1024,
        )
        
        reply = chat_completion.choices[0].message.content
        return schemas.ChatResponse(reply=reply)
    except Exception as e:
        error_msg = str(e)
        if "413" in error_msg or "rate" in error_msg.lower():
             return schemas.ChatResponse(reply="AI Advisor is currently processing many requests. Please take a deep breath and try again in a few seconds.")
        
        # Fallback if API fails
        return schemas.ChatResponse(reply="I'm currently updating my knowledge base. For now, please ensure you keep your nails clean and dry. If you have specific symptoms, check your scan history or consult a dermatologist.")

@app.post("/voice-command", response_model=schemas.VoiceCommandResponse)
def handle_voice_command(request: schemas.VoiceCommandRequest, current_user: models.User = Depends(auth.get_current_user), db: Session = Depends(get_db)):
    try:
        current_date = datetime.datetime.now().strftime("%A, %B %d, %Y")
        
        # Pre-fetch recent history to give Gemini context directly
        scans = db.query(models.Scan).filter(models.Scan.user_id == current_user.id).order_by(models.Scan.created_at.desc()).limit(3).all()
        history_summary = []
        for s in scans:
            disease_info = DISEASE_DETAILS.get(s.result_class, {})
            name = disease_info.get("name", s.result_class)
            history_summary.append(f"Scan on {s.created_at.strftime('%Y-%m-%d')}: {name} (Conf: {s.confidence:.1f}%)")
        history_context = " | ".join(history_summary) if history_summary else "No previous scans."

        system_prompt = f"""You are the NailVital AI Voice Assistant. Today is {current_date}. 
The user speaking is {current_user.name}. Their recent scan history: {history_context}.

You MUST return a valid JSON object matching this schema exactly. Do not use markdown blocks.
Schema:
{{
  "action_type": "SPEAK" | "NAVIGATE" | "ACTION" | "MULTI",
  "message": "spoken response (optional)",
  "target": "screen or action name (optional)",
  "commands": [{{"type": "NAVIGATE"|"ACTION", "target": "..."}}] (only if action_type is MULTI)
}}

Valid targets for NAVIGATE: home, scan, history, chat, profile, health_wiki, login, register, personal_details, change_password, guest_account, about, logout

Valid targets for ACTION: 
- take_photo
- generate_report
- scroll_up, scroll_down (for ANY screen)
- open_disease:<disease_name> (e.g. open_disease:onychomycosis to expand a record in history)
- continue (to proceed through dialogs or multi-step processes)

Guidelines:
1. For simple navigation: action_type="NAVIGATE", target="screen_name".
2. For scrolling: action_type="ACTION", target="scroll_down"/"scroll_up".
3. To open a specific disease from history: Use action_type="MULTI" with NAVIGATE to "history" AND ACTION to "open_disease:<name>".
4. If a user asks a question, use action_type="SPEAK".
5. For "Continue as Guest", use NAVIGATE to "guest_account".
6. For "About icon" or "Tell me about this app", use NAVIGATE to "about".
7. Always provide a helpful "message".

Rules:
- Questions about history/profile -> `action_type: "SPEAK", message: "..."`
- Go to a screen -> `action_type: "NAVIGATE", target: "screen", message: "navigating..."`
- Do something in app -> `action_type: "ACTION", target: "take_photo", message: "opening camera"`
- Multiple operations -> `action_type: "MULTI", commands: [...]`
"""
        chat_completion = groq_client.chat.completions.create(
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": request.message}
            ],
            model="llama-3.3-70b-versatile",
            temperature=0.2,
            max_tokens=500,
            response_format={"type": "json_object"}
        )
        
        reply = chat_completion.choices[0].message.content
        data = json.loads(reply)
        
        return schemas.VoiceCommandResponse(
            action_type=data.get("action_type", "SPEAK"),
            message=data.get("message"),
            target=data.get("target"),
            commands=data.get("commands")
        )
    except Exception as e:
        print("Voice Command Error:", str(e))
        return schemas.VoiceCommandResponse(
            action_type="SPEAK",
            message="I'm having trouble processing that voice command right now."
        )

@app.get("/health")
def health_check():
    """Uptime health check endpoint for Render and monitoring services."""
    return {"status": "ok", "version": "2.2.0"}

@app.get("/health-wiki")
def get_health_wiki():
    """
    Returns the complete structured encyclopedia of nail conditions.
    This powers the Health Wiki section of the NailVital AI frontend.
    Each entry includes the condition key, display name, description,
    clinical recommendation, and a severity flag.
    """
    SEVERE_CONDITIONS = {
        "bluish_nail", "clubbing", "half_and_half_nails", "melanoma",
        "pale_nail", "red_lunula", "splinter_hemorrhage", "terrys_nail"
    }
    
    wiki = []
    for key, details in DISEASE_DETAILS.items():
        wiki.append({
            "condition_key": key,
            "name": details["name"],
            "description": details["description"],
            "recommendation": details["recommendation"],
            "is_severe": key in SEVERE_CONDITIONS
        })
    
    # Sort alphabetically by display name
    wiki.sort(key=lambda x: x["name"])
    return {"conditions": wiki, "total": len(wiki)}

@app.get("/")
def root():
    return {"message": "Welcome to NailVital AI API"}

# --- Informational Endpoints ---

@app.get("/app/about")
async def get_about():
    return {
        "app_name": "NailVital AI",
        "version": "1.2.0",
        "description": "NailVital AI is a comprehensive nail health companion that uses state-of-the-art Computer Vision to analyze your nails for 22 different conditions. Our mission is to provide accessible, early-stage screening for dermatology concerns.",
        "developer": "NailVital Team",
        "contact": "support@nailvital.ai"
    }

@app.get("/app/how-to-use")
async def get_how_to_use():
    return {
        "steps": [
            {
                "id": 1,
                "title": "Find Good Lighting",
                "description": "Natural daylight is best. Avoid harsh shadows or extremely dark environments."
            },
            {
                "id": 2,
                "title": "Position Your Finger",
                "description": "Keep your finger flat and about 10-15cm (4-6 inches) away from the camera lens."
            },
            {
                "id": 3,
                "title": "Focus & Center",
                "description": "Ensure the nail is centered in the frame. Tap your screen to focus if needed."
            },
            {
                "id": 4,
                "title": "Capture & Analyze",
                "description": "The AI will automatically validate the photo and provide a detailed health report."
            }
        ],
        "tips": [
            "Keep your nails clean and remove any nail polish for accurate results.",
            "If the AI rejects your photo, try adjusting the distance or lighting."
        ]
    }

@app.get("/app/faqs")
async def get_faqs():
    return {
        "faqs": [
            {
                "question": "Is this a medical diagnosis?",
                "answer": "No. NailVital AI is a screening tool designed for informational purposes. It is not a substitute for professional medical advice, diagnosis, or treatment."
            },
            {
                "question": "How accurate is the AI?",
                "answer": "Our AI model is trained on thousands of clinical images. However, factors like lighting and image resolution can affect accuracy. Always consult a doctor."
            },
            {
                "question": "Why is my photo being rejected?",
                "answer": "We use strict 'Clinical Quality Control' to ensure accuracy. If your photo is blurry, too dark, or doesn't clearly show a nail, our AI will ask you to retake it."
            },
            {
                "question": "Is my data private?",
                "answer": "Yes. We use industry-standard encryption (AES-256) to protect your profile and scan history. Your photos are only used for your personal health tracking."
            }
        ]
    }
