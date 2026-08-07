package com.nailvital.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.composed
import androidx.compose.animation.core.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.IntSize
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import okhttp3.ResponseBody
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════
//  NAILVITAL PREMIUM DESIGN SYSTEM — iPhone 17 Style
// ═══════════════════════════════════════════════════

// Backgrounds
val iOSBg    = Color(0xFFF9FAFB)  // Crisp, professional cool-gray
val iOSCard  = Color(0xFFFFFFFF)  // Pure White Card

// Brand / Accent
val MintColor  = Color(0xFF0F766E)  // Professional Medical Teal
val MintLight  = Color(0xFF14B8A6)  // Lighter teal for gradients
val PurpleAccent = Color(0xFF6366F1) // Professional Indigo/Purple

// Semantic
val DeepColor  = Color(0xFF101828)  // Standard deep slate for text
val SlateColor = Color(0xFFEAECF0)  // Light, crisp borders
val MutedColor = Color(0xFF667085)  // Professional gray secondary text
val WarnColor  = Color(0xFFD92D20)  // Professional red
val GoldColor  = Color(0xFFF79009)  // Warning honey/gold

// Glass & Overlay
val GlassColor = Color(0xF2FFFFFF)  // Professional slight blur
val GlassDark  = Color(0x99000000)  // Standard dark overlay

// Gradient helpers
val MintGradient = listOf(MintColor, MintLight)
val PurpleGradient = listOf(PurpleAccent, Color(0xFF5B8DEF))

// ═══════════════════════════════════════════════════
//  COMPONENTS
// ═══════════════════════════════════════════════════

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                brush = if (enabled) Brush.horizontalGradient(MintGradient)
                        else Brush.horizontalGradient(listOf(SlateColor, SlateColor)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else MutedColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.sp
        )
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepColor),
        border = BorderStroke(1.5.dp, SlateColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WarnColor.copy(alpha = 0.1f),
            contentColor = WarnColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    autofillHints: List<String>? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = DeepColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MutedColor) },
            trailingIcon = if (isPassword) {
                {
                    val image = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = MutedColor)
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (isPassword && keyboardOptions == KeyboardOptions.Default)
                KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            else keyboardOptions,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DeepColor,
                unfocusedTextColor = DeepColor,
                focusedBorderColor = MintColor,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = SlateColor.copy(alpha = 0.45f),
                selectionColors = TextSelectionColors(MintColor, MintColor.copy(alpha = 0.4f))
            ),
            isError = isError,
            singleLine = true
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = WarnColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun BackBtn(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(44.dp)
            .background(GlassColor, RoundedCornerShape(12.dp))
            .border(1.dp, SlateColor.copy(0.6f), RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBackIosNew,
            contentDescription = "Back",
            tint = DeepColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onScanClick: () -> Unit,
    onChatClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // Glassmorphism nav bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(GlassColor)
            .border(BorderStroke(1.dp, SlateColor.copy(0.4f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Outlined.Home,    "Home",    currentRoute == "home",    onHomeClick)
            NavItem(Icons.Outlined.History, "History", currentRoute == "history", onHistoryClick)

            // Central FAB-style scan button
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .offset(y = (-6).dp)
                    .background(
                        Brush.linearGradient(MintGradient),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onScanClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CloudUpload,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            NavItem(Icons.Outlined.AutoAwesome, "AI Chat", currentRoute == "chat",    onChatClick)
            NavItem(Icons.Outlined.Person,      "Profile", currentRoute == "profile", onProfileClick)
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MintColor else MutedColor.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MintColor else MutedColor.copy(alpha = 0.5f)
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(MintColor, CircleShape)
            )
        }
    }
}

suspend fun saveFileToDownloads(context: Context, body: ResponseBody, fileName: String, mimeType: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            body.byteStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        return@withContext Pair(true, "Downloads/$fileName")
                    }
                } catch (e: Exception) {
                    // fallback
                }
            }
            val file = File(context.filesDir, fileName)
            file.outputStream().use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Pair(true, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: e.javaClass.simpleName)
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue  = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1200)),
        label = "shimmer_offset"
    )
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                SlateColor.copy(alpha = 0.4f),
                SlateColor.copy(alpha = 0.85f),
                SlateColor.copy(alpha = 0.4f)
            ),
            start = Offset(startOffsetX, 0f),
            end   = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned { size = it.size }
}

