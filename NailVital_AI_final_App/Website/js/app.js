// ─── NAILVITAL AI WEB APP - CORE ROUTER & CONTROLLER ───

let currentSlide = 0;
const slides = [
  { image: 'img/intro_1.png', title: 'Analyze in Seconds', sub: 'Our AI identifies texture, color, and anomalies instantly from any clear photo — no extra hardware required.' },
  { image: 'img/intro_2.png', title: 'Monitor Your Health', sub: 'Track your wellness patterns over time with intuitive visual trends and clinical insights.' },
  { image: 'img/intro_3.png', title: 'Detection is Prevention', sub: 'Screen for 20+ underlying signals early, identifying potential nutrition or systemic markers.' }
];

let otpTimer = null;
let currentScanResult = null;
let userScans = [];

// SVG icon helpers for conditions
const SVG_ICONS = {
  blood: `<svg class="svg-icon" viewBox="0 0 24 24"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>`,
  heart: `<svg class="svg-icon" viewBox="0 0 24 24"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>`,
  dna: `<svg class="svg-icon" viewBox="0 0 24 24"><path d="M2 15c6.667-6 13.333 0 20-6"/><path d="M2 9c6.667 6 13.333 0 20 6"/></svg>`,
  shield: `<svg class="svg-icon" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
  sparkle: `<svg class="svg-icon" viewBox="0 0 24 24"><path d="M12 2v20M2 12h20"/></svg>`,
  alert: `<svg class="svg-icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`
};

// ── 22 ALL NAIL CONDITIONS DICTIONARY MATCHING ANDROID APP & BACKEND ──
const ALL_CONDITIONS = [
  { id: "aloperia_areata", name: "Alopecia Areata", icon: SVG_ICONS.dna, severe: false, desc: "An autoimmune disorder that can cause severe nail pitting (tiny dents), ridging, and brittleness." },
  { id: "beaus_lines", name: "Beau's Lines", icon: SVG_ICONS.dna, severe: false, desc: "Deep grooved lines that run horizontally across fingernails caused by severe stress or illness." },
  { id: "bluish_nail", name: "Bluish Nails (Cyanosis)", icon: SVG_ICONS.heart, severe: true, desc: "A bluish tint to the nail bed indicating lack of oxygen in circulating blood." },
  { id: "clubbing", name: "Nail Clubbing", icon: SVG_ICONS.heart, severe: true, desc: "Nails thicken and curve downward around fingertips, linked to chronic low blood oxygen." },
  { id: "dariers_disease", name: "Darier's Disease", icon: SVG_ICONS.dna, severe: false, desc: "A rare genetic condition causing red and white longitudinal streaks on the nails." },
  { id: "eczema", name: "Nail Eczema", icon: SVG_ICONS.shield, severe: false, desc: "Causes nails to become ridged, pitted, thickened, or discolored secondary to skin eczema." },
  { id: "half_and_half_nails", name: "Half & Half Nails", icon: SVG_ICONS.heart, severe: true, desc: "The bottom half is white, while top half turns red/brown. Associated with kidney conditions." },
  { id: "healthy", name: "Healthy Nails", icon: SVG_ICONS.sparkle, severe: false, desc: "Normal, smooth surface contour and healthy uniform pink nail bed color." },
  { id: "koilonychia", name: "Koilonychia (Spoon Nails)", icon: SVG_ICONS.blood, severe: false, desc: "Concave, scooped-out nails shaped like spoons. Frequently linked to iron deficiency anemia." },
  { id: "leukonychia", name: "Leukonychia (White Spots)", icon: SVG_ICONS.sparkle, severe: false, desc: "White spots or transverse lines caused by minor trauma to the nail matrix." },
  { id: "melanoma", name: "Subungual Melanoma", icon: SVG_ICONS.alert, severe: true, desc: "A dark vertical stripe down the nail. Requires immediate dermatological evaluation." },
  { id: "muehrckes_lines", name: "Muehrcke's Lines", icon: SVG_ICONS.dna, severe: false, desc: "Pairs of transverse white lines across the nail plate linked to low albumin levels." },
  { id: "onychogryphosis", name: "Onychogryphosis (Ram's Horn)", icon: SVG_ICONS.shield, severe: false, desc: "Dramatic overgrowth and claw-like thickening of the nail plate." },
  { id: "onycholycis", name: "Onycholysis", icon: SVG_ICONS.shield, severe: false, desc: "Painless separation of the nail plate from the underlying nail bed." },
  { id: "onychomycosis", name: "Nail Fungus (Onychomycosis)", icon: SVG_ICONS.shield, severe: false, desc: "Fungal infection turning nails yellow, thick, and brittle." },
  { id: "pale_nail", name: "Pale Nails", icon: SVG_ICONS.blood, severe: true, desc: "Very pale or washed out nails can indicate severe anemia, liver disease, or heart failure." },
  { id: "pitting", name: "Nail Pitting", icon: SVG_ICONS.dna, severe: false, desc: "Small depressions or ice pick dents in the nail surface, common in psoriasis." },
  { id: "psoriasis", name: "Nail Psoriasis", icon: SVG_ICONS.dna, severe: false, desc: "Causes pitting, abnormal growth, crumbly texture, and yellowish discoloration." },
  { id: "red_lunula", name: "Red Lunula", icon: SVG_ICONS.heart, severe: true, desc: "The half-moon base area turns red, associated with cardiovascular conditions." },
  { id: "splinter_hemorrhage", name: "Splinter Hemorrhage", icon: SVG_ICONS.blood, severe: true, desc: "Thin reddish-brown lines of blood under nails. Can indicate trauma or cardiac valve infection." },
  { id: "terrys_nail", name: "Terry's Nails", icon: SVG_ICONS.heart, severe: true, desc: "Most of the nail appears white with a narrow pink tip band. Highly linked to liver failure." },
  { id: "yellow_nails", name: "Yellow Nail Syndrome", icon: SVG_ICONS.shield, severe: true, desc: "Thick yellow nails with slow growth linked to respiratory and lymphatic conditions." }
];

// ── APP VIEWPORT MODE TOGGLE (REMOVED - FULL DESKTOP RESPONSIVE) ──
// ── SCREEN ROUTER ──
const PROTECTED_SCREENS = ['home', 'scan', 'chatbot', 'history', 'profile'];

function showScreen(id) {
  if (PROTECTED_SCREENS.includes(id) && !Auth.isLoggedIn()) {
    toast('Please sign in to access your nail health dashboard', 'error');
    id = 'login';
  }

  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const target = document.getElementById(id);
  if (target) target.classList.add('active');

  if (id === 'scan') {
    initWebcam();
  } else {
    stopWebcam();
  }

  if (id === 'home') {
    renderHealthSignals();
  }

  if (id === 'history') {
    renderHistoryCards();
  }

  if (id === 'wiki') {
    renderHealthWikiList();
  }

  document.querySelectorAll('.nav-item').forEach(item => {
    const attr = item.getAttribute('onclick');
    item.classList.toggle('active', attr && attr.includes(`'${id}'`));
  });

  const micBtn = document.getElementById('micBtn');
  const sidebar = document.getElementById('desktopSidebar');
  const isProtected = ['home', 'scan', 'chatbot', 'history', 'profile', 'wiki'].includes(id);

  if (sidebar) {
    sidebar.style.display = isProtected ? 'flex' : 'none';
  }
  if (micBtn) {
    micBtn.style.display = isProtected ? 'flex' : 'none';
  }
}

function showTab(tabId) {
  showScreen(tabId);
}

// ── AUTO-ADVANCE SPLASH ──
setTimeout(() => {
  if (document.getElementById('splash').classList.contains('active')) {
    if (Auth.isLoggedIn()) {
      showScreen('home');
    } else {
      showScreen('getting');
    }
  }
}, 2200);

// ── SLIDES ──
function nextSlide() {
  if (currentSlide < slides.length - 1) {
    currentSlide++;
    updateSlide();
  } else {
    showScreen('login');
  }
}

function updateSlide() {
  const s = slides[currentSlide];
  document.getElementById('getVisual').style.backgroundImage = `url('${s.image}')`;
  document.getElementById('getTitle').textContent = s.title;
  document.getElementById('getSub').textContent = s.sub;
  document.querySelectorAll('.get-dot').forEach((d, i) => {
    d.classList.toggle('active', i === currentSlide);
  });
  
  const nextBtn = document.getElementById('getNextBtn');
  const disclaimer = document.getElementById('getDisclaimer');
  const check = document.getElementById('getDisclaimerCheck');
  
  if (currentSlide === slides.length - 1) {
    disclaimer.style.display = 'block';
    nextBtn.textContent = 'START YOUR JOURNEY';
    toggleGetStartedBtn();
  } else {
    disclaimer.style.display = 'none';
    nextBtn.textContent = 'NEXT';
    nextBtn.disabled = false;
    nextBtn.style.opacity = '1';
  }
}

function toggleGetStartedBtn() {
  const nextBtn = document.getElementById('getNextBtn');
  const check = document.getElementById('getDisclaimerCheck');
  if (currentSlide === slides.length - 1) {
    if (check && check.checked) {
      nextBtn.disabled = false;
      nextBtn.style.opacity = '1';
    } else {
      nextBtn.disabled = true;
      nextBtn.style.opacity = '0.5';
    }
  }
}

function togglePasswordVisibility(id) {
  const el = document.getElementById(id);
  if (el) {
    if (el.type === 'password') {
      el.type = 'text';
    } else {
      el.type = 'password';
    }
  }
}

async function doGuestLogin() {
  toast('Signing in as Guest...');
  // Bypass API auth and seed a mock guest user
  Auth.currentUser = { name: 'Guest User', email: 'guest@nailvital.app' };
  Auth.pendingRegisterData = null;
  localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(Auth.currentUser));
  ApiService.setToken('demo_guest_token'); // mock token
  
  updateUserDisplay(Auth.currentUser);
  toast('Welcome Guest 👋');
  setTimeout(() => showScreen('home'), 600);
}

// ── TOAST NOTIFICATION ──
function toast(msg, type = '') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast show' + (type ? ' ' + type : '');
  setTimeout(() => t.classList.remove('show'), 3000);
}

// ── LOGIN ──
async function doLogin() {
  const email = document.getElementById('loginEmail').value.trim().toLowerCase();
  const pass = document.getElementById('loginPass').value;

  if (!email || !email.includes('@')) {
    showError('loginEmailField');
    toast('Please enter a valid email address', 'error');
    return;
  }
  if (!pass || pass.length < 6) {
    showError('loginPassField');
    toast('Please enter your password (minimum 6 characters)', 'error');
    return;
  }

  toast('Signing you in…');
  try {
    const user = await Auth.login(email, pass);
    updateUserDisplay(user);

    // If user account is not yet verified, prompt OTP verification
    if (user && user.is_verified === false) {
      Auth.pendingRegisterData = { email: user.email };
      const otpDisplayEl = document.getElementById('otpPhone');
      if (otpDisplayEl) otpDisplayEl.textContent = user.email;
      showScreen('otp');
      toast('Account is unverified. Sending verification email…', 'warning');
      try {
        await Auth.resendOtp(user.email);
        toast('✓ Verification OTP sent to your email!');
      } catch (e) {
        console.warn('Resend OTP error:', e);
      }
      startOtpTimer();
      return;
    }

    await loadUserHistory();
    toast('Signed in successfully!');
    setTimeout(() => showScreen('home'), 600);
  } catch (err) {
    toast(err.message || 'Incorrect email or password.', 'error');
  }
}

async function socialLogin(provider) {
  toast(`${provider} login requires OAuth backend configuration. Please sign in with your email and password.`, 'error');
}

// ── REGISTER ──
function validateNameInput(el) {
  const hint = document.getElementById('regNameHint');
  const field = document.getElementById('regNameField');
  const val = el.value;

  if (!val.trim()) {
    if (field) field.classList.remove('error');
    if (hint) {
      hint.style.color = 'var(--muted)';
      hint.textContent = 'Only letters, spaces and dots allowed';
    }
    return;
  }

  // Check if contains numbers or invalid symbols
  if (/[^A-Za-z .]/.test(val)) {
    if (field) field.classList.add('error');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Numbers and symbols are not allowed';
    }
  } else if (val.trim().length < 2) {
    if (field) field.classList.add('error');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Name must be at least 2 characters';
    }
  } else {
    if (field) field.classList.remove('error');
    if (hint) {
      hint.style.color = 'var(--mint-dark)';
      hint.textContent = '✓ Valid name';
    }
  }
}

function handlePhoneInput(el) {
  const hint = document.getElementById('regPhoneHint');
  const field = document.getElementById('regPhoneField');

  let val = el.value.replace(/[^0-9]/g, '');
  // If user typed a leading digit not in 6-9, reject the invalid leading digits
  let invalidStart = false;
  if (val.length > 0 && !/^[6-9]/.test(val)) {
    val = val.replace(/^[0-5]+/, '');
    invalidStart = true;
  }

  el.value = val.substring(0, 10);
  const cleanVal = el.value;

  if (invalidStart && cleanVal.length === 0) {
    if (field) field.classList.add('error');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Phone number must start with 6, 7, 8 or 9';
    }
    return;
  }

  if (cleanVal.length === 0) {
    if (field) field.classList.remove('error');
    if (hint) {
      hint.style.color = 'var(--muted)';
      hint.textContent = '10 digits, starting with 6, 7, 8 or 9';
    }
  } else if (cleanVal.length < 10) {
    if (field) field.classList.remove('error');
    if (hint) {
      hint.style.color = '#F59E0B';
      hint.textContent = `${10 - cleanVal.length} more digit${10 - cleanVal.length !== 1 ? 's' : ''} needed`;
    }
  } else {
    if (field) field.classList.remove('error');
    if (hint) {
      hint.style.color = 'var(--mint-dark)';
      hint.textContent = '✓ Valid phone number';
    }
  }
}

function validatePhoneHint(val) {
  const el = document.getElementById('regPhone');
  if (el) handlePhoneInput(el);
}

function handlePasswordStrength(el) {
  const val = el.value;
  const bar = document.getElementById('regPassBar');
  const hint = document.getElementById('regPassHint');
  const field = document.getElementById('regPassField');

  if (!val) {
    if (bar) { bar.style.width = '0%'; bar.style.background = 'transparent'; }
    if (hint) { hint.textContent = 'Enter password'; hint.style.color = 'var(--text-muted)'; }
    if (field) field.classList.remove('error');
    return;
  }

  let score = 0;
  if (val.length >= 8) score++;
  if (/[A-Z]/.test(val)) score++;
  if (/[0-9]/.test(val)) score++;
  if (/[^A-Za-z0-9]/.test(val)) score++;

  let color = 'transparent';
  let text = '';

  if (score === 1) { color = '#EF4444'; text = 'Weak'; }
  else if (score === 2) { color = '#F59E0B'; text = 'Fair'; }
  else if (score >= 3) { color = 'var(--accent-cyan)'; text = score === 4 ? 'Strong' : 'Good'; }

  if (bar) {
    bar.style.width = (score * 25) + '%';
    bar.style.background = color;
  }

  if (hint) {
    hint.textContent = text;
    hint.style.color = color === 'transparent' ? 'var(--text-muted)' : color;
  }

  if (val.length < 8) {
    if (field) field.classList.add('error');
  } else {
    if (field) field.classList.remove('error');
  }
}


function showError(fieldId) {
  const field = document.getElementById(fieldId);
  if (field) {
    field.classList.add('error');
    // Basic shake animation if we want it, or just the error class
    field.classList.add('shake');
    setTimeout(() => field.classList.remove('shake'), 400);
  }
}

async function doRegister() {
  const name = document.getElementById('regName').value.trim();
  const email = document.getElementById('regEmail').value.trim().toLowerCase();
  const phone = document.getElementById('regPhone').value.trim();
  const pass = document.getElementById('regPass').value;

  let ok = true;

  // Full name: letters, spaces and dots only, minimum 2 characters
  if (!name) {
    showError('regNameField');
    const hint = document.getElementById('regNameHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Full name is required';
    }
    ok = false;
  } else if (/[^A-Za-z .]/.test(name)) {
    showError('regNameField');
    const hint = document.getElementById('regNameHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Numbers and symbols are not allowed';
    }
    ok = false;
  } else if (name.length < 2) {
    showError('regNameField');
    const hint = document.getElementById('regNameHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Name must be at least 2 characters';
    }
    ok = false;
  }

  // Email
  if (!email || !email.includes('@')) {
    showError('regEmailField');
    ok = false;
  }

  // Phone: exactly 10 digits, must start with 6, 7, 8 or 9
  if (!phone) {
    showError('regPhoneField');
    const hint = document.getElementById('regPhoneHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Phone number is required';
    }
    ok = false;
  } else if (!/^[6-9]/.test(phone)) {
    showError('regPhoneField');
    const hint = document.getElementById('regPhoneHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Phone number must start with 6, 7, 8 or 9';
    }
    ok = false;
  } else if (phone.length !== 10) {
    showError('regPhoneField');
    const hint = document.getElementById('regPhoneHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = `⚠ Must be exactly 10 digits (you entered ${phone.length})`;
    }
    ok = false;
  }

  // Password
  if (pass.length < 8) { 
    showError('regPassField');
    const hint = document.getElementById('regPassHint');
    if (hint) {
      hint.style.color = '#EF4444';
      hint.textContent = '⚠ Minimum 8 characters required';
    }
    ok = false; 
  }

  // Terms and Privacy Checkbox
  const termsCheck = document.getElementById('regTermsCheck');
  if (termsCheck && !termsCheck.checked) {
    toast('⚠ You must agree to the Terms of Service and Privacy Policy', 'warning');
    ok = false;
  }

  if (!ok) {
    toast('Please fix the highlighted fields', 'error');
    return;
  }

  toast('Creating your account…');
  try {
    await Auth.register(name, email, phone, pass);
    const otpDisplayEl = document.getElementById('otpPhone');
    if (otpDisplayEl) otpDisplayEl.textContent = email;
    showScreen('otp');
    toast('✓ Account created! Verification OTP sent to your email.');
    startOtpTimer();
  } catch (err) {
    toast(err.message, 'error');
  }
}

// ── OTP VERIFICATION ──
function otpInput(el, nextId) {
  el.classList.toggle('filled', el.value.length > 0);
  if (el.value.length === 1 && nextId) {
    document.getElementById(nextId).focus();
  }
  const all = ['o1', 'o2', 'o3', 'o4', 'o5', 'o6'].every(id => document.getElementById(id).value.length === 1);
  if (all) setTimeout(verifyOtp, 300);
}

function otpBack(e, el, prevId) {
  if (e.key === 'Backspace' && !el.value && prevId) {
    document.getElementById(prevId).focus();
  }
}

async function verifyOtp() {
  let code = '';
  ['o1', 'o2', 'o3', 'o4', 'o5', 'o6'].forEach(id => code += (document.getElementById(id).value || '').trim());

  if (code.length !== 6) {
    toast('Please enter the full 6-digit OTP code', 'error');
    return;
  }

  toast('Verifying OTP…');
  try {
    const user = await Auth.verifyOtp(code);
    updateUserDisplay(user);
    if (otpTimer) clearInterval(otpTimer);
    toast('Account verified successfully! Welcome 👋');
    setTimeout(() => showScreen('home'), 600);
  } catch (err) {
    toast(err.message, 'error');
  }
}

async function triggerResendOtp() {
  const email = (Auth.pendingRegisterData && Auth.pendingRegisterData.email) ||
    (Auth.currentUser && Auth.currentUser.email) || '';
  if (!email) {
    toast('No target email found. Please re-enter your email.', 'error');
    return;
  }
  toast('Resending verification code to email…');
  try {
    await Auth.resendOtp(email);
    toast(`✓ Verification OTP sent to ${email}! Check inbox/spam.`);
    startOtpTimer();
  } catch (err) {
    toast(err.message, 'error');
  }
}

function startOtpTimer() {
  let t = 30;
  const el = document.getElementById('resendTimer');
  if (otpTimer) clearInterval(otpTimer);
  otpTimer = setInterval(() => {
    t--;
    if (t <= 0) {
      clearInterval(otpTimer);
      if (el) el.innerHTML = '<span class="link" onclick="triggerResendOtp()">Resend OTP</span>';
    } else {
      if (el) el.textContent = `Resend in ${t}s`;
    }
  }, 1000);
}

// ── FORGOT PASSWORD & RESET FLOW ──
let resetTargetEmail = '';

async function sendReset() {
  const email = document.getElementById('forgotEmail').value.trim().toLowerCase();
  if (!email || !email.includes('@')) {
    showError('forgotEmailField');
    toast('Please enter a valid email address', 'error');
    return;
  }

  toast('Sending password reset code to email…');
  try {
    await Auth.forgotPassword(email);
    resetTargetEmail = email;
    const sentEmailEl = document.getElementById('forgotSentEmail');
    if (sentEmailEl) sentEmailEl.textContent = email;

    document.getElementById('forgotStep1').style.display = 'none';
    document.getElementById('forgotStep2').style.display = 'block';
    toast('✓ Reset OTP code sent to ' + email + '!');
  } catch (err) {
    toast(err.message, 'error');
  }
}

async function doResetPassword() {
  const otpEl = document.getElementById('forgotCode');
  const newPassEl = document.getElementById('forgotNewPass');
  
  if (!otpEl || !newPassEl) {
    toast('Error: form elements not found', 'error');
    return;
  }
  
  const otp = otpEl.value.trim();
  const newPass = newPassEl.value;

  if (otp.length !== 6) {
    toast('Please enter the full 6-digit OTP code', 'error');
    return;
  }
  if (!newPass || newPass.length < 8) {
    toast('Password must be at least 8 characters long', 'error');
    return;
  }

  toast('Updating your password…');
  try {
    await Auth.resetPassword(resetTargetEmail, otp, newPass);
    toast('✓ Password reset successfully! Please sign in with your new password.');
    setTimeout(() => {
      document.getElementById('forgotStep1').style.display = 'block';
      document.getElementById('forgotStep2').style.display = 'none';
      document.getElementById('forgotCode').value = '';
      document.getElementById('forgotNewPass').value = '';
      showScreen('login');
    }, 1000);
  } catch (err) {
    toast(err.message, 'error');
  }
}

// ── CAMERA & WEBCAM ──
let videoStream = null;
let analyzeInterval = null;

async function initWebcam() {
  console.log("Webcam disabled as per user request. Upload only.");
}

function stopWebcam() {
  console.log("Webcam disabled.");
}
// ── CAPTURE & ANALYZE IMAGE ──
async function doScan() {
  document.getElementById('analyzingModal').classList.add('show');
  
  // Dynamic UI state for analyzing modal
  let progress = 0;
  let msgIdx = 0;
  const msgs = [
    "Extracting nail bed contours...",
    "Analyzing color variations...",
    "Cross-referencing medical database...",
    "Detecting ridges and anomalies...",
    "Finalizing AI insights..."
  ];
  
  const subEl = document.getElementById('analyzingSubtext');
  const progEl = document.getElementById('analyzeProgress');
  
  if (subEl) subEl.textContent = "Initiating scan...";
  if (progEl) progEl.style.width = '0%';
  
  if (analyzeInterval) clearInterval(analyzeInterval);
  analyzeInterval = setInterval(() => {
    progress += Math.random() * 12 + 3; // Add 3-15% randomly
    if (progress > 92) progress = 92; // Cap at 92% until finished
    if (progEl) progEl.style.width = `${progress}%`;
    
    if (Math.random() > 0.4 && subEl) {
        subEl.textContent = msgs[msgIdx % msgs.length];
        msgIdx++;
    }
  }, 700);
  
  // Also disable the file input to be safe
  const fileInput = document.getElementById('scanFileInput');
  if (fileInput) fileInput.disabled = true;

  toast('Analyzing nail image with AI…');

  let selectedFinger = "Finger 1";
  const activeFingerBtn = document.querySelector('.scan-fingers .finger-btn.active');
  if (activeFingerBtn) selectedFinger = activeFingerBtn.dataset.finger || activeFingerBtn.textContent;

  let blob = Scanner.currentImageBlob;

  if (!blob) {
    const videoEl = document.getElementById('webcamFeed');
    if (videoEl && videoEl.srcObject) {
      const canvas = document.createElement('canvas');
      canvas.width = 640; canvas.height = 480;
      canvas.getContext('2d').drawImage(videoEl, 0, 0, canvas.width, canvas.height);
      blob = await new Promise(r => canvas.toBlob(r, 'image/jpeg'));
    }
  }

  try {
    const result = await Scanner.analyzeScan(blob);
    currentScanResult = result;

    userScans.unshift(result);
    renderHealthSignals();

    setTimeout(() => {
      showScanResultModal(result);
      toast('✓ AI Scan Complete!');
    }, 400);
  } catch (err) {
    // 🛑 Hard rejection: show Nail Not Detected modal, never a random fallback 🛑
    if (err instanceof NailNotDetectedError) {
      showNailNotDetectedModal(err.category);
    } else {
      toast(err.message, 'error');
    }
  } finally {
    const progEl = document.getElementById('analyzeProgress');
    if (progEl) progEl.style.width = '100%';
    setTimeout(() => {
      document.getElementById('analyzingModal').classList.remove('show');
      if (analyzeInterval) clearInterval(analyzeInterval);
    }, 400); // Give it a brief moment to show 100%
    
    const fileInput = document.getElementById('scanFileInput');
    if (fileInput) fileInput.disabled = false;
  }
}

function handleFileUpload(input) {
  if (input.files && input.files[0]) {
    const file = input.files[0];
    Scanner.currentImageBlob = file;
    toast(`Uploaded photo: ${file.name}`);
    doScan();
  }
}

function showScanResultModal(res) {
  const diagName = res.display_name || res.result_class;
  const confText = `${Math.round(res.confidence || 91.4)}%`;

  document.getElementById('resDiagnosisName').textContent = diagName;
  document.getElementById('resConfidence').textContent = confText;
  
  const elFinger = document.getElementById('resFingerName');
  if (elFinger) elFinger.textContent = res.finger || 'Finger 1';
  
  const elDiag2 = document.getElementById('resDiagnosisName2');
  if (elDiag2) elDiag2.textContent = diagName;
  
  const elConf2 = document.getElementById('resConfidence2');
  if (elConf2) elConf2.textContent = confText;

  document.getElementById('resDescription').textContent = res.description || 'Analysis completed.';
  document.getElementById('resRecommendation').textContent = res.recommendation || 'Consult a medical professional for advice.';

  document.getElementById('scanResultModal').classList.add('show');
}

function closeScanResultModal() {
  document.getElementById('scanResultModal').classList.remove('show');
  showScreen('home');
}

// ─── NAIL NOT DETECTED MODAL ───────────────────────────────────────────────

/**
 * Maps a Gemini rejection_category to a human-readable subtitle message.
 */
function _rejectionCategoryMessage(category = '') {
  const cat = category.toUpperCase();
  if (cat.includes('BLANK') || cat.includes('SOLID_COLOR') || cat.includes('EMPTY') || cat.includes('CORRUPTED'))
    return 'The image appears to be blank, empty, or unreadable. Please upload a clear photo.';
  if (cat.includes('FULLY_COVERED') || cat.includes('POLISH') || cat.includes('NAIL_ART_SAMPLE'))
    return 'The nail appears fully covered by polish, gel, or nail wraps. Please scan a natural nail with visible nail surface.';
  if (cat.includes('CARTOON') || cat.includes('CLIPART') || cat.includes('ILLUSTRATION') || cat.includes('AI_GENERATED') || cat.includes('RENDERED'))
    return 'Only real photographs are accepted. Illustrations, cartoons, and AI-generated images are not supported.';
  if (cat.includes('ANIMAL') || cat.includes('CLAW') || cat.includes('PAW'))
    return 'Animal claws or paws are not supported. Please scan a human finger or toenail.';
  if (cat.includes('SCREENSHOT') || cat.includes('DOCUMENT') || cat.includes('TEXT') || cat.includes('SCREEN_OR_MONITOR'))
    return 'Screenshots, documents, and text-heavy images are not supported. Please upload a real nail photo.';
  if (cat.includes('ICON') || cat.includes('LOGO') || cat.includes('SYMBOL') || cat.includes('CHART') || cat.includes('DIAGRAM'))
    return 'Icons, logos, charts, and diagrams are not supported. Please upload a real nail photo.';
  if (cat.includes('BLURRY') || cat.includes('DARK') || cat.includes('POOR_QUALITY'))
    return 'The image is too blurry or dark to analyse. Please upload a well-lit, in-focus photo.';
  if (cat.includes('NO_NAIL') || cat.includes('OTHER_BODY_PART') || cat.includes('HAND_OR_FOOT_BUT_NAIL_NOT_VISIBLE'))
    return 'No visible nail was found. Ensure the nail is clearly in frame and not turned away from the camera.';
  if (cat.includes('STOCK_PHOTO') || cat.includes('WATERMARK'))
    return 'Watermarked or staged stock photos are not accepted. Please use a real, unedited nail photo.';
  return 'Please upload a clear, close-up photograph of a real human finger or toenail.';
}

function showGuideDialog() {
  document.getElementById('guideDialogModal').classList.add('show');
}

function closeGuideDialog() {
  document.getElementById('guideDialogModal').classList.remove('show');
}

function continueFromGuide() {
  closeGuideDialog();
  document.getElementById('scanFileInput').click();
}

function showNailNotDetectedModal(category = 'UNKNOWN') {
  const msgEl = document.getElementById('nndReason');
  if (msgEl) msgEl.textContent = _rejectionCategoryMessage(category);
  document.getElementById('nailNotDetectedModal').classList.add('show');
}

function closeNailNotDetectedModal() {
  document.getElementById('nailNotDetectedModal').classList.remove('show');
}

async function exportCurrentReportPdf() {
  toast('Generating clinical PDF report…');
  // Ensure history is always loaded before exporting
  if (userScans.length === 0) {
    userScans = await History.loadHistory();
  }
  if (currentScanResult && currentScanResult.id) {
    History.exportSinglePdf(currentScanResult.id);
  } else {
    History.exportHistoryPdf();
  }
}

// ── DYNAMIC HEALTH SIGNALS RENDERING WITH VECTOR SVG ICONS ──
async function loadUserHistory() {
  try {
    userScans = await History.loadHistory();
  } catch (err) {
    userScans = [];
  }
  renderHealthSignals();
}

function renderHealthSignals() {
  const container = document.getElementById('homeHealthSignalsGrid');
  const heroText = document.getElementById('homeHeroLastScan');
  
  if (!container) return;
  
  // 1. Update Hero Card
  if (userScans.length > 0) {
    const latest = userScans[0];
    const cName = latest.display_name || latest.result_class;
    if (heroText) heroText.textContent = `Last scan: ${cName}`;
  } else {
    if (heroText) heroText.textContent = `Last scan: None`;
  }

  // 2. Render all 22 conditions
  let html = '';
  
  const latestScan = userScans.length > 0 ? userScans[0] : null;

  ALL_CONDITIONS.forEach(cond => {
    // Check if the latest scan matches this condition
    let match = null;
    if (latestScan) {
      if (latestScan.result_class === cond.id || 
          (latestScan.display_name && latestScan.display_name === cond.name) ||
          (cond.id === 'healthy' && latestScan.result_class === 'normal')) {
        match = latestScan;
      } else if (latestScan.findings && Array.isArray(latestScan.findings)) {
        match = latestScan.findings.find(f => 
          f.result_class === cond.id || 
          (f.display_name && f.display_name === cond.name) ||
          (cond.id === 'healthy' && f.result_class === 'normal')
        );
      }
    }
    
    let prob = 0;
    let riskLabel = "LOW";
    let badgeColor = "#06b6d4"; // Cyan
    let badgeBg = "rgba(6,182,212,0.1)";
    
    if (match) {
      prob = match.confidence || 0;
      if (prob >= 65) {
        riskLabel = "HIGH RISK";
        badgeColor = "#ef4444";
        badgeBg = "rgba(239,68,68,0.1)";
      } else if (prob >= 35) {
        riskLabel = "MEDIUM RISK";
        badgeColor = "#f59e0b";
        badgeBg = "rgba(245,158,11,0.1)";
      } else {
        riskLabel = "LOW RISK";
      }
    }
    
    html += `
      <div class="glass-card" onclick="showScreen('wiki')" style="cursor:pointer; display:flex; flex-direction:column; justify-content:space-between; padding:20px; background:var(--bg-card); border:1px solid var(--border-light); border-radius:var(--radius-sm); box-shadow:var(--shadow-card);">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:12px;">
          <div style="width:36px; height:36px; border-radius:10px; background:rgba(15,118,110,0.1); display:flex; align-items:center; justify-content:center; color:var(--accent-cyan);">
            ${cond.icon}
          </div>
          <div style="font-size:10px; font-weight:700; background:${badgeBg}; color:${badgeColor}; padding:4px 8px; border-radius:12px;">
            ${riskLabel}
          </div>
        </div>
        
        <h4 style="font-size:14px; font-weight:700; margin-bottom:12px; color:var(--text-main);">${cond.name}</h4>
        
        <div style="height:4px; width:100%; background:var(--border-light); border-radius:2px; margin-bottom:8px; overflow:hidden;">
          <div style="height:100%; width:${prob}%; background:${badgeColor};"></div>
        </div>
        <p style="font-size:11px; color:var(--text-muted); margin:0;">${prob.toFixed(0)}% probability</p>
      </div>
    `;
  });
  
  container.innerHTML = html;
}

// ── RENDER HISTORY SCAN RECORDS ──
async function renderHistoryCards() {
  const container = document.getElementById('historyCardList');
  if (!container) return;

  container.innerHTML = '<div style="text-align:center; padding:40px; color:var(--muted);">Loading scan records…</div>';

  userScans = await History.loadHistory();

  const chartEl = document.getElementById('clinicalTrendChart');
  if (chartEl) {
    if (userScans.length === 0) {
      chartEl.innerHTML = '';
    } else {
      const reversedScans = [...userScans].reverse();
      const n = reversedScans.length;
      let pointsStr = '';
      let circlesHtml = '';
      reversedScans.forEach((s, i) => {
        const conf = Math.round(s.confidence || 91.4);
        const cx = n === 1 ? 200 : 10 + (i / (n - 1)) * 380;
        const cy = 100 - (conf * 0.8 + 10); // Map 0-100 to 90-10
        const dateObj = new Date(s.created_at || Date.now());
        const dateStr = dateObj.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
        const timeStr = dateObj.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
        pointsStr += `${cx},${cy} `;
        circlesHtml += `
          <circle cx="${cx}" cy="${cy}" r="5" fill="#0f766e" stroke="white" stroke-width="2"/>
          <text x="${cx}" y="${cy + 16}" font-family="sans-serif" font-size="7" fill="#9ca3af" text-anchor="middle">${dateStr}</text>
          <text x="${cx}" y="${cy + 26}" font-family="sans-serif" font-size="7" fill="#9ca3af" text-anchor="middle">${timeStr}</text>
        `;
      });
      chartEl.innerHTML = `
        <polyline points="${pointsStr.trim()}" fill="none" stroke="#0f766e" stroke-width="4" stroke-linejoin="round"/>
        ${circlesHtml}
      `;
    }
  }

  if (userScans.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:60px 20px; background:var(--card-light); border-radius:18px; border:1px solid var(--card-border);">
        <div style="margin-bottom:12px; color:var(--muted);">
          <svg class="svg-icon-xl" viewBox="0 0 24 24"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/></svg>
        </div>
        <div style="font-family:'Sora',sans-serif; font-size:18px; font-weight:700; color:var(--deep);">No scan history found</div>
        <p style="font-size:13px; color:var(--muted); margin-top:6px; margin-bottom:20px;">Start your first nail scan to track diagnostics here.</p>
        <button class="btn btn-primary" onclick="showScreen('scan')">📸 Take First Scan</button>
      </div>
    `;
    return;
  }

  container.innerHTML = userScans.map((scan, idx) => {
    const conf = Math.round(scan.confidence || 91.4);
    const date = new Date(scan.created_at || Date.now()).toISOString().split('T')[0];
    const diag = scan.display_name || scan.result_class;
    const finger = scan.finger || 'Finger 1';
    const imgSrc = scan.image_path ? `${CONFIG.API_BASE_URL}/${scan.image_path}` : 'https://images.unsplash.com/photo-1599839619722-39751411ea63?w=200&h=200&fit=crop';
    
    return `
    <div style="background:white; border-radius:24px; padding:20px; box-shadow:0 10px 30px rgba(0,0,0,0.03); display:flex; align-items:center; gap:20px;">
      
      <!-- Nail Image -->
      <img src="${imgSrc}" style="width:80px; height:80px; border-radius:16px; object-fit:cover; border:1px solid #f3f4f6;" onerror="this.src='https://images.unsplash.com/photo-1599839619722-39751411ea63?w=200&h=200&fit=crop'">
      
      <!-- Info -->
      <div style="flex:1;">
        <h3 style="font-size:18px; font-weight:800; color:#111827; margin:0 0 8px 0;">${diag}</h3>
        <div style="display:flex; align-items:center; gap:6px; margin-bottom:8px;">
          <div style="width:6px; height:6px; border-radius:50%; background:#0f766e;"></div>
          <span style="font-size:14px; color:#6b7280;">${conf}% confidence • ${finger}</span>
        </div>
        <div style="font-size:12px; color:#9ca3af;">${date}</div>
      </div>
      
      <!-- Actions -->
      <div style="display:flex; gap:12px;">
        <button onclick="History.exportSinglePdf(${scan.id})" style="width:44px; height:44px; border-radius:50%; background:#f8fafc; border:none; display:flex; align-items:center; justify-content:center; cursor:pointer;">
          <svg class="svg-icon" viewBox="0 0 24 24" style="width:18px; height:18px; stroke:#0f766e;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        </button>
        <button onclick="deleteHistoryScan(${scan.id})" style="width:44px; height:44px; border-radius:50%; background:#fef2f2; border:none; display:flex; align-items:center; justify-content:center; cursor:pointer;">
          <svg class="svg-icon" viewBox="0 0 24 24" style="width:18px; height:18px; stroke:#ef4444;"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>
      
    </div>
  `}).join('');
}

async function deleteHistoryScan(id) {
  if (confirm('Are you sure you want to delete this scan record?')) {
    await History.deleteScan(id);
    await renderHistoryCards();
    toast('Scan record deleted.');
  }
}

// ── RENDER 22 NAIL CONDITIONS HEALTH WIKI ──
function renderHealthWikiList() {
  const container = document.getElementById('wikiList');
  if (!container) return;

  container.innerHTML = ALL_CONDITIONS.map(item => `
    <div class="wiki-card">
      <div class="wiki-card-title">
        <span style="display:flex; align-items:center; gap:8px;">${item.icon} ${item.name}</span>
        ${item.severe ? '<span style="font-size:9px; background:rgba(239,68,68,0.12); color:#EF4444; padding:3px 8px; border-radius:6px;">SEVERE</span>' : '<span style="font-size:9px; background:var(--mint-light); color:var(--mint-dark); padding:3px 8px; border-radius:6px;">NORMAL</span>'}
      </div>
      <p style="font-size:13px; color:var(--muted); margin-top:8px; line-height:1.5;">${item.desc}</p>
    </div>
  `).join('');
}

// ── AI CHATBOT (MEDICAL ASSISTANT) ──
async function sendChatMessage() {
  const input = document.getElementById('chatInputText');
  const text = input.value.trim();
  if (!text) return;
  input.value = '';

  appendChatMessage('user', text);
  const reply = await Chatbot.sendMessage(text);
  appendChatMessage('ai', reply);
}

function sendSuggestion(text) {
  appendChatMessage('user', text);
  Chatbot.sendMessage(text).then(reply => {
    appendChatMessage('ai', reply);
  });
}

function appendChatMessage(sender, text) {
  const container = document.getElementById('chatMsgList');
  if (!container) return;

  const row = document.createElement('div');
  row.className = `msg-row ${sender}`;
  row.innerHTML = `
    <div class="msg-bubble ${sender}">${text}</div>
  `;
  container.appendChild(row);
  container.scrollTop = container.scrollHeight;
}

function clearChat() {
  Chatbot.clearHistory();
  document.getElementById('chatMsgList').innerHTML = `
    <div class="msg-row ai">
      <div class="msg-bubble ai">Hi Priya! I'm your NailVital Medical Assistant. Ask me any questions about your scan results, nail symptoms, or healthcare guidance!</div>
    </div>
  `;
}

// ── PROFILE & USER DISPLAY ──
function updateUserDisplay(user) {
  if (!user) user = { name: 'Priya Sharma', email: 'priya@example.com' };
  const initials = user.name ? user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() : 'PS';

  const homeUserNameDisplay = document.getElementById('homeUserNameDisplay');
  if (homeUserNameDisplay) homeUserNameDisplay.textContent = user.name.split(' ')[0];
  
  const sidebarAvatar = document.getElementById('sidebarAvatar');
  if (sidebarAvatar) sidebarAvatar.textContent = initials;
  
  const sidebarName = document.getElementById('sidebarName');
  if (sidebarName) sidebarName.textContent = user.name;
  
  const profAvatarDisplay = document.getElementById('profAvatarDisplay');
  if (profAvatarDisplay) profAvatarDisplay.textContent = initials;
  
  const profNameDisplay = document.getElementById('profNameDisplay');
  if (profNameDisplay) profNameDisplay.textContent = user.name;
  
  const profEmailDisplay = document.getElementById('profEmailDisplay');
  if (profEmailDisplay) profEmailDisplay.textContent = user.email;
}

function exportHistoryPdf() {
  History.exportHistoryPdf();
  toast('Downloading clinical PDF report…');
}

function exportHealthDataJson() {
  Profile.exportUserData();
  toast('Exported health records JSON!');
}

// ── LOGOUT & DELETE ──
function showLogout() { document.getElementById('logoutModal').classList.add('show'); }
function closeModal() { document.getElementById('logoutModal').classList.remove('show'); }

function confirmLogout() {
  closeModal();
  Auth.logout();
  toast('Signed out successfully.');
  setTimeout(() => showScreen('login'), 600);
}

function doDelete() {
  const confirmVal = document.getElementById('deleteConfirm').value;
  const pass = document.getElementById('deletePass').value;

  if (confirmVal !== 'DELETE') { showError('deleteConfirmField'); return; }
  if (!pass) { showError('deletePassField'); return; }

  Profile.deleteAccount(pass).then(() => {
    toast('Account deleted.', 'error');
    setTimeout(() => showScreen('getting'), 1200);
  });
}

// ── UTILS ──
function checkPwStrength(val) {
  const res = Auth.calculatePasswordStrength(val);
  const bar = document.getElementById('pwBar');
  const label = document.getElementById('pwLabel');
  if (bar) { bar.style.width = res.percent; bar.style.background = res.color; }
  if (label) { label.textContent = res.text; label.style.color = res.color; }
}

function togglePw(inputId, icon) {
  const inp = document.getElementById(inputId);
  inp.type = inp.type === 'password' ? 'text' : 'password';
  icon.textContent = inp.type === 'password' ? '👁' : '🙈';
}

function showError(fieldId) { document.getElementById(fieldId).classList.add('error'); }
function clearError(fieldId) { document.getElementById(fieldId).classList.remove('error'); }

// Init listeners
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.scan-fingers .finger-btn').forEach(btn => {
    btn.addEventListener('click', function () {
      document.querySelectorAll('.scan-fingers .finger-btn').forEach(b => b.classList.remove('active'));
      this.classList.add('active');
    });
  });

  const savedUser = Auth.currentUser;
  if (savedUser) {
    updateUserDisplay(savedUser);
    loadUserHistory();
  }
});
