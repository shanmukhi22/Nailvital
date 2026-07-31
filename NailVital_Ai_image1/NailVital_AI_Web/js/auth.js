// ─── AUTHENTICATION CONTROLLER ───

const Auth = {
  currentUser: null,
  pendingRegisterData: null,
  otpTimer: null,

  init() {
    const savedUser = localStorage.getItem(CONFIG.USER_KEY);
    const token = ApiService.getToken();
    if (savedUser && token) {
      try {
        this.currentUser = JSON.parse(savedUser);
      } catch (e) {
        this.currentUser = null;
        ApiService.clearToken();
      }
    } else {
      this.currentUser = null;
      ApiService.clearToken();
    }
  },

  isLoggedIn() {
    const token = ApiService.getToken();
    return !!token && !!this.currentUser;
  },

  async login(email, password) {
    if (!email || !email.includes('@')) {
      throw new Error('Please enter a valid email address');
    }
    if (!password || password.length < 6) {
      throw new Error('Password must be at least 6 characters');
    }

    try {
      const res = await ApiService.login(email, password);
      ApiService.setToken(res.access_token);
      this.currentUser = res.user || { name: email.split('@')[0], email };
      localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(this.currentUser));
      return this.currentUser;
    } catch (err) {
      console.error('[Auth] Database Login Error:', err.message);
      throw new Error(err.message || 'Login failed. Please check your email and password.');
    }
  },

  async register(name, email, phone, password) {
    // Full name: letters, spaces and dots only, minimum 2 characters
    if (!name || !name.trim()) throw new Error('Please enter your full name');
    if (!/^[A-Za-z .]{2,}$/.test(name.trim())) {
      if (/[^A-Za-z .]/.test(name.trim())) {
        throw new Error('Full name must not contain numbers or symbols');
      }
      throw new Error('Full name must be at least 2 characters');
    }

    // Email
    if (!email || !email.includes('@')) throw new Error('Please enter a valid email address');

    // Phone: exactly 10 digits, must start with 6, 7, 8, or 9
    const cleanPhone = phone ? phone.replace(/\D/g, '') : '';
    if (!cleanPhone) throw new Error('Please enter your phone number');
    if (!/^[6-9]/.test(cleanPhone)) throw new Error('Phone number must start with 6, 7, 8, or 9');
    if (cleanPhone.length !== 10) throw new Error(`Phone number must be exactly 10 digits (you entered ${cleanPhone.length})`);

    // Password
    if (password.length < 8) throw new Error('Password must be at least 8 characters');

    this.pendingRegisterData = { name, email, phone, password };

    try {
      await ApiService.register({
        name,
        email,
        phone,
        password,
        age: 25,
        gender: "Not Specified",
        height: "Not Specified"
      });
      return true;
    } catch (err) {
      console.error('[Auth] Database Registration Error:', err.message);
      throw new Error(err.message || 'Registration failed. Email may already be in use.');
    }
  },

  async verifyOtp(otpCode) {
    if (otpCode.length !== 6) throw new Error('Please enter the complete 6-digit OTP');

    const email = this.pendingRegisterData ? this.pendingRegisterData.email : (this.currentUser ? this.currentUser.email : '');
    if (!email) {
      throw new Error('Registration email not found. Please try registering or signing in again.');
    }

    try {
      const res = await ApiService.verifyOtp(email, otpCode);
      ApiService.setToken(res.access_token);
      this.currentUser = res.user || { name: email.split('@')[0], email, is_verified: true };
      localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(this.currentUser));
      return this.currentUser;
    } catch (err) {
      console.error('[Auth] OTP Verification Error:', err.message);
      throw new Error(err.message || 'OTP verification failed. Please check your 6-digit code.');
    }
  },

  async resendOtp(email) {
    const targetEmail = email || (this.pendingRegisterData ? this.pendingRegisterData.email : (this.currentUser ? this.currentUser.email : ''));
    if (!targetEmail || !targetEmail.includes('@')) {
      throw new Error('Valid email address required to resend verification code');
    }

    try {
      await ApiService.resendOtp(targetEmail);
      return true;
    } catch (err) {
      console.error('[Auth] Resend OTP Error:', err.message);
      throw new Error(err.message || 'Failed to resend verification code email.');
    }
  },

  async forgotPassword(email) {
    if (!email || !email.includes('@')) {
      throw new Error('Please enter a valid email address');
    }
    try {
      await ApiService.forgotPassword(email);
      return true;
    } catch (err) {
      console.error('[Auth] Forgot Password Error:', err.message);
      throw new Error(err.message || 'Failed to send password reset code. Please verify your email.');
    }
  },

  async resetPassword(email, otp, newPassword) {
    if (!email || !email.includes('@')) throw new Error('Email address required');
    if (!otp || otp.trim().length !== 6) throw new Error('Please enter complete 6-digit OTP code');
    if (!newPassword || newPassword.length < 8) throw new Error('New password must be at least 8 characters');

    try {
      await ApiService.resetPassword(email, otp, newPassword);
      return true;
    } catch (err) {
      console.error('[Auth] Reset Password Error:', err.message);
      throw new Error(err.message || 'Password reset failed. Please verify your 6-digit code.');
    }
  },

  logout() {
    ApiService.clearToken();
    this.currentUser = null;
  },

  calculatePasswordStrength(password) {
    let score = 0;
    if (!password) return { percent: '0%', color: 'transparent', text: 'Enter password' };
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    const levels = [
      { percent: '20%', color: '#FF6B6B', text: 'Weak' },
      { percent: '40%', color: '#FFB547', text: 'Fair' },
      { percent: '75%', color: '#00C9A7', text: 'Good' },
      { percent: '100%', color: '#00C9A7', text: 'Strong ✓' }
    ];
    return levels[score - 1] || levels[0];
  }
};

Auth.init();
