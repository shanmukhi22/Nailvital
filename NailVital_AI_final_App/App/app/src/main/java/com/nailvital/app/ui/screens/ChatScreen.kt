package com.nailvital.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.ChatRequest
import com.nailvital.app.api.SessionManager
import kotlinx.coroutines.launch
import com.nailvital.app.voice.VoiceState

data class Message(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onHomeClick: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val langManager = remember { LanguageManager(context) }
    
    var messages by remember { mutableStateOf(listOf(
        Message("Hello! I'm your NailVital AI Advisor. How can I help you with your nail health today? 💅", false)
    )) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current
    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    val current = listState.firstVisibleItemIndex
                    listState.animateScrollToItem(current + 5)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    val current = listState.firstVisibleItemIndex
                    listState.animateScrollToItem(kotlin.math.max(0, current - 5))
                }
                else -> {}
            }
        }
    }

    Scaffold(
        containerColor = iOSBg,
        floatingActionButton = {
            VoiceFab(
                voiceState = voiceState,
                onMicClick = onVoiceMicClick
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "chat",
                onHomeClick = onHomeClick,
                onHistoryClick = onNavigateToHistory,
                onScanClick = onNavigateToScan,
                onChatClick = { /* Already here */ },
                onProfileClick = onNavigateToProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(iOSBg)
        ) {
            // App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BackBtn(onClick = onBack)
                Column {
                    Text(
                        text = "AI ADVISOR", 
                        color = MintColor, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Medical Assistant", 
                        color = DeepColor, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = -1.sp
                    )
                }
            }

            // Chat History
            Box(modifier = Modifier.weight(1f)) {
                // Subtle background glow
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.Center)
                        .background(MintColor.copy(alpha = 0.03f), CircleShape)
                        .blur(80.dp)
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                    
                    if (isTyping) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }

            // Disclaimer Notice
            Text(
                text = langManager.strings.aiAssessmentNotice,
                color = MutedColor.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).align(Alignment.CenterHorizontally)
            )

            // Input Area (Glassmorphism)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = iOSCard,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about your nail health...", color = MutedColor, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor = MintColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = DeepColor,
                            unfocusedTextColor = DeepColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                val userMsg = inputText
                                inputText = ""
                                messages = messages + Message(userMsg, true)
                                isTyping = true
                                
                                scope.launch {
                                    try {
                                        val token = sessionManager.fetchAuthToken()
                                        val response = ApiClient.instance.chat("Bearer $token", ChatRequest(userMsg))
                                        messages = messages + Message(response.reply, false)
                                    } catch (_: Exception) {
                                        messages = messages + Message("Consultation failed. Check network.", false)
                                    } finally {
                                        isTyping = false
                                    }
                                }
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMsg = inputText
                                inputText = ""
                                messages = messages + Message(userMsg, true)
                                isTyping = true
                                
                                scope.launch {
                                    try {
                                        val token = sessionManager.fetchAuthToken()
                                        val response = ApiClient.instance.chat("Bearer $token", ChatRequest(userMsg))
                                        messages = messages + Message(response.reply, false)
                                    } catch (_: Exception) {
                                        messages = messages + Message("Consultation failed. Check network.", false)
                                    } finally {
                                        isTyping = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MintColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) MintColor else iOSCard
    val textColor = if (isUser) Color.White else DeepColor
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser) {
            Text(
                text = "AI ADVISOR", 
                color = MintColor, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                letterSpacing = 0.5.sp
            )
        }
        
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 300.dp),
            shadowElevation = if (isUser) 4.dp else 2.dp
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .background(iOSCard, RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MintColor.copy(alpha = scale))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("Thinking...", color = MutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
