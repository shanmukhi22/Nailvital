package com.nailvital.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.nailvital.app.R

@Composable
fun SplashScreen(onNavigateToGettingStarted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { LanguageManager(context) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val loaderProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "loader"
    )

    LaunchedEffect(Unit) {
        delay(2500)
        onNavigateToGettingStarted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg),
        contentAlignment = Alignment.Center
    ) {
        // Enhanced Radial Glow
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(
                colors = listOf(MintColor.copy(alpha = 0.15f), Color.Transparent),
                radius = 1200f
            ))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "NailVital Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = buildAnnotatedString {
                    append("Nail")
                    withStyle(style = SpanStyle(color = MintColor)) {
                        append("Vital")
                    }
                    append(" AI")
                },
                color = DeepColor,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = langManager.strings.precisionDiagnostics.uppercase(),
                color = MutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Animated Loader bar
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .background(SlateColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(loaderProgress)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(listOf(MintColor.copy(alpha = 0.5f), MintColor)),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
