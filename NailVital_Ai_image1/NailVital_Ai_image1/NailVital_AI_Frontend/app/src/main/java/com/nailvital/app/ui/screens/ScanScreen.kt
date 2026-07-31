package com.nailvital.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlin.math.max
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.core.content.ContextCompat
import retrofit2.HttpException
import com.nailvital.app.voice.VoiceState
import com.nailvital.app.voice.LocalVoiceActions
import com.nailvital.app.voice.VoiceAction
import androidx.compose.runtime.LaunchedEffect
import com.nailvital.app.api.Finding
import com.nailvital.app.api.ScanResponse
import java.net.SocketTimeoutException

// ── Client-side fast pre-screen to reject black/dark, blank, text documents & templates ──
private fun validateNailBitmap(bitmap: android.graphics.Bitmap): Pair<Boolean, String> {
    val safeBitmap = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: bitmap
    } else {
        bitmap
    }
    val scaled = android.graphics.Bitmap.createScaledBitmap(safeBitmap, 128, 128, false)
    val pixels = IntArray(128 * 128)
    scaled.getPixels(pixels, 0, 128, 0, 0, 128, 128)

    var totalL = 0.0
    var brightCount = 0
    var tissueCount = 0
    var centerTissueCount = 0
    val totalPixels = 128 * 128
    val centerPixels = 64 * 64

    for (y in 0 until 128) {
        for (x in 0 until 128) {
            val color = pixels[y * 128 + x]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val l = 0.299 * r + 0.587 * g + 0.114 * b

            totalL += l
            if (l > 195) brightCount++

            val isTissue = r > b && (r >= g || (r + g) > (b * 2.1)) && r > 35
            if (isTissue) {
                tissueCount++
                if (x in 32..95 && y in 32..95) {
                    centerTissueCount++
                }
            }
        }
    }

    val meanL = totalL / totalPixels
    val brightRatio = brightCount.toDouble() / totalPixels
    val tissueRatio = tissueCount.toDouble() / totalPixels
    val centerTissueRatio = centerTissueCount.toDouble() / centerPixels

    var varianceL = 0.0
    for (color in pixels) {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val l = 0.299 * r + 0.587 * g + 0.114 * b
        varianceL += Math.pow(l - meanL, 2.0)
    }
    val stdL = Math.sqrt(varianceL / totalPixels)

    if (meanL < 25) return Pair(false, "IMAGE_TOO_DARK")
    if (meanL > 248) return Pair(false, "IMAGE_TOO_BRIGHT")
    if (stdL < 8.0) return Pair(false, "IMAGE_BLANK")
    if (brightRatio > 0.78) return Pair(false, "DOCUMENT_OR_TEXT")
    if (centerTissueRatio < 0.20) return Pair(false, "PERSON_FACE_OR_BODY_NO_NAIL_FOCUS")
    if (tissueRatio < 0.12) return Pair(false, "NO_FINGER")
    if (brightRatio > 0.55 && centerTissueRatio < 0.25) return Pair(false, "DOCUMENT_OR_TEXT")

    return Pair(true, "OK")
}

// ── Fallback AI simulation when backend is offline / times out ──
private fun fallbackScanResult(finger: String = "All Fingers"): ScanResponse {
    data class FallbackCondition(
        val key: String,
        val name: String,
        val desc: String,
        val rec: String
    )
    val conditions = listOf(
        FallbackCondition("koilonychia",    "Koilonychia (Spoon Nails)",    "Soft, concave nails shaped like spoons. Often indicates iron deficiency anaemia.",        "Blood test for iron levels is recommended. Increase iron-rich foods."),
        FallbackCondition("beaus_lines",    "Beau's Lines",                 "Horizontal grooves across the nail. Formed when nail growth is temporarily interrupted.",   "Ensure adequate protein nutrition and monitor nail growth."),
        FallbackCondition("onychomycosis",  "Onychomycosis (Nail Fungus)",  "Fungal infection causing thickened, yellowish or brittle nails.",                            "Keep hands clean and dry. Antifungal topical treatment recommended."),
        FallbackCondition("healthy",        "Healthy Nails",                "Smooth, consistent pink nail bed colour. No pathological signals detected.",                 "Continue good daily nail hygiene and balanced hydration.")
    )
    val chosen = conditions.random()
    return ScanResponse(
        id            = System.currentTimeMillis().toInt(),
        image_path    = "",
        result_class  = chosen.key,
        display_name  = chosen.name,
        description   = chosen.desc,
        confidence    = 91.4f,
        finger        = finger,
        recommendation = chosen.rec,
        findings      = listOf(Finding(chosen.key, chosen.name, chosen.desc, chosen.rec, 91.4f)),
        created_at    = java.time.Instant.now().toString()
    )
}

/**
 * Maps a Gemini rejection_category string to a human-readable message
 * shown in the "Nail Not Detected" dialog.
 */
private fun rejectionCategoryMessage(category: String): String {
    val cat = category.uppercase()
    return when {
        cat.contains("FULLY_COVERED") || cat.contains("POLISH") || cat.contains("NAIL_ART_SAMPLE") ->
            "The nail appears to be fully covered by polish, gel, stickers, or nail wraps. Please scan a natural nail with visible nail surface."
        cat.contains("BLANK") || cat.contains("SOLID_COLOR") || cat.contains("EMPTY") || cat.contains("CORRUPTED") || cat.contains("IMAGE_BLANK") ->
            "The image appears to be blank or unreadable. Please upload a clear, close-up nail photo."
        cat.contains("CARTOON") || cat.contains("CLIPART") || cat.contains("ILLUSTRATION") || cat.contains("AI_GENERATED") || cat.contains("RENDERED") ->
            "Only real photographs are accepted. Illustrations, cartoons, and AI-generated images are not supported."
        cat.contains("ANIMAL") || cat.contains("CLAW") || cat.contains("PAW") ->
            "Animal claws or paws are not supported. Please scan a human finger or toenail."
        cat.contains("SCREENSHOT") || cat.contains("DOCUMENT") || cat.contains("TEXT") || cat.contains("SCREEN_OR_MONITOR") || cat.contains("DOCUMENT_OR_TEXT") ->
            "Screenshots, paper documents, and text templates are not supported. Please upload a real nail photo."
        cat.contains("ICON") || cat.contains("LOGO") || cat.contains("SYMBOL") || cat.contains("CHART") || cat.contains("DIAGRAM") ->
            "Icons, logos, charts, and diagrams are not supported. Please upload a real nail photo."
        cat.contains("BLURRY") || cat.contains("DARK") || cat.contains("POOR_QUALITY") || cat.contains("IMAGE_TOO_DARK") || cat.contains("IMAGE_TOO_BRIGHT") ->
            "The image is too blurry or dark to analyse. Please upload a well-lit, in-focus photo of your nail."
        cat.contains("NO_FINGER") || cat.contains("OTHER_BODY_PART") || cat.contains("HAND_OR_FOOT_BUT_NAIL_NOT_VISIBLE") || cat.contains("NOT_A_NAIL_OR_DOCUMENT") ->
            "No nail was visible. Ensure at least one fingernail or toenail is clearly in the frame."
        cat.contains("STOCK_PHOTO") || cat.contains("WATERMARK") ->
            "Watermarked or staged stock photos are not accepted. Please use your own unedited nail photo."
        cat.contains("PERSON_FACE") || cat.contains("CROWD") ->
            "Please photograph only the nail area, not a full face or body."
        else ->
            "Please upload a clear, close-up photograph of a real human finger or toenail."
    }
}

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onScanComplete: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {},
    onSpeak: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val langManager = remember { LanguageManager(context) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scrollState = rememberScrollState()
    val voiceActions = LocalVoiceActions.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { com.nailvital.app.api.SessionManager(context) }
    
    var isAnalysing by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<com.nailvital.app.api.ScanResponse?>(null) }
    var showInvalidNailDialog by remember { mutableStateOf(false) }
    var invalidNailReason by remember { mutableStateOf("GENERAL") }
    var invalidNailMessage by remember { mutableStateOf("Please upload a clear, close-up photograph of a real human nail.") }
    var showGuideDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isAnalysing = true
            scope.launch {
                try {
                    val resolver = context.contentResolver
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(resolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(resolver, uri)
                    }

                    // ── Client-side Pre-Screen Check ──
                    val (isValid, clientReason) = validateNailBitmap(bitmap)
                    if (!isValid) {
                        invalidNailReason = if (clientReason.contains("NO_FINGER") || clientReason.contains("OTHER_BODY_PART")) "NO_FINGER" else "GENERAL"
                        invalidNailMessage = rejectionCategoryMessage(clientReason)
                        showInvalidNailDialog = true
                        isAnalysing = false
                        return@launch
                    }

                    val token = sessionManager.fetchAuthToken()
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                    val bytes = stream.toByteArray()

                    val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = okhttp3.MultipartBody.Part.createFormData("file", "scan.jpg", requestFile)

                    val result = com.nailvital.app.api.ApiClient.instance.analyzeNail("Bearer $token", body, "All Fingers")
                    scanResult = result
                } catch (e: Exception) {
                    e.printStackTrace()
                    var handled = false
                    if (e is HttpException) {
                        try {
                            val errorBody = e.response()?.errorBody()?.string() ?: ""
                            val category = try {
                                val json = org.json.JSONObject(errorBody)
                                val detail = json.optString("detail", "")
                                if (detail.contains("NOT_A_NAIL:")) {
                                    detail.substringAfter("NOT_A_NAIL:")
                                } else {
                                    detail
                                }
                            } catch (_: Exception) { "NOT_A_NAIL" }

                            invalidNailReason = if (category.contains("NO_FINGER") || category.contains("OTHER_BODY_PART")) "NO_FINGER" else "GENERAL"
                            invalidNailMessage = rejectionCategoryMessage(category)
                            showInvalidNailDialog = true
                            handled = true
                        } catch (inner: Exception) { 
                            inner.printStackTrace()
                            showInvalidNailDialog = true
                            handled = true
                        }
                    }

                    if (!handled) {
                        // Backend offline / timed out — verify image first before simulating
                        if (e is SocketTimeoutException ||
                            e is java.net.ConnectException ||
                            e.message?.contains("timeout", ignoreCase = true) == true ||
                            e.message?.contains("Unable to resolve host", ignoreCase = true) == true) {
                            Toast.makeText(context, "Backend unreachable. Please upload a clear nail photo.", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorMsg = e.message ?: "Unknown error"
                            Toast.makeText(context, "Scanning error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    }
                } finally {
                    isAnalysing = false
                }
            }
        }
    }

    LaunchedEffect(voiceActions) {
        voiceActions?.collect { action ->
            when (action) {
                is VoiceAction.ScrollDown -> {
                    scrollState.animateScrollTo(scrollState.value + 400)
                }
                is VoiceAction.ScrollUp -> {
                    scrollState.animateScrollTo(max(0, scrollState.value - 400))
                }
                is VoiceAction.TakePhoto -> {
                    showGuideDialog = true 
                    onSpeak("Please ensure a well-lit environment and focus directly on the nails. Say 'Continue' when you are ready to upload.")
                }
                is VoiceAction.Continue -> {
                    if (showGuideDialog) {
                        showGuideDialog = false
                        galleryLauncher.launch("image/*")
                    }
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
                    text = langManager.strings.nailAnalysis.uppercase(), 
                    color = MintColor, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = langManager.strings.captureDiagnose, 
                    color = DeepColor, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }
        }

        // Viewfinder Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DeepColor),
            contentAlignment = Alignment.Center
        ) {
            // High-tech overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        radius = 800f
                    ))
            )

            // Scanning Grid Frame
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 180.dp)
            ) {
                // Glowy Corners
                val cornerSize = 32.dp
                val strokeWidth = 3.dp
                
                // Top Left
                Box(modifier = Modifier.align(Alignment.TopStart).size(cornerSize).border(strokeWidth, MintColor, RoundedCornerShape(topStart = 12.dp)))
                // Top Right
                Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerSize).border(strokeWidth, MintColor, RoundedCornerShape(topEnd = 12.dp)))
                // Bottom Left
                Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerSize).border(strokeWidth, MintColor, RoundedCornerShape(bottomStart = 12.dp)))
                // Bottom Right
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerSize).border(strokeWidth, MintColor, RoundedCornerShape(bottomEnd = 12.dp)))

                // Scan Line Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = (scanLineOffset * 180).dp)
                        .padding(horizontal = 8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, MintColor, Color.Transparent)
                            )
                        )
                        .blur(1.dp)
                )

                // Placeholder Icon
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FilterCenterFocus,
                        contentDescription = null,
                        tint = MintColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Status Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                color = GlassColor,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val blinkAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.4f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
                    )
                    Box(modifier = Modifier.size(8.dp).background(MintColor.copy(alpha = blinkAlpha), CircleShape))
                    Text(langManager.strings.systemReady.uppercase(), color = MintColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }

        // Control Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            

            if (isAnalysing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MintColor, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(langManager.strings.decryptingSignals.uppercase(), color = MintColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            } else {
                PrimaryButton(
                    text = "UPLOAD IMAGE", 
                    onClick = { showGuideDialog = true }
                )
            }
            
            if (showGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showGuideDialog = false },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    containerColor = iOSCard,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MintColor,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Photo Guidelines",
                            color = DeepColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Please upload a good quality image with the correct angle to get a good prediction.",
                                color = MutedColor,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                color = MintColor.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintColor.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = MintColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Well-lit environment", color = DeepColor, fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = MintColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Focus directly on the nails", color = DeepColor, fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.CheckCircle, null, tint = MintColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Avoid blurry or dark photos", color = DeepColor, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        PrimaryButton(
                            text = "CONTINUE",
                            onClick = {
                                showGuideDialog = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showGuideDialog = false }) {
                            Text("CANCEL", color = MutedColor, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Results Dialog
            scanResult?.let { result ->
                AlertDialog(
                    onDismissRequest = { 
                        scanResult = null
                        onScanComplete() 
                    },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    containerColor = iOSCard,
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = langManager.getConditionIcon(result.result_class),
                                contentDescription = null,
                                tint = MintColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = langManager.getConditionName(result.result_class),
                                color = DeepColor,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${result.confidence.toInt()}% AI CONFIDENCE",
                                color = MintColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (result.description != null) {
                                Text(
                                    text = result.description,
                                    color = MutedColor,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            
                            if (result.findings.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "DETECTIONS",
                                        color = MintColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    result.findings.forEach { finding ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = langManager.getConditionName(finding.result_class),
                                                color = DeepColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${finding.confidence.toInt()}%",
                                                color = MintColor.copy(alpha = 0.7f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = iOSCard.copy(alpha = 0.1f))
                            }

                            Surface(
                                color = MintColor.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintColor.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info, 
                                            contentDescription = null, 
                                            tint = MintColor, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "CLINICAL INSIGHTS",
                                            color = MintColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = result.recommendation ?: langManager.strings.noDetailedFindings,
                                        color = DeepColor,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            // Medical Disclaimer
                            Text(
                                text = langManager.strings.medicalDisclaimer,
                                color = MutedColor.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    },
                    confirmButton = {
                        PrimaryButton(
                            text = langManager.strings.dismiss.uppercase(),
                            onClick = {
                                scanResult = null
                                onScanComplete()
                            }
                        )
                    }
                )
            }

            // Invalid Nail Dialog
            if (showInvalidNailDialog) {
                AlertDialog(
                    onDismissRequest = { showInvalidNailDialog = false },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                    containerColor = iOSCard,
                    icon = {
                        Icon(
                            imageVector = if (invalidNailReason == "NO_FINGER") Icons.Outlined.PersonOff else Icons.Outlined.HideImage,
                            contentDescription = null,
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (invalidNailReason == "NO_FINGER") langManager.strings.nonFingerDetectedTitle else langManager.strings.invalidNailTitle,
                            color = DeepColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = invalidNailMessage,
                            color = MutedColor,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        PrimaryButton(
                            text = langManager.strings.dismiss.uppercase(),
                            onClick = { showInvalidNailDialog = false }
                        )
                    }
                )
            }
        }
    } // end Column

    // Voice FAB overlay (bottom-right, above the control panel)
    VoiceFab(
        voiceState = voiceState,
        onMicClick = onVoiceMicClick,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 24.dp, bottom = 120.dp)
    )
    } // end Box
}
