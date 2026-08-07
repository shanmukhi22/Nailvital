package com.nailvital.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nailvital.app.api.ApiClient
import com.nailvital.app.api.ScanResponse
import com.nailvital.app.api.SessionManager
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun ProgressChart(history: List<ScanResponse>) {
    if (history.size < 2) return
    
    // Sort oldest to newest for the chart
    val sortedHistory = history.sortedBy { it.created_at }
    val maxConfidence = 100f
    
    Surface(
        color = iOSCard,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(bottom = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = MintColor, modifier = Modifier.size(16.dp))
                Text("CLINICAL TRENDS", color = DeepColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                // Draw horizontal grid lines
                for (i in 0..4) {
                    val y = height - (height * (i * 25f / 100f))
                    drawLine(
                        color = SlateColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.5f
                    )
                }
                
                // Draw data line
                val stepX = width / max(1, sortedHistory.size - 1)
                val path = Path()
                val points = mutableListOf<Offset>()
                
                sortedHistory.forEachIndexed { index, scan ->
                    val x = index * stepX
                    val y = height - (height * (scan.confidence / maxConfidence))
                    val point = Offset(x, y)
                    points.add(point)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        // Smooth curve logic or straight lines
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = MintColor,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                
                // Draw data points and labels
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#9ca3af")
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())

                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = iOSCard,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = MintColor,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    
                    val scan = sortedHistory[index]
                    val dateObj = try { scan.created_at.let { inputFormat.parse(it) } } catch (e: Exception) { null }
                    val dateStr = dateObj?.let { dateFormat.format(it) } ?: ""
                    val timeStr = dateObj?.let { timeFormat.format(it) } ?: ""
                    
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(dateStr, point.x, point.y + 40f, textPaint)
                        canvas.nativeCanvas.drawText(timeStr, point.x, point.y + 70f, textPaint)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    scan: ScanResponse,
    langManager: LanguageManager,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    isInitiallyExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }
    
    // Sync external expansion if it changes (e.g. via deep link)
    LaunchedEffect(isInitiallyExpanded) {
        if (isInitiallyExpanded) isExpanded = true
    }

    Surface(
        color = iOSCard,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scan Image Preview
                Box {
                    AsyncImage(
                        model = ApiClient.BASE_URL + scan.image_path.removePrefix("/"),
                        contentDescription = "Nail Scan",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = langManager.getConditionName(scan.result_class),
                        color = DeepColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val statusColor = if (scan.confidence > 70) MintColor else if (scan.confidence > 40) Color(0xFFFFD166) else WarnColor
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        Text(
                            text = "${scan.confidence.toInt()}% confidence" + (if (scan.finger != null) " • ${scan.finger}" else ""),
                            color = MutedColor,
                            fontSize = 12.sp
                        )
                    }
                    
                    Text(
                        text = scan.created_at.split("T").first(),
                        color = MutedColor.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(36.dp).background(SlateColor.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = MintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).background(SlateColor.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = WarnColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SlateColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = langManager.strings.clinicalFindings,
                    color = DeepColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (scan.findings.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "DETECTIONS",
                                color = MintColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            scan.findings.forEach { finding ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = langManager.getConditionName(finding.result_class),
                                        color = DeepColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${finding.confidence.toInt()}%",
                                        color = MintColor.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = SlateColor.copy(alpha = 0.1f))
                    }

                    if (scan.description != null) {
                        Text(
                            text = scan.description,
                            color = MutedColor,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                    
                    Surface(
                        color = MintColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RECOMMENDATION",
                                color = MintColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scan.recommendation ?: langManager.strings.noDetailedFindings,
                                color = DeepColor,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onHomeClick: () -> Unit,
    initialExpandDisease: String? = null
) {
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    val listState = rememberLazyListState()

    var history by remember { mutableStateOf<List<ScanResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // UI States
    var scanToDelete by remember { mutableStateOf<ScanResponse?>(null) }
    var savedFilePath by remember { mutableStateOf<String?>(null) }


    suspend fun loadHistory() {
        try {
            val token = sessionManager.fetchAuthToken()
            if (token != null) {
                val fetched = ApiClient.instance.getHistory("Bearer $token")
                history = fetched
                
                // Handle Deep Link Expansion
                if (initialExpandDisease != null) {
                    val targetIndex = fetched.indexOfFirst { it.result_class.equals(initialExpandDisease, ignoreCase = true) }
                    if (targetIndex != -1) {
                        scope.launch {
                            // Delay slightly to ensure list is rendered
                            kotlinx.coroutines.delay(300)
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                }
            } else {
                errorMessage = "Session expired. Please login again."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load history."
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch { loadHistory() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = iOSBg,
        floatingActionButton = {
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = "history",
                onHomeClick = onHomeClick,
                onHistoryClick = { /* Already here */ },
                onScanClick = onNavigateToScan,
                onChatClick = onNavigateToChat,
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
                Text(
                    text = langManager.strings.healthJournal, 
                    color = DeepColor, 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Header with Refresh + Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = langManager.strings.journalEntries.uppercase(), 
                            color = MutedColor, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = langManager.strings.clinicalHistory, 
                            color = DeepColor, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Refresh Button
                        Surface(
                            onClick = {
                                isRefreshing = true
                                scope.launch { loadHistory() }
                            },
                            color = iOSCard,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(color = MintColor, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MintColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        // Export PDF Button
                        Surface(
                            onClick = {
                                scope.launch {
                                    try {
                                        val token = sessionManager.fetchAuthToken()
                                        val response = ApiClient.instance.exportGlobalHistoryPdf("Bearer $token")
                                        if (response.isSuccessful && response.body() != null) {
                                            val fileName = "NailVital_Clinical_Report.pdf"
                                            val (success, resultMsg) = saveFileToDownloads(context, response.body()!!, fileName, "application/pdf")
                                            if (success) {
                                                savedFilePath = resultMsg
                                            } else {
                                                Toast.makeText(context, "Save error: $resultMsg", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Server export failed", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                            color = iOSCard,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MintColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(langManager.strings.exportAll.uppercase(), color = MintColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } // End Row (Refresh + Export)
                }
                
                if (isLoading) {
                    // Shimmer skeleton placeholders
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(), 
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = WarnColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage!!, color = MutedColor, fontSize = 14.sp)
                    }
                } else if (history.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(), 
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MutedColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(langManager.strings.noRecordsFound, color = MutedColor, fontSize = 14.sp)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(history.size) { index ->
                                val scan = history[index]
                                val shouldExpand = initialExpandDisease != null && 
                                    scan.result_class.equals(initialExpandDisease, ignoreCase = true)
                                
                                HistoryCard(
                                    scan = scan,
                                    langManager = langManager,
                                    onDelete = { scanToDelete = scan },
                                    onExport = {
                                         scope.launch {
                                            try {
                                                val token = sessionManager.fetchAuthToken()
                                                val response = ApiClient.instance.exportScan("Bearer $token", scan.id)
                                                if (response.isSuccessful && response.body() != null) {
                                                    val fileName = "NailVital_Record_${scan.id}.pdf"
                                                    val (success, resultMsg) = saveFileToDownloads(context, response.body()!!, fileName, "application/pdf")
                                                    if (success) {
                                                        savedFilePath = resultMsg
                                                    } else {
                                                        Toast.makeText(context, "Save error: $resultMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    isInitiallyExpanded = shouldExpand
                                )
                            }
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            scanToDelete?.let { scan ->
                AlertDialog(
                    onDismissRequest = { scanToDelete = null },
                    title = { Text(langManager.strings.purgeRecord, color = DeepColor, fontWeight = FontWeight.Bold) },
                    text = { Text(langManager.strings.purgeRecordDesc, color = MutedColor) },
                    containerColor = iOSCard,
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    val token = sessionManager.fetchAuthToken()
                                    ApiClient.instance.deleteScan("Bearer $token", scan.id)
                                    history = history.filter { it.id != scan.id }
                                    Toast.makeText(context, "Record removed", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    scanToDelete = null
                                }
                            }
                        }) {
                            Text("DELETE", color = WarnColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { scanToDelete = null }) {
                            Text("CANCEL", color = DeepColor)
                        }
                    }
                )
            }

            // Save Confirmation Dialog
            savedFilePath?.let { path ->
                AlertDialog(
                    onDismissRequest = { savedFilePath = null },
                    title = { Text(langManager.strings.reportGenerated, color = DeepColor, fontWeight = FontWeight.Bold) },
                    text = { Text(langManager.strings.reportGeneratedDesc + "\n\nPath: $path", color = MutedColor) },
                    containerColor = iOSCard,
                    confirmButton = {
                        TextButton(onClick = { savedFilePath = null }) {
                            Text(langManager.strings.dismiss.uppercase(), color = MintColor, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            
            // Medical Disclaimer Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = langManager.strings.medicalDisclaimer,
                    color = MutedColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
