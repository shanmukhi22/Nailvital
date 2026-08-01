package com.nailvital.app.ui.screens

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.DeleteAccountReq
import com.nailvital.app.api.SessionManager
import com.nailvital.app.api.UserResponse
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import com.nailvital.app.voice.VoiceState

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToPersonalDetails: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isReminderEnabled by remember { mutableStateOf(sessionManager.isReminderEnabled()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
            try {
                userProfile = ApiClient.instance.getProfile("Bearer $token")
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Failed to load profile."
            } finally {
                isLoading = false
            }
        } else {
            onLogout()
        }
    }

    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 500)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 500))
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
                currentRoute = "profile",
                onHomeClick = onBack,
                onHistoryClick = { /* Handled via nav */ },
                onScanClick = { }, // Handle via nav
                onChatClick = { },
                onProfileClick = { }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackBtn(onClick = onBack)
                    Text(
                        text = strings.profile.uppercase(), 
                        color = DeepColor, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Box(modifier = Modifier.size(24.dp)) // Spacer to balance BackBtn
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(iOSCard, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = userProfile?.name?.take(2)?.uppercase() ?: "NV"
                    Text(initial, color = MintColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    
                    // Edit badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .background(MintColor, CircleShape)
                            .border(3.dp, iOSBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = DeepColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(color = MintColor, modifier = Modifier.padding(top = 24.dp), strokeWidth = 3.dp)
                } else {
                    Text(
                        text = userProfile?.name ?: "Guest User", 
                        color = DeepColor, 
                        fontSize = 28.sp, 
                        letterSpacing = -1.sp,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    Text(
                        text = userProfile?.email ?: "login to sync data", 
                        color = MutedColor, 
                        fontSize = 14.sp, 
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Settings Sections
            SettingsSection(strings.personalDetails) {
                SettingsItem(
                    Icons.Rounded.Person, 
                    strings.personalDetails, 
                    userProfile?.phone ?: "Not linked", 
                    onClick = onNavigateToPersonalDetails
                )
            }

            SettingsSection("Preference & Security") {
                SettingsToggleItem(
                    icon = Icons.Rounded.NotificationsActive, 
                    title = strings.reminders, 
                    sub = if (isReminderEnabled) "Health check alarm is active" else "Set a daily nail health alarm",
                    isChecked = isReminderEnabled,
                    onToggle = { enabled ->
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val alarmIntent = Intent(context, com.nailvital.app.AlarmReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(
                            context, 0, alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        if (enabled) {
                            val cal = java.util.Calendar.getInstance()
                            android.app.TimePickerDialog(context, { _, hour, min ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val channel = NotificationChannel("nailvital_reminders", "NailVital Reminders", NotificationManager.IMPORTANCE_HIGH)
                                    val nm = context.getSystemService(NotificationManager::class.java)
                                    nm.createNotificationChannel(channel)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val nm = context.getSystemService(NotificationManager::class.java)
                                    if (!nm.areNotificationsEnabled()) {
                                        Toast.makeText(context, "Please enable notifications in Settings", Toast.LENGTH_LONG).show()
                                        return@TimePickerDialog
                                    }
                                }
                                val triggerAt = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                                    set(java.util.Calendar.MINUTE, min)
                                    set(java.util.Calendar.SECOND, 0)
                                    if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DATE, 1)
                                }.timeInMillis
                                
                                try {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                                    isReminderEnabled = true
                                    sessionManager.setReminderEnabled(true)
                                    Toast.makeText(context, "✅ Reminder set for ${String.format("%02d:%02d", hour, min)}", Toast.LENGTH_SHORT).show()
                                } catch (e: SecurityException) {
                                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                                    isReminderEnabled = true
                                    sessionManager.setReminderEnabled(true)
                                    Toast.makeText(context, "✅ Reminder set for ${String.format("%02d:%02d", hour, min)}", Toast.LENGTH_SHORT).show()
                                }
                            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).apply {
                                setOnCancelListener { isReminderEnabled = false }
                            }.show()
                        } else {
                            alarmManager.cancel(pendingIntent)
                            isReminderEnabled = false
                            sessionManager.setReminderEnabled(false)
                        }
                    }
                )

                SettingsItem(Icons.Rounded.LockOpen, "Change Password", "Update your account security") { onNavigateToChangePassword() }
            }

            SettingsSection("Data Management") {
                SettingsItem(Icons.Rounded.CloudDownload, strings.downloadData, "Download clinical history (PDF)") {
                    scope.launch {
                        try {
                            val token = sessionManager.fetchAuthToken()
                            val response = ApiClient.instance.exportGlobalHistoryPdf("Bearer $token")
                            if (response.isSuccessful && response.body() != null) {
                                val (success, _) = saveFileToDownloads(context, response.body()!!, "NailVital_Data.pdf", "application/pdf")
                                if (success) {
                                    Toast.makeText(context, "Data downloaded as PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                SettingsItem(Icons.Rounded.DeleteSweep, strings.deleteAccount, "Permanently wipe all records", isDanger = true, onClick = { showDeleteDialog = true })
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                DangerButton(text = strings.logout.uppercase(), onClick = {
                    sessionManager.clearSession()
                    onLogout()
                })

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "NailVital AI v2.2.0 • Build ID 99281",
                    color = MutedColor.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                )
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = iOSCard,
            title = { Text(strings.selectLanguage, color = DeepColor, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Language.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    langManager.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = langManager.currentLanguage == lang,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MintColor)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(lang.label, color = DeepColor, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDeleting) {
                    showDeleteDialog = false
                    deletePassword = ""
                    deleteError = null
                }
            },
            containerColor = iOSCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(imageVector = Icons.Rounded.WarningAmber, contentDescription = null, tint = WarnColor)
                    Text("Delete Account?", color = DeepColor, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "This will permanently erase all your clinical data, scan history, and encrypted reports. This action cannot be undone.",
                        color = MutedColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    
                    AuthTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = "Verify Password",
                        placeholder = "Enter your password",
                        isPassword = true
                    )
                    
                    if (deleteError != null) {
                        Text(deleteError!!, color = WarnColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    if (isDeleting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(CircleShape),
                            color = MintColor,
                            trackColor = SlateColor
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deletePassword.isBlank()) {
                            deleteError = "Password is required"
                            return@TextButton
                        }
                        isDeleting = true
                        deleteError = null
                        scope.launch {
                            try {
                                val token = sessionManager.fetchAuthToken()
                                val response = ApiClient.instance.deleteAccount("Bearer $token", DeleteAccountReq(deletePassword))
                                if (response.isSuccessful) {
                                    sessionManager.clearSession()
                                    showDeleteDialog = false
                                    Toast.makeText(context, "Account Deleted Successfully", Toast.LENGTH_LONG).show()
                                    onLogout()
                                } else {
                                    deleteError = if (response.code() == 401 || response.code() == 400) "Incorrect password" else "Deletion failed"
                                }
                            } catch (e: Exception) {
                                deleteError = "Connection error. Please try again."
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting && deletePassword.isNotBlank()
                ) {
                    Text("CONFIRM DELETE", color = WarnColor, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("CANCEL", color = MutedColor)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    onBack: () -> Unit,
    onUpdateSuccess: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val langManager = remember { LanguageManager(context) }
    val strings = langManager.strings
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = sessionManager.fetchAuthToken()
        if (token != null) {
            try {
                isLoading = true
                val profile = ApiClient.instance.getProfile("Bearer $token")
                name = profile.name
                email = profile.email
                phone = profile.phone ?: ""
                age = profile.age?.toString() ?: ""
                gender = profile.gender ?: ""
                height = profile.height ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Failed to synchronize profile data."
            } finally {
                isLoading = false
            }
        }
    }

    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 500)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 500))
                }
                else -> {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.padding(top = 40.dp, start = 24.dp, end = 24.dp)) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Personal Profile", 
                color = DeepColor, 
                fontSize = 32.sp, 
                letterSpacing = -1.sp
            )
            Text(
                "Verify your clinical details for more accurate diagnostic insights.",
                color = MutedColor,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (isLoading) {
            Box(Modifier.height(400.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
            }
        } else {
            Column(modifier = Modifier.padding(24.dp, 32.dp, 24.dp, 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AuthTextField(value = name, onValueChange = { name = it }, label = "Full name", placeholder = "Your name")
                AuthTextField(value = email, onValueChange = { email = it }, label = "Email", placeholder = "your@email.com", enabled = false)
                AuthTextField(value = phone, onValueChange = { phone = it }, label = strings.phone, placeholder = "+91 00000 00000", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        AuthTextField(value = age, onValueChange = { age = it }, label = strings.age, placeholder = "25", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Box(Modifier.weight(1f)) {
                        AuthTextField(value = gender, onValueChange = { gender = it }, label = strings.gender, placeholder = "Male/Female")
                    }
                }
                
                AuthTextField(value = height, onValueChange = { height = it }, label = strings.height, placeholder = "175 cm")
                
                if (errorMessage != null) {
                    Text(errorMessage!!, color = WarnColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                if (isSaving) {
                    CircularProgressIndicator(color = MintColor, modifier = Modifier.align(Alignment.CenterHorizontally), strokeWidth = 3.dp)
                } else {
                    PrimaryButton(
                        text = strings.saveChanges.uppercase(), 
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    val token = sessionManager.fetchAuthToken()
                                    val updates = mapOf(
                                        "name" to name,
                                        "phone" to phone,
                                        "age" to age.ifEmpty { null },
                                        "gender" to gender.ifEmpty { null },
                                        "height" to height.ifEmpty { null }
                                    )
                                    ApiClient.instance.updateProfile("Bearer $token", updates)
                                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                    onUpdateSuccess()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    errorMessage = "Save failed. Please try again."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = name.isNotBlank()
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    } // end Column
    VoiceFab(
        voiceState = voiceState,
        onMicClick = onVoiceMicClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 24.dp, bottom = 32.dp)
    )
    } // end Box
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 0.dp)) {
        Text(
            text = title.uppercase(), 
            color = MutedColor, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold, 
            letterSpacing = 1.sp, 
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        Surface(
            color = iOSCard,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    sub: String, 
    isDanger: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp, 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isDanger) WarnColor.copy(alpha = 0.1f) else MintColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (isDanger) WarnColor else MintColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isDanger) WarnColor else DeepColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = MutedColor, fontSize = 12.sp)
        }
        
        Icon(
            imageVector = Icons.Rounded.ChevronRight, 
            contentDescription = null, 
            tint = MutedColor.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    sub: String, 
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MintColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MintColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DeepColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = MutedColor, fontSize = 12.sp)
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MintColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SlateColor
            )
        )
    }
}

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onChangeSuccess: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 500)
                }
                is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(kotlin.math.max(0, scrollState.value - 500))
                }
                else -> {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBg)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.padding(top = 40.dp, start = 24.dp, end = 24.dp)) {
            BackBtn(onClick = onBack)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Change Password", 
                color = DeepColor, 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
            Text(
                "Update your login credentials to keep your clinical data secure.",
                color = MutedColor,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(modifier = Modifier.padding(24.dp, 32.dp, 24.dp, 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthTextField(
                value = currentPass, 
                onValueChange = { currentPass = it }, 
                label = "Current Password", 
                placeholder = "••••••••",
                isPassword = true
            )
            
            AuthTextField(
                value = newPass, 
                onValueChange = { newPass = it }, 
                label = "New Password", 
                placeholder = "At least 8 characters",
                isPassword = true
            )
            
            AuthTextField(
                value = confirmPass, 
                onValueChange = { confirmPass = it }, 
                label = "Confirm New Password", 
                placeholder = "Repeat new password",
                isPassword = true
            )
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = WarnColor, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            
            // Helpful tip
            Surface(
                color = MintColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = MintColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Use a mix of letters, numbers, and symbols.", color = MintColor, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            if (isSaving) {
                CircularProgressIndicator(color = MintColor, modifier = Modifier.align(Alignment.CenterHorizontally), strokeWidth = 3.dp)
            } else {
                PrimaryButton(
                    text = "UPDATE PASSWORD", 
                    onClick = {
                        if (newPass.length < 8) {
                            errorMessage = "New password must be at least 8 characters"
                            return@PrimaryButton
                        }
                        if (newPass != confirmPass) {
                            errorMessage = "Passwords do not match"
                            return@PrimaryButton
                        }
                        
                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val token = sessionManager.fetchAuthToken()
                                // The backend updateProfile supports 'password' field
                                ApiClient.instance.updateProfile("Bearer $token", mapOf("password" to newPass))
                                Toast.makeText(context, "Password Updated", Toast.LENGTH_SHORT).show()
                                onChangeSuccess()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorMessage = "Failed to update password. Try again."
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = currentPass.isNotBlank() && newPass.isNotBlank() && confirmPass.isNotBlank()
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    } // end Column
    VoiceFab(
        voiceState = voiceState,
        onMicClick = onVoiceMicClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 24.dp, bottom = 32.dp)
    )
    } // end Box
}

