package com.nailvital.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Calendar
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.ScanResponse
import com.nailvital.app.api.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

data class HealthSignal(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val name: String,
    val risk: String, // Low, Medium, High
    val percentage: Int,
    val color: Color
)

@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToWiki: () -> Unit = {}
) {
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var history by remember { mutableStateOf<List<ScanResponse>>(emptyList()) }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isGuest by remember { mutableStateOf(false) }
    var showGuestBanner by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    try {
                        val token = sessionManager.fetchAuthToken()
                        if (token != null) {
                            // Only show loading if we don't have data already to prevent flicker on resume
                            if (userName.isEmpty() || history.isEmpty()) {
                                isLoading = true
                            }
                            isGuest = false
                            
                            // Parallelize calls for maximum speed
                            val profileDeferred = async { ApiClient.instance.getProfile("Bearer $token") }
                            val historyDeferred = async { ApiClient.instance.getHistory("Bearer $token", limit = 10) }
                            
                            val profile = profileDeferred.await()
                            userName = profile.name
                            history = historyDeferred.await()
                        } else {
                            isGuest = true
                            userName = "Guest"
                            history = emptyList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val dynamicSignals = remember(history, strings) {
        val lastScan = history.firstOrNull()
        val findings = lastScan?.findings ?: emptyList()
        val detectedClassIds = findings.map { it.result_class }.toSet()
        
        listOf(
            "aloperia_areata", "beaus_lines", "bluish_nail", "clubbing", 
            "dariers_disease", "eczema", "half_and_half_nails", "healthy", 
            "koilonychia", "leukonychia", "melanoma", "muehrckes_lines", 
            "onychogryphosis", "onycholycis", "onychomycosis", "pale_nail", 
            "pitting", "psoriasis", "red_lunula", "splinter_hemorrhage", 
            "terrys_nail", "yellow_nails"
        ).map { classId ->
            val finding = findings.find { it.result_class == classId }
            val isDetected = classId in detectedClassIds
            val confidence = if (isDetected) (finding?.confidence ?: 5f).toInt() else 5
            
            val (risk, color) = when {
                !isDetected -> "Low" to MintColor
                confidence < 20 -> "Low" to MintColor
                confidence < 40 -> "Medium" to GoldColor
                confidence < 70 -> "High" to androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
                else -> "Very High" to WarnColor
            }

            HealthSignal(
                icon = langManager.getConditionIcon(classId),
                name = langManager.getConditionName(classId),
                risk = risk,
                percentage = confidence,
                color = color
            )
        }
    }

    val greeting = remember(strings) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> strings.goodMorning
            in 12..16 -> strings.goodAfternoon
            else -> strings.goodEvening
        }
    }

    Scaffold(
        containerColor = iOSBg,
        floatingActionButton = {
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "home",
                onHomeClick = { },
                onHistoryClick = onNavigateToHistory,
                onScanClick = onNavigateToScan,
                onChatClick = onNavigateToChat,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { padding ->
        if (showHelpDialog) {
            HelpDialog(strings, onDismiss = { showHelpDialog = false })
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        color = MutedColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (userName.isNotEmpty() && userName != "Guest") userName else "Discover",
                        color = DeepColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { showHelpDialog = true },
                        color = iOSCard,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Help",
                                tint = MintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = { onNavigateToProfile() },
                        color = iOSCard,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = DeepColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Guest Mode Reminder Banner
            if (isGuest && showGuestBanner) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    color = GoldColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = GoldColor, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("You're in Guest Mode", color = GoldColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Create an account to save your scan history.", color = GoldColor.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = GoldColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp).clickable { showGuestBanner = false }
                        )
                    }
                }
            }

            // Main Scan Card — Deep Gradient Hero
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0D1B2A), Color(0xFF1A3040), Color(0xFF0D2E2A))
                        )
                    )
                    .clickable { onNavigateToScan() }
            ) {
                // Decorative glow orbs
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .offset(x = 160.dp, y = (-80).dp)
                        .background(
                            Brush.radialGradient(listOf(MintColor.copy(0.35f), Color.Transparent)),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-40).dp, y = 100.dp)
                        .background(
                            Brush.radialGradient(listOf(PurpleAccent.copy(0.25f), Color.Transparent)),
                            CircleShape
                        )
                )

                if (isLoading && history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MintColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = MintColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UPLOAD NAIL IMAGE",
                            color = MintColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.analyzeNailsSeconds,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp,
                        letterSpacing = 0.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (history.isNotEmpty()) strings.lastResult + langManager.getConditionName(history.first().result_class) else strings.noRecentScans,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(MintGradient),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) {
                            Text("UPLOAD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Quick action row — AI Advisor + Wiki
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Advisor card
                Surface(
                    modifier = Modifier.weight(1f).clickable { onNavigateToChat() },
                    color = iOSCard,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(PurpleGradient),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Text("AI Advisor", color = DeepColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Ask AI", color = MutedColor, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Open", color = PurpleAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Outlined.ArrowForwardIos, null, tint = PurpleAccent, modifier = Modifier.size(10.dp).padding(start = 2.dp))
                        }
                    }
                }

                // Wiki card
                Surface(
                    modifier = Modifier.weight(1f).clickable { onNavigateToWiki() },
                    color = iOSCard,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(MintGradient),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MenuBook, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Text("Health Wiki", color = DeepColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Condition Guide", color = MutedColor, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Open", color = MintColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Outlined.ArrowForwardIos, null, tint = MintColor, modifier = Modifier.size(10.dp).padding(start = 2.dp))
                        }
                    }
                }
            }

            // About Section (REMOVED - MOVED TO MODAL)

            // How to Use Section (REMOVED - MOVED TO MODAL)

            // FAQs Section (REMOVED - MOVED TO MODAL)

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.healthSignals,
                    color = DeepColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
                Text(
                    text = "See History",
                    color = MintColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }

            // Signals Grid
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                if (isLoading && dynamicSignals.isEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                        Box(modifier = Modifier.weight(1f).height(120.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                    }
                } else {
                    dynamicSignals.chunked(2).forEach { rowSignals ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowSignals.forEach { signal ->
                                RiskTile(signal, Modifier.weight(1f))
                            }
                            if (rowSignals.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            // Medical Disclaimer Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = langManager.strings.medicalDisclaimer,
                    color = MutedColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HelpDialog(strings: AppStrings, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.dismiss.uppercase(), color = MintColor, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = iOSCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = MintColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(strings.howToUseTitle, color = DeepColor, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // About section
                Text(
                    text = strings.aboutTitle,
                    color = DeepColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.aboutDesc,
                    color = MutedColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = SlateColor.copy(alpha = 0.5f))
                
                // Steps
                Text(
                    text = strings.howToUseTitle,
                    color = DeepColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                listOf(
                    strings.step1Title to strings.step1Desc,
                    strings.step2Title to strings.step2Desc,
                    strings.step3Title to strings.step3Desc
                ).forEach { (title, desc) ->
                    Row(modifier = Modifier.padding(bottom = 12.dp)) {
                        Box(modifier = Modifier.padding(top = 4.dp).size(6.dp).background(MintColor, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, color = DeepColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = MutedColor, fontSize = 12.sp)
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = SlateColor.copy(alpha = 0.5f))
                
                // FAQs
                Text(
                    text = strings.faqsTitle,
                    color = DeepColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                listOf(
                    strings.faq1Q to strings.faq1A,
                    strings.faq2Q to strings.faq2A,
                    strings.faq3Q to strings.faq3A,
                    strings.faq4Q to strings.faq4A
                ).forEach { (q, a) ->
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text("Q: $q", color = DeepColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("A: $a", color = MutedColor, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun RiskTile(signal: HealthSignal, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { LanguageManager(context) }
    
    // Animation for the progress bar
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) signal.percentage / 100f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Surface(
        modifier = modifier,
        color = iOSCard,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(signal.color.copy(0.2f), signal.color.copy(0.08f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = signal.icon,
                        contentDescription = null,
                        tint = signal.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            when (signal.risk) {
                                "High"   -> WarnColor.copy(alpha = 0.12f)
                                "Very High" -> WarnColor.copy(alpha = 0.18f)
                                "Medium" -> GoldColor.copy(alpha = 0.12f)
                                else     -> MintColor.copy(alpha = 0.12f)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = signal.risk.uppercase(),
                        color = when (signal.risk) {
                            "High", "Very High" -> WarnColor
                            "Medium" -> GoldColor
                            else     -> MintColor
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = signal.name,
                color = DeepColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Premium animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(SlateColor.copy(alpha = 0.5f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(signal.color, signal.color.copy(alpha = 0.6f))
                            ),
                            CircleShape
                        )
                )
            }

            Text(
                text = "${signal.percentage}% probability",
                color = MutedColor,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
