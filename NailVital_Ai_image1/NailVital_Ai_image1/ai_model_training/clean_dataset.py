import os
import hashlib
from PIL import Image
import random
import shutil

# Configuration
DATASET_PATH = 'dataset/unified_dataset'
CLEAN_DATASET_PATH = 'dataset/cleaned_dataset'
MAX_IMAGES_PER_CLASS = 1000
MIN_IMAGE_SIZE = (64, 64)

def get_image_hash(image_path):
    """Computes a hash of the image metadata/content to find duplicates."""
    try:
        with open(image_path, 'rb') as f:
            return hashlib.md5(f.read()).hexdigest()
    except:
        return None

def clean_dataset():
    if os.path.exists(CLEAN_DATASET_PATH):
        print(f"Deleting existing cleaned dataset: {CLEAN_DATASET_PATH}")
        shutil.rmtree(CLEAN_DATASET_PATH)
    
    os.makedirs(CLEAN_DATASET_PATH, exist_ok=True)
    
    all_hashes = set()
    total_original = 0
    total_cleaned = 0
    
    classes = [d for d in os.listdir(DATASET_PATH) if os.path.isdir(os.path.join(DATASET_PATH, d))]
    
    for cls in classes:
        cls_path = os.path.join(DATASET_PATH, cls)
        target_cls_path = os.path.join(CLEAN_DATASET_PATH, cls)
        os.makedirs(target_cls_path, exist_ok=True)
        
        images = [f for f in os.listdir(cls_path) if f.lower().endswith(('.png', '.jpg', '.jpeg'))]
        total_original += len(images)
        
        # Shuffle to pick random 1000 if over max
        random.shuffle(images)
        
        count = 0
        for img_name in images:
            if count >= MAX_IMAGES_PER_CLASS:
                break
                
            img_path = os.path.join(cls_path, img_name)
            
            # 1. Duplicate Check
            h = get_image_hash(img_path)
            if h in all_hashes:
                continue
            
            # 2. Quality Check (Size)
            try:
                with Image.open(img_path) as img:
                    if img.size[0] < MIN_IMAGE_SIZE[0] or img.size[1] < MIN_IMAGE_SIZE[1]:
                        continue
            except:
                continue
            
            # Valid image, copy to clean dataset
            shutil.copy2(img_path, os.path.join(target_cls_path, img_name))
            all_hashes.add(h)
            count += 1
            total_cleaned += 1
            
        print(f"Class {cls}: {len(images)} -> {count}")

    print("\n--- Summary ---")
    print(f"Original images: {total_original}")
    print(f"Cleaned images:  {total_cleaned}")
    print(f"Reduction:       {((total_original - total_cleaned)/total_original)*100:.2f}%")
    print(f"Cleaned dataset saved to: {CLEAN_DATASET_PATH}")

if __name__ == "__main__":
    clean_dataset()
