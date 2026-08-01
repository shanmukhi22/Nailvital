import tensorflow as tf
import os
import glob
from tensorflow.keras.utils import load_img, img_to_array, save_img

# Configuration
SOURCE_DIR = 'dataset/unified_dataset'
TARGET_AUGMENT_COUNT = 5  # Create 5 new variations for every 1 original image
IMG_SIZE = (224, 224)

def augment_image(image):
    """Applies random transformations using tf.image (no scipy required)."""
    image = tf.image.random_flip_left_right(image)
    image = tf.image.random_flip_up_down(image)
    image = tf.image.random_brightness(image, max_delta=0.2)
    image = tf.image.random_contrast(image, lower=0.8, upper=1.2)
    image = tf.image.random_hue(image, max_delta=0.02)
    image = tf.image.random_saturation(image, lower=0.8, upper=1.2)
    return image

def expand_dataset():
    print(f"🚀 Starting dataset expansion in {SOURCE_DIR}...")
    
    if not os.path.exists(SOURCE_DIR):
        print(f"Error: Directory {SOURCE_DIR} not found.")
        return

    classes = [d for d in os.listdir(SOURCE_DIR) if os.path.isdir(os.path.join(SOURCE_DIR, d))]
    
    total_added = 0
    for cls in classes:
        print(f"Processing class: {cls}...")
        cls_path = os.path.join(SOURCE_DIR, cls)
        
        # Find all images
        images = []
        for ext in ['*.jpg', '*.jpeg', '*.png']:
            images.extend(glob.glob(os.path.join(cls_path, ext)))
        
        if not images:
            print(f"  No images found in {cls}. Skipping.")
            continue

        count = 0
        for img_path in images:
            # Load and convert image
            try:
                img = load_img(img_path, target_size=IMG_SIZE)
                img_array = img_to_array(img)
                
                # Generate augmented images
                for i in range(TARGET_AUGMENT_COUNT):
                    # Apply augmentation
                    augmented_tensor = augment_image(tf.convert_to_tensor(img_array))
                    
                    # Generate unique filename
                    base_name = os.path.basename(img_path).split('.')[0]
                    new_filename = f"{base_name}_aug_{i}.jpg"
                    new_path = os.path.join(cls_path, new_filename)
                    
                    # Save the image
                    save_img(new_path, augmented_tensor.numpy())
                    count += 1
            except Exception as e:
                print(f"  Error processing {img_path}: {e}")

        total_added += count
        print(f"  Done! Added {count} augmented images to {cls}.")

    print(f"\n✨ Dataset expansion complete! Total images added: {total_added}")

if __name__ == "__main__":
    expand_dataset()
