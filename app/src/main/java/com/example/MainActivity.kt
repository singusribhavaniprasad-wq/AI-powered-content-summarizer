package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.TextStyle
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.SummaryViewModel
import com.example.ui.UiState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: SummaryViewModel by viewModels()
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init TTS for native audio explanations support
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            } else {
                Log.e("MainActivity", "Text to Speech initialization failed")
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel, tts)
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// Sealed hierarchy representing screen state
sealed class ActiveScreen {
    object Dashboard : ActiveScreen()
    object PreferenceCenter : ActiveScreen()
    data class Deconstructor(val summary: ContentSummary) : ActiveScreen()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppContainer(
    viewModel: SummaryViewModel,
    tts: TextToSpeech?
) {
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Dashboard) }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSummary by viewModel.selectedSummary.collectAsStateWithLifecycle()

    // Preferences sync
    val preferredLanguage by viewModel.preferredLanguage.collectAsStateWithLifecycle()
    val preferredStyle by viewModel.preferredStyle.collectAsStateWithLifecycle()
    val difficultyLevel by viewModel.difficultyLevel.collectAsStateWithLifecycle()
    val customInstructions by viewModel.customInstructions.collectAsStateWithLifecycle()

    // Observe active summary selected to route there when loaded
    LaunchedEffect(selectedSummary) {
        if (selectedSummary != null && activeScreen is ActiveScreen.Dashboard) {
            activeScreen = ActiveScreen.Deconstructor(selectedSummary!!)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (activeScreen !is ActiveScreen.Deconstructor) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = activeScreen is ActiveScreen.Dashboard,
                        onClick = { activeScreen = ActiveScreen.Dashboard },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = activeScreen is ActiveScreen.PreferenceCenter,
                        onClick = { activeScreen = ActiveScreen.PreferenceCenter },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "AI Memory") },
                        label = { Text("AI Brain", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background cosmic gradients
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radialBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.08f), Color.Transparent),
                    radius = size.width * 1.2f
                )
                drawRect(brush = radialBrush)
            }

            AnimatedContent(
                targetState = activeScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "screen_trans"
            ) { screen ->
                when (screen) {
                    is ActiveScreen.Dashboard -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            summaries = summaries,
                            prefLang = preferredLanguage,
                            prefStyle = preferredStyle,
                            prefDifficulty = difficultyLevel,
                            prefCustom = customInstructions,
                            onExploreSummary = { summary ->
                                viewModel.selectSummary(summary)
                                activeScreen = ActiveScreen.Deconstructor(summary)
                            }
                        )
                    }
                    is ActiveScreen.PreferenceCenter -> {
                        PreferenceCenterScreen(
                            viewModel = viewModel,
                            prefLang = preferredLanguage,
                            prefStyle = preferredStyle,
                            prefDiff = difficultyLevel,
                            prefCustom = customInstructions
                        )
                    }
                    is ActiveScreen.Deconstructor -> {
                        DeconstructorDesk(
                            summary = screen.summary,
                            tts = tts,
                            onBackToDashboard = {
                                viewModel.selectSummary(null)
                                viewModel.setIdleState()
                                activeScreen = ActiveScreen.Dashboard
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: SummaryViewModel,
    uiState: UiState,
    summaries: List<ContentSummary>,
    prefLang: String,
    prefStyle: String,
    prefDifficulty: String,
    prefCustom: String,
    onExploreSummary: (ContentSummary) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(prefLang) }
    var selectedStyle by remember { mutableStateOf(prefStyle) }
    var selectedDifficulty by remember { mutableStateOf(prefDifficulty) }

    // Synchronize defaults on startup
    LaunchedEffect(prefLang, prefStyle, prefDifficulty) {
        selectedLanguage = prefLang
        selectedStyle = prefStyle
        selectedDifficulty = prefDifficulty
    }

    // Interactive input selector state
    var selectedTab by remember { mutableStateOf(0) } // 0 = Copy/Paste, 1 = YouTube URL, 2 = Docs
    var youtubeUrl by remember { mutableStateOf("") }
    var docNameSimulated by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // App Identity Header
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "AI-powered Content Summarizer",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Analyze any long-form assets and absorb them instantly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Stats Dashboard Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Summarizer Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Summarized: ${summaries.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Analysis Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Main Smart Compiler Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Input Learning Asset",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhiteState
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input type Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(4.dp)
                    ) {
                        val tabs = listOf("Copied Text", "YouTube Link", "Document Info")
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input Form based on Tab
                    when (selectedTab) {
                        0 -> {
                            OutlinedTextField(
                                value = rawText,
                                onValueChange = { rawText = it },
                                placeholder = { Text("Paste article body, pdf sections, meeting logs, audio logs or transcript content here...", color = TextGrayState) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .testTag("article_input_text"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                textStyle = TextStyle(fontSize = 14.sp)
                            )
                        }
                        1 -> {
                            Column {
                                OutlinedTextField(
                                    value = youtubeUrl,
                                    onValueChange = { youtubeUrl = it },
                                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = "YouTube", tint = Color.Red) },
                                    placeholder = { Text("Paste YouTube URL (e.g., https://youtu.be/...) ", color = TextGrayState) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("youtube_url_input"),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 Enter a relevant topic/title below if this is a video to assist transcript summarization:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = rawText,
                                    onValueChange = { rawText = it },
                                    placeholder = { Text("Topic details or custom video concepts...", color = TextGrayState) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        2 -> {
                            Column {
                                OutlinedTextField(
                                    value = docNameSimulated,
                                    onValueChange = { docNameSimulated = it },
                                    placeholder = { Text("Enter Document Name (e.g. Physics_Syllabus.pdf)...", color = TextGrayState) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = rawText,
                                    onValueChange = { rawText = it },
                                    placeholder = { Text("Paste extracted contents or outline details of the lecture notes/PDF files here...", color = TextGrayState) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Personalize Section with dropdown/expandable indicators
                    Text(
                        text = "Customize Summary Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Quick Toggle Options
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary Style Chip
                        AssistChip(
                            onClick = {
                                val nextStyle = when (selectedStyle) {
                                    "BULLET SUMMARY" -> "SHORT SUMMARY"
                                    "SHORT SUMMARY" -> "STUDENT NOTES"
                                    "STUDENT NOTES" -> "BEGINNER EXPLANATION"
                                    "BEGINNER EXPLANATION" -> "PROFESSIONAL"
                                    "PROFESSIONAL" -> "STORY STYLE"
                                    "STORY STYLE" -> "SOCIAL MEDIA"
                                    else -> "BULLET SUMMARY"
                                }
                                selectedStyle = nextStyle
                            },
                            label = { Text("Style: $selectedStyle") },
                            leadingIcon = { Icon(Icons.Default.List, contentDescription = null, size = 18.dp) }
                        )

                        // Language Chip
                        AssistChip(
                            onClick = {
                                val nextLang = when (selectedLanguage) {
                                    "English" -> "Telugu"
                                    "Telugu" -> "Hindi"
                                    "Hindi" -> "Tamil"
                                    "Tamil" -> "Kannada"
                                    else -> "English"
                                }
                                selectedLanguage = nextLang
                            },
                            label = { Text("Lang: $selectedLanguage") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, size = 18.dp) }
                        )

                        // Difficulty Level Chip
                        AssistChip(
                            onClick = {
                                val nextDiff = when (selectedDifficulty) {
                                    "Beginner" -> "Intermediate"
                                    "Intermediate" -> "Advanced"
                                    else -> "Beginner"
                                }
                                selectedDifficulty = nextDiff
                            },
                            label = { Text("Level: $selectedDifficulty") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, size = 18.dp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Execute Button
                    Button(
                        onClick = {
                            val promptToSend = if (selectedTab == 1) {
                                "YouTube URL: $youtubeUrl. Video Title/Concepts: $rawText"
                            } else {
                                rawText
                            }
                            viewModel.summarizeContent(
                                content = promptToSend,
                                mode = selectedStyle,
                                lang = selectedLanguage,
                                difficulty = selectedDifficulty,
                                customPrompt = prefCustom
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("summarize_action_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Summarize & De-clutter",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // AI Glimmer/Loading States & Error states
        item {
            AnimatedVisibility(
                visible = uiState is UiState.Loading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Synthesizing Knowledge Node...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Extracting semantic points, generating interactive study games, quizzes, and microflashcards. Please hold on.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState is UiState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val errMsg = (uiState as? UiState.Error)?.message ?: "Error"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compilation Miss",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Remember to open the Secrets panel in AI Studio to input your real GEMINI_API_KEY environment variable. Do not share raw keys in files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // History Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Summarization History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhiteState
                )
                Text(
                    text = "${summaries.size} saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (summaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved summaries yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(summaries) { summary ->
                HistorySummaryCard(
                    summary = summary,
                    onTap = { onExploreSummary(summary) },
                    onDelete = { viewModel.deleteSummary(summary.id) }
                )
            }
        }
    }
}

@Composable
fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}

@Composable
fun HistorySummaryCard(
    summary: ContentSummary,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    var isDeleteDialogVisible by remember { mutableStateOf(false) }

    if (isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogVisible = false },
            title = { Text("Delete Node?") },
            text = { Text("Are you sure you want to permanently erase the summary of '${summary.title}' from local database?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    isDeleteDialogVisible = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category & Category Emoji
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryEmoji = when (summary.category.lowercase().trim()) {
                        "technical" -> "💻"
                        "educational" -> "🧠"
                        "medical" -> "🏥"
                        "business" -> "📊"
                        "news" -> "📰"
                        "research" -> "🔬"
                        "motivational" -> "🔥"
                        "entertainment" -> "🍿"
                        else -> "📝"
                    }
                    Text(text = "$categoryEmoji ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = summary.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Sentiment marker
                val sentimentEmoji = when (summary.sentiment.lowercase().trim()) {
                    "positive" -> "💖"
                    "negative" -> "🌧️"
                    "motivational" -> "🔥"
                    "stressful" -> "🚨"
                    else -> "⚖️"
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$sentimentEmoji ${summary.sentiment}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { isDeleteDialogVisible = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete summary",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhiteState,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Body snippet
            Text(
                text = summary.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: timestamp or mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(summary.timestamp) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                    sdf.format(Date(summary.timestamp))
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = summary.mode,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = summary.language,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceCenterScreen(
    viewModel: SummaryViewModel,
    prefLang: String,
    prefStyle: String,
    prefDiff: String,
    prefCustom: String
) {
    var rawCustomCode by remember { mutableStateOf(prefCustom) }

    LaunchedEffect(prefCustom) {
        rawCustomCode = prefCustom
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Preference Header
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "AI Cognitive Engine Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Configure default summarizer preferences to shape summary profiles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section default values
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Preferred Language Core",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhiteState
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val languages = listOf("English", "Telugu", "Hindi", "Tamil", "Kannada")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        languages.forEach { lang ->
                            val isSelected = prefLang == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.savePreference("PREF_LANG", lang) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextWhiteState
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Preferred Core Learning Profile Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhiteState
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val styles = listOf("BULLET SUMMARY", "STUDENT NOTES", "BEGINNER EXPLANATION", "STORY STYLE")
                    styles.forEach { style ->
                        val isSelected = prefStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.savePreference("PREF_STYLE", style) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.savePreference("PREF_STYLE", style) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = style, style = MaterialTheme.typography.bodyMedium, color = TextWhiteState)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Difficulty Level Tuning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhiteState
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val difficulties = listOf("Beginner", "Intermediate", "Advanced")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        difficulties.forEach { diff ->
                            val isSelected = prefDiff == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.savePreference("PREF_DIFFICULTY", diff) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextWhiteState
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Cognitive Instructions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Custom Learning Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhiteState
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inject personalized instruction rules (e.g. 'Use visual metaphors', 'Highlight chemical equations')",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rawCustomCode,
                        onValueChange = {
                            rawCustomCode = it
                            viewModel.savePreference("PREF_CUSTOM", it)
                        },
                        placeholder = { Text("E.g., Speak like a college professor, explain acronyms first, construct custom analogies about cars...", color = TextGrayState) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Automatically synchronizes with your profile cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun DeconstructorDesk(
    summary: ContentSummary,
    tts: TextToSpeech?,
    onBackToDashboard: () -> Unit
) {
    var selectedDeskTab by remember { mutableStateOf(0) } // 0=Summary, 1=Highlights, 2=Flashcards, 3=Interactive Quiz, 4=Speech Support
    val tabs = listOf("Summary", "Highlights", "Flashcards", "Quiz", "Audio Notes")

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToDashboard,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhiteState,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${summary.category} • ${summary.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Category Indicator badge
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = summary.sentiment,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Horizontal Category Tab Menu
        ScrollableTabRow(
            selectedTabIndex = selectedDeskTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDeskTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, deskTitle ->
                val isSelected = selectedDeskTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedDeskTab = index },
                    text = {
                        Text(
                            text = deskTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Interactive Content display area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            when (selectedDeskTab) {
                0 -> DeconstructNotesTab(summary)
                1 -> HighlightsTab(summary)
                2 -> FlashcardsTab(summary)
                3 -> QuizTab(summary)
                4 -> AudioNotesTab(summary, tts)
            }
        }
    }
}

@Composable
fun DeconstructNotesTab(summary: ContentSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Main Summary Node",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary.summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhiteState,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Key Points Checklist
        if (summary.keyPoints.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Key Summary Points",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        summary.keyPoints.forEachIndexed { index, point ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "⚡",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhiteState
                                )
                            }
                        }
                    }
                }
            }
        }

        // Beginner Metaphor Explanation Block
        if (summary.simplifiedExplanation.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌱 ", fontSize = 20.sp)
                            Text(
                                text = "Simple Translation Metaphor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summary.simplifiedExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhiteState,
                            lineHeight = 22.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Actionable Insights
        if (summary.actionableInsights.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Actionable Next Steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        summary.actionableInsights.forEachIndexed { index, insight ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}. ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = insight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhiteState
                                )
                            }
                        }
                    }
                }
            }
        }

        // Extracted Tags
        if (summary.keywords.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Extracted Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summary.keywords) { kw ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "#$kw",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightsTab(summary: ContentSummary) {
    if (summary.smartHighlights.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Smart Highlights found in summary data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Smart Core Highlights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhiteState
            )
            Text(
                text = "Key logical quotes extracted directly to accelerate learning",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(summary.smartHighlights) { hl ->
            val highlightContainerColor = when (hl.type.lowercase().trim()) {
                "concept" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                "action" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                "deadline" -> SoftPink.copy(alpha = 0.12f)
                "term" -> TechTeal.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            val highlightBorderColor = when (hl.type.lowercase().trim()) {
                "concept" -> MaterialTheme.colorScheme.primary
                "action" -> MaterialTheme.colorScheme.secondary
                "deadline" -> SoftPink
                "term" -> TechTeal
                else -> MaterialTheme.colorScheme.outline
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = highlightContainerColor),
                border = BorderStroke(1.dp, highlightBorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = highlightBorderColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = hl.type.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${hl.text}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhiteState,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun FlashcardsTab(summary: ContentSummary) {
    if (summary.flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No flashcards generated for this item.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var activeCardIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Flip rotation setup
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flash_flip"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Rapid Active Recall",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhiteState
            )
            Text(
                text = "Card ${activeCardIndex + 1} of ${summary.flashcards.size} • Tap to Flip",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val card = summary.flashcards[activeCardIndex]

        // 3D Flip Card Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12 * density
                }
                .clickable { isFlipped = !isFlipped }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // FRONT
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Question",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhiteState,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // BACK (rotated back 180 so it displays correctly!)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Answer",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = card.back,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextWhiteState,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swipe Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeCardIndex > 0) {
                        isFlipped = false
                        activeCardIndex--
                    }
                },
                enabled = activeCardIndex > 0,
                modifier = Modifier
                    .background(
                        if (activeCardIndex > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.5f
                        ), CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Flashcard", tint = TextWhiteState)
            }

            Text(
                text = "${activeCardIndex + 1} / ${summary.flashcards.size}",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhiteState,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    if (activeCardIndex < summary.flashcards.size - 1) {
                        isFlipped = false
                        activeCardIndex++
                    }
                },
                enabled = activeCardIndex < summary.flashcards.size - 1,
                modifier = Modifier
                    .background(
                        if (activeCardIndex < summary.flashcards.size - 1) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.5f
                        ), CircleShape
                    )
                    .size(48.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Flashcard", tint = TextWhiteState)
            }
        }
    }
}

@Composable
fun QuizTab(summary: ContentSummary) {
    if (summary.quizQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No Interactive MCQ Quizzes generated.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableStateOf(0) }
    var answersSubmitted by remember { mutableStateOf(mutableMapOf<Int, Int>()) } // QuestionIndex -> OptionSelected
    var quizCompleted by remember { mutableStateOf(false) }

    val activeQuestion = summary.quizQuestions[currentQuestionIndex]

    if (quizCompleted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Summarization Assessment Complete!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhiteState,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val scorePercent = (score.toFloat() / summary.quizQuestions.size) * 100
            val feedbackMsg = when {
                scorePercent >= 80f -> "Outstanding! You have mastered this content. 🎓"
                scorePercent >= 60f -> "Great job! High semantic absorption. ⚡"
                else -> "Revision suggested. Check card notes again. 📝"
            }

            Text(
                text = "Your Score: $score / ${summary.quizQuestions.size} (${scorePercent.toInt()}%)",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feedbackMsg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    currentQuestionIndex = 0
                    selectedAnswerIndex = null
                    score = 0
                    answersSubmitted.clear()
                    quizCompleted = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Replay Assessment", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semantic Verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhiteState
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Q: ${currentQuestionIndex + 1} / ${summary.quizQuestions.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Question Screen
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = activeQuestion.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhiteState,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            val wasAnswered = answersSubmitted.containsKey(currentQuestionIndex)
            val selectedOption = answersSubmitted[currentQuestionIndex]

            activeQuestion.options.forEachIndexed { optIndex, option ->
                val isSelected = selectedAnswerIndex == optIndex || selectedOption == optIndex
                val isCorrect = optIndex == activeQuestion.correctIndex

                val cardBg = when {
                    wasAnswered && isCorrect -> Color(0xFF1B5E20).copy(alpha = 0.2f) // Light Green
                    wasAnswered && isSelected && !isCorrect -> Color(0xFFB71C1C).copy(alpha = 0.2f) // Light Red
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }

                val borderStrokeColor = when {
                    wasAnswered && isCorrect -> Color(0xFF4CAF50)
                    wasAnswered && isSelected && !isCorrect -> Color(0xFFF44336)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(enabled = !wasAnswered) { selectedAnswerIndex = optIndex },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderStrokeColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (optIndex) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else TextGrayState,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhiteState,
                            modifier = Modifier.weight(1f)
                        )

                        if (wasAnswered) {
                            if (isCorrect) {
                                Icon(Icons.Default.Check, "Correct Option", tint = Color(0xFF4CAF50))
                            } else if (isSelected) {
                                Icon(Icons.Default.Close, "Incorrect Option", tint = Color(0xFFF44336))
                            }
                        }
                    }
                }
            }

            // Correct Action Explanation Block
            if (wasAnswered) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, "Explanation", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rational Explanation:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhiteState,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Footer
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val wasAnswered = answersSubmitted.containsKey(currentQuestionIndex)

            if (!wasAnswered) {
                Button(
                    onClick = {
                        if (selectedAnswerIndex != null) {
                            answersSubmitted[currentQuestionIndex] = selectedAnswerIndex!!
                            if (selectedAnswerIndex == activeQuestion.correctIndex) {
                                score++
                            }
                        }
                    },
                    enabled = selectedAnswerIndex != null,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Check Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (currentQuestionIndex < summary.quizQuestions.size - 1) {
                            currentQuestionIndex++
                            selectedAnswerIndex = null
                        } else {
                            quizCompleted = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(
                        text = if (currentQuestionIndex < summary.quizQuestions.size - 1) "Next Question" else "Compile Score",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AudioNotesTab(summary: ContentSummary, tts: TextToSpeech?) {
    var isSpeakingState by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Audio Memory Assist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhiteState
            )
            Text(
                text = "Listen to your summary details to sync procedural memory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Sleek Audio Visualizer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isSpeakingState) {
                    // Pulsating waves
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(60.dp, 80.dp, 100.dp, 40.dp, 90.dp, 70.dp, 110.dp, 50.dp)
                        heights.forEachIndexed { i, h ->
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(h)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(8) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                }
            }
        }

        // Large circular Play/Pause button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isSpeakingState) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary)
                .clickable {
                    if (isSpeakingState) {
                        tts?.stop()
                        isSpeakingState = false
                    } else {
                        val spokenText = "${summary.title}. Summary nodes details: ${summary.summaryText}. Key summarization details: ${summary.keyPoints.joinToString(". ")}"
                        tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "SummarySpeak")
                        isSpeakingState = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSpeakingState) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isSpeakingState) "Pause audio" else "Play audio",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggested Follow ups
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Suggested Follow-up Areas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                summary.suggestedTopics.forEach { topic ->
                    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔍 ", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = topic, style = MaterialTheme.typography.bodySmall, color = TextWhiteState)
                    }
                }
            }
        }
    }
}
