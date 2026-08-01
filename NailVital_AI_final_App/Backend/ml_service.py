import numpy as np
from PIL import Image
import os
import io
import json
import base64
import hashlib
import tempfile
import cv2
from dotenv import load_dotenv

load_dotenv()

# We need ultralytics and huggingface_hub
from ultralytics import YOLO
from huggingface_hub import hf_hub_download

# Gemini
try:
    import google.generativeai as genai_old
except ImportError:
    genai_old = None

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
HF_TOKEN = os.getenv("HF_TOKEN")
YOLO_MODEL_REPO = "mnemic/nails_seg_yolov8"
GEMINI_MODEL = "gemini-flash-latest"

CONFIDENCE_THRESH = 0.25

# We need to map the exact conditions expected by main.py
CONDITION_MAPPING_PROMPT = """
Match features to these conditions exactly as named (do not change the names). Use ONLY these exact string keys for the 'name' and 'primary_diagnosis' fields:
- "aloperia_areata": Alopecia Areata
- "beaus_lines": Beau's Lines
- "bluish_nail": Bluish Nails
- "clubbing": Nail Clubbing
- "dariers_disease": Darier's Disease
- "eczema": Nail Eczema
- "half_and_half_nails": Half-and-Half Nails
- "koilonychia": Koilonychia (Spoon Nails)
- "leukonychia": Leukonychia (White Spots)
- "melanoma": Subungual Melanoma (URGENT)
- "muehrckes_lines": Muehrcke's Lines
- "onychogryphosis": Onychogryphosis (Ram's Horn Nails)
- "onycholycis": Onycholysis
- "onychomycosis": Onychomycosis (Fungal Nail Infection)
- "pale_nail": Pale Nails
- "pitting": Nail Pitting
- "psoriasis": Nail Psoriasis
- "red_lunula": Red Lunula
- "splinter_hemorrhage": Splinter Hemorrhage
- "terrys_nail": Terry's Nails
- "yellow_nails": Yellow Nail Syndrome
- "healthy": Healthy Nails

Only include conditions with confidence_percent > 10%.
The primary_diagnosis should be the exact string key from the left side of the list above (e.g. "onychomycosis").
Also for any possible_conditions, the name should be the exact string key (e.g. "psoriasis").
"""

SYSTEM_INSTRUCTION = """
You are NailCheck AI — an expert dermatological image observation assistant
specialized in fingernail and toenail visual analysis.

RULES:
1. Always analyze ONLY the nail visible in the image
2. Return ONLY valid JSON — no extra text, no markdown
3. NEVER provide a definitive medical diagnosis
4. NEVER recommend specific medications
5. ALWAYS include the medical disclaimer in your response
6. If no nail is visible → set nail_detected: false immediately
7. Use temperature=0.1 reasoning: be precise, not creative
"""

NAIL_ANALYSIS_PROMPT = f"""
You are analyzing a cropped fingernail/toenail image for health indicators.
Follow ALL 6 steps in order. Do NOT skip any step.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 1 ── IMAGE QUALITY CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Answer these:
✓ Is a fingernail or toenail clearly visible?
✓ Is the image sharp and in focus?
✓ Is the lighting adequate (not too dark/bright)?
✓ Is the nail surface unobstructed (no heavy nail polish)?

→ If NO nail visible: set "nail_detected": false, stop here.
→ If image is blurry: set "image_quality": "poor"
→ If image is acceptable: set "image_quality": "fair" or "good"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 2 ── NAIL ANATOMY IDENTIFICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Identify which parts are visible:
□ nail_plate    → the hard surface of the nail
□ nail_bed      → skin beneath the plate (pink area)
□ cuticle       → skin at the base of the nail
□ nail_tip      → free edge at top
□ nail_fold     → skin folds on sides
□ periungual    → surrounding skin

List only what you can actually see.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 3 ── VISUAL FEATURE DETECTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CRITICAL INSTRUCTION: You must actively evaluate the image against ALL 22 supported conditions. Do not default to common conditions if subtle markers of rarer ones exist.

[A] COLOR ABNORMALITIES:
   • White spots or patches → leukonychia
   • White nails with a dark pink/brown band at the tip (Terry's Nails) → terrys_nail
   • Bottom half white, top half red/brown (Half-and-Half) → half_and_half_nails
   • Pale or white nail bed → pale_nail
   • Yellow or yellow-brown thickening → yellow_nails or onychomycosis
   • Dark/black longitudinal streak → melanoma (URGENT)
   • Reddish-brown vertical splinter lines → splinter_hemorrhage
   • Redness in the lunula (base half-moon) → red_lunula
   • Blue or purple tint → bluish_nail
   • "Salmon patch" or "oil drop" discoloration → psoriasis

[B] TEXTURE & SURFACE ABNORMALITIES:
   • Horizontal deep ridges/grooves → beaus_lines
   • Narrow white transverse lines (not depressed) → muehrckes_lines
   • Small dents or pits (like a thimble) → pitting or aloperia_areata
   • Longitudinal streaks with V-shaped nicks at the edge → dariers_disease
   • Rough, crumbling, powdery, brittle → onychomycosis
   • Smooth, shiny, uniform → healthy

[C] SHAPE & STRUCTURE ABNORMALITIES:
   • Spoon-shaped / concave nails → koilonychia
   • Bulging, rounded, drumstick-like tips (loss of normal angle) → clubbing
   • Overgrown, thick, curved like a ram's horn → onychogryphosis
   • Nail lifting/separating from the nail bed → onycholycis
   • Extreme thickening of the nail plate → onychomycosis

[D] SURROUNDING SKIN / INFLAMMATION:
   • Scaly, red, inflamed skin around nail → eczema
   • Cuticle destruction / generalized nail dystrophy → dariers_disease
   • Redness + swelling around nail fold → paronychia
   • Darkening of skin under nail → melanoma (URGENT)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 4 ── CONDITION MATCHING TABLE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
{CONDITION_MAPPING_PROMPT}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 5 ── URGENCY CLASSIFICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Classify urgency_level as ONE of:

"normal"  → Healthy nail, no action needed
"monitor" → Minor changes, watch for 2-4 weeks
"consult" → Should see a doctor within 1-2 weeks
"urgent"  → Needs immediate medical attention

Urgent triggers (always set urgent):
⚠ Dark longitudinal streak (possible melanoma)
⚠ Severe infection signs (pus, extreme swelling)
⚠ Sudden unexplained nail loss

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
STEP 6 ── JSON OUTPUT FORMAT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Return ONLY this JSON. No text before or after. Keep it extremely concise to ensure fast response times:

{{
  "nail_detected": true,
  "possible_conditions": [
    {{
      "name": "condition_key",
      "confidence_percent": 82
    }}
  ],
  "primary_diagnosis": "condition_key"
}}
"""

class MLService:
    def __init__(self):
        self.yolo_model = None
        self._load_yolo_model()
        self.gemini_model = None
        self._configure_gemini()

    def _load_yolo_model(self):
        try:
            print("🔷 Loading YOLOv8 model locally...")
            model_path = os.path.join(os.path.dirname(__file__), "ml_models", "nails_seg_s_yolov8_v1.pt")
            
            # Fallback to HuggingFace if local file is missing
            if not os.path.exists(model_path):
                print(f"⚠️ Local model not found at {model_path}. Downloading from HuggingFace...")
                model_path = hf_hub_download(
                    repo_id=YOLO_MODEL_REPO,
                    filename="nails_seg_s_yolov8_v1.pt",
                    token=HF_TOKEN
                )
                
            self.yolo_model = YOLO(model_path)
            print("✅ YOLOv8 model loaded successfully.")
        except Exception as e:
            print(f"❌ Failed to load YOLOv8 model: {e}")
            self.yolo_model = None

    def _configure_gemini(self):
        print("🔷 Configuring Gemini for nail analysis...")
        if genai_old and GEMINI_API_KEY:
            genai_old.configure(api_key=GEMINI_API_KEY)
            self.gemini_model = genai_old.GenerativeModel(
                model_name=GEMINI_MODEL,
                system_instruction=SYSTEM_INSTRUCTION
            )
            print("✅ Gemini configured successfully.")
        else:
            print("❌ Gemini configuration failed (Missing API Key or google.generativeai module)")
            self.gemini_model = None

    def detect_and_crop_nail(self, image_bytes: bytes):
        if not self.yolo_model:
            return None, "YOLO model not loaded", 0.0

        nparr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if image is None:
             return None, "Invalid image format", 0.0
             
        tmp_in = tempfile.NamedTemporaryFile(suffix=".jpg", delete=False)
        tmp_in.close()
        cv2.imwrite(tmp_in.name, image)

        results = self.yolo_model.predict(
            source=tmp_in.name,
            conf=CONFIDENCE_THRESH,
            imgsz=640,
            verbose=False
        )

        try:
            os.unlink(tmp_in.name)
        except Exception as e:
            print(f"Failed to delete temp file: {e}")

        for result in results:
            if result.boxes is None or len(result.boxes) == 0:
                return None, "no_nail", 0.0

            # Get highest confidence detection
            best_idx = result.boxes.conf.argmax()
            best_box = result.boxes[best_idx]
            conf = float(best_box.conf[0])

            x1, y1, x2, y2 = map(int, best_box.xyxy[0].tolist())

            # Add padding
            pad = 15
            h, w = image.shape[:2]
            x1 = max(0, x1 - pad)
            y1 = max(0, y1 - pad)
            x2 = min(w, x2 + pad)
            y2 = min(h, y2 + pad)

            # Crop nail
            nail_crop = image[y1:y2, x1:x2]

            # Convert to PIL Image
            crop_rgb = cv2.cvtColor(nail_crop, cv2.COLOR_BGR2RGB)
            pil_img = Image.fromarray(crop_rgb)
            return pil_img, "success", conf

        return None, "no_nail", 0.0

    def analyze_with_gemini(self, pil_image: Image.Image):
        if not self.gemini_model:
            raise Exception("Gemini model not initialized")

        try:
            import google.generativeai as genai
            generation_config = genai.types.GenerationConfig(
                temperature=0.1,                    # Low = accurate
                top_p=0.8,
                max_output_tokens=2048,
                response_mime_type="application/json"  # Force JSON
            )
            
            response = self.gemini_model.generate_content(
                [pil_image, NAIL_ANALYSIS_PROMPT],
                generation_config=generation_config
            )

            v_text = response.text.strip()
            if "```" in v_text:
                parts = v_text.split("```")
                v_text = parts[1] if len(parts) > 1 else parts[0]
                if v_text.startswith("json"):
                    v_text = v_text[4:]
            v_text = v_text.strip()
            
            return json.loads(v_text)
        except Exception as e:
            print(f"Gemini Analysis Error: {e}")
            raise Exception(f"Gemini API failure: {str(e)}")

    def predict(self, image_bytes: bytes):
        try:
            print("🔷 Detecting nail with YOLOv8...")
            cropped_img, message, conf = self.detect_and_crop_nail(image_bytes)
            
            if cropped_img is None:
                print(f"⚠️ YOLO failed to detect a nail ({message}), rejecting image...")
                return {"error": "INVALID_IMAGE", "reason": "No nail detected in the image. Please provide a clear image of a nail."}
            else:
                print(f"✅ Nail found! Confidence: {conf:.1%}")
            
            print("🔷 Analyzing nail disease with Gemini...")
            analysis_result = self.analyze_with_gemini(cropped_img)
            
            if not analysis_result.get("nail_detected", False):
                return {"error": "INVALID_IMAGE", "reason": "GEMINI_REJECTION: No nail detected in crop"}

            primary_diagnosis = analysis_result.get("primary_diagnosis", "healthy")
            possible_conditions = analysis_result.get("possible_conditions", [])
            
            # Find the primary condition's confidence
            primary_conf = 85.0 # fallback
            for cond in possible_conditions:
                if cond.get("name") == primary_diagnosis:
                    primary_conf = cond.get("confidence_percent", 85.0)
                    break
            
            # Format findings for main.py backward compatibility
            findings = []
            for cond in possible_conditions:
                findings.append({
                    "result_class": cond.get("name", "healthy"),
                    "confidence": float(cond.get("confidence_percent", 50.0))
                })
                
            if not findings:
                 findings = [{"result_class": primary_diagnosis, "confidence": primary_conf}]

            return {
                "result_class": primary_diagnosis,
                "confidence": primary_conf,
                "findings": findings,
                "extra": analysis_result
            }
            
        except Exception as e:
            print(f"Error in MLService predict: {e}")
            return {"error": "MODEL_ERROR", "reason": str(e)}

ml_predictor = MLService()
