// ─── API SERVICE LAYER ───

/**
 * Thrown when the backend AI gate rejects an image as "not a nail".
 * Distinct from generic network/server errors so callers can show the
 * correct "Nail Not Detected" UI without falling back to a simulation.
 */
class NailNotDetectedError extends Error {
  constructor(detail = 'NOT_A_NAIL', category = 'UNKNOWN') {
    super(detail);
    this.name = 'NailNotDetectedError';
    this.category = category; // e.g. NAIL_FULLY_COVERED_BY_POLISH_OR_ART
  }
}

const ApiService = {
  getToken() {
    return localStorage.getItem(CONFIG.TOKEN_KEY);
  },

  setToken(token) {
    localStorage.setItem(CONFIG.TOKEN_KEY, token);
  },

  clearToken() {
    localStorage.removeItem(CONFIG.TOKEN_KEY);
    localStorage.removeItem(CONFIG.USER_KEY);
  },

  getHeaders(isMultipart = false) {
    const headers = {};
    if (!isMultipart) {
      headers['Content-Type'] = 'application/json';
    }
    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  },

  async request(endpoint, options = {}, timeoutMs = 10000) {
    const url = `${CONFIG.API_BASE_URL}${endpoint}`;
    const isMultipart = options.body instanceof FormData;
    const headers = { ...this.getHeaders(isMultipart), ...(options.headers || {}) };

    // Abort after timeoutMs so offline/unreachable backend fails fast
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, { ...options, headers, signal: controller.signal });
      clearTimeout(timer);
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ detail: 'Network request failed' }));
        const detail = errorData.detail || `Error ${response.status}`;

        // ── Hard Rejection Gate: image not a valid nail ──
        // Backend sends: "NOT_A_NAIL:<REJECTION_CATEGORY>"
        if (typeof detail === 'string' && detail.includes('NOT_A_NAIL')) {
          const category = detail.replace('NOT_A_NAIL:', '').replace('NOT_A_NAIL', 'UNKNOWN').trim();
          throw new NailNotDetectedError(detail, category);
        }

        let errorMsg = detail;
        if (Array.isArray(detail)) {
          errorMsg = detail.map(e => e.msg).join(', ');
        } else if (typeof detail === 'object') {
          errorMsg = JSON.stringify(detail);
        }
        
        throw new Error(errorMsg);
      }
      return await response.json();
    } catch (err) {
      clearTimeout(timer);
      if (err.name === 'AbortError') {
        throw new Error('Request timed out – backend unreachable. Using AI simulation.');
      }
      // Re-throw NailNotDetectedError as-is so Scanner/doScan can handle it specially
      if (err instanceof NailNotDetectedError) throw err;
      console.warn(`[API] Server unavailable or request failed on ${endpoint}:`, err.message);
      throw err;
    }
  },

  // Check Backend Health
  async checkBackendStatus() {
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/openapi.json`, { method: 'HEAD', mode: 'no-cors' });
      return true;
    } catch {
      return false;
    }
  },

  // Auth Endpoints
  async login(email, password) {
    const formData = new URLSearchParams();
    formData.append('username', email);
    formData.append('password', password);

    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: 'Invalid credentials' }));
        throw new Error(err.detail || 'Login failed');
      }
      return await res.json();
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.includes('fetch'))) {
        throw new Error('Unable to connect to backend server. Please make sure the Python backend is running on http://10.14.242.73:8000.');
      }
      throw err;
    }
  },

  register(userData) {
    return this.request('/register', {
      method: 'POST',
      body: JSON.stringify(userData)
    });
  },

  async verifyOtp(email, otp) {
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/verify-otp?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}`, {
        method: 'POST'
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: 'Invalid OTP' }));
        throw new Error(err.detail || 'OTP verification failed');
      }
      return await res.json();
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.includes('fetch'))) {
        throw new Error('Unable to connect to backend server. Please make sure the Python backend is running on http://10.14.242.73:8000.');
      }
      throw err;
    }
  },

  async resendOtp(email) {
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/resend-otp?email=${encodeURIComponent(email)}`, {
        method: 'POST'
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: 'Failed to resend OTP email' }));
        throw new Error(err.detail || 'Failed to resend verification email');
      }
      return await res.json();
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.includes('fetch'))) {
        throw new Error('Unable to connect to backend server. Please make sure the Python backend is running on http://10.14.242.73:8000.');
      }
      throw err;
    }
  },

  async forgotPassword(email) {
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/forgot-password?email=${encodeURIComponent(email)}`, {
        method: 'POST'
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: 'User not found or request failed' }));
        throw new Error(err.detail || 'Password reset request failed');
      }
      return await res.json();
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.includes('fetch'))) {
        throw new Error('Unable to connect to backend server. Please make sure the Python backend is running on http://10.14.242.73:8000.');
      }
      throw err;
    }
  },

  async resetPassword(email, otp, newPassword) {
    try {
      const res = await fetch(`${CONFIG.API_BASE_URL}/reset-password?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}&new_password=${encodeURIComponent(newPassword)}`, {
        method: 'POST'
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: 'Invalid OTP code' }));
        throw new Error(err.detail || 'Password reset failed');
      }
      return await res.json();
    } catch (err) {
      if (err.name === 'TypeError' || (err.message && err.message.includes('fetch'))) {
        throw new Error('Unable to connect to backend server. Please make sure the Python backend is running on http://10.14.242.73:8000.');
      }
      throw err;
    }
  },

  // User Profile
  getUserProfile() {
    return this.request('/users/me');
  },

  updateUserProfile(data) {
    return this.request('/users/me', {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  deleteAccount(password) {
    return this.request('/users/me', {
      method: 'DELETE',
      body: JSON.stringify({ password })
    });
  },

  exportDataJson() {
    return this.request('/users/me/export-data');
  },

  // Scan AI Endpoint
  scanNail(fileBlob, finger = "Unknown") {
    const formData = new FormData();
    formData.append('file', fileBlob, 'scan.jpg');
    formData.append('finger', finger);

    return this.request(`/scan?finger=${encodeURIComponent(finger)}`, {
      method: 'POST',
      body: formData
    });
  },

  // History Endpoints
  getScanHistory(limit) {
    const url = limit ? `/history?limit=${limit}` : '/history';
    return this.request(url);
  },

  deleteScan(scanId) {
    return this.request(`/scans/${scanId}`, {
      method: 'DELETE'
    });
  },

  getSinglePdfUrl(scanId) {
    return `${CONFIG.API_BASE_URL}/scans/${scanId}/export-pdf`;
  },

  getHistoryPdfUrl() {
    return `${CONFIG.API_BASE_URL}/history/export-pdf`;
  },

  // Chat Endpoint
  sendChatMessage(message) {
    return this.request('/chat', {
      method: 'POST',
      body: JSON.stringify({ message })
    });
  }
};
