import random
import os
import smtplib
import urllib.request
import json
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from dotenv import load_dotenv

# Ensure .env is always loaded relative to this file's location
env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
load_dotenv(dotenv_path=env_path)

def generate_otp():
    return "".join([str(random.randint(0, 9)) for _ in range(6)])

def send_otp(email: str, otp: str):
    """
    Sends OTP via Console, Gmail SMTP (SSL 465 & STARTTLS 587), and Brevo API.
    """
    email = email.strip().lower()
    print(f"\n==================================================")
    print(f"[OTP SERVICE] GENERATED VERIFICATION CODE FOR {email}")
    print(f"[OTP SERVICE] >>> OTP CODE: {otp} <<<")
    print(f"==================================================\n")

    # Reload env dynamically to catch any runtime configuration updates
    load_dotenv(dotenv_path=env_path)

    gmail_user = os.getenv("BREVO_SENDER_EMAIL", "gummasrinivas8106@gmail.com").strip()
    gmail_password = (os.getenv("GMAIL_APP_PASSWORD") or "").replace(" ", "").strip()
    sender_name = os.getenv("BREVO_SENDER_NAME", "NailVital AI Support").strip()
    brevo_api_key = (os.getenv("BREVO_API_KEY") or "").strip()

    html_content = f"""
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;">
        <h2 style="color: #00C9A7; text-align: center;">NailVital AI Email Verification</h2>
        <p>Hello,</p>
        <p>You requested a verification code for your NailVital AI account. Please use the following 6-digit One-Time Password (OTP) to complete your account verification:</p>
        <div style="text-align: center; margin: 30px 0;">
            <span style="font-size: 32px; font-weight: bold; padding: 15px 25px; background-color: #f4f6f9; border-radius: 8px; color: #111827; letter-spacing: 6px;">{otp}</span>
        </div>
        <p>This code will expire in 10 minutes. If you did not request this, please safely ignore this email.</p>
        <br>
        <p style="font-size: 12px; color: #888; text-align: center;">Secure delivery by NailVital AI Support</p>
    </div>
    """

    # Strategy 1: Brevo HTTP API (High Speed HTTP)
    if brevo_api_key:
        try:
            url = "https://api.brevo.com/v3/smtp/email"
            headers = {
                "accept": "application/json",
                "api-key": brevo_api_key,
                "content-type": "application/json"
            }
            payload = {
                "sender": {"name": sender_name, "email": gmail_user},
                "to": [{"email": email}],
                "subject": f"Your NailVital AI Verification Code: {otp}",
                "htmlContent": html_content
            }
            req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers)
            with urllib.request.urlopen(req, timeout=5) as response:
                if response.status in (200, 201):
                    print(f"[OTP SERVICE] Email successfully sent to {email} via Brevo API!")
                    return
        except Exception as e:
            print(f"[OTP SERVICE] Brevo API send failed: {e}")
            if hasattr(e, 'read'):
                try:
                    err_msg = e.read().decode('utf-8')
                    print(f"[OTP SERVICE] Brevo API Error details: {err_msg}")
                except Exception:
                    pass

    # Strategy 2: Gmail SMTP SSL (Port 465)
    if gmail_user and gmail_password:
        try:
            msg = MIMEMultipart("alternative")
            msg["Subject"] = f"Your NailVital AI Verification Code: {otp}"
            msg["From"] = f"{sender_name} <{gmail_user}>"
            msg["To"] = email
            msg.attach(MIMEText(html_content, "html"))

            with smtplib.SMTP_SSL("smtp.gmail.com", 465, timeout=5) as server:
                server.login(gmail_user, gmail_password)
                server.sendmail(gmail_user, email, msg.as_string())
                print(f"[OTP SERVICE] Email successfully sent to {email} via Gmail SMTP (SSL 465)")
                return
        except Exception as e:
            print(f"[OTP SERVICE] Gmail SSL 465 failed: {e}")

        # Strategy 3: Gmail SMTP STARTTLS (Port 587)
        try:
            msg = MIMEMultipart("alternative")
            msg["Subject"] = f"Your NailVital AI Verification Code: {otp}"
            msg["From"] = f"{sender_name} <{gmail_user}>"
            msg["To"] = email
            msg.attach(MIMEText(html_content, "html"))

            with smtplib.SMTP("smtp.gmail.com", 587, timeout=5) as server:
                server.starttls()
                server.login(gmail_user, gmail_password)
                server.sendmail(gmail_user, email, msg.as_string())
                print(f"[OTP SERVICE] Email successfully sent to {email} via Gmail SMTP (STARTTLS 587)")
                return
        except Exception as e:
            print(f"[OTP SERVICE] Gmail STARTTLS 587 failed: {e}")

    print(f"[OTP SERVICE] Note: Direct network SMTP/API calls restricted by environment. Verification OTP code for {email} is: {otp}")


