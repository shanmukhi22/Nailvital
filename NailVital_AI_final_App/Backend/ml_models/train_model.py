import tensorflow as tf
from tensorflow.keras import layers, models
import numpy as np
import os
import argparse

# Dataset paths
DATASET_PATH = 'dataset/cleaned_dataset'
TFLITE_MODEL_PATH = 'nail_model_quantized.tflite'

# Hyperparameters
IMG_SIZE = (224, 224)
BATCH_SIZE = 32  # Larger batch for speed, V2B0 is small enough
INITIAL_EPOCHS = 15
FINE_TUNE_EPOCHS = 35

def get_callbacks(checkpoint_path, patience_early=8, patience_lr=3):
    """Returns a list of standard callbacks for training."""
    log_file = checkpoint_path.replace('.keras', '_log.csv')
    return [
        tf.keras.callbacks.CSVLogger(log_file, append=True),
        tf.keras.callbacks.EarlyStopping(
            patience=patience_early,
            restore_best_weights=True,
            monitor='val_accuracy',
            verbose=1
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            patience=patience_lr,
            factor=0.2,
            min_lr=1e-7,
            verbose=1
        ),
        tf.keras.callbacks.ModelCheckpoint(
            checkpoint_path,
            save_best_only=True,
            monitor='val_accuracy',
            verbose=1
        )
    ]

def build_model(num_classes, base_trainable=False):
    """Builds the EfficientNetV2B0 model architecture (Faster than V2S)."""
    base_model = tf.keras.applications.EfficientNetV2B0(
        input_shape=(224, 224, 3),
        include_top=False,
        weights='imagenet'
    )
    base_model.trainable = base_trainable

    inputs = tf.keras.Input(shape=(224, 224, 3))
    x = base_model(inputs, training=base_trainable)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)

    return tf.keras.Model(inputs, outputs), base_model

def train():
    print("🚀 Starting Optimized High-Efficiency Training...")

    if not os.path.exists(DATASET_PATH):
        print(f"❌ Error: {DATASET_PATH} not found. Run clean_dataset.py first!")
        return

    # Load datasets (80% Train, 20% Val/Test split)
    full_ds = tf.keras.utils.image_dataset_from_directory(
        DATASET_PATH,
        seed=42,
        image_size=IMG_SIZE,
        batch_size=BATCH_SIZE,
        label_mode='categorical'
    )

    class_names = full_ds.class_names
    num_classes = len(class_names)
    print(f"Detected {num_classes} classes.")

    # Split dataset
    ds_size = tf.data.experimental.cardinality(full_ds).numpy()
    train_size = int(0.8 * ds_size)
    val_ds = full_ds.skip(train_size)
    train_ds_raw = full_ds.take(train_size)

    # Simple augmentation pipeline
    data_augmentation = models.Sequential([
        layers.RandomFlip("horizontal"),
        layers.RandomRotation(0.2),
        layers.RandomZoom(0.2),
    ], name="augmentation")

    AUTOTUNE = tf.data.AUTOTUNE
    def augment(images, labels):
        return data_augmentation(images, training=True), labels

    train_ds = train_ds_raw.map(augment, num_parallel_calls=AUTOTUNE)
    train_ds = train_ds.prefetch(buffer_size=AUTOTUNE)
    val_ds = val_ds.prefetch(buffer_size=AUTOTUNE)

    # PHASE 1: Train top layers
    print("\n[PHASE 1] Training top layers (Warmup)...")
    model, base_model = build_model(num_classes, base_trainable=False)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=INITIAL_EPOCHS,
        callbacks=get_callbacks('best_warmup.keras')
    )

    # PHASE 2: Fine-tune entire model
    print("\n[PHASE 2] Full model fine-tuning...")
    base_model.trainable = True
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.0001),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )

    model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=INITIAL_EPOCHS + FINE_TUNE_EPOCHS,
        initial_epoch=INITIAL_EPOCHS,
        callbacks=get_callbacks('best_model.keras')
    )

    # Evaluate
    model.load_weights('best_model.keras')
    print("\n🏁 Final Evaluation...")
    loss, acc = model.evaluate(val_ds)
    print(f"Validation Accuracy: {acc*100:.2f}%")

    # Save and Convert
    print("\n✅ Saving results...")
    model.save('nail_model_final.keras')

    print("Converting to TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    with open(TFLITE_MODEL_PATH, 'wb') as f:
        f.write(tflite_model)

    print(f"\n✨ DONE! Model saved to {TFLITE_MODEL_PATH}")

    with open('class_names.txt', 'w') as f:
        for name in class_names:
            f.write(f"{name}\n")

if __name__ == "__main__":
    train()

if __name__ == "__main__":
    train()
