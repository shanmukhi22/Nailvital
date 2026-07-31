package com.nailvital.app.ui.screens

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector

enum class Language(val code: String, val label: String) {
    ENGLISH("en", "English"),
    TELUGU("te", "తెలుగు"),
    HINDI("hi", "हिन्दी"),
    TAMIL("ta", "தமிழ்")
}

data class AppStrings(
    val appName: String = "NailVital AI",
    val welcome: String = "Welcome",
    val profile: String = "Profile",
    val login: String = "Login",
    val register: String = "Register",
    val email: String = "Email",
    val password: String = "Password",
    val phone: String = "Phone",
    val age: String = "Age",
    val gender: String = "Gender",
    val height: String = "Height",
    val personalDetails: String = "Personal Details",
    val saveChanges: String = "Save Changes",
    val logout: String = "Log Out",
    val deleteAccount: String = "Delete Account",
    val scanNow: String = "Scan Now",
    val exportPdf: String = "Export PDF Report",
    val downloadData: String = "Download My Data",
    val language: String = "Language",
    val selectLanguage: String = "Select Language",
    
    val goodMorning: String = "Good morning",
    val goodAfternoon: String = "Good afternoon",
    val goodEvening: String = "Good evening",
    val healthSignals: String = "Health signals",
    val todaysScan: String = "Today's scan",
    val lastScan: String = "Last scan",
    val startScan: String = "Start scan",
    val home: String = "Home",
    val aiChat: String = "AI Chat",
    val createAccount: String = "Create Account",
    val signIn: String = "Sign in",
    val alreadyHaveAccount: String = "Already have an account?",
    val verifyEmail: String = "Verify Email",
    val enterOtp: String = "Enter the 6-digit code sent to",
    val resendCode: String = "Resend code",

    val signInContinue: String = "Sign in to continue your health journey",
    val forgotPassword: String = "Forgot Password?",
    val orContinueWith: String = "Or continue with",
    val dontHaveAccount: String = "Don't have an account?",
    val signUp: String = "Sign up",
    val startPersonalizedJourney: String = "Start your personalized health journey today",
    val fullName: String = "Full Name",
    val phoneNumber: String = "Phone Number",
    val enterPassword: String = "Enter password",
    val weak: String = "Weak",
    val fair: String = "Fair",
    val good: String = "Good",
    val strong: String = "Strong",
    val enter6DigitCode: String = "Enter the 6-digit code sent to %s",
    val verificationCode: String = "Verification Code",
    val verifyContinue: String = "Verify & Continue",

    val accountExists: String = "Account Exists",
    val emailAlreadyRegistered: String = "This email is already registered. Please sign in instead.",
    val cancel: String = "Cancel",
    val dismiss: String = "Dismiss",
    val delete: String = "Delete",
    val skip: String = "Skip",
    val start: String = "Start",
    val historyLog: String = "History",
    val captureDiagnose: String = "Capture & Diagnose",
    val nailAnalysis: String = "Nail Analysis",
    val systemReady: String = "System Ready",
    val decryptingSignals: String = "Decrypting Health Signals...",
    val initiateScan: String = "Initiate Scan",
    val clinicalFindings: String = "Clinical Findings",
    val noDetailedFindings: String = "No detailed clinical findings recorded for this anomaly.",
    val healthJournal: String = "Health Journal",
    val journalEntries: String = "Journal Entries",
    val clinicalHistory: String = "Clinical History",
    val exportAll: String = "Export All",
    val noRecordsFound: String = "No records found",
    val invalidNailTitle: String = "Nail Not Detected",
    val invalidNailDesc: String = "The system could not detect a valid human nail in this image. Please ensure you are uploading a clear, close-up photo of a fingernail.",
    val nonFingerDetectedTitle: String = "Finger Not Detected",
    val nonFingerDetectedDesc: String = "The system can only analyze human fingers and nails. Please ensure your finger is clearly visible in the frame.",
    val purgeRecord: String = "Purge Record",
    val purgeRecordDesc: String = "Are you sure you want to permanently delete this clinical record? This cannot be undone.",
    val reportGenerated: String = "Report Generated",
    val reportGeneratedDesc: String = "The medical report has been encrypted and saved to your device.",
    val secureAccount: String = "Secure Account",
    val resetPassword: String = "Reset Password",
    val enterResetCodeEmail: String = "Enter the clinical verification code sent to %s.",
    val emailAddress: String = "Email address",
    val newSecurePassword: String = "New Secure Password",
    val askAboutHealth: String = "Ask about your nail health...",
    val thinking: String = "Thinking...",
    val medicalAssistant: String = "Medical Assistant",
    val personalAiAdvisor: String = "Personal AI Advisor",
    val consultGemini: String = "Consult AI about your results",
    val analyzeNailsSeconds: String = "Analyze your nail\nhealth in seconds",
    val dataLossWarning: String = "Data Loss Warning",
    val irreversibleAction: String = "This action is irreversible. All clinical history, encrypted reports, and biometric data will be permanently purged.",
    val purgeRecordsDescList: String = "• Permanent deletion of disease history\n• Loss of all encrypted PDF reports",
    val typeDeleteConfirm: String = "Type DELETE to confirm",
    val verifyIdentityPass: String = "Verify Identity (Password)",
    val personalProfile: String = "Personal Profile",
    val verifyClinicalDetails: String = "Verify your clinical details for more accurate diagnostic insights.",
    val enterEmailResetCode: String = "Enter your email to receive a reset code.",
    val precisionDiagnostics: String = "PRECISION DIAGNOSTICS FOR NAIL HEALTH",
    val lastResult: String = "Last result: ",
    val noRecentScans: String = "No recent scans",
    val probability: String = "probability",
    val reminders: String = "Reminders",
    
    val ironDeficiency: String = "Iron deficiency",
    val thyroidMarkers: String = "Thyroid markers",
    val fungalInfection: String = "Fungal infection",
    val heartMarkers: String = "Heart markers",
    val iron: String = "Iron",
    val thyroid: String = "Thyroid",
    val fungal: String = "Fungal",
    val heart: String = "Heart",

    val alopeciaAreata: String = "Alopecia Areata (Nail Signs)",
    val beausLines: String = "Beau's Lines (Systemic Stress)",
    val bluishNail: String = "Bluish Nails (Cyanosis)",
    val clubbing: String = "Nail Clubbing (Heart/Lung Marker)",
    val dariersDisease: String = "Darier's Disease",
    val eczema: String = "Nail Eczema",
    val halfAndHalf: String = "Half-and-Half (Kidney Marker)",
    val healthyNail: String = "Healthy Nails",
    val koilonychia: String = "Koilonychia (Spoon Nails/Iron)",
    val leukonychia: String = "Leukonychia (White Spots)",
    val melanoma: String = "Subungual Melanoma (Caution)",
    val muehrckesLines: String = "Muehrcke's Lines (Albumin)",
    val onychogryphosis: String = "Onychogryphosis (Ram's Horn)",
    val onycholysis: String = "Onycholysis (Thyroid/Psoriasis)",
    val onychomycosis: String = "Onychomycosis (Fungal)",
    val paleNail: String = "Pale Nails (Anemia Marker)",
    val pitting: String = "Nail Pitting",
    val psoriasis: String = "Nail Psoriasis",
    val redLunula: String = "Red Lunula (Heart Marker)",
    val splinterHemorrhage: String = "Splinter Hemorrhage",
    val terrysNail: String = "Terry's Nails (Liver Marker)",
    val yellowNails: String = "Yellow Nail Syndrome",
    
    val medicalDisclaimer: String = "Disclaimer: This app provides AI-assisted insights for educational purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of your physician or other qualified health provider with any questions you may have regarding a medical condition.",
    val aiAssessmentNotice: String = "AI assessment only. Consult a clinical professional.",
    
    // Informational Strings
    val aboutTitle: String = "About NailVital AI",
    val aboutDesc: String = "NailVital AI is a high-precision clinical screening tool powered by advanced deep learning and anatomical verification. Designed for early detection, it analyzes nail architecture for 22 distinct dermatological conditions, providing users with evidence-based health insights.",
    val howToUseTitle: String = "How to Use NailVital",
    val step1Title: String = "1. Image Selection",
    val step1Desc: String = "Choose a clear, high-resolution photo from your gallery. Ensure the nail is clean and free of polish.",
    val step2Title: String = "2. Quality Verification",
    val step2Desc: String = "The image must be a close-up (10-15cm). Ensure the nail is centered and well-lit with natural light.",
    val step3Title: String = "3. Upload & Analyze",
    val step3Desc: String = "Upload your photo and wait for the AI to perform a dual-stage anatomical check and report.",
    val faqsTitle: String = "User FAQs",
    val faq1Q: String = "Is this a medical diagnosis?",
    val faq1A: String = "No. NailVital AI provides diagnostic indicators and screening aids. All results should be formally reviewed by a medical professional.",
    val faq2Q: String = "Does it work on toenails?",
    val faq2A: String = "Yes. The AI is trained to recognize and analyze both human fingernails and toenails with equal precision.",
    val faq3Q: String = "Why was my photo rejected?",
    val faq3A: String = "Rejections occur if the AI cannot find a clear nail (due to blur, shadows, or distance). Try a closer, sharper photo.",
    val faq4Q: String = "Is my data secure?",
    val faq4A: String = "Yes. Your clinical data and images are protected with industry-standard encryption to ensure total privacy."
)

val EnglishStrings = AppStrings()

val TeluguStrings = AppStrings(
    welcome = "స్వాగతం",
    profile = "ప్రొఫైల్",
    login = "లాగిన్",
    register = "రిజిస్టర్",
    email = "ఈమెయిల్",
    password = "పాస్‌వర్డ్",
    phone = "ఫోన్",
    age = "వయస్సు",
    gender = "లింగం",
    height = "ఎత్తు",
    personalDetails = "వ్యక్తిగత వివరాలు",
    saveChanges = "మార్పులను సేవ్ చేయి",
    logout = "లాగ్ అవుట్",
    deleteAccount = "ఖాతాను తొలగించు",
    scanNow = "స్కాన్ చేయి",
    exportPdf = "PDF రిపోర్ట్ పంపండి",
    downloadData = "నా డేటాను డౌన్‌లోడ్ చేయి",
    language = "భాష",
    selectLanguage = "భాషను ఎంచుకోండి",
    goodMorning = "శుభోదయం",
    goodAfternoon = "శుభాహ్నం",
    goodEvening = "శుభ సాయంత్రం",
    healthSignals = "ఆరోగ్య సంకేతాలు",
    todaysScan = "నేటి స్కాన్",
    lastScan = "చివరి స్కాన్",
    startScan = "స్కాన్ ప్రారంభించండి",
    home = "హోమ్",
    aiChat = "AI చాట్",
    createAccount = "ఖాతాను సృష్టించండి",
    signIn = "సైన్ ఇన్",
    alreadyHaveAccount = "ఇప్పటికే ఖాతా ఉందా?",
    verifyEmail = "ఈమెయిల్ ధృవీకరించండి",
    enterOtp = "కోడ్ నమోదు చేయండి",
    resendCode = "కోడ్‌ని మళ్లీ పంపండి",
    signInContinue = "మీ ఆరోగ్య ప్రయాణాన్ని కొనసాగించడానికి సైన్ ఇన్ చేయండి",
    forgotPassword = "పాస్‌వర్డ్ మర్చిపోయారా?",
    orContinueWith = "లేదా వీటితో కొనసాగించండి",
    dontHaveAccount = "ఖాతా లేదా?",
    signUp = "సైన్ అప్",
    startPersonalizedJourney = "ఈరోజే మీ వ్యక్తిగత ఆరోగ్య ప్రయాణాన్ని ప్రారంభించండి",
    fullName = "పూర్తి పేరు",
    phoneNumber = "ఫోన్ నంబర్",
    enterPassword = "పాస్‌వర్డ్ నమోదు చేయండి",
    weak = "బలహీనంగా ఉంది",
    fair = "పర్వాలేదు",
    good = "మంచిది",
    strong = "బలంగా ఉంది",
    enter6DigitCode = "%s కి పంపిన 6 అంకెల కోడ్‌ని నమోదు చేయండి",
    verificationCode = "ధృవీకరణ కోడ్",
    verifyContinue = "ధృవీకరించి కొనసాగించండి",
    ironDeficiency = "ఐరన్ లోపం",
    thyroidMarkers = "థైరాయిడ్ సంకేతాలు",
    fungalInfection = "ఫంగల్ ఇన్ఫెక్షన్",
    heartMarkers = "గుండె సంకేతాలు",
    iron = "ఐరన్",
    thyroid = "థైరాయిడ్",
    fungal = "ఫంగల్",
    heart = "గుండె",
    
    alopeciaAreata = "అలోపేసియా అరేటా (గోరు సంకేతాలు)",
    beausLines = "బ్యూస్ లైన్స్ (శరీర ఒత్తిడి)",
    bluishNail = "నీలం రంగు గోర్లు (సైనోసిస్)",
    clubbing = "గోరు క్లబ్బింగ్ (గుండె/ఊపిరితిత్తులు)",
    dariersDisease = "డారియర్స్ వ్యాధి",
    eczema = "గోరు ఎగ్జిమా",
    halfAndHalf = "హాఫ్ & హాఫ్ (కిడ్నీ మార్కర్)",
    healthyNail = "ఆరోగ్యవంతమైన గోర్లు",
    koilonychia = "కొయిలోనికియా (చెంచా ఆకారపు/ఐరన్)",
    leukonychia = "ల్యూకోనికియా (తెల్లని మచ్చలు)",
    melanoma = "మెలనోమా (జాగ్రత్త)",
    muehrckesLines = "ముహర్కే లైన్స్ (ప్రోటీన్ మార్కర్)",
    onychogryphosis = "గోరు వంకర్లు (రామ్స్ హార్న్)",
    onycholysis = "ఒనికోలిసిస్ (థైరాయిడ్/సోరియాసిస్)",
    onychomycosis = "ఫంగల్ ఇన్ఫెక్షన్",
    paleNail = "తెల్లని గోరు (రక్తహీనత)",
    pitting = "గోరు పట్టింగ్",
    psoriasis = "గోరు సోరియాసిస్",
    redLunula = "ఎర్రని లునులా (గుండె మార్కర్)",
    splinterHemorrhage = "స్ప్లింటర్ హెమరేజ్",
    terrysNail = "టెర్రీ గోర్లు (కాలేయ మార్కర్)",
    yellowNails = "పసుపు రంగు గోర్లు",
    invalidNailTitle = "గోరు గుర్తించబడలేదు",
    invalidNailDesc = "సిస్టమ్ ఈ చిత్రంలో సరైన మానవ గోరును గుర్తించలేకపోయింది. దయచేసి మీరు గోరు యొక్క స్పష్టమైన చిత్రాన్ని అప్‌లోడ్ చేస్తున్నారని నిర్ధారించుకోండి.",
    nonFingerDetectedTitle = "వేలు గుర్తించబడలేదు",
    nonFingerDetectedDesc = "సిస్టమ్ మానవ వేళ్లు మరియు గోళ్లను మాత్రమే విశ్లేషించగలదు. దయచేసి మీ వేలు స్పష్టంగా కనిపిస్తోందని నిర్ధారించుకోండి.",
    medicalDisclaimer = "నిరాకరణ: ఈ యాప్ విద్యా ప్రయోజనాల కోసం మాత్రమే AI-సహాయక అంతర్దృష్టులను అందిస్తుంది. ఇది వృత్తిపరమైన వైద్య సలహా, నిర్ధారణ లేదా చికిత్సకు ప్రత్యామ్నాయం కాదు. వైద్య పరిస్థితికి సంబంధించి మీకు ఏవైనా సందేహాలు ఉంటే ఎల్లప్పుడూ మీ వైద్యుని లేదా ఇతర అర్హత కలిగిన ఆరోగ్య ప్రదాత సలహాను పొందండి.",
    aiAssessmentNotice = "AI అంచనా మాత్రమే. వైద్యుడిని సంప్రదించండి."
)

val HindiStrings = AppStrings(
    welcome = "स्वागत है",
    profile = "प्रोफ़ाइल",
    login = "लॉगिन",
    register = "रजिस्टर",
    email = "ईमेल",
    password = "पासवर्ड",
    phone = "फ़ोन",
    age = "आयु",
    gender = "लिंग",
    height = "ऊंचाई",
    personalDetails = "व्यक्तिगत विवरण",
    saveChanges = "परिवर्तन सहेजें",
    logout = "लॉग आउट",
    deleteAccount = "खाता हटाएं",
    scanNow = "अभी स्कैन करें",
    exportPdf = "PDF रिपोर्ट निर्यात करें",
    downloadData = "मेरा डेटा डाउनलोड करें",
    language = "भाषा",
    selectLanguage = "भाषा चुनें",
    goodMorning = "सुप्रभात",
    goodAfternoon = "शुभ दोपहर",
    goodEvening = "शुभ संध्या",
    healthSignals = "स्वास्थ्य संकेत",
    todaysScan = "आज का स्कैन",
    lastScan = "पिछला स्कैन",
    startScan = "स्कैन शुरू करें",
    home = "होम",
    aiChat = "AI चैट",
    createAccount = "खाता बनाएं",
    signIn = "साइन इन करें",
    alreadyHaveAccount = "क्या आपके पास पहले से खाता है?",
    verifyEmail = "ईमेल सत्यापित करें",
    enterOtp = "कोड दर्ज करें",
    resendCode = "कोड पुनः भेजें",
    signInContinue = "अपनी स्वास्थ्य यात्रा जारी रखने के लिए साइन इन करें",
    forgotPassword = "पासवर्ड भूल गए?",
    orContinueWith = "या इसके साथ जारी रखें",
    dontHaveAccount = "खाता नहीं है?",
    signUp = "साइन अप करें",
    startPersonalizedJourney = "आज ही अपनी व्यक्तिगत स्वास्थ्य यात्रा शुरू करें",
    fullName = "पूरा नाम",
    phoneNumber = "फ़ोन नंबर",
    enterPassword = "पासवर्ड दर्ज करें",
    weak = "कमज़ोर",
    fair = "ठीक",
    good = "अच्छा",
    strong = "मजबूत",
    enter6DigitCode = "%s पर भेजा गया 6-अंकीय कोड दर्ज करें",
    verificationCode = "सत्यापन कोड",
    verifyContinue = "सत्यापित करें और जारी रखें",
    
    alopeciaAreata = "अलोपेसिया एरीटा (नाखून के लक्षण)",
    beausLines = "ब्यू लाइन्स (सिस्टमैटिक तनाव)",
    bluishNail = "नीले नाखून (साइनोसिस)",
    clubbing = "क्लबिंग (दिल/फेफड़ों के संकेत)",
    dariersDisease = "डारीयर्स रोग",
    eczema = "नाखून एक्जिमा",
    halfAndHalf = "हाफ एंड हाफ (किडनी मार्कर)",
    healthyNail = "स्वस्थ नाखून",
    koilonychia = "कोइलोनीचिया (चम्मच जैसे नाखून)",
    leukonychia = "ल्यूकोनीचिया (सफेद धब्बे)",
    melanoma = "मेलानोमा (सावधानी)",
    muehrckesLines = "मुहर्के लाइन्स (प्रोटीन मार्कर)",
    onychogryphosis = "ओनिकोप्रिफ़ोसिस",
    onycholysis = "ओनिकोलाइसिस (थायरॉयड/सोरायसिस)",
    onychomycosis = "फंगल इन्फेक्शन",
    paleNail = "पीले/सफेद नाखून (एनीमिया)",
    pitting = "नाखून पिटिंग",
    psoriasis = "नाखून सोरायसिस",
    redLunula = "लाल लुनुला (हृदय मार्कर)",
    splinterHemorrhage = "स्प्लिंटर हेमरेज",
    terrysNail = "टेरी नाखून (लीवर मार्कर)",
    yellowNails = "येलो नेल सिंड्रोम",
    invalidNailTitle = "नाखून की पहचान नहीं हुई",
    invalidNailDesc = "सिस्टम इस छवि में मानव नाखून का पता नहीं लगा सका। कृपया सुनिश्चित करें कि आप नाखून की स्पष्ट तस्वीर अपलोड कर रहे हैं।",
    nonFingerDetectedTitle = "उंगली की पहचान नहीं हुई",
    nonFingerDetectedDesc = "सिस्टम केवल मानव उंगलियों और नाखूनों का विश्लेषण कर सकता है। कृपया सुनिश्चित करें कि आपकी उंगली स्पष्ट रूप से दिखाई दे रही है।",
    medicalDisclaimer = "अस्वीकरण: यह ऐप केवल शैक्षिक उद्देश्यों के लिए एआई-सहायता प्राप्त जानकारी प्रदान करता है। यह पेशेवर चिकित्सा सलाह, निदान या उपचार का विकल्प नहीं है। किसी चिकित्सीय स्थिति के संबंध में किसी भी प्रश्न के लिए हमेशा अपने चिकित्सक या अन्य योग्य स्वास्थ्य प्रदाता की सलाह लें।",
    aiAssessmentNotice = "केवल एआई मूल्यांकन। डॉक्टर से परामर्श लें।"
)

val TamilStrings = AppStrings(
    welcome = "வரவேற்பு",
    profile = "சுயவிவரம்",
    login = "உள்நுழை",
    register = "பதிவு செய்",
    email = "மின்னஞ்சல்",
    password = "கடவுச்சொல்",
    phone = "தொலைபேசி",
    age = "வயது",
    gender = "பாலினம்",
    height = "உயரம்",
    personalDetails = "தனிப்பட்ட விவரங்கள்",
    saveChanges = "மாற்றங்களைச் சேமி",
    logout = "வெளியேறு",
    deleteAccount = "கணக்கை நீக்கு",
    scanNow = "ஸ்கேன் செய்",
    exportPdf = "PDF அறிக்கையை ஏற்றுமதி செய்",
    downloadData = "எனது தரவைப் பதிவிறக்கு",
    language = "மொழி",
    selectLanguage = "மொழியைத் தேர்ந்தெடுக்கவும்",
    goodMorning = "காலை வணக்கம்",
    goodAfternoon = "மதிய வணக்கம்",
    goodEvening = "மாலை வணக்கம்",
    healthSignals = "சுகாதார சமிக்ஞைகள்",
    todaysScan = "இன்றைய ஸ்கேன்",
    lastScan = "கடைசி ஸ்கேன்",
    startScan = "ஸ்கேன் தொடங்கவும்",
    home = "முகப்பு",
    aiChat = "AI சாட்",
    createAccount = "கணக்கை உருவாக்கு",
    signIn = "உள்நுழைய",
    alreadyHaveAccount = "ஏற்கனவே கணக்கு உள்ளதா?",
    verifyEmail = "மின்னஞ்சலை சரிபார்க்கவும்",
    enterOtp = "குறியீட்டை உள்ளிடவும்",
    resendCode = "குறியீட்டை மீண்டும் அனுப்ப",
    signInContinue = "உங்கள் ஆரோக்கிய பயணத்தைத் தொடர உள்நுழைக",
    forgotPassword = "கடவுச்சொல்லை மறந்துவிட்டீர்களா?",
    orContinueWith = "அல்லது இதனுடன் தொடரவும்",
    dontHaveAccount = "கணக்கு இல்லையா?",
    signUp = "பதிவு செய்",
    startPersonalizedJourney = "இன்று உங்கள் தனிப்பயனாக்கப்பட்ட சுகாதார பயணத்தைத் தொடங்குங்கள்",
    fullName = "முழு பெயர்",
    phoneNumber = "தொலைபேசி எண்",
    enterPassword = "கடவுச்சொல்லை உள்ளிடவும்",
    weak = "பலவீனமானது",
    fair = "பரவாயில்லை",
    good = "நல்லது",
    strong = "வலிமையானது",
    enter6DigitCode = "%s க்கு அனுப்பப்பட்ட 6 இலக்கக் குறியீட்டை உள்ளிடவும்",
    verificationCode = "சரிபார்ப்புக் குறியீடு",
    verifyContinue = "சரிபார்த்து தொடரவும்",

    alopeciaAreata = "அலோபீசியா அரேட்டா (நக மாற்றங்கள்)",
    beausLines = "பியூஸ் வரிகள் (உடல் மன அழுத்தம்)",
    bluishNail = "நீல நிற நகங்கள் (சயனோசிஸ்)",
    clubbing = "நக கிளபிங் (இதய/நுரையீரல் அறிகுறி)",
    dariersDisease = "டேரியர் நோய்",
    eczema = "நக எக்ஸிமா",
    halfAndHalf = "பாதி மற்றும் பாதி (சிறுநீரக அறிகுறி)",
    healthyNail = "ஆரோக்கியமான நகங்கள்",
    koilonychia = "கொய்லோனிச்சியா (ஸ்பூன் நகங்கள்/இரும்பு)",
    leukonychia = "லுகோனிச்சியா (வெள்ளை புள்ளிகள்)",
    melanoma = "மெலனோமா (எச்சரிக்கை)",
    muehrckesLines = "முஹர்கே கோடுகள் (புரத அறிகுறி)",
    onychogryphosis = "நக தடிப்பு (ராம்ஸ் ஹார்ன்)",
    onycholysis = "நகப் பிரிப்பு (தைராய்டு/சோரியாசிஸ்)",
    onychomycosis = "நக பூஞ்சை தொற்று",
    paleNail = "வெளிறிய நகங்கள் (இரத்த சோகை)",
    pitting = "நகப் பள்ளங்கள்",
    psoriasis = "நக சோரியாசிஸ்",
    redLunula = "சிவப்பு லுனுலா (இதய அறிகுறி)",
    splinterHemorrhage = "ஸ்ப்ளிண்டர் ரத்தக்கசிவு",
    terrysNail = "டெர்ரி நகங்கள் (கல்லீரல் அறிகுறி)",
    yellowNails = "மஞ்சள் நக நோய்க்குறி",
    invalidNailTitle = "நகத்தைக் கண்டறிய முடியவில்லை",
    invalidNailDesc = "இந்த படத்தில் மனித நகத்தை சிஸ்டத்தால் கண்டறிய முடியவில்லை. நகத்தின் தெளிவான புகைப்படத்தை பதிவேற்றுவதை உறுதி செய்யவும்.",
    nonFingerDetectedTitle = "விரலைக் கண்டறிய முடியவில்லை",
    nonFingerDetectedDesc = "கணினி மனித விரல்களையும் நகங்களையும் மட்டுமே ஆய்வு செய்ய முடியும். உங்கள் விரல் தெளிவாகத் தெரிவதை உறுதி செய்யவும்.",
    medicalDisclaimer = "பொறுப்புத் துறப்பு: இந்த பயன்பாடு கல்வி நோக்கங்களுக்காக மட்டுமே AI- உதவியுடனான தகவல்களை வழங்குகிறது. இது தொழில்முறை மருத்துவ ஆலோசனை, நோயறிதல் அல்லது சிகிச்சைக்கு மாற்றாகாது. மருத்துவ நிலை குறித்தான எந்தவொரு கேள்விகளுக்கும் எப்போதும் உங்கள் மருத்துவரை அணுகவும்.",
    aiAssessmentNotice = "AI மதிப்பீடு மட்டுமே. மருத்துவரை அணுகவும்."
)

class LanguageManager(context: Context) {
    private val prefs = context.getSharedPreferences("nailvital_langs", Context.MODE_PRIVATE)
    
    private val _currentLanguage = mutableStateOf(loadLanguage())
    val currentLanguage: Language get() = _currentLanguage.value
    
    private val _strings = mutableStateOf(getStrings(currentLanguage))
    val strings: AppStrings get() = _strings.value

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
        _strings.value = getStrings(language)
        prefs.edit().putString("lang_code", language.code).apply()
    }

    private fun loadLanguage(): Language {
        val code = prefs.getString("lang_code", "en")
        return Language.values().find { it.code == code } ?: Language.ENGLISH
    }

    private fun getStrings(language: Language): AppStrings {
        return when (language) {
            Language.ENGLISH -> EnglishStrings
            Language.TELUGU -> TeluguStrings
            Language.HINDI -> HindiStrings
            Language.TAMIL -> TamilStrings
        }
    }

    fun getConditionName(classId: String): String {
        return when (classId) {
            "aloperia_areata" -> strings.alopeciaAreata
            "beaus_lines" -> strings.beausLines
            "bluish_nail" -> strings.bluishNail
            "clubbing" -> strings.clubbing
            "dariers_disease" -> strings.dariersDisease
            "eczema" -> strings.eczema
            "half_and_half_nails" -> strings.halfAndHalf
            "healthy" -> strings.healthyNail
            "koilonychia" -> strings.koilonychia
            "leukonychia" -> strings.leukonychia
            "melanoma" -> strings.melanoma
            "muehrckes_lines" -> strings.muehrckesLines
            "onychogryphosis" -> strings.onychogryphosis
            "onycholycis" -> strings.onycholysis
            "onychomycosis" -> strings.onychomycosis
            "pale_nail" -> strings.paleNail
            "pitting" -> strings.pitting
            "psoriasis" -> strings.psoriasis
            "red_lunula" -> strings.redLunula
            "splinter_hemorrhage" -> strings.splinterHemorrhage
            "terrys_nail" -> strings.terrysNail
            "yellow_nails" -> strings.yellowNails
            else -> classId.replace("_", " ").capitalize()
        }
    }

    fun getConditionIcon(classId: String): ImageVector {
        return when (classId) {
            "aloperia_areata" -> Icons.Rounded.AccountCircle
            "beaus_lines" -> Icons.Rounded.TrendingDown
            "bluish_nail" -> Icons.Rounded.AcUnit
            "clubbing" -> Icons.Rounded.FiberManualRecord
            "dariers_disease" -> Icons.Rounded.Science
            "eczema" -> Icons.Rounded.Spa
            "half_and_half_nails" -> Icons.Rounded.Contrast
            "healthy" -> Icons.Rounded.CheckCircle
            "koilonychia" -> Icons.Rounded.Eject
            "leukonychia" -> Icons.Rounded.BlurCircular
            "melanoma" -> Icons.Rounded.Warning
            "muehrckes_lines" -> Icons.Rounded.LineWeight
            "onychogryphosis" -> Icons.Rounded.Gavel
            "onycholycis" -> Icons.Rounded.ContentCopy
            "onychomycosis" -> Icons.Rounded.BubbleChart
            "pale_nail" -> Icons.Rounded.Cloud
            "pitting" -> Icons.Rounded.Grain
            "psoriasis" -> Icons.Rounded.Waves
            "red_lunula" -> Icons.Rounded.Brightness1
            "splinter_hemorrhage" -> Icons.Rounded.FormatListBulleted
            "terrys_nail" -> Icons.Rounded.SettingsBrightness
            "yellow_nails" -> Icons.Rounded.WarningAmber
            else -> Icons.Rounded.HealthAndSafety
        }
    }
}
