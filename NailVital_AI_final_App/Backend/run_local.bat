@echo off
title NailVital AI Backend
color 0A
echo.
echo  ============================================
echo   NailVital AI Backend - Local Server
echo  ============================================
echo.

:: Activate virtual environment if it exists
if exist venv\Scripts\activate.bat (
    call venv\Scripts\activate.bat
    echo [OK] Virtual environment activated
) else (
    echo [WARN] No venv found, using system Python
)

:: Start the server
echo.
echo [STARTING] Backend is starting on http://10.14.242.73:8000
echo [DOCS]     Open http://10.14.242.73:8000/docs to test the API
echo [HEALTH]   Open http://10.14.242.73:8000/health to verify it's running
echo.
echo  Press Ctrl+C to stop the server
echo  ============================================
echo.

venv\Scripts\python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
pause
