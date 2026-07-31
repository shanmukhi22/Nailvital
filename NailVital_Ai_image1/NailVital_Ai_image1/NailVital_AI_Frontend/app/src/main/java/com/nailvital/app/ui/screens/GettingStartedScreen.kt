package com.nailvital.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailvital.app.R

data class IntroSlide(
    val imageRes: Int,
    val title: String,
    val sub: String
)

@Composable
fun GettingStartedScreen(onNavigateToLogin: () -> Unit) {
    val onboardingSlides = listOf(
        IntroSlide(R.drawable.intro_1, "Analyze in Seconds", "Our AI identifies texture, color, and anomalies instantly from any clear photo — no extra hardware required."),
        IntroSlide(R.drawable.intro_2, "Monitor Your Health", "Track your wellness patterns over time with intuitive visual trends and clinical insights."),
        IntroSlide(R.drawable.intro_3, "Detection is Prevention", "Screen for 20+ underlying signals early, identifying potential nutrition or systemic markers.")
    )
    
    var currentSlide by remember { mutableIntStateOf(0) }
    var isDisclaimerAccepted by remember { androidx.compose.runtime.mutableStateOf(false) }
    val slide = onboardingSlides[currentSlide]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "SKIP",
                color = MutedColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToLogin() }
                    .padding(8.dp)
            )
        }

        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = {
                (scaleIn(initialScale = 0.9f, animationSpec = tween(200)) + fadeIn(tween(200)))
                    .togetherWith(scaleOut(targetScale = 1.1f, animationSpec = tween(200)) + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "slide_animation"
        ) { targetSlideIndex ->
            val slide = onboardingSlides[targetSlideIndex]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Illustration Area
                Box(
                    modifier = Modifier.size(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(280.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = iOSCard,
                        shadowElevation = 8.dp
                    ) {
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = slide.title,
                    color = DeepColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    letterSpacing = -1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = slide.sub,
                    color = MutedColor,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
        
        // Indicators
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in onboardingSlides.indices) {
                    val width by animateDpAsState(
                        targetValue = if (i == currentSlide) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(width)
                            .background(if (i == currentSlide) MintColor else SlateColor, CircleShape)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val langManager = remember { LanguageManager(context) }
            
            if (currentSlide == onboardingSlides.size - 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { isDisclaimerAccepted = !isDisclaimerAccepted }
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isDisclaimerAccepted,
                        onCheckedChange = { isDisclaimerAccepted = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = MintColor)
                    )
                    Text(
                        text = "I acknowledge that NailVital AI provides unverified insights and is not a substitute for professional medical advice.",
                        color = DeepColor,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            PrimaryButton(
                text = if (currentSlide == onboardingSlides.size - 1) "START YOUR JOURNEY" else "NEXT",
                onClick = {
                    if (currentSlide < onboardingSlides.size - 1) {
                        currentSlide++
                    } else {
                        onNavigateToLogin()
                    }
                },
                enabled = currentSlide < onboardingSlides.size - 1 || isDisclaimerAccepted
            )

            // General Disclaimer for previous slides
            if (currentSlide < onboardingSlides.size - 1) {
                Text(
                    text = langManager.strings.medicalDisclaimer,
                    color = MutedColor.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
