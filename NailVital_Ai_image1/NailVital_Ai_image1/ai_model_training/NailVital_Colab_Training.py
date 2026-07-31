# ===========================================================
# NAILVITAL AI - GOOGLE COLAB TRAINING SCRIPT
# Run each cell one by one in Google Colab
# ===========================================================

# -------------------------------------------------------
# CELL 1: Check GPU
# -------------------------------------------------------
import tensorflow as tf
print("TF Version:", tf.__version__)
print("GPU:", tf.config.list_physical_devices('GPU'))

# -------------------------------------------------------
# CELL 2: Mount Google Drive (where you upload the zip)
# -------------------------------------------------------
from google.colab import drive
drive.mount('/content/drive')

# -------------------------------------------------------
# CELL 3: Upload and Extract Dataset (with path correction)
# Upload nail_dataset_fixed.zip to your Google Drive root first,
# then run this cell.
# -------------------------------------------------------
import zipfile, os, shutil

# ⬇️ CHANGE THIS PATH if your zip is in a subfolder in Google Drive
zip_path = '/content/drive/MyDrive/nail_dataset_fixed.zip'

print(f"Using zip: {zip_path}")
extract_path = '/content/dataset'

# Ensure the extraction path exists and is empty to avoid conflicts
shutil.rmtree(extract_path, ignore_errors=True)
os.makedirs(extract_path, exist_ok=True)

print("Extracting dataset...")
with zipfile.ZipFile(zip_path, 'r') as z:
    z.extractall(extract_path)
print("Done! Dataset extracted to:", extract_path)

print("\nCorrecting dataset structure (handling Windows-style paths in zip)...")
temp_corrected_path = '/content/corrected_dataset_temp'
shutil.rmtree(temp_corrected_path, ignore_errors=True)
os.makedirs(temp_corrected_path, exist_ok=True)

# Collect all extracted items (files and directories)
all_extracted_items = []
for root, dirs, files in os.walk(extract_path):
    for f in files:
        all_extracted_items.append(os.path.join(root, f))
    for d in dirs:
        all_extracted_items.append(os.path.join(root, d))

fixed_files_count = 0
for old_path in all_extracted_items:
    relative_path = os.path.relpath(old_path, extract_path)
    corrected_relative_path = relative_path.replace('\\', '/')
    new_path = os.path.join(temp_corrected_path, corrected_relative_path)

    os.makedirs(os.path.dirname(new_path), exist_ok=True)

    if os.path.isfile(old_path):
        shutil.move(old_path, new_path)
        fixed_files_count += 1
    elif os.path.isdir(old_path) and not os.listdir(old_path):
        shutil.move(old_path, new_path)

print(f"Moved and corrected {fixed_files_count} files.")

shutil.rmtree(extract_path, ignore_errors=True)
shutil.move(temp_corrected_path, extract_path)
print(f"Corrected dataset now in: {extract_path}")

# Dynamically find the DATASET_ROOT within the corrected structure
DATASET_ROOT = None
potential_roots = [extract_path]

subdirs_at_extract_path = [d for d in os.listdir(extract_path) if os.path.isdir(os.path.join(extract_path, d))]
if len(subdirs_at_extract_path) == 1:
    potential_roots.append(os.path.join(extract_path, subdirs_at_extract_path[0]))
elif 'unified_dataset' in subdirs_at_extract_path:
    potential_roots.append(os.path.join(extract_path, 'unified_dataset'))

for candidate_root in potential_roots:
    class_folders = [d for d in os.listdir(candidate_root) if os.path.isdir(os.path.join(candidate_root, d))]
    if len(class_folders) > 5:
        DATASET_ROOT = candidate_root
        break

if DATASET_ROOT is None:
    DATASET_ROOT = extract_path
    print(f"\n⚠️ Could not auto-detect nested root. Using: {DATASET_ROOT}")

classes = [d for d in os.listdir(DATASET_ROOT) if os.path.isdir(os.path.join(DATASET_ROOT, d))]
print(f"\n✅ Detected dataset root: {DATASET_ROOT}")
print(f"Found {len(classes)} classes: {classes}")

# -------------------------------------------------------
# CELL 4: Clean & Balance Dataset (Cap at 1000 per class)
# -------------------------------------------------------
import hashlib, shutil, random, os
from PIL import Image

CLEAN_PATH   = '/content/cleaned_dataset'
MAX_PER_CLASS = 1000
MIN_SIZE = (64, 64)

classes = [d for d in os.listdir(DATASET_ROOT) if os.path.isdir(os.path.join(DATASET_ROOT, d)) and d != os.path.basename(CLEAN_PATH)]
print(f"Found {len(classes)} classes: {classes}")

shutil.rmtree(CLEAN_PATH, ignore_errors=True)
os.makedirs(CLEAN_PATH, exist_ok=True)

all_hashes = set()
total_orig = total_clean = 0

for cls in classes:
    src = os.path.join(DATASET_ROOT, cls)
    dst = os.path.join(CLEAN_PATH, cls)
    os.makedirs(dst, exist_ok=True)

    imgs = [f for f in os.listdir(src) if f.lower().endswith(('.jpg','.jpeg','.png'))]
    random.shuffle(imgs)
    total_orig += len(imgs)

    count = 0
    for img in imgs:
        if count >= MAX_PER_CLASS: break
        p = os.path.join(src, img)
        h = hashlib.md5(open(p,'rb').read()).hexdigest()
        if h in all_hashes: continue
        try:
            with Image.open(p) as im:
                if im.size[0] < MIN_SIZE[0] or im.size[1] < MIN_SIZE[1]: continue
        except: continue
        shutil.copy2(p, os.path.join(dst, img))
        all_hashes.add(h)
        count += 1
        total_clean += 1

    print(f"  {cls}: {len(imgs)} → {count}")

if total_orig > 0:
    print(f"\nTotal: {total_orig} → {total_clean} ({(total_orig-total_clean)/total_orig*100:.1f}% reduction)")
print("✅ Cleaned dataset ready!")

# -------------------------------------------------------
# CELL 5: Train the Model
# -------------------------------------------------------
import tensorflow as tf
from tensorflow.keras import layers, models

CLEAN_PATH     = '/content/cleaned_dataset'
IMG_SIZE       = (224, 224)
BATCH_SIZE     = 64
INITIAL_EPOCHS = 15
FINE_TUNE_EPOCHS = 35

def get_callbacks(ckpt):
    return [
        tf.keras.callbacks.EarlyStopping(patience=8, restore_best_weights=True, monitor='val_accuracy'),
        tf.keras.callbacks.ReduceLROnPlateau(monitor='val_loss', patience=3, factor=0.2, min_lr=1e-7),
        tf.keras.callbacks.ModelCheckpoint(ckpt, save_best_only=True, monitor='val_accuracy'),
    ]

# Load Dataset
full_ds = tf.keras.utils.image_dataset_from_directory(
    CLEAN_PATH, seed=42, image_size=IMG_SIZE, batch_size=BATCH_SIZE, label_mode='categorical')

class_names = full_ds.class_names
num_classes = len(class_names)
print(f"{num_classes} classes found.")

ds_size = tf.data.experimental.cardinality(full_ds).numpy()
train_ds = full_ds.take(int(0.8 * ds_size))
val_ds   = full_ds.skip(int(0.8 * ds_size))

# Augmentation
augment_layer = models.Sequential([
    layers.RandomFlip("horizontal"),
    layers.RandomRotation(0.2),
    layers.RandomZoom(0.2),
])
AUTOTUNE = tf.data.AUTOTUNE
train_ds = train_ds.map(lambda x,y: (augment_layer(x, training=True), y), num_parallel_calls=AUTOTUNE).prefetch(AUTOTUNE)
val_ds   = val_ds.prefetch(AUTOTUNE)

# Build Model
base_model = tf.keras.applications.EfficientNetV2B0(
    input_shape=(224, 224, 3), include_top=False, weights='imagenet')
base_model.trainable = False

inputs  = tf.keras.Input(shape=(224, 224, 3))
x       = base_model(inputs, training=False)
x       = layers.GlobalAveragePooling2D()(x)
x       = layers.Dropout(0.3)(x)
outputs = layers.Dense(num_classes, activation='softmax')(x)
model   = tf.keras.Model(inputs, outputs)

# Phase 1: Warmup
print("\n[PHASE 1] Warmup...")
model.compile(optimizer=tf.keras.optimizers.Adam(0.001), loss='categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, validation_data=val_ds, epochs=INITIAL_EPOCHS, callbacks=get_callbacks('/content/best_warmup.keras'))

# Phase 2: Fine-tune
print("\n[PHASE 2] Fine-tuning...")
base_model.trainable = True
model.compile(optimizer=tf.keras.optimizers.Adam(0.0001), loss='categorical_crossentropy', metrics=['accuracy'])
model.fit(train_ds, validation_data=val_ds,
          epochs=INITIAL_EPOCHS + FINE_TUNE_EPOCHS,
          initial_epoch=INITIAL_EPOCHS,
          callbacks=get_callbacks('/content/best_model.keras'))

# Final eval
model.load_weights('/content/best_model.keras')
loss, acc = model.evaluate(val_ds)
print(f"\n✅ Final Validation Accuracy: {acc*100:.2f}%")

# -------------------------------------------------------
# CELL 6: Convert to TFLite and Download
# -------------------------------------------------------
from google.colab import files

with open('/content/class_names.txt', 'w') as f:
    for c in class_names:
        f.write(c + '\n')

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

with open('/content/nail_model_quantized.tflite', 'wb') as f:
    f.write(tflite_model)

print("Downloading model files...")
files.download('/content/nail_model_quantized.tflite')
files.download('/content/class_names.txt')
print("✨ Done! Copy these files to nailvital-ai-android/app/src/main/assets/models/")
