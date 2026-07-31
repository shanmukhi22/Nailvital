package com.nailvital.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailvital.app.voice.VoiceState

data class WikiEntry(
    val conditionKey: String,
    val description: String,
    val isSevere: Boolean
)

@Composable
fun HealthWikiScreen(
    onBack: () -> Unit,
    voiceState: VoiceState = VoiceState.IDLE,
    onVoiceMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val langManager = remember { LanguageManager(context) }
    
    // Comprehensive dictionary of common conditions
    val conditions = listOf(
        WikiEntry("aloperia_areata", "An autoimmune disorder that can cause severe nail pitting (tiny dents), ridging, and brittleness.", false),
        WikiEntry("beaus_lines", "Deep grooved lines that run from side to side on the fingernail. Often caused by severe illness, malnutrition, or trauma.", false),
        WikiEntry("bluish_nail", "A bluish tint to the nail bed (cyanosis). Usually indicates a lack of oxygen in the blood, potentially pointing to lung or heart issues.", true),
        WikiEntry("clubbing", "Nails thicken and curve around the fingertips. Associated with chronic low blood oxygen levels and lung diseases.", true),
        WikiEntry("dariers_disease", "A rare genetic condition causing red and white longitudinal streaks on the nails, occasionally leading to splitting.", false),
        WikiEntry("eczema", "Can cause the nails to become ridged, pitted, thickened, or discolored as a secondary effect to skin eczema.", false),
        WikiEntry("half_and_half_nails", "The bottom half of the nail is white, while the top half turns red or brown. Often associated with kidney diseases.", true),
        WikiEntry("koilonychia", "Also known as 'spoon nails', where nails become exceptionally thin and lose their convexity, looking like a spoon. Common in iron deficiency anemia.", false),
        WikiEntry("leukonychia", "White spots or lines on the nails. Most commonly a result of minor trauma to the nail matrix.", false),
        WikiEntry("melanoma", "Appears as a dark vertical stripe (melanonychia) down the nail. A critical form of skin cancer that requires immediate dermatological attention.", true),
        WikiEntry("onychogryphosis", "Referred to as 'Ram's horn nails'. Causes dramatic overgrowth and thickening of the nails, usually on the toes.", false),
        WikiEntry("onycholycis", "Painless separation of the nail from the nail bed. Can be caused by trauma, psoriasis, or fungal infections.", false),
        WikiEntry("onychomycosis", "A highly common fungal infection that causes nails to become yellow, thick, and brittle.", false),
        WikiEntry("pale_nail", "Very pale nails can occasionally be a sign of serious illnesses, such as anemia, congestive heart failure, or liver disease.", true),
        WikiEntry("pitting", "Small depressions or 'ice pick' dents in the surface of the nail. Very common in individuals with psoriasis.", false),
        WikiEntry("psoriasis", "Nail psoriasis changes the appearance of nails causing pitting, abnormal growth, and discoloration (often a yellowish-red drop under the nail).", false),
        WikiEntry("red_lunula", "The usually white half-moon shape at the base of the nail turns red. Can be linked to cardiovascular issues or underlying systemic conditions.", true),
        WikiEntry("splinter_hemorrhage", "Thin, red-to-reddish-brown lines of blood under the nails. Can be caused by local trauma or serious heart valve infections (endocarditis).", true),
        WikiEntry("terrys_nail", "The entire nail appears white except for a narrow pink or red band at the top edge. Highly associated with severe liver disease or kidney failure.", true),
        WikiEntry("yellow_nails", "Nails thicken and grow slower, turning yellowish. Can be linked to chronic respiratory diseases or severe lymphatic problems.", true)
    ).sortedBy { langManager.getConditionName(it.conditionKey) }

    Scaffold(
        containerColor = iOSBg,
        floatingActionButton = {
            VoiceFab(
                voiceState = voiceState,
                onMicClick = onVoiceMicClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(iOSBg)
        ) {
            // Header
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
                        text = "EDUCATIONAL", 
                        color = MintColor, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Health Wiki", 
                        color = DeepColor, 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = -1.sp
                    )
                }
            }

            // Description Header
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                color = iOSCard,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MintColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.MenuBook, contentDescription = null, tint = MintColor)
                    }
                    Text(
                        text = "A comprehensive guide to understanding the various health signals our AI detects in your clinical scans.",
                        color = MutedColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val voiceActions = com.nailvital.app.voice.LocalVoiceActions.current

            androidx.compose.runtime.LaunchedEffect(voiceActions) {
                voiceActions?.collect { action ->
                    when (action) {
                        is com.nailvital.app.voice.VoiceAction.ScrollDown -> {
                            val current = listState.firstVisibleItemIndex
                            listState.animateScrollToItem(current + 4)
                        }
                        is com.nailvital.app.voice.VoiceAction.ScrollUp -> {
                            val current = listState.firstVisibleItemIndex
                            listState.animateScrollToItem(kotlin.math.max(0, current - 4))
                        }
                        else -> {}
                    }
                }
            }

            // Wiki List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(conditions) { wiki ->
                    WikiCard(wiki, langManager)
                }
            }
        }
    }
}

@Composable
fun WikiCard(entry: WikiEntry, langManager: LanguageManager) {
    Surface(
        color = iOSCard,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = langManager.getConditionIcon(entry.conditionKey),
                        contentDescription = null,
                        tint = if (entry.isSevere) WarnColor else MintColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = langManager.getConditionName(entry.conditionKey),
                        color = DeepColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (entry.isSevere) {
                    Surface(
                        color = WarnColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SEVERE",
                            color = WarnColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = entry.description,
                color = MutedColor,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
