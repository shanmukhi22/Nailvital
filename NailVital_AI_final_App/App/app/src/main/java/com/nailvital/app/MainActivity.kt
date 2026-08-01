package com.nailvital.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.nailvital.app.ui.screens.*
import com.nailvital.app.voice.VoiceAction
import com.nailvital.app.voice.VoiceAssistantManager
import com.nailvital.app.voice.LocalVoiceActions

class MainActivity : ComponentActivity() {

    private lateinit var voiceManager: VoiceAssistantManager

    // Permission launcher
    private val recordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voiceManager.startListening()
    }

    fun requestMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED -> voiceManager.toggleListening()
            else -> recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceManager = VoiceAssistantManager(this)
        setContent {
            NailVitalApp(voiceManager = voiceManager, onRequestMic = ::requestMicAndListen)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

@Composable
fun NailVitalApp(voiceManager: VoiceAssistantManager, onRequestMic: () -> Unit) {
    val navController = rememberNavController()
    val voiceState by voiceManager.voiceState.collectAsState()

    // Wire all voice actions to navController globally
    LaunchedEffect(Unit) {
        voiceManager.voiceActions.collect { action ->
            handleVoiceAction(action, navController, speak = { voiceManager.speak(it) })
        }
    }

    NailVitalAITheme {
        CompositionLocalProvider(LocalVoiceActions provides voiceManager.voiceActions) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DeepColor
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)) },
                        exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)) },
                        popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)) },
                        popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)) }
                    ) {
                        composable("splash") {
                            SplashScreen(onNavigateToGettingStarted = {
                                navController.navigate("getting_started") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }

                        composable("getting_started") {
                            GettingStartedScreen(onNavigateToLogin = {
                                navController.navigate("login")
                            })
                        }

                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToForgot = { navController.navigate("forgot_password") },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onGuestLogin = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.navigate("login") },
                                onNavigateToOtp = { email ->
                                    navController.navigate("otp/$email")
                                },
                                onBack = { navController.popBackStack() },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("otp/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            OtpScreen(
                                email = email,
                                onVerifySuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("forgot_password") {
                            ForgotPasswordScreen(
                                onNavigateToReset = { email -> navController.navigate("reset_password/$email") },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("reset_password/{email}") { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email") ?: ""
                            ResetPasswordScreen(
                                email = email,
                                onResetSuccess = {
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onNavigateToScan = { navController.navigate("scan") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToWiki = { navController.navigate("health_wiki") },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("chat") {
                            ChatScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToScan = { navController.navigate("scan") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onHomeClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("history?disease={disease}") { backStackEntry ->
                            val diseaseToExpand = backStackEntry.arguments?.getString("disease")
                            HistoryScreen(
                                onBack = { navController.popBackStack() },
                                onNavigateToScan = { navController.navigate("scan") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onHomeClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic,
                                initialExpandDisease = diseaseToExpand
                            )
                        }

                        composable("scan") {
                            ScanScreen(
                                onBack = { navController.popBackStack() },
                                onScanComplete = {
                                    navController.navigate("history") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic,
                                onSpeak = { voiceManager.speak(it) }
                            )
                        }

                        composable("profile") {
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onNavigateToPersonalDetails = { navController.navigate("personal_details") },
                                onNavigateToChangePassword = { navController.navigate("change_password") },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("change_password") {
                            ChangePasswordScreen(
                                onBack = { navController.popBackStack() },
                                onChangeSuccess = { navController.popBackStack() },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("personal_details") {
                            PersonalDetailsScreen(
                                onBack = { navController.popBackStack() },
                                onUpdateSuccess = { navController.popBackStack() },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }

                        composable("health_wiki") {
                            HealthWikiScreen(
                                onBack = { navController.popBackStack() },
                                voiceState = voiceState,
                                onVoiceMicClick = onRequestMic
                            )
                        }
                    }

                    // Global Voice Feedback Overlay
                    val lastText by voiceManager.lastHeardText.collectAsState()
                    VoiceOverlay(
                        state = voiceState,
                        text = lastText,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/** Central dispatcher — maps VoiceAction to navController calls */
fun handleVoiceAction(action: VoiceAction, nav: NavController, speak: (String) -> Unit) {
    val authRoutes = listOf("login", "register", "forgot_password", "otp", "reset_password", "splash", "getting_started")
    val currentRoute = nav.currentBackStackEntry?.destination?.route ?: ""
    val isAuthFlow = authRoutes.any { currentRoute.contains(it) }

    when (action) {
        is VoiceAction.GoHome -> {
            if (isAuthFlow) {
                speak("Please sign in to access the home page.")
            } else {
                nav.navigate("home") { popUpTo("home") { inclusive = true } }
            }
        }
        is VoiceAction.GoScan -> {
            if (isAuthFlow) speak("Please sign in first.") else nav.navigate("scan")
        }
        is VoiceAction.GoHistory -> {
            if (isAuthFlow) speak("Please sign in to view your history.") else nav.navigate("history")
        }
        is VoiceAction.GoChat -> {
            if (isAuthFlow) speak("Please sign in to use the AI assistant.") else nav.navigate("chat")
        }
        is VoiceAction.GoProfile -> {
            if (isAuthFlow) speak("Please sign in to see your profile.") else nav.navigate("profile")
        }
        is VoiceAction.GoWiki -> {
            nav.navigate("health_wiki") // Wiki is public
        }
        is VoiceAction.GoBack       -> nav.popBackStack()
        is VoiceAction.GoLogin      -> nav.navigate("login")
        is VoiceAction.GoRegister   -> nav.navigate("register")
        is VoiceAction.GoForgotPass -> nav.navigate("forgot_password")
        is VoiceAction.Logout       -> nav.navigate("login") { popUpTo(0) { inclusive = true } }
        is VoiceAction.GoPersonalDetails -> nav.navigate("personal_details")
        is VoiceAction.GoChangePassword  -> nav.navigate("change_password")
        is VoiceAction.OpenHelp          -> { /* handled per-screen via showHelpDialog */ }
        is VoiceAction.GoAbout           -> { 
            if (nav.currentDestination?.route != "home") {
                nav.navigate("home")
            }
        }
        is VoiceAction.LoginGuest        -> {
            nav.navigate("home") { popUpTo(0) { inclusive = true } }
        }
        is VoiceAction.GoToDisease       -> {
            if (isAuthFlow) speak("Please sign in first.") else nav.navigate("history?disease=${action.name}")
        }
        // Scroll/Photo/Report are handled inside individual screens via their own lambdas
        else -> { /* no-op for actions the nav layer doesn't handle directly */ }
    }
}

@Composable
fun VoiceOverlay(state: com.nailvital.app.voice.VoiceState, text: String, modifier: Modifier = Modifier) {
    val isVisible = state != com.nailvital.app.voice.VoiceState.IDLE
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, 
                when(state) {
                    com.nailvital.app.voice.VoiceState.LISTENING -> MintColor
                    com.nailvital.app.voice.VoiceState.PROCESSING -> Color.Cyan
                    com.nailvital.app.voice.VoiceState.SPEAKING -> Color.White
                    else -> WarnColor
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pulsating indicator
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(if (state == com.nailvital.app.voice.VoiceState.LISTENING) scale else 1f)
                        .background(
                            when(state) {
                                com.nailvital.app.voice.VoiceState.LISTENING -> MintColor
                                com.nailvital.app.voice.VoiceState.PROCESSING -> Color.Cyan
                                com.nailvital.app.voice.VoiceState.SPEAKING -> Color.White
                                else -> WarnColor
                            },
                            CircleShape
                        )
                )
                
                Column {
                    Text(
                        text = when(state) {
                            com.nailvital.app.voice.VoiceState.LISTENING -> "LISTENING…"
                            com.nailvital.app.voice.VoiceState.PROCESSING -> "THINKING…"
                            com.nailvital.app.voice.VoiceState.SPEAKING -> "RESPONDING…"
                            else -> "ERROR"
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (text.isBlank()) "Say something…" else "\"$text\"",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NailVitalAITheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = com.nailvital.app.ui.screens.DeepColor,
            primary = com.nailvital.app.ui.screens.MintColor,
            onPrimary = Color.Black
        ),
        content = content
    )
}
