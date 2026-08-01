import sys
import json
import os
from ml_service import ml_predictor

def test_model(image_path):
    if not os.path.exists(image_path):
        print(f"Error: {image_path} not found.")
        return

    with open(image_path, "rb") as f:
        img_bytes = f.read()

    print(f"Testing {image_path} with ml_service...")
    try:
        res = ml_predictor.predict(img_bytes)
        print("Result:")
        print(json.dumps(res, indent=2))
    except Exception as e:
        print(f"Prediction failed: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python test_yolo_local.py <image_path>")
    else:
        test_model(sys.argv[1])
