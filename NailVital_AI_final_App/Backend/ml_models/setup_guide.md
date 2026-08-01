# 💻 Fresh Laptop Setup Guide: NailVital AI

Follow these steps to set up and run the complete NailVital AI project (Android + Backend) on a new Windows machine.

## 1. Required Tooling (Install These First)

### 🔹 General Tools
1. **Git**: [Download Git](https://git-scm.com/download/win)
2. **Python 3.12** (⚠️ **STRONGLY RECOMMENDED**): [Download Python 3.12](https://www.python.org/downloads/windows/)
   - **IMPORTANT**: Avoid Python 3.13 for now as some AI dependencies (Scipy, TensorFlow) may encounter installation issues on Windows with the latest releases.
   - Check "Add Python to PATH" during installation.
3. **Android Studio**: [Download Android Studio Iguana+](https://developer.android.com/studio)
4. **Java JDK 17**: Usually bundled with Android Studio, but you can download it from [Oracle](https://www.oracle.com/java/technologies/downloads/) if needed.

---

## 2. Infrastructure Setup (Command Line)

Open **PowerShell** or **Command Prompt** and run these commands:

### 🔹 Backend Environment
```bash
# Navigate to the backend folder
cd c:\projects\NailVital_AI\nailvital-ai-backend

# Create a virtual environment
python -m venv venv

# Activate the virtual environment
.\venv\Scripts\activate

# Install all required Python packages
pip install -r requirements.txt
```

### 🔹 AI Training Environment (Optional)
If you want to re-train the model using your dataset:
```bash
# Ensure you are in the root directory
cd c:\projects\NailVital_AI

# (Optional) Expand dataset if you have few images
python augment_dataset.py

# Install AI dependencies
pip install tensorflow tensorflow-intel pillow scipy numpy
```

---

## 3. Running the Application

### 🚀 Step 1: Start the Backend
Keep your PowerShell window open with the virtual environment activated:
```bash
cd c:\projects\NailVital_AI\nailvital-ai-backend
python -m uvicorn app.main:app --reload --host 0.0.0.0
```
> [!NOTE]
> The `--host 0.0.0.0` allows your Android phone to connect to your laptop's IP address.

### 🚀 Step 2: Train the AI Model (If not already done)
```bash
cd c:\projects\NailVital_AI
python train_model.py
```
After training, copy `nail_model_quantized.tflite` to:
`nailvital-ai-android\app\src\main\assets\models\`

### 🚀 Step 3: Test with an Image (Optional)
If you want to test the model without the Android app:
```bash
python predict.py
```
Enter the path to any nail image when prompted.

### 🚀 Step 4: Run the Android App
1. Open **Android Studio**.
2. Select **"Open"** and choose `c:\projects\NailVital_AI\nailvital-ai-android`.
3. Wait for the **Gradle Sync** to finish (this may take 5-10 minutes on a fresh install).
4. Connect a physical Android phone via USB (highly recommended for Camera & AI features).
5. Click the **"Run" (Green Play button)** in Android Studio.

---

## 🛠 Troubleshooting
- **Gradle Error**: Ensure your **JDK Location** in Android Studio (Settings > Build, Execution, Deployment > Build Tools > Gradle) is set to JDK 17.
- **Connection Error**: If the app can't talk to the backend, ensure both laptop and phone are on the same Wi-Fi. Update the `BASE_URL` in the Android code to your laptop's Local IP (e.g., `http://192.168.1.5:8000`).
- **Camera Error**: Ensure you grant Camera permissions when prompted.

---
Medical Disclaimer: This app is a tracking and educational tool. It is NOT a medical device. Always consult a professional for health concerns.
