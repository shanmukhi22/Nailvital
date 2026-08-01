# NailVital AI - Complete Setup Guide

Welcome to the **NailVital AI** project! This repository contains a full-stack AI application designed to detect and diagnose 22 different nail conditions. 

The project is split into four main parts:
- **`App`**: The Android Kotlin application.
- **`Website`**: The HTML/CSS web dashboard.
- **`Backend`**: The Python/FastAPI server that runs the AI models.
- **`Model`**: The dataset used to train the computer vision model.

Follow this guide step-by-step to get the entire ecosystem running on your local machine with 100% perfection.

---

## 🛠️ Prerequisites

Before you start, make sure you have the following installed on your computer:
1. **[Python 3.10 or newer](https://www.python.org/downloads/)** (Make sure to check "Add Python to PATH" during installation).
2. **[XAMPP](https://www.apachefriends.org/index.html)** (Required for the MySQL database).
3. **[Android Studio](https://developer.android.com/studio)** (Required to build and run the Android app).

---

## Step 1: Database Setup (MySQL)

The backend relies on a local MySQL database to store users and history.

1. Open the **XAMPP Control Panel**.
2. Click the **Start** button next to **Apache**.
3. Click the **Start** button next to **MySQL**. Both should turn green.
4. *Do not worry about creating tables manually — we have an automated script for that in the next step!*

---

## Step 2: Backend Setup (Python & AI)

The backend powers the AI inference and the API. Because Python virtual environments use hardcoded absolute paths, **you must create a fresh virtual environment on your computer**.

1. Open a **Command Prompt** (Windows) or **Terminal** (Mac/Linux).
2. Navigate into the `Backend` folder:
   ```cmd
   cd path\to\NailVital_Ai_image1\Backend
   ```
3. Delete the old virtual environment (if it exists) and create a fresh one:
   ```cmd
   rmdir /S /Q venv
   python -m venv venv
   ```
4. Activate the virtual environment:
   ```cmd
   venv\Scripts\activate
   ```
5. Install the required Python packages (this might take a few minutes as it downloads PyTorch and TensorFlow):
   ```cmd
   pip install -r requirements.txt
   ```
6. **Set up the Database Tables**: Run the automated setup script to configure your XAMPP MySQL database.
   ```cmd
   python setup_local_db.py
   ```
7. **API Keys Configuration**: Open the `.env` file located in the `Backend` folder. Ensure the `GEMINI_API_KEY` and `HF_TOKEN` are already filled out. (They should be included by default, so you likely don't need to change anything!)
8. **Start the Backend Server**:
   ```cmd
   uvicorn main:app --reload
   ```
   *Success! The backend is now running at `http://127.0.0.1:8000`. Leave this command prompt open.*

---

## Step 3: Website Setup (Dashboard)

1. Open a **new, separate Command Prompt** or Terminal.
2. Navigate to the `Website` folder:
   ```cmd
   cd path/to/NailVital_Ai_image1/Website
   ```
3. Start the local python web server:
   ```cmd
   python server.py
   ```
4. Open your web browser and go to `http://localhost:3000`. You should now see the web dashboard!

---

## Step 4: Android App Setup

1. Open **Android Studio**.
2. Click **Open** and select the `App` folder inside the project.
3. Wait for Gradle to finish syncing the project (watch the loading bar at the bottom right).
4. **CRITICAL STEP - Network Configuration**:
   The app needs to know where your backend is running. You must update the hardcoded IP address.
   - In Android Studio, navigate to `App > app > src > main > java > com > nailvital > app > api > ApiClient.kt`.
   - Scroll down to around line 195 and look for:
     ```kotlin
     private const val DEV_URL = "http://10.14.242.73:8000/"
     ```
   - **If you are using the Android Studio Emulator:** Change the IP to `10.0.2.2` (this is the special loopback IP for the emulator).
     ```kotlin
     private const val DEV_URL = "http://10.0.2.2:8000/"
     ```
   - **If you are plugging in a physical Android phone:** You must connect your phone and computer to the same Wi-Fi network. Find your computer's local IPv4 address (e.g., `192.168.1.X`) and update the URL:
     ```kotlin
     private const val DEV_URL = "http://192.168.1.X:8000/"
     ```
5. Click the green **Play** button at the top of Android Studio to launch the app!

---

## ❓ Frequently Asked Questions

**What do I do with the `Model` folder?**
Absolutely nothing! The `Model` folder simply contains the 10,000 images we used to originally train the computer vision model. You do not need to interact with it to run the app. The backend will automatically download the pre-trained, finished AI weights directly from Hugging Face when it starts up.
