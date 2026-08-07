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

class MainActivity : ComponentActivity() {



    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NailVitalApp()
        }
    }

}

@Composable
fun NailVitalApp() {
    val navController = rememberNavController()


    NailVitalAITheme {
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
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onNavigateToLogin = { navController.navigate("login") },
                            onNavigateToOtp = { email ->
                                navController.navigate("otp/$email")
                            },
                            onBack = { navController.popBackStack() },
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
                        )
                    }

                    composable("change_password") {
                        ChangePasswordScreen(
                            onBack = { navController.popBackStack() },
                            onChangeSuccess = { navController.popBackStack() },
                        )
                    }

                    composable("personal_details") {
                        PersonalDetailsScreen(
                            onBack = { navController.popBackStack() },
                            onUpdateSuccess = { navController.popBackStack() },
                        )
                    }

                    composable("health_wiki") {
                        HealthWikiScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }

                // Global Voice Feedback Overlay
                VoiceOverlay(
                    text = lastText,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    
    }
}

