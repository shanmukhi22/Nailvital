from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime

class UserBase(BaseModel):
    name: str
    email: EmailStr
    phone: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    height: Optional[str] = None

class UserCreate(UserBase):
    password: str

class UserUpdate(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    height: Optional[str] = None
    password: Optional[str] = None

class DeleteAccountRequest(BaseModel):
    password: str

class ForgotPasswordRequest(BaseModel):
    email: str

class ResetPasswordRequest(BaseModel):
    email: str
    otp: str
    new_password: str

class VerifyOtpRequest(BaseModel):
    email: str
    otp: str

class ResendOtpRequest(BaseModel):
    email: str

class UserResponse(UserBase):
    id: int
    is_verified: bool = False
    created_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class Finding(BaseModel):
    result_class: str
    display_name: Optional[str] = None
    description: Optional[str] = None
    recommendation: Optional[str] = None
    confidence: float

class ScanResponse(BaseModel):
    id: int
    image_path: str
    finger: Optional[str] = None
    result_class: str
    display_name: Optional[str] = None
    description: Optional[str] = None
    recommendation: Optional[str] = None
    confidence: float
    findings: List[Finding] = []
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str
    user: UserResponse

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str

class VoiceCommandRequest(BaseModel):
    message: str

class VoiceCommandResponse(BaseModel):
    action_type: str
    message: Optional[str] = None
    target: Optional[str] = None
    commands: Optional[List[dict]] = None
