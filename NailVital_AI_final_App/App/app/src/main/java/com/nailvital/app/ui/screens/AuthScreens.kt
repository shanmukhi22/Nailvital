package com.nailvital.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.SessionManager
import kotlinx.coroutines.launch
import com.nailvital.app.voice.VoiceState

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    onGuestLogin: () -> Unit = {},
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 400)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 400))
                }
                is com.nailvital.app.voice.VoiceAction.LoginGuest -> {
                    onGuestLogin()
                }
                else -> {}
            }
        }
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 56.dp, start = 28.dp, end = 28.dp)
        ) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "${strings.welcome} Back",
                color = DeepColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = strings.signInContinue,
                color = MutedColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(32.dp, 28.dp, 32.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = strings.email,
                placeholder = "you@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage != null
            )

            AuthTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = strings.password,
                placeholder = "••••••••",
                isPassword = true,
                isError = errorMessage != null,
                errorMessage = errorMessage
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = strings.forgotPassword,
                    color = MintColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateToForgot() }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(28.dp, 20.dp, 28.dp, 44.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
            } else {
                PrimaryButton(
                    text = strings.signIn.uppercase(),
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val response = ApiClient.instance.login(email, password)
                                sessionManager.saveAuthToken(response.access_token)
                                sessionManager.saveUserDetails(response.user.name, response.user.email)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                errorMessage = "Login failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )
                
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${strings.dontHaveAccount} ", color = MutedColor, fontSize = 14.sp)
                Text(
                    strings.signUp,
                    color = MintColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            // Medical Disclaimer
            Text(
                text = strings.medicalDisclaimer,
                color = MutedColor.copy(alpha = 0.5f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    } // end Column

    } // end Box
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    onBack: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 400)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 400))
                }
                else -> {}
            }
        }
    }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var showAlreadyRegisteredDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    // ── Derived validation states ──
    val isNameValid = name.trim().length >= 2 && name.trim().matches(Regex("^[A-Za-z .]+$"))
    val isPhoneValid = phone.length == 10 && phone.first().toString().matches(Regex("[6-9]"))
    val phoneHintColor = when {
        phone.isEmpty() -> MutedColor
        !phone.first().toString().matches(Regex("[6-9]")) -> WarnColor
        phone.length < 10 -> GoldColor
        else -> MintColor
    }
    val phoneHintText = when {
        phone.isEmpty() -> "10 digits, starting with 6, 7, 8 or 9"
        !phone.first().toString().matches(Regex("[6-9]")) -> "⚠ Must start with 6, 7, 8 or 9"
        phone.length < 10 -> "${10 - phone.length} more digit${if (10 - phone.length != 1) "s" else ""} needed"
        else -> "✓ Valid phone number"
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings

    // Simulated Password Strength
    val score = remember(password) {
        var s = 0
        if (password.length >= 8) s++
        if (password.any { it.isUpperCase() }) s++
        if (password.any { it.isDigit() }) s++
        if (password.any { !it.isLetterOrDigit() }) s++
        s
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 56.dp, start = 28.dp, end = 28.dp)
        ) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = strings.createAccount,
                color = DeepColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = strings.startPersonalizedJourney,
                color = MutedColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(28.dp, 32.dp, 28.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                Box(modifier = Modifier.weight(1f).height(4.dp).background(MintColor, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(SlateColor, RoundedCornerShape(2.dp)))
                Box(modifier = Modifier.weight(1f).height(4.dp).background(SlateColor, RoundedCornerShape(2.dp)))
            }

            // ── Full Name Field (letters, spaces and dots only) ──
            Column {
                AuthTextField(
                    value = name,
                    onValueChange = { raw ->
                        name = raw
                        nameError = when {
                            raw.isEmpty() -> null
                            raw.contains(Regex("[^A-Za-z .]")) -> "⚠ Numbers and symbols are not allowed"
                            raw.trim().length < 2 -> "⚠ Name must be at least 2 characters"
                            else -> null
                        }
                        errorMessage = null
                    },
                    label = strings.fullName,
                    placeholder = "Priya Sharma",
                    isError = nameError != null
                )
                Text(
                    text = if (nameError != null) nameError!! else "Only letters, spaces and dots allowed",
                    color = if (nameError != null) WarnColor else MutedColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                )
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = strings.email,
                placeholder = "you@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // ── Phone Number Field (10 digits, starts with 6–9) ──
            Column {
                AuthTextField(
                    value = phone,
                    onValueChange = { raw ->
                        // Keep digits only, max 10
                        var digits = raw.replace(Regex("[^0-9]"), "")
                        if (digits.isNotEmpty() && !digits.first().toString().matches(Regex("[6-9]"))) {
                            digits = digits.replace(Regex("^[0-5]+"), "")
                            phoneError = "⚠ Phone number must start with 6, 7, 8 or 9"
                        } else {
                            phoneError = null
                        }
                        phone = digits.take(10)
                        errorMessage = null
                    },
                    label = strings.phoneNumber,
                    placeholder = "e.g. 9876543210",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = phoneError != null
                )
                Text(
                    text = if (phoneError != null) phoneError!! else phoneHintText,
                    color = if (phoneError != null) WarnColor else phoneHintColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 3.dp)
                )
            }
            
            Column {
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = strings.password,
                    placeholder = "Min. 8 characters",
                    isPassword = true
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                // Strength Bar
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(SlateColor, RoundedCornerShape(2.dp))) {
                    val color = when(score) {
                        1 -> WarnColor
                        2 -> GoldColor
                        3, 4 -> MintColor
                        else -> Color.Transparent
                    }
                    val widthFactor = score / 4f
                    if (score > 0) {
                        Box(modifier = Modifier.fillMaxWidth(widthFactor).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
                    }
                }
                Text(
                    text = when(score) {
                        0 -> strings.enterPassword
                        1 -> strings.weak
                        2 -> strings.fair
                        3 -> strings.good
                        4 -> strings.strong
                        else -> ""
                    },
                    color = MutedColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(28.dp, 20.dp, 28.dp, 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { termsAccepted = !termsAccepted }
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MintColor)
                    )
                    Text("I agree to the ", color = MutedColor, fontSize = 11.sp)
                    Text("Terms of Service", color = MintColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showTermsDialog = true })
                    Text(" & ", color = MutedColor, fontSize = 11.sp)
                    Text("Privacy Policy", color = MintColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { showPrivacyDialog = true })
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            if (isLoading) {
                CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
            } else {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = WarnColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                PrimaryButton(
                    text = strings.createAccount.uppercase(),
                    onClick = {
                        // ── Pre-submission validation ──
                        var hasError = false

                        if (!isNameValid) {
                            nameError = when {
                                name.isBlank() -> "⚠ Full name is required"
                                name.trim().length < 2 -> "⚠ Name must be at least 2 characters"
                                name.trim().contains(Regex("[^A-Za-z .]")) -> "⚠ Numbers and symbols are not allowed"
                                else -> "⚠ Invalid name"
                            }
                            hasError = true
                        }

                        if (!isPhoneValid) {
                            phoneError = when {
                                phone.isBlank() -> "⚠ Phone number is required"
                                !phone.first().toString().matches(Regex("[6-9]")) -> "⚠ Must start with 6, 7, 8 or 9"
                                phone.length != 10 -> "⚠ Must be exactly 10 digits (entered ${phone.length})"
                                else -> "⚠ Invalid phone number"
                            }
                            hasError = true
                        }

                        if (hasError) return@PrimaryButton

                        isLoading = true
                        scope.launch {
                            try {
                                val user = mapOf(
                                    "name" to name.trim(),
                                    "email" to email,
                                    "phone" to phone,
                                    "password" to password
                                )
                                ApiClient.instance.register(user)
                                onNavigateToOtp(email)
                            } catch (e: Exception) {
                                if (e.message?.contains("400") == true || e.message?.contains("already", ignoreCase = true) == true) {
                                    showAlreadyRegisteredDialog = true
                                } else {
                                    errorMessage = "Registration failed: ${e.message}"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = termsAccepted && score >= 3 && isNameValid && email.contains("@") && isPhoneValid
                )
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text("${strings.alreadyHaveAccount} ", color = MutedColor, fontSize = 13.sp)
                Text(
                    strings.signIn,
                    color = MintColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            // Medical Disclaimer
            Text(
                text = strings.medicalDisclaimer,
                color = MutedColor.copy(alpha = 0.5f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }

    if (showAlreadyRegisteredDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyRegisteredDialog = false },
            title = { Text(strings.accountExists, color = DeepColor, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = { Text(strings.emailAlreadyRegistered, color = MutedColor, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { 
                    showAlreadyRegisteredDialog = false
                    onNavigateToLogin()
                }) {
                    Text(strings.signIn.uppercase(), color = MintColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = iOSCard
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", color = DeepColor, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "1. Data Collection\nWe collect images and diagnostic metadata exclusively to generate your personalized clinical reports.\n\n" +
                        "2. Data Security\nYour images are processed securely. We implement industry-standard encryption.\n\n" +
                        "3. Your Rights\nYou can delete your account and all associated health data at any time from the Profile tab.",
                        color = MutedColor, fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                PrimaryButton(text = "I UNDERSTAND", onClick = { showPrivacyDialog = false })
            },
            containerColor = iOSCard
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service", color = DeepColor, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "1. Medical Disclaimer\nNailVital AI is an informational tool and does NOT substitute professional medical advice.\n\n" +
                        "2. Acceptable Use\nYou agree not to misuse this service or attempt to reverse-engineer the clinical models.\n\n" +
                        "3. Liability\nWe are not liable for any misdiagnosis. Always consult a certified dermatologist.",
                        color = MutedColor, fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                PrimaryButton(text = "I AGREE", onClick = { showTermsDialog = false })
            },
            containerColor = iOSCard
        )
    }


    } // end Box
} // end RegisterScreen

@Composable
fun OtpScreen(
    email: String,
    onVerifySuccess: () -> Unit,
    onBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(top = 56.dp, start = 28.dp, end = 28.dp)
        ) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = strings.verifyEmail,
                color = DeepColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "${strings.enterOtp} $email",
                color = MutedColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(32.dp, 40.dp, 32.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it; errorMessage = null },
                label = strings.verificationCode,
                placeholder = "000000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = errorMessage != null,
                errorMessage = errorMessage
            )

            Text(
                text = strings.resendCode,
                color = MintColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    scope.launch {
                        try {
                            ApiClient.instance.resendOtp(email)
                        } catch (e: Exception) {
                            // Silently fail or show toast
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(28.dp, 20.dp, 28.dp, 44.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
                }
            } else {
                PrimaryButton(
                    text = strings.verifyContinue.uppercase(),
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val response = ApiClient.instance.verifyOtp(email, otp)
                                sessionManager.saveAuthToken(response.access_token)
                                sessionManager.saveUserDetails(response.user.name, response.user.email)
                                onVerifySuccess()
                            } catch (e: Exception) {
                                errorMessage = "Verification failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = otp.length == 6
                )
            }
        }
    }
}
@Composable
fun ForgotPasswordScreen(
    onNavigateToReset: (String) -> Unit,
    onBack: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 400)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 400))
                }
                else -> {}
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(top = 56.dp, start = 28.dp, end = 28.dp)
        ) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = strings.forgotPassword,
                color = DeepColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Enter your email to receive a password reset code.",
                color = MutedColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(32.dp, 40.dp, 32.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = strings.email,
                placeholder = "you@email.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage != null,
                errorMessage = errorMessage
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(28.dp, 20.dp, 28.dp, 44.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
                }
            } else {
                PrimaryButton(
                    text = "SEND RESET CODE",
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                ApiClient.instance.forgotPassword(email)
                                onNavigateToReset(email)
                            } catch (e: Exception) {
                                errorMessage = "Failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = email.contains("@") && email.contains(".")
                )
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    email: String,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 400)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 400))
                }
                else -> {}
            }
        }
    }

    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(top = 56.dp, start = 28.dp, end = 28.dp)
        ) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Reset Password",
                color = DeepColor,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Enter the code sent to $email and your new password.",
                color = MutedColor,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MintColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MintColor, modifier = Modifier.size(16.dp))
                    Text(
                        "Tip: Use at least 8 characters with a mix of letters, numbers, and symbols for a strong password.",
                        color = DeepColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(32.dp, 32.dp, 32.dp, 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it; errorMessage = null },
                label = strings.verificationCode,
                placeholder = "000000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            AuthTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                label = "NEW PASSWORD",
                placeholder = "••••••••",
                isPassword = true,
                isError = errorMessage != null,
                errorMessage = errorMessage
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(28.dp, 20.dp, 28.dp, 44.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
                }
            } else {
                PrimaryButton(
                    text = "RESET PASSWORD",
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                ApiClient.instance.resetPassword(email, otp, newPassword)
                                onResetSuccess()
                            } catch (e: Exception) {
                                errorMessage = "Failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = otp.length == 6 && newPassword.length >= 8
                )
            }
        }
    }
}
