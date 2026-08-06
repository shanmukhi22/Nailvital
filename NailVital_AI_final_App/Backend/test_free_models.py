import os, io
from dotenv import load_dotenv
load_dotenv()
from google import genai
from google.genai import types as genai_types
from PIL import Image

key = os.getenv("GEMINI_API_KEY")
client = genai.Client(api_key=key)

img = Image.new("RGB", (100, 100), color=(220, 180, 160))
buf = io.BytesIO()
img.save(buf, format="JPEG")
img_bytes = buf.getvalue()
image_part = genai_types.Part.from_bytes(data=img_bytes, mime_type="image/jpeg")

free_candidates = [
    "models/gemini-flash-lite-latest",
    "models/gemini-3.1-flash-lite",
    "models/gemini-2.0-flash-lite",
    "models/gemini-2.0-flash-lite-001",
]

for m in free_candidates:
    try:
        r = client.models.generate_content(
            model=m,
            contents=[image_part, "Say: OK"],
            config=genai_types.GenerateContentConfig(max_output_tokens=10, temperature=0.1)
        )
        txt = r.text.strip() if r.text else "(no text)"
        print("FREE+WORKS: " + m + " -> " + txt)
    except Exception as e:
        err = str(e)
        if "429" in err:
            status = "QUOTA HIT (free limit reached for today)"
        elif "404" in err:
            status = "NOT FOUND"
        elif "billing" in err.lower() or "paid" in err.lower():
            status = "PAID ONLY"
        else:
            status = "ERROR: " + err[:60]
        print(status + ": " + m)
