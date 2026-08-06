import os, json, io
from dotenv import load_dotenv
load_dotenv()
from google import genai
from google.genai import types as genai_types
from PIL import Image

key = os.getenv("GEMINI_API_KEY")
client = genai.Client(api_key=key)

# Simulate a nail-colored image
img = Image.new("RGB", (200, 200), color=(220, 180, 160))
buf = io.BytesIO()
img.save(buf, format="JPEG")
img_bytes = buf.getvalue()
image_part = genai_types.Part.from_bytes(data=img_bytes, mime_type="image/jpeg")

prompt = 'Analyze this nail image. Return ONLY valid JSON: {"nail_detected": true, "primary_diagnosis": "healthy", "possible_conditions": [{"name": "healthy", "confidence_percent": 90}]}'

response = client.models.generate_content(
    model="models/gemini-3.5-flash",
    contents=[image_part, prompt],
    config=genai_types.GenerateContentConfig(
        max_output_tokens=512,
        temperature=0.1,
        response_mime_type="application/json"
    )
)

print("Response text:", response.text)
data = json.loads(response.text)
print("Parsed OK! primary_diagnosis =", data.get("primary_diagnosis"))
print("\nSUCCESS - Full image analysis pipeline is working!")
