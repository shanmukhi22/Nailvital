import tensorflow as tf
import numpy as np
from PIL import Image
import os

# Configuration
TFLITE_MODEL_PATH = 'nail_model_quantized.tflite'
CLASS_NAMES_PATH = 'class_names.txt'
IMG_SIZE = (224, 224)  # Must match train_model.py

# Disease info database
DISEASE_INFO = {
    'aloperia_areata': {
        'display_name': 'Alopecia Areata',
        'description': 'An autoimmune condition causing patchy hair and nail loss.',
        'severity': 'Moderate',
        'action': 'Consult a dermatologist for immunotherapy treatment.',
        'emoji': '⚠️'
    },
    'beaus_lines': {
        'display_name': "Beau's Lines",
        'description': 'Deep grooved lines running across the nail caused by illness or injury.',
        'severity': 'Moderate',
        'action': 'Check for underlying systemic illness. Visit a doctor.',
        'emoji': '⚠️'
    },
    'bluish_nail': {
        'display_name': 'Bluish Nail',
        'description': 'Blue discoloration indicating possible oxygen deficiency or heart issues.',
        'severity': 'High',
        'action': 'Seek immediate medical attention. May indicate heart or lung issues.',
        'emoji': '🔴'
    },
    'clubbing': {
        'display_name': 'Nail Clubbing',
        'description': 'Nails curve around enlarged fingertips, often linked to lung or heart disease.',
        'severity': 'High',
        'action': 'Consult a doctor immediately for cardiac or pulmonary evaluation.',
        'emoji': '🔴'
    },
    'dariers_disease': {
        'display_name': "Darier's Disease",
        'description': 'A genetic skin disorder affecting nails with red and white streaks.',
        'severity': 'Moderate',
        'action': 'See a dermatologist. Genetic counseling may be recommended.',
        'emoji': '⚠️'
    },
    'eczema': {
        'display_name': 'Nail Eczema',
        'description': 'Inflammatory condition causing pitting, ridging, and discoloration of nails.',
        'severity': 'Low',
        'action': 'Use moisturizers and consult a dermatologist for topical treatment.',
        'emoji': '🟡'
    },
    'half_and_half_nails': {
        'display_name': 'Half and Half Nails',
        'description': 'Nail split into white and brown halves, often linked to kidney disease.',
        'severity': 'High',
        'action': 'Consult a nephrologist. May indicate chronic kidney disease.',
        'emoji': '🔴'
    },
    'healthy': {
        'display_name': 'Healthy Nail',
        'description': 'Your nail appears healthy with no signs of disease.',
        'severity': 'None',
        'action': 'Keep maintaining good nail hygiene!',
        'emoji': '✅'
    },
    'koilonychia': {
        'display_name': 'Koilonychia (Spoon Nails)',
        'description': 'Nails are thin and curved upward like a spoon, often due to iron deficiency.',
        'severity': 'Moderate',
        'action': 'Check iron levels. Iron supplements may be needed.',
        'emoji': '⚠️'
    },
    'leukonychia': {
        'display_name': 'Leukonychia (White Nails)',
        'description': 'White spots or lines on nails, usually from minor trauma or deficiency.',
        'severity': 'Low',
        'action': 'Usually harmless. Check zinc and calcium levels.',
        'emoji': '🟡'
    },
    'melanoma': {
        'display_name': 'Nail Melanoma',
        'description': 'A serious form of skin cancer appearing as dark streaks under the nail.',
        'severity': 'Critical',
        'action': 'URGENT: See an oncologist immediately. Early detection is critical.',
        'emoji': '🚨'
    },
    'muehrckes_lines': {
        'display_name': "Muehrcke's Lines",
        'description': 'Paired white lines across the nail linked to low protein or liver disease.',
        'severity': 'Moderate',
        'action': 'Check albumin/protein levels. Consult a physician.',
        'emoji': '⚠️'
    },
    'onychogryphosis': {
        'display_name': 'Onychogryphosis (Ram Horn Nail)',
        'description': 'Nails thicken and curve like a rams horn, common in elderly.',
        'severity': 'Moderate',
        'action': 'Visit a podiatrist for nail trimming and treatment.',
        'emoji': '⚠️'
    },
    'onycholycis': {
        'display_name': 'Onycholysis',
        'description': 'Nail separates from the nail bed, often due to infection or trauma.',
        'severity': 'Moderate',
        'action': 'Keep nails dry and short. See a dermatologist if it spreads.',
        'emoji': '⚠️'
    },
    'onychomycosis': {
        'display_name': 'Onychomycosis (Fungal Nail)',
        'description': 'Fungal infection causing thick, discolored, brittle nails.',
        'severity': 'Moderate',
        'action': 'Antifungal medication needed. Consult a dermatologist.',
        'emoji': '⚠️'
    },
    'pale_nail': {
        'display_name': 'Pale Nails',
        'description': 'Very pale nails may indicate anemia, heart disease, or malnutrition.',
        'severity': 'Moderate',
        'action': 'Check blood count. May indicate anemia or liver disease.',
        'emoji': '⚠️'
    },
    'pitting': {
        'display_name': 'Nail Pitting',
        'description': 'Small depressions on the nail surface, commonly linked to psoriasis.',
        'severity': 'Moderate',
        'action': 'Consult a dermatologist. Often associated with psoriasis.',
        'emoji': '⚠️'
    },
    'psoriasis': {
        'display_name': 'Nail Psoriasis',
        'description': 'Psoriasis affecting nails causing pitting, thickening, and discoloration.',
        'severity': 'Moderate',
        'action': 'See a dermatologist for topical or systemic treatment.',
        'emoji': '⚠️'
    },
    'red_lunula': {
        'display_name': 'Red Lunula',
        'description': 'Red half-moon at nail base, associated with heart failure or autoimmune disease.',
        'severity': 'High',
        'action': 'Consult a cardiologist or rheumatologist immediately.',
        'emoji': '🔴'
    },
    'splinter_hemorrhage': {
        'display_name': 'Splinter Hemorrhage',
        'description': 'Tiny blood clots under the nail, can indicate heart valve infection.',
        'severity': 'High',
        'action': 'Seek medical attention. May indicate endocarditis.',
        'emoji': '🔴'
    },
    'terrys_nail': {
        'display_name': "Terry's Nail",
        'description': 'Nails appear mostly white with a narrow pink band, linked to liver disease.',
        'severity': 'High',
        'action': 'Consult a hepatologist. May indicate liver cirrhosis.',
        'emoji': '🔴'
    },
    'yellow_nails': {
        'display_name': 'Yellow Nail Syndrome',
        'description': 'Yellow, thickened nails possibly linked to respiratory or lymphatic issues.',
        'severity': 'Moderate',
        'action': 'Consult a pulmonologist or lymphologist.',
        'emoji': '⚠️'
    }
}

def load_class_names():
    if not os.path.exists(CLASS_NAMES_PATH):
        raise FileNotFoundError(f"Error: {CLASS_NAMES_PATH} not found. Run train_model.py first.")
    with open(CLASS_NAMES_PATH, 'r') as f:
        return [line.strip() for line in f.readlines()]

def preprocess_image(image_path):
    img = Image.open(image_path).convert('RGB')
    img = img.resize(IMG_SIZE)
    img_array = np.array(img, dtype=np.float32)
    # Note: EfficientNetV2 includes its own normalization inside the model layers
    # but we should match the training behavior (which had its own rescaling)
    # The training script used `layers.Rescaling(1./127.5, offset=-1)`
    # Wait, the aggressive script used EfficientNetV2S without manual rescaling 
    # because it's built-in. Let's stick to that.
    img_array = np.expand_dims(img_array, axis=0)
    return img_array

def predict(image_path):
    # Load class names
    class_names = load_class_names()

    # Load TFLite model
    interpreter = tf.lite.Interpreter(model_path=TFLITE_MODEL_PATH)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Preprocess image
    img_array = preprocess_image(image_path)

    # Run inference
    interpreter.set_tensor(input_details[0]['index'], img_array)
    interpreter.invoke()
    predictions = interpreter.get_tensor(output_details[0]['index'])[0]

    # Get top 3 predictions
    top3_indices = np.argsort(predictions)[::-1][:3]
    top3_results = []
    for idx in top3_indices:
        class_key = class_names[idx]
        confidence = predictions[idx] * 100
        info = DISEASE_INFO.get(class_key, {
            'display_name': class_key.replace('_', ' ').title(),
            'description': 'No information available.',
            'severity': 'Unknown',
            'action': 'Consult a doctor.',
            'emoji': '❓'
        })
        top3_results.append({
            'class_key': class_key,
            'confidence': confidence,
            **info
        })

    return top3_results

def print_result(results):
    print("\n" + "="*55)
    print("       NAIL VITAL AI - DIAGNOSIS RESULT")
    print("="*55)

    top = results[0]
    print(f"\n{top['emoji']}  PRIMARY DIAGNOSIS")
    print(f"    Disease    : {top['display_name']}")
    print(f"    Confidence : {top['confidence']:.2f}%")
    print(f"    Severity   : {top['severity']}")
    print(f"    Description: {top['description']}")
    print(f"    Action     : {top['action']}")

    print("\n─"*55)
    print("  OTHER POSSIBILITIES:")
    for r in results[1:]:
        print(f"  {r['emoji']}  {r['display_name']:<30} {r['confidence']:.2f}%")

    print("\n─"*55)
    print("  ⚠️  DISCLAIMER: This is an AI screening tool only.")
    print("      Always consult a qualified doctor for diagnosis.")
    print("="*55 + "\n")

def main():
    print("\nNAIL VITAL AI - Disease Detector")
    print("-"*35)
    print(f"Model: {TFLITE_MODEL_PATH}")

    while True:
        image_path = input("\nEnter image path (eg: sample.jpg) or 'q' to quit: ").strip()

        if image_path.lower() == 'q':
            break

        if not os.path.exists(image_path):
            print(f"Error: File '{image_path}' not found.")
            continue

        print("Analyzing...")
        try:
            results = predict(image_path)
            print_result(results)
        except Exception as e:
            print(f"Prediction Error: {e}")

if __name__ == "__main__":
    main()
