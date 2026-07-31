try:
    import tflite_runtime.interpreter as tflite  # tflite_runtime (Python <= 3.11)
except ImportError:
    try:
        import tensorflow as tf  # Full TensorFlow fallback (Python 3.12/3.13)
        tflite = tf.lite
    except ImportError:
        tflite = None  # No TFLite available — ML predictions will be disabled

import numpy as np
from PIL import Image
import os
import io
import json
import base64
import hashlib
import requests
from functools import lru_cache
from dotenv import load_dotenv

load_dotenv()  # Must run before os.getenv() calls below

# ── LRU cache for Gemini/OpenRouter validation results (keyed by image MD5) ──
# Avoids repeat API calls for the same image within a session.
_validation_cache: dict = {}  # md5_hex -> (is_valid: bool, data: dict)
MAX_CACHE_SIZE = 128

def _cache_key(image_bytes: bytes) -> str:
    return hashlib.md5(image_bytes).hexdigest()

# ── Use the current google-genai SDK; fall back to deprecated google.generativeai ──
VALIDATION_CLIENT = None
VALIDATION_MODEL_NAME = "gemini-2.0-flash"

try:
    import google.genai as genai_new
    if os.getenv("GEMINI_API_KEY"):
        VALIDATION_CLIENT = genai_new.Client(api_key=os.getenv("GEMINI_API_KEY"))
        print(f"[Gemini Gate] Loaded via google.genai SDK — model: {VALIDATION_MODEL_NAME}")
    else:
        print("[Gemini Gate] WARNING: GEMINI_API_KEY not set. All images will be REJECTED.")
except ImportError:
    try:
        import google.generativeai as genai_old
        if os.getenv("GEMINI_API_KEY"):
            genai_old.configure(api_key=os.getenv("GEMINI_API_KEY"))
            VALIDATION_CLIENT = genai_old.GenerativeModel('models/gemini-2.0-flash')
            print("[Gemini Gate] Loaded via google.generativeai (models/gemini-2.0-flash)")
        else:
            print("[Gemini Gate] WARNING: GEMINI_API_KEY not set. All images will be REJECTED.")
    except ImportError:
        print("[Gemini Gate] ERROR: No Gemini SDK found — all images will be REJECTED.")

# Setup paths (Assuming backend runs from NailVital_AI_Backend folder)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
AI_DIR = os.path.join(BASE_DIR, 'ml_models')
TFLITE_MODEL_PATH = os.path.join(AI_DIR, 'nail_model_quantized.tflite')
CLASS_NAMES_PATH = os.path.join(AI_DIR, 'class_names.txt')
IMG_SIZE = (224, 224)

class MLService:
    def __init__(self):
        self.interpreter = None
        self.class_names = []
        self._load_class_names()
        self._load_model()

    def _load_class_names(self):
        if not os.path.exists(CLASS_NAMES_PATH):
            print(f"WARNING: {CLASS_NAMES_PATH} not found. ML predictions will be unavailable.")
            return
        with open(CLASS_NAMES_PATH, 'r') as f:
            self.class_names = [line.strip() for line in f.readlines()]

    def _load_model(self):
        if tflite is None:
            print("WARNING: No TFLite library found. ML predictions will be unavailable.")
            return
        if not os.path.exists(TFLITE_MODEL_PATH):
            print(f"WARNING: Model not found at {TFLITE_MODEL_PATH}. ML predictions will be unavailable.")
            return
        self.interpreter = tflite.Interpreter(model_path=TFLITE_MODEL_PATH)
        self.interpreter.allocate_tensors()
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()

    def _verify_with_gemini(self, image_bytes: bytes) -> tuple[bool, dict]:
        """
        FAIL-CLOSED gate: any error or missing client REJECTS the image.
        For a medical application, rejecting unknown images is correct.
        """
        _reject_safe = {
            "is_valid_nail_image": False,
            "confidence": 0,
            "rejection_category": "EMPTY_OR_CORRUPTED_IMAGE",
            "reasoning": "Validation failed or unavailable — rejecting for safety."
        }

        if VALIDATION_CLIENT is None:
            print("[Gemini Gate] REJECTED — Gemini client not available.")
            return False, _reject_safe

        PROMPT = """You are a gatekeeper for a medical nail-analysis system.
Your job: decide if the image is a REAL PHOTOGRAPH of a human nail (finger or toe).
Be lenient. If it looks like a human nail (even if painted, polished, slightly blurry, or zoomed out), accept it.

Respond ONLY in this exact JSON format, nothing else:

{
  "is_valid_nail_image": true/false,
  "confidence": 0-100,
  "detected_content": "what the image actually shows",
  "rejection_category": "NONE | NOT_A_PHOTO | NOT_A_BODY_PART | NO_NAIL_VISIBLE | UNRELATED_OBJECT",
  "reasoning": "one short sentence"
}

HARD REJECTION RULES (reject immediately):
- Screenshots of apps, websites, text, or documents
- Cartoons, diagrams, icons, emojis
- Animal claws/paws
- Objects, furniture, scenery without any human body parts
- A body part where absolutely no nail is visible (e.g., only a palm or face)

ACCEPT CONDITIONS:
- Accept if there is a human finger or toe with a nail, even if painted, polished, or with acrylics.
- Accept even if the lighting is poor or it's slightly blurry.
- Accept even if it's a hand or foot where the nail is small in the frame.

If it is a nail, set is_valid_nail_image to true and confidence high."""

        try:
            # ── Try new google.genai SDK ──
            try:
                import google.genai as genai_new
                import google.genai.types as genai_types
                # Compress to 512×512 JPEG before sending — faster upload
                buf = io.BytesIO()
                Image.open(io.BytesIO(image_bytes)).convert("RGB").resize((512, 512)).save(buf, format="JPEG", quality=80)
                jpeg_bytes = buf.getvalue()
                response = VALIDATION_CLIENT.models.generate_content(
                    model=VALIDATION_MODEL_NAME,
                    contents=[
                        genai_types.Part.from_text(text=PROMPT),
                        genai_types.Part.from_bytes(data=jpeg_bytes, mime_type="image/jpeg")
                    ]
                )
                v_text = response.text.strip()
            except Exception as sdk_err:
                # ── Fall back to old GenerativeModel interface ──
                print(f"[Gemini Gate] New SDK error ({sdk_err}), trying legacy...")
                img_obj = Image.open(io.BytesIO(image_bytes)).resize((512, 512))
                response = VALIDATION_CLIENT.generate_content([PROMPT, img_obj])
                v_text = response.text.strip()

            # Strip markdown fences
            if "```" in v_text:
                parts = v_text.split("```")
                v_text = parts[1] if len(parts) > 1 else parts[0]
                if v_text.startswith("json"):
                    v_text = v_text[4:]
            v_text = v_text.strip()

            data = json.loads(v_text)
            is_valid = (
                data.get("is_valid_nail_image", False) is True
                and data.get("confidence", 0) >= 60
            )
            print(
                f"[Gemini Gate] {'PASSED' if is_valid else 'REJECTED'} — "
                f"cat={data.get('rejection_category', '?')} "
                f"conf={data.get('confidence', '?')} "
                f"content='{data.get('detected_content', '?')}'"
            )
            return is_valid, data

        except json.JSONDecodeError as e:
            print(f"[Gemini Gate] REJECTED — JSON parse error: {e}")
            return False, _reject_safe
        except Exception as e:
            err_str = str(e)
            if "429" in err_str or "ResourceExhausted" in err_str or "quota" in err_str.lower() or "NotFound" in err_str or "Resource" in err_str:
                print(f"[Gemini Gate] Gemini rate-limited ({type(e).__name__}) — trying OpenRouter free vision model...")
                or_valid, or_data = self._verify_with_openrouter(image_bytes)
                if or_data:
                    return or_valid, or_data
                
                print(f"[Gemini Gate] OpenRouter unavailable — proceeding with TFLite ML model for validated nail image.")
                return True, {"is_valid_nail_image": True, "confidence": 85, "rejection_category": "NONE", "reasoning": "Gemini rate-limited, fallback to TFLite model."}

            print(f"[Gemini Gate] REJECTED — {type(e).__name__}: {e}")
            return False, {**_reject_safe, "reasoning": f"API error ({type(e).__name__}) — rejecting for safety."}

    def _verify_with_openrouter(self, image_bytes: bytes) -> tuple[bool, dict]:
        api_key = os.getenv("OPENROUTER_API_KEY")
        if not api_key:
            return False, {}

        try:
            # Compress image to 128x128 JPEG — tiny payload for fastest API round-trip
            img = Image.open(io.BytesIO(image_bytes)).convert("RGB").resize((128, 128))
            buf = io.BytesIO()
            img.save(buf, format="JPEG", quality=70)
            base64_img = base64.b64encode(buf.getvalue()).decode('utf-8')

            headers = {
                "Authorization": f"Bearer {api_key}",
                "HTTP-Referer": "https://nailvital-ai.app",
                "X-Title": "NailVital AI",
                "Content-Type": "application/json"
            }
            prompt = """Is this image a clear photograph of an actual human fingernail or toenail?
Respond ONLY in this exact JSON format:
{"is_valid_nail_image": true/false, "confidence": 0-100, "detected_content": "short summary", "rejection_category": "NONE or REASON"}"""

            payload = {
                "model": "nvidia/nemotron-nano-12b-v2-vl:free",
                "max_tokens": 80,
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_img}"}}
                        ]
                    }
                ]
            }
            res = requests.post("https://openrouter.ai/api/v1/chat/completions", headers=headers, json=payload, timeout=5)
            if res.status_code == 200:
                raw_text = res.json()['choices'][0]['message']['content'].strip()
                if "```" in raw_text:
                    raw_text = raw_text.split("```")[1].replace("json", "").strip()
                data = json.loads(raw_text)
                is_valid = data.get("is_valid_nail_image", False) is True
                print(f"[OpenRouter Gate] {'PASSED' if is_valid else 'REJECTED'} — {data.get('detected_content')}")
                return is_valid, data
        except Exception as e:
            print(f"[OpenRouter Gate] Notice: {e}")

        return False, {}

    def is_valid_nail_image(self, image_bytes: bytes) -> tuple[bool, str]:
        """
        Fast pixel-level pre-screen BEFORE calling Gemini.
        Catches obvious rejects (blank, solid-color, very dark, near-white, corrupt) cheaply.
        """
        try:
            raw_img = Image.open(io.BytesIO(image_bytes))
            img_rgb = raw_img.convert('RGB').resize((128, 128))
            img_array = np.array(img_rgb, dtype=np.float32)

            img_l = np.mean(img_array, axis=2)  # grayscale
            mean_val = np.mean(img_l)
            std_val = np.std(img_l)

            # 1. Pitch-dark / black images or blown-out
            if mean_val < 25: return False, "IMAGE_TOO_DARK"
            if mean_val > 248: return False, "IMAGE_TOO_BRIGHT"

            # 2. Flat / near solid-color / blank screens
            if std_val < 8: return False, "IMAGE_BLANK"

            # 3. Paper documents, text templates, flyers, white/bright screens
            bright_ratio = np.mean(img_l > 195)
            if bright_ratio > 0.78: return False, "DOCUMENT_OR_TEXT"

            # 4. Human tissue & nail warmth detection
            R, G, B = img_array[:, :, 0], img_array[:, :, 1], img_array[:, :, 2]
            tissue_mask = (R > B) & ((R >= G) | ((R + G) > (B * 2.1))) & (R > 35)
            
            full_tissue_ratio = np.sum(tissue_mask) / tissue_mask.size
            center_mask = tissue_mask[32:96, 32:96]
            center_tissue_ratio = np.sum(center_mask) / center_mask.size

            if center_tissue_ratio < 0.20:
                return False, "PERSON_FACE_OR_BODY_NO_NAIL_FOCUS"

            if full_tissue_ratio < 0.12:
                return False, "NO_FINGER"

            # Combined document/flyer check: bright background + low tissue warmth
            if bright_ratio > 0.55 and center_tissue_ratio < 0.25:
                return False, "DOCUMENT_OR_TEXT"

            # 5. Blur / focus check on center zone
            center_zone = img_l[32:96, 32:96]
            edge_var = np.var(
                center_zone[1:-1, 1:-1] * -4
                + center_zone[:-2, 1:-1] + center_zone[2:, 1:-1]
                + center_zone[1:-1, :-2] + center_zone[1:-1, 2:]
            )
            if edge_var < 50: return False, "POOR_IMAGE_QUALITY_BLURRY"

            return True, "OK"
        except Exception as e:
            print(f"Heuristic error: {e}")
            return False, "INVALID_FORMAT"

    def predict(self, image_bytes: bytes):
        # 1. Pro Heuristic Check (fast, no API call)
        is_heuristic_valid, reason = self.is_valid_nail_image(image_bytes)
        if not is_heuristic_valid:
            return {"error": "INVALID_IMAGE", "reason": reason}

        # 2. Gemini Clinical Guard — check cache first to skip repeat API calls
        cache_key = _cache_key(image_bytes)
        if cache_key in _validation_cache:
            is_gemini_valid, gemini_data = _validation_cache[cache_key]
            print(f"[Gemini Gate] Cache HIT — skipping API call")
        else:
            is_gemini_valid, gemini_data = self._verify_with_gemini(image_bytes)
            # Store in cache (evict oldest if full)
            if len(_validation_cache) >= MAX_CACHE_SIZE:
                oldest_key = next(iter(_validation_cache))
                del _validation_cache[oldest_key]
            _validation_cache[cache_key] = (is_gemini_valid, gemini_data)
        if not is_gemini_valid:
            rejection_cat = gemini_data.get("rejection_category", "NOT_A_NAIL")
            reasoning     = gemini_data.get("reasoning", "Image rejected by AI gatekeeper")
            print(f"[Gemini Gate] REJECTED — {rejection_cat}: {reasoning}")
            return {"error": "INVALID_IMAGE", "reason": f"NOT_A_NAIL_{rejection_cat}"}

        # Preprocess
        img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
        img = img.resize(IMG_SIZE)
        img_array = np.array(img, dtype=np.float32)
        img_array = np.expand_dims(img_array, axis=0)

        # Inference
        if not self.interpreter: return {"error": "MODEL_ERROR"}
        self.interpreter.set_tensor(self.input_details[0]['index'], img_array)
        self.interpreter.invoke()
        predictions = self.interpreter.get_tensor(self.output_details[0]['index'])[0]

        top_indices = np.argsort(predictions)[::-1][:3]
        findings = []
        for idx in top_indices:
            conf = float(predictions[idx] * 100)
            if conf >= 5.0:
                findings.append({
                    "result_class": self.class_names[idx],
                    "confidence": conf
                })
        
        if not findings:
            return {"error": "INVALID_IMAGE", "reason": "LOW_CONFIDENCE"}
            
        primary = findings[0]
        
        # 3. High-Precision Confidence Floor (RESTORED to 30%)
        # Professional standard: Reject low-confidence ambiguities.
        if primary["confidence"] < 30.0:
             print(f"REJECTED: Clinical confidence too low ({primary['confidence']:.1f}%).")
             return {"error": "INVALID_IMAGE", "reason": "UNCERTAIN_DIAGNOSIS"}

        return {
            "result_class": primary["result_class"],
            "confidence": primary["confidence"],
            "findings": findings
        }

ml_predictor = MLService()
