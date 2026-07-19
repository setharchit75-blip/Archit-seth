package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.AIProjectResponse
import com.example.api.StoryboardScene
import com.example.data.TimelineTrackItem
import com.example.data.VideoProject
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainEditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp

    val selectedProject by viewModel.selectedProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Studio, 1: AI Magic, 2: Hub & Watermark
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Sync state values with viewModel
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val isSmartTrimming by viewModel.isSmartTrimming.collectAsState()
    val isGeneratingSubtitles by viewModel.isGeneratingSubtitles.collectAsState()
    val isEnhancingAudio by viewModel.isEnhancingAudio.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(DeepSlate)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = ElegantTextAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "VISIONARY AI",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Pro Editor • v4.2",
                            color = TextSoft,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ElegantDarkGray)
                            .border(1.dp, ActiveGrey, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "4K",
                            color = CyberPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Done",
                            tint = CyberPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPurple,
                            contentColor = ElegantTextAccent
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp).testTag("top_bar_export_button")
                    ) {
                        Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = ActiveGrey, thickness = 1.dp)
            }
        },
        bottomBar = {
            if (!isLandscape) {
                NavigationBar(
                    containerColor = DeepSlate,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Movie, contentDescription = "Studio") },
                        label = { Text("Studio", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberPurple,
                            selectedTextColor = CyberPurple,
                            indicatorColor = ElegantAccentPurple
                        ),
                        modifier = Modifier.testTag("nav_tab_studio")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Magic") },
                        label = { Text("AI Magic", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberPurple,
                            selectedTextColor = CyberPurple,
                            indicatorColor = ElegantAccentPurple
                        ),
                        modifier = Modifier.testTag("nav_tab_ai")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Cloud & VIP") },
                        label = { Text("Velo Hub", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldVip,
                            selectedTextColor = GoldVip,
                            indicatorColor = ElegantAccentPurple
                        ),
                        modifier = Modifier.testTag("nav_tab_hub")
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tablet Navigation Rail (Landscape support)
            if (isLandscape) {
                NavigationRail(
                    containerColor = DeepSlate,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "V",
                        color = CyberPurple,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(Icons.Default.Movie, contentDescription = "Studio") },
                        label = { Text("Studio") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = CyberPurple,
                            selectedTextColor = CyberPurple,
                            indicatorColor = ElegantAccentPurple
                        )
                    )
                    NavigationRailItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Magic") },
                        label = { Text("AI Magic") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = CyberPurple,
                            selectedTextColor = CyberPurple,
                            indicatorColor = ElegantAccentPurple
                        )
                    )
                    NavigationRailItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Cloud & VIP") },
                        label = { Text("Velo Hub") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = GoldVip,
                            selectedTextColor = GoldVip,
                            indicatorColor = ElegantAccentPurple
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> StudioTab(
                        viewModel = viewModel,
                        selectedProject = selectedProject,
                        allProjects = allProjects,
                        timelineItems = timelineItems,
                        currentTimeMs = currentTimeMs,
                        isPlaying = isPlaying,
                        isLandscape = isLandscape,
                        onCreateProjectClick = { showCreateProjectDialog = true },
                        onExportClick = { showExportDialog = true }
                    )
                    1 -> AiMagicTab(
                        viewModel = viewModel,
                        isAiGenerating = isAiGenerating,
                        aiResult = aiResult,
                        isSmartTrimming = isSmartTrimming,
                        isGeneratingSubtitles = isGeneratingSubtitles,
                        isEnhancingAudio = isEnhancingAudio
                    )
                    2 -> VeloHubTab(
                        viewModel = viewModel,
                        selectedProject = selectedProject,
                        isCloudSyncing = isCloudSyncing,
                        onExportClick = { showExportDialog = true }
                    )
                }

                // Global overlay processing states
                if (isSmartTrimming || isGeneratingSubtitles || isEnhancingAudio || isCloudSyncing) {
                    GlobalProcessOverlay(
                        title = when {
                            isSmartTrimming -> "AI Smart Trimming..."
                            isGeneratingSubtitles -> "AI Transcribing captions..."
                            isEnhancingAudio -> "AI Audio Vocal Isolate..."
                            else -> "VeloCloud Syncing..."
                        },
                        subtitle = "Executing premium Hollywood neural filters..."
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { title, ratio ->
                viewModel.createNewProject(title, ratio)
                showCreateProjectDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportVideoDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }
}

// ==========================================
// STUDIO TAB (TIMELINE & PLAYER SCREEN)
// ==========================================

@Composable
fun StudioTab(
    viewModel: EditorViewModel,
    selectedProject: VideoProject?,
    allProjects: List<VideoProject>,
    timelineItems: List<TimelineTrackItem>,
    currentTimeMs: Long,
    isPlaying: Boolean,
    isLandscape: Boolean,
    onCreateProjectClick: () -> Unit,
    onExportClick: () -> Unit
) {
    if (selectedProject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextMuted)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Welcome to VeloEdit AI", color = PureWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Create your first video editing track below", color = TextMuted)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateProjectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                    modifier = Modifier.testTag("create_first_proj_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Cinematic Track")
                }
            }
        }
        return
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Live Preview (Left)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                ProjectHeader(
                    project = selectedProject,
                    allProjects = allProjects,
                    onProjectSelect = { viewModel.selectProject(it) },
                    onCreateClick = onCreateProjectClick,
                    onDeleteClick = { viewModel.deleteCurrentProject() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                VideoPlayerPreview(
                    project = selectedProject,
                    viewModel = viewModel,
                    timelineItems = timelineItems,
                    currentTimeMs = currentTimeMs,
                    isPlaying = isPlaying,
                    onTogglePlayback = { viewModel.togglePlayback() },
                    onSeek = { viewModel.seekTo(it) },
                    onExportClick = onExportClick
                )
            }
            // Multi-track Timeline & Controls (Right)
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                MultiTrackTimeline(
                    project = selectedProject,
                    timelineItems = timelineItems,
                    currentTimeMs = currentTimeMs,
                    onSeek = { viewModel.seekTo(it) },
                    onClipClick = { /* Can manage clip specific editing */ },
                    onClipDelete = { viewModel.deleteClip(it) },
                    onAddClipClick = { type ->
                        viewModel.addClip(type, "New $type Track", "#9b59b6", 4000L)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP - Video Preview & controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                ProjectHeader(
                    project = selectedProject,
                    allProjects = allProjects,
                    onProjectSelect = { viewModel.selectProject(it) },
                    onCreateClick = onCreateProjectClick,
                    onDeleteClick = { viewModel.deleteCurrentProject() }
                )
                Spacer(modifier = Modifier.height(10.dp))
                VideoPlayerPreview(
                    project = selectedProject,
                    viewModel = viewModel,
                    timelineItems = timelineItems,
                    currentTimeMs = currentTimeMs,
                    isPlaying = isPlaying,
                    onTogglePlayback = { viewModel.togglePlayback() },
                    onSeek = { viewModel.seekTo(it) },
                    onExportClick = onExportClick
                )
            }

            Divider(color = ActiveGrey, thickness = 1.dp)

            // BOTTOM - Multi-track Timeline Editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                MultiTrackTimeline(
                    project = selectedProject,
                    timelineItems = timelineItems,
                    currentTimeMs = currentTimeMs,
                    onSeek = { viewModel.seekTo(it) },
                    onClipClick = { /* Select / edit clip filter/transitions */ },
                    onClipDelete = { viewModel.deleteClip(it) },
                    onAddClipClick = { type ->
                        val duration = 4000L
                        val defaultColor = when (type) {
                            "VIDEO" -> "#6C5CE7"
                            "AUDIO" -> "#00B894"
                            else -> "#E17055"
                        }
                        viewModel.addClip(type, "Cinematic $type Segment", defaultColor, duration)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHeader(
    project: VideoProject,
    allProjects: List<VideoProject>,
    onProjectSelect: (VideoProject) -> Unit,
    onCreateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(DeepSlate, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Movie, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))

        Box {
            TextButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.textButtonColors(contentColor = PureWhite),
                modifier = Modifier.testTag("project_dropdown_trigger")
            ) {
                Text(
                    project.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DeepSlate)
            ) {
                allProjects.forEach { proj ->
                    DropdownMenuItem(
                        text = { Text(proj.title, color = PureWhite) },
                        onClick = {
                            onProjectSelect(proj)
                            expanded = false
                        }
                    )
                }
                Divider(color = ActiveGrey)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Cinematic Track", color = NeonCyan)
                        }
                    },
                    onClick = {
                        onCreateClick()
                        expanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Cloud sync indicator
        Icon(
            imageVector = if (project.isCloudSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
            contentDescription = "Cloud Status",
            tint = if (project.isCloudSynced) GlowGreen else TextMuted,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Ratio Badge
        Box(
            modifier = Modifier
                .background(ActiveGrey, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                if (project.aspectRatio == "9:16") "Reels 9:16 📱" else "FHD 16:9 🎬",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (project.aspectRatio == "9:16") NeonCyan else CyberPurple
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Delete track
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.testTag("delete_project_btn")
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Delete", tint = CinemaCoral)
        }
    }
}

@Composable
fun VideoPlayerPreview(
    project: VideoProject,
    viewModel: EditorViewModel,
    timelineItems: List<TimelineTrackItem>,
    currentTimeMs: Long,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onExportClick: () -> Unit
) {
    // Determine active filter at currentTimeMs
    val activeVideoClip = timelineItems
        .filter { it.trackType == "VIDEO" && currentTimeMs >= it.startTimeMs && currentTimeMs < it.startTimeMs + it.durationMs }
        .firstOrNull()
    val activeFilter = activeVideoClip?.filterType ?: "NONE"
    val activeTransition = activeVideoClip?.transitionType ?: "NONE"
    
    // Find active subtitle/caption clip
    val activeSubtitleClip = timelineItems
        .filter { it.trackType == "EFFECT" && it.clipTitle.startsWith("Sub:") && currentTimeMs >= it.startTimeMs && currentTimeMs < it.startTimeMs + it.durationMs }
        .firstOrNull()
    val subtitleText = activeSubtitleClip?.clipTitle?.replace("Sub:", "")?.trim() ?: ""
    
    // Smart Crop States
    val cropX by viewModel.smartCropSubjectX.collectAsState()
    val cropY by viewModel.smartCropSubjectY.collectAsState()
    val cropZoom by viewModel.smartCropZoom.collectAsState()
    val smartCropLabel by viewModel.smartCropLabel.collectAsState()
    val isSmartCropping by viewModel.isSmartCropping.collectAsState()
    val isEndingSlideEnabled by viewModel.isEndingSlideEnabled.collectAsState()
    val isUltraGraphics by viewModel.isUltraGraphicsEnabled.collectAsState()
    
    var showManualCropControls by remember { mutableStateOf(false) }

    // Calculate Aspect Ratio modifier
    val previewAspectRatio = when (project.aspectRatio) {
        "9:16" -> 9f / 16f
        "1:1" -> 1f
        else -> 16f / 9f
    }
    
    val isAtEnd = currentTimeMs >= project.durationMs - 300L && isEndingSlideEnabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSlate, RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Live Screen Canvas Frame
        Box(
            modifier = Modifier
                .fillMaxWidth(if (project.aspectRatio == "9:16") 0.45f else if (project.aspectRatio == "1:1") 0.65f else 1f)
                .aspectRatio(previewAspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .background(PitchBlack)
                .border(1.dp, ActiveGrey, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isAtEnd) {
                // Ending slide watermark "Archit - Editor Ka Tadka"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F0C1B))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = ElegantTextAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "EDITOR KA TADKA ⚡",
                            color = GoldVip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Directed & Edited by Archit",
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Rendered via Visionary AI Engine",
                            color = TextMuted,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                // Beautiful dynamic canvas that draws animated scenes based on current playback and filters!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    withTransform({
                        this.scale(cropZoom, cropZoom, center)
                        this.translate(
                            left = (0.5f - cropX) * size.width * cropZoom,
                            top = (0.5f - cropY) * size.height * cropZoom
                        )
                    }) {
                        drawLiveAIGraphics(
                            filter = activeFilter,
                            transition = activeTransition,
                            timeMs = currentTimeMs,
                            durationMs = project.durationMs,
                            videoTitle = activeVideoClip?.clipTitle ?: "Scenic Render",
                            isUltra = isUltraGraphics
                        )
                    }
                }
            }

            // Custom Subtitle Overlay Renders here
            if (subtitleText.isNotEmpty() && !isAtEnd) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        subtitleText,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Custom Watermark Layer (renders directly on video, customizable!)
            if (project.isWatermarkEnabled && !isAtEnd) {
                val wmAlign = when (project.watermarkPosition) {
                    "TOP_LEFT" -> Alignment.TopStart
                    "TOP_RIGHT" -> Alignment.TopEnd
                    "BOTTOM_LEFT" -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                
                Box(
                    modifier = Modifier
                        .align(wmAlign)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Water, contentDescription = null, tint = GoldVip, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            project.watermarkText,
                            color = PureWhite.copy(alpha = project.watermarkOpacity),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // VIP Unlocked Watermark Notice
            if (!isAtEnd) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(GoldVip, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VIP UNLOCKED", color = GoldVip, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Smart cropping active scanning overlay
            if (isSmartCropping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Subject Reframing...", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active AI Crop Status and Manual Adjustment Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    smartCropLabel,
                    fontSize = 10.sp,
                    color = TextSoft,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(
                onClick = { showManualCropControls = !showManualCropControls },
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showManualCropControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (showManualCropControls) "Hide Manual Reframe" else "Manual Reframing",
                        fontSize = 10.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Expanded manual cropping adjustment sliders
        if (showManualCropControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchBlack, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "FINE-TUNE SUBJECT TRACKING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPurple,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // X slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Horiz Shift", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = cropX,
                        onValueChange = { viewModel.adjustSmartCropOffset(it, cropY) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple),
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                    Text(String.format("%.2f", cropX), color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                }

                // Y slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vert Shift", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = cropY,
                        onValueChange = { viewModel.adjustSmartCropOffset(cropX, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple),
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                    Text(String.format("%.2f", cropY), color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                }

                // Zoom slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom Scale", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(60.dp))
                    Slider(
                        value = cropZoom,
                        onValueChange = { viewModel.adjustSmartCropZoom(it) },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                    Text(String.format("%.1fx", cropZoom), color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Playback Seek Slider Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTime(currentTimeMs),
                color = TextSoft,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = currentTimeMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(project.durationMs.toFloat().coerceAtLeast(1000f)),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("playback_seek_slider"),
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = ActiveGrey,
                    thumbColor = NeonCyan
                )
            )
            Text(
                formatTime(project.durationMs),
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Live Controls Dock
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            IconButton(onClick = { onSeek(0L) }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Restart", tint = PureWhite)
            }
            IconButton(onClick = { onSeek((currentTimeMs - 2000).coerceAtLeast(0L)) }) {
                Icon(Icons.Default.Replay5, contentDescription = "Back 2s", tint = PureWhite)
            }
            
            // Major play action
            FloatingActionButton(
                onClick = onTogglePlayback,
                containerColor = NeonCyan,
                contentColor = PitchBlack,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("play_pause_fab")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = { onSeek((currentTimeMs + 2000).coerceIn(0L, project.durationMs)) }) {
                Icon(Icons.Default.Forward5, contentDescription = "Forward 2s", tint = PureWhite)
            }

            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = GoldVip, contentColor = PitchBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("open_export_dialog_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// Draw the visually matched canvas filters and mock render scenes
fun DrawScope.drawLiveAIGraphics(
    filter: String,
    transition: String,
    timeMs: Long,
    durationMs: Long,
    videoTitle: String,
    isUltra: Boolean = false
) {
    val progress = (timeMs % 1000) / 1000f
    val cycleTime = timeMs / 1000f

    clipRect {
        // Render base landscape environment elements
        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F0C1B), Color(0xFF050510))
        )
        drawRect(brush = backgroundGradient)

        // If ultra graphics is enabled, draw premium color grading and dynamic HDR light flares
        if (isUltra) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00F2FE).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.6f
                ),
                center = Offset(0f, 0f),
                radius = size.width * 0.6f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8E2DE2).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width, size.height),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width, size.height),
                radius = size.width * 0.6f
            )

            // Ultra fine high fidelity grain / ambient star field simulation
            for (j in 1..12) {
                val jFloat = j.toFloat()
                val angle = jFloat * 3.8f + cycleTime * 0.15f
                val dotX = (size.width * 0.5f + cos(angle) * (180f + jFloat * 8f)) % size.width
                val dotY = (size.height * 0.5f + sin(angle) * (120f + jFloat * 8f)) % size.height
                drawCircle(
                    color = Color(0xFFE0F7FA).copy(alpha = 0.45f + 0.35f * sin(cycleTime + jFloat)),
                    center = Offset(dotX, dotY),
                    radius = 2f
                )
            }
        }

        // Draw animated background objects depending on filter theme
        when (filter) {
            "CYBERPUNK" -> {
                // Cyberpunk Neon Grid Draw
                val gridColor = NeonCyan.copy(alpha = 0.4f)
                val spacing = 40f
                val offset = (progress * spacing) % spacing

                // Horizontal perspective grid
                for (y in (size.height / 2).toInt()..size.height.toInt() step spacing.toInt()) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 2f
                    )
                }
                // Perspective vertical rays converging in center
                val centerX = size.width / 2
                val horizonY = size.height / 2
                for (x in 0..size.width.toInt() step 60) {
                    drawLine(
                        color = gridColor,
                        start = Offset(centerX, horizonY),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = 2f
                    )
                }

                // Cyber neon sun sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CinemaCoral, Color.Transparent),
                        center = Offset(centerX, horizonY - 40f),
                        radius = 120f
                    ),
                    center = Offset(centerX, horizonY - 40f),
                    radius = 100f
                )
            }

            "CINEMATIC" -> {
                // Golden cinematic floating dust particles
                val centerX = size.width / 2
                val centerY = size.height / 2

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEE58).copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = size.width / 2
                    ),
                    center = Offset(centerX, centerY),
                    radius = size.width / 2
                )

                // Render cinematic floating dust stars
                for (i in 1..25) {
                    val angle = i * 2.3f + cycleTime * 0.1f
                    val radiusOffset = (sin(angle) * 150f).coerceAtLeast(0f)
                    val starX = (centerX + sin(angle) * (100f + radiusOffset)) % size.width
                    val starY = (centerY + sin(i.toFloat()) * (100f + radiusOffset)) % size.height
                    
                    drawCircle(
                        color = Color(0xFFFFF59D).copy(alpha = 0.6f + 0.4f * sin(cycleTime + i)),
                        center = Offset(starX, starY),
                        radius = 3f + (i % 3)
                    )
                }
            }

            "VINTAGE" -> {
                // Warm Sepia + Vertical Vintage scratch lines
                drawRect(color = Color(0xFFE2C993).copy(alpha = 0.25f))

                // Vertical noise lines
                val scratchX1 = (size.width * 0.3f + sin(cycleTime * 15f) * size.width * 0.4f).coerceIn(0f, size.width)
                drawLine(
                    color = Color.Black.copy(alpha = 0.4f),
                    start = Offset(scratchX1, 0f),
                    end = Offset(scratchX1 + (sin(cycleTime) * 10f), size.height),
                    strokeWidth = 1f
                )

                val scratchX2 = (size.width * 0.7f + sin(cycleTime * 28f) * size.width * 0.2f).coerceIn(0f, size.width)
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(scratchX2, 0f),
                    end = Offset(scratchX2, size.height),
                    strokeWidth = 1.5f
                )
            }

            "GLITCH" -> {
                // Glitch warp, chromatic horizontal offset slices
                drawRect(color = Color(0xFF001020))
                
                // Red/Cyan horizontal sliced lines
                val barY1 = (size.height * 0.4f + sin(cycleTime * 10f) * size.height * 0.3f).coerceIn(0f, size.height)
                drawRect(
                    color = NeonCyan.copy(alpha = 0.6f),
                    topLeft = Offset(0f, barY1),
                    size = Size(size.width, 15f)
                )

                val barY2 = (size.height * 0.7f + sin(cycleTime * 22f) * size.height * 0.2f).coerceIn(0f, size.height)
                drawRect(
                    color = CinemaCoral.copy(alpha = 0.7f),
                    topLeft = Offset(0f, barY2),
                    size = Size(size.width, 10f)
                )
            }

            else -> {
                // Standard default visual renders beautiful fluid wave oscillations
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                for (i in 0..3) {
                    val amplitude = 30f + i * 15f
                    val waveY = centerY + sin(cycleTime * 4f + i) * amplitude
                    
                    drawLine(
                        color = CyberPurple.copy(alpha = 0.5f - i * 0.1f),
                        start = Offset(0f, waveY),
                        end = Offset(size.width, waveY),
                        strokeWidth = 4f
                    )
                }
            }
        }

        // Apply visual transitions overlay if transitioning
        if (transition != "NONE" && progress < 0.25f) {
            val transProgress = progress / 0.25f
            when (transition) {
                "FADE" -> {
                    drawRect(color = PitchBlack.copy(alpha = 1f - transProgress))
                }
                "CROSS_ZOOM" -> {
                    drawCircle(
                        color = PitchBlack.copy(alpha = 1f - transProgress),
                        center = Offset(size.width/2, size.height/2),
                        radius = (size.width * (1f - transProgress))
                    )
                }
                "GLITCH_WARP" -> {
                    if (progress < 0.15f) {
                        drawRect(color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Draw track text indicators for cinematic polish
        // Drawing watermarks or scene numbers
        if (isUltra) {
            // Draw a subtle, glowing high-fidelity green indicator dot
            val dotAlpha = 0.5f + 0.5f * sin(cycleTime * 6f)
            drawCircle(
                color = GlowGreen.copy(alpha = dotAlpha),
                center = Offset(30f, 30f),
                radius = 6f
            )
            drawCircle(
                color = GlowGreen,
                center = Offset(30f, 30f),
                radius = 3f
            )
        }
    }
}

// ==========================================
// MULTI-TRACK TIMELINE COMPONENT
// ==========================================

@Composable
fun MultiTrackTimeline(
    project: VideoProject,
    timelineItems: List<TimelineTrackItem>,
    currentTimeMs: Long,
    onSeek: (Long) -> Unit,
    onClipClick: (TimelineTrackItem) -> Unit,
    onClipDelete: (TimelineTrackItem) -> Unit,
    onAddClipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val videoClips = timelineItems.filter { it.trackType == "VIDEO" }
    val audioClips = timelineItems.filter { it.trackType == "AUDIO" }
    val effectClips = timelineItems.filter { it.trackType == "EFFECT" }

    val timelineDurationMs = project.durationMs.coerceAtLeast(10000L)
    val scaleFactor = 0.05f // pixels per ms

    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepSlate, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Ruler & Navigation Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Gesture, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Multi-Track Timeline (Pinch/Drag seeking)", fontSize = 12.sp, color = TextSoft, fontWeight = FontWeight.SemiBold)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    "Playhead: ${formatTime(currentTimeMs)}",
                    fontSize = 11.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeline container supporting touch dragging playhead seeking!
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PitchBlack, RoundedCornerShape(12.dp))
                    .border(1.dp, ActiveGrey, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            // Intuitive seeking gesture: slide finger horizontally to scrub through film frames
                            val deltaMs = (dragAmount.x / scaleFactor).toLong()
                            onSeek((currentTimeMs + deltaMs).coerceIn(0L, timelineDurationMs))
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val seekMs = (offset.x / scaleFactor).toLong()
                            onSeek(seekMs.coerceIn(0L, timelineDurationMs))
                        }
                    }
            ) {
                // Vertical scrolling for tracks
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    // TRACK 1: Video Track
                    TimelineTrackRow(
                        trackLabel = "📽️ VIDEO",
                        clips = videoClips,
                        scaleFactor = scaleFactor,
                        timelineDurationMs = timelineDurationMs,
                        onClipClick = onClipClick,
                        onClipDelete = onClipDelete,
                        onAddClick = { onAddClipClick("VIDEO") }
                    )

                    Divider(color = ActiveGrey, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // TRACK 2: Audio Track
                    TimelineTrackRow(
                        trackLabel = "🎵 AUDIO",
                        clips = audioClips,
                        scaleFactor = scaleFactor,
                        timelineDurationMs = timelineDurationMs,
                        onClipClick = onClipClick,
                        onClipDelete = onClipDelete,
                        onAddClick = { onAddClipClick("AUDIO") }
                    )

                    Divider(color = ActiveGrey, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // TRACK 3: Effects/Subtitles Track
                    TimelineTrackRow(
                        trackLabel = "✨ VFX/TEXT",
                        clips = effectClips,
                        scaleFactor = scaleFactor,
                        timelineDurationMs = timelineDurationMs,
                        onClipClick = onClipClick,
                        onClipDelete = onClipDelete,
                        onAddClick = { onAddClipClick("EFFECT") }
                    )
                }

                // RED PLAYHEAD SEEKING LINE (Drawn dynamically!)
                val playheadOffset = currentTimeMs * scaleFactor
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = playheadOffset.dp)
                        .background(CinemaCoral)
                ) {
                    // Small top flag indicating playhead tip
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(CinemaCoral, CircleShape)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineTrackRow(
    trackLabel: String,
    clips: List<TimelineTrackItem>,
    scaleFactor: Float,
    timelineDurationMs: Long,
    onClipClick: (TimelineTrackItem) -> Unit,
    onClipDelete: (TimelineTrackItem) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header Label
        Column(
            modifier = Modifier.width(72.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(trackLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(ActiveGrey, RoundedCornerShape(4.dp))
                    .clickable { onAddClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                    Text("ADD", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Clips Row
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            clips.forEach { clip ->
                val clipStartOffset = clip.startTimeMs * scaleFactor
                val clipWidth = clip.durationMs * scaleFactor
                val clipColor = remember(clip.clipColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(clip.clipColorHex))
                    } catch (e: Exception) {
                        CyberPurple
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = clipStartOffset.dp)
                        .width(clipWidth.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(clipColor, clipColor.copy(alpha = 0.75f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .clickable { onClipClick(clip) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                clip.clipTitle,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onClipDelete(clip) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = PureWhite.copy(alpha = 0.8f), modifier = Modifier.size(10.dp))
                            }
                        }
                        if (clip.filterType != "NONE") {
                            Text(
                                "FX: " + clip.filterType,
                                fontSize = 7.sp,
                                color = GoldVip,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// AI MAGIC TAB (TEXT-TO-VIDEO & SMART TOOLS)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMagicTab(
    viewModel: EditorViewModel,
    isAiGenerating: Boolean,
    aiResult: AIProjectResponse?,
    isSmartTrimming: Boolean,
    isGeneratingSubtitles: Boolean,
    isEnhancingAudio: Boolean
) {
    var textPrompt by remember { mutableStateOf("") }
    
    val quickPrompts = listOf(
        "📱 Cinematic vertical Reel: cyberpunk neon traffic flowing fast",
        "🌌 Deep space flight toward a cosmic nebula hyperdrive",
        "🌊 Relaxing crystal ocean waves hitting volcanic sand",
        "🎸 Retro 80s vaporwave palm sunset driving loop"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Text-To-Video Suite (Powered by Gemini 3.5)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Describe your ideal video scene. VeloEdit AI will synthesize scripts, storyboard visuals, choose sound effects, and auto-compose a complete multitrack timeline.",
                        fontSize = 11.sp,
                        color = TextSoft
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = textPrompt,
                        onValueChange = { textPrompt = it },
                        placeholder = { Text("e.g. A sleek black sports car racing down a wet neon cyberpunk street at night, atmospheric glow", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ai_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = PitchBlack,
                            unfocusedContainerColor = PitchBlack,
                            focusedBorderColor = CyberPurple,
                            unfocusedBorderColor = ActiveGrey
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateAiVideo(textPrompt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_generate_video_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        enabled = !isAiGenerating && textPrompt.isNotBlank()
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Gemini Video Engine Synthesizing...")
                        } else {
                            Icon(Icons.Default.MovieFilter, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Movie Sequence", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Or select a Cinematic Blueprint:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(ActiveGrey, RoundedCornerShape(8.dp))
                                .clickable { textPrompt = prompt.substring(prompt.indexOf(":") + 1).trim() }
                                .padding(10.dp)
                        ) {
                            Text(prompt, fontSize = 11.sp, color = PureWhite)
                        }
                    }
                }
            }
        }

        // Live Storyboard compilation progress
        if (isAiGenerating) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = BorderStroke(1.dp, CyberPurple)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AI NEURAL COMPILING ACTIVE", color = CyberPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(color = CyberPurple, trackColor = ActiveGrey, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(NeonCyan, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1. Gemini creating Hollywood video structure...", fontSize = 10.sp, color = TextSoft)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(CinemaCoral, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("2. Matching cinematic filter palettes and SFX score...", fontSize = 10.sp, color = TextSoft)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(GoldVip, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3. Synthesizing multi-track clips on local DB...", fontSize = 10.sp, color = TextSoft)
                        }
                    }
                }
            }
        }

        // If storyboard is ready
        if (aiResult != null && !isAiGenerating) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    border = BorderStroke(1.dp, GlowGreen)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = GlowGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Core Generation Success!", color = GlowGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Title: " + aiResult.scriptTitle, fontWeight = FontWeight.Black, fontSize = 13.sp, color = PureWhite)
                        Text("Overview: " + aiResult.overview, fontSize = 11.sp, color = TextSoft)
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = ActiveGrey)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("STORYBOARD SEQUENCE PREVIEW (Loaded on Studio Tab):", fontSize = 10.sp, color = GoldVip, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        aiResult.scenes.forEach { scene ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("Scene ${scene.sceneNumber} (${scene.durationSeconds}s) - ${scene.cinematicCamera}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PureWhite)
                                Text(scene.visualDescription, fontSize = 10.sp, color = TextMuted)
                                Text("💬 voiceover: \"${scene.dialogueOrSubtitle}\"", fontSize = 10.sp, color = NeonCyan, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // AI POWER TOOLS SECTION
        item {
            Column {
                Text(
                    "AI Smart Power Tools (Reels & Shorts Optimizers)",
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Instant Hollywood grading and editing assistance in 1-tap", color = TextMuted, fontSize = 11.sp)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Smart Trim
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.triggerSmartTrim() }
                        .testTag("ai_smart_trim_btn"),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = CinemaCoral)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Smart Trim", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Trim silent pauses & dead space automatically", fontSize = 9.sp, color = TextMuted)
                    }
                }

                // Auto Subtitles
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.triggerAiSubtitles() }
                        .testTag("ai_subtitles_btn"),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.ClosedCaption, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Captions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Synthesize automatic screen dialogue overlay", fontSize = 9.sp, color = TextMuted)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vocal Clean
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.triggerAudioEnhance() }
                        .testTag("ai_audio_enhance_btn"),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.SettingsVoice, contentDescription = null, tint = GoldVip)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Audio Enhance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Isolate human vocals & reduce noise background", fontSize = 9.sp, color = TextMuted)
                    }
                }

                // AI reframing
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.triggerSmartCrop(viewModel.selectedAspectRatio.value) }
                        .testTag("ai_smart_crop_btn"),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = GlowGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Smart Crop", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PureWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Scan subjects & reframe across multi-dimensions", fontSize = 9.sp, color = TextMuted)
                    }
                }
            }
        }

        // Active project aspect ratio quick selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepSlate)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("PROJECT CANVAS DIMENSIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberPurple, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentRatio by viewModel.selectedAspectRatio.collectAsState()
                        listOf("16:9", "9:16", "1:1").forEach { ratio ->
                            val isSel = currentRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) CyberPurple else PitchBlack, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) CyberPurple else ActiveGrey, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateAspectRatio(ratio) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (ratio) {
                                        "16:9" -> "🎬 Landscape 16:9"
                                        "9:16" -> "📱 Reels 9:16"
                                        else -> "⏹️ Square 1:1"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) PureWhite else TextSoft
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI Highlight Reels Generator Card
        item {
            val isGeneratingHighlightReel by viewModel.isGeneratingHighlightReel.collectAsState()
            val highlightMessage by viewModel.highlightReelMessage.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                border = if (isGeneratingHighlightReel) BorderStroke(1.dp, NeonCyan) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Social Highlight Reels Generator", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Analyze video and extract key high-energy moments. Our neural engine will automatically compile a short, highly engaging social-ready clip suitable for Instagram Reels or TikTok.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isGeneratingHighlightReel) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(color = NeonCyan, trackColor = ActiveGrey, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Analyzing frames & audio spikes for viral hooks...", fontSize = 9.sp, color = NeonCyan)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.generateHighlightReel("9:16 Reels") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                            modifier = Modifier.fillMaxWidth().testTag("ai_highlight_reels_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze & Create Highlight Reels", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    highlightMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlowGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GlowGreen, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GlowGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, color = GlowGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// VELO HUB TAB (CLOUD & WATERMARK & VIP DOWNLOADS)
// ==========================================

@Composable
fun VeloHubTab(
    viewModel: EditorViewModel,
    selectedProject: VideoProject?,
    isCloudSyncing: Boolean,
    onExportClick: () -> Unit
) {
    var watermarkInputText by remember { mutableStateOf("") }
    var showDownloadCompleteMessage by remember { mutableStateOf(false) }

    // Initialize watermark text
    LaunchedEffect(selectedProject) {
        if (selectedProject != null) {
            watermarkInputText = selectedProject.watermarkText
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIP Active Card (All Features Unlocked!)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(2.dp, GoldVip, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = GoldVip, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIP ALL-ACCESS UNLOCKED ✨", color = GoldVip, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Every single advanced feature is unlocked! You have 4K encoding enabled, high-definition background keying, unlimited cloud synching, and custom watermark controls completely free.",
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("SUBSCRIPTION MODEL: FOREVER FREE PRO LICENSE", color = GoldVip, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cinematic High-Graphics Upgrade & Asset Pack (900MB)
        item {
            val isUltraEnabled by viewModel.isUltraGraphicsEnabled.collectAsState()
            val isDownloading by viewModel.isDownloadingAssetPack.collectAsState()
            val downloadProgress by viewModel.assetPackProgress.collectAsState()
            val isInstalled by viewModel.isAssetPackInstalled.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("high_graphics_card")
                    .border(
                        1.5.dp,
                        if (isUltraEnabled) NeonCyan else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ultra-High Graphics Upgrade", fontWeight = FontWeight.Black, color = PureWhite, fontSize = 15.sp)
                            Text("Engine Version: v3.5 HDR Studio Edition", color = TextSoft, fontSize = 10.sp)
                        }
                        
                        // Toggle for Ultra High Quality
                        Switch(
                            checked = isUltraEnabled,
                            onCheckedChange = { viewModel.toggleUltraGraphics() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = ActiveGrey
                            ),
                            modifier = Modifier.testTag("ultra_graphics_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable Ultra-High Graphics Mode for real-time canvas HDR glow rendering, higher density premium visual particles, and 120 FPS simulation speed.",
                        color = TextSoft,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = ActiveGrey.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Cinematic Asset Expansion Pack (900 MB)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = if (isInstalled) GlowGreen else GoldVip, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cinematic Asset Expansion Pack (900 MB)", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Download 900 MB of studio-quality filters, 4K ProRes dynamic light leaks, cinematic motion transitions, and LUT templates to maximize rendering depth.",
                        color = TextSoft,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isInstalled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlowGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, GlowGreen, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GlowGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("900 MB HD Expansion Pack Installed & Active! ✅", color = GlowGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else if (isDownloading) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Downloading Assets... (${(downloadProgress * 900).toInt()} MB / 900 MB)", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${(downloadProgress * 100).toInt()}% Done • 45 MB/s", color = GlowGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .testTag("download_progress_bar"),
                                color = NeonCyan,
                                trackColor = ActiveGrey
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startDownloadingAssetPack() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldVip, contentColor = PitchBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("download_900mb_pack_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download & Inject High-Gfx Pack (900 MB)", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Custom Watermark Lab
        if (selectedProject != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Water, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIP Custom Watermark Lab", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Add your personal logo label overlays to outputs, or toggle off watermarks completely for full branding freedom.",
                            color = TextSoft,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Enabled status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enable Watermark Overlays", color = PureWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = selectedProject.isWatermarkEnabled,
                                onCheckedChange = { viewModel.toggleWatermark(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = ActiveGrey
                                ),
                                modifier = Modifier.testTag("watermark_enable_switch")
                            )
                        }

                        if (selectedProject.isWatermarkEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Edit label
                            OutlinedTextField(
                                value = watermarkInputText,
                                onValueChange = {
                                    watermarkInputText = it
                                    viewModel.updateWatermarkText(it)
                                },
                                label = { Text("Watermark Text Label", color = TextMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("watermark_text_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = ActiveGrey
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Position anchor selector
                            Text("Watermark Position on Screen", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val positions = listOf("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT")
                                positions.forEach { pos ->
                                    val isSelected = selectedProject.watermarkPosition == pos
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) NeonCyan else ActiveGrey, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.updateWatermarkPosition(pos) }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            pos.replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PitchBlack else PureWhite
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Opacity slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Watermark Opacity:", color = PureWhite, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                                Slider(
                                    value = selectedProject.watermarkOpacity,
                                    onValueChange = { viewModel.updateWatermarkOpacity(it) },
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = NeonCyan,
                                        thumbColor = NeonCyan
                                    ),
                                    modifier = Modifier.weight(1f).testTag("watermark_opacity_slider")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cross-device VeloCloud projects syncing
        if (selectedProject != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Seamless Cross-Device VeloCloud Sync", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Sync your timeline assets, VFX settings, and custom watermark parameters. Start on Android and finish seamlessly on iOS or desktop.",
                            color = TextSoft,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud Sync Status", color = TextMuted, fontSize = 9.sp)
                                Text(
                                    if (selectedProject.isCloudSynced) "Synced & Cloud Saved ✅" else "Local Only (Sync Pending ⚠️)",
                                    color = if (selectedProject.isCloudSynced) GlowGreen else CinemaCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.syncProjectToCloud() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                                modifier = Modifier.testTag("cloud_sync_now_btn")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- Create Project Email Draft / Send Mail Card ---
        if (selectedProject != null) {
            item {
                val context = LocalContext.current
                val comments by viewModel.timelineComments.collectAsState()
                val versions by viewModel.projectVersions.collectAsState()
                val collaborators by viewModel.collaborators.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create & Share Project Email", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Open your favorite email client to draft a detailed summary of the current project state, comments, versions, and collaborators.",
                            color = TextSoft,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val commentSummary = comments.joinToString("\n") { "- [${formatTime(it.timestampMs)}] ${it.author}: ${it.text}" }
                                val versionSummary = versions.joinToString("\n") { "- Name: ${it.name} (${it.timestamp}, ${it.clipsCount} clips)" }
                                val collaboratorSummary = collaborators.joinToString(", ")
                                
                                val emailBody = """
                                    Hello,

                                    Here is the status report for our Visionary AI video project:

                                    PROJECT DETAILS:
                                    - Title: ${selectedProject.title}
                                    - Aspect Ratio: ${selectedProject.aspectRatio}
                                    - Duration: ${formatTime(selectedProject.durationMs)}
                                    - Watermark Enabled: ${if (selectedProject.isWatermarkEnabled) "Yes ('${selectedProject.watermarkText}')" else "No"}
                                    - Cloud Synced: ${if (selectedProject.isCloudSynced) "Yes" else "No"}

                                    ACTIVE COLLABORATORS:
                                    $collaboratorSummary

                                    PROJECT VERSIONS:
                                    ${versionSummary.ifEmpty { "No saved versions yet." }}

                                    TIMELINE COMMENTS & FEEDBACK:
                                    ${commentSummary.ifEmpty { "No comments yet." }}

                                    Best regards,
                                    ${if (collaborators.isNotEmpty()) collaborators.first().substringBefore(" (") else "Visionary AI Studio Team"}
                                """.trimIndent()

                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_SUBJECT, "Visionary AI Project Update: ${selectedProject.title}")
                                    putExtra(Intent.EXTRA_TEXT, emailBody)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Send Project Email"))
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("create_project_email_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open App to Create Mail", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- ADVANCED AUDIO EDITING FX CONSOLE ---
        if (selectedProject != null) {
            item {
                val noiseEnabled by viewModel.isNoiseReductionEnabled.collectAsState()
                val echoEnabled by viewModel.isEchoCancellationEnabled.collectAsState()
                val duckingEnabled by viewModel.isAudioDuckingEnabled.collectAsState()
                var isolateEnabled by remember { mutableStateOf(false) }
                val eqLow by viewModel.eqBass.collectAsState()
                val eqMid by viewModel.eqMid.collectAsState()
                val eqHigh by viewModel.eqTreble.collectAsState()
                val voiceoverRecording by viewModel.isVoiceoverRecording.collectAsState()
                val voiceoverSecs by viewModel.voiceoverDurationSec.collectAsState()
                val recordedDuration = voiceoverSecs * 1000L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsVoice, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Advanced Audio DSP Console", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Professional sound design suite including neural noise reduction, multi-band equalization, background ducking, and real-time voiceover recording.",
                            color = TextSoft,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switches Row 1
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Noise Reduction
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PitchBlack, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleNoiseReduction() }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Noise Reduction", color = TextMuted, fontSize = 9.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (noiseEnabled) "ACTIVE" else "OFF", color = if (noiseEnabled) GlowGreen else TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = noiseEnabled,
                                            onCheckedChange = { viewModel.toggleNoiseReduction() },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GlowGreen),
                                            modifier = Modifier.height(20.dp).testTag("noise_reduction_switch")
                                        )
                                    }
                                }
                            }

                            // Echo Cancellation
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PitchBlack, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleEchoCancellation() }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Echo Cancellation", color = TextMuted, fontSize = 9.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (echoEnabled) "ACTIVE" else "OFF", color = if (echoEnabled) GlowGreen else TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = echoEnabled,
                                            onCheckedChange = { viewModel.toggleEchoCancellation() },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GlowGreen),
                                            modifier = Modifier.height(20.dp).testTag("echo_cancellation_switch")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Switches Row 2
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Audio Ducking
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PitchBlack, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleDucking() }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Auto Ducking (BGM)", color = TextMuted, fontSize = 9.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (duckingEnabled) "ACTIVE" else "OFF", color = if (duckingEnabled) GlowGreen else TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = duckingEnabled,
                                            onCheckedChange = { viewModel.toggleDucking() },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GlowGreen),
                                            modifier = Modifier.height(20.dp).testTag("audio_ducking_switch")
                                        )
                                    }
                                }
                            }

                            // Vocal Isolate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(PitchBlack, RoundedCornerShape(8.dp))
                                    .clickable { isolateEnabled = !isolateEnabled }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("AI Vocal Isolate", color = TextMuted, fontSize = 9.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (isolateEnabled) "ACTIVE" else "OFF", color = if (isolateEnabled) GlowGreen else TextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = isolateEnabled,
                                            onCheckedChange = { isolateEnabled = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GlowGreen),
                                            modifier = Modifier.height(20.dp).testTag("vocal_isolate_switch")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- Equalizer Section ---
                        Text("MULTI-BAND GRAPHIC EQUALIZER (EQ)", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            // Low Band
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Low Bass (60Hz)", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = eqLow,
                                    onValueChange = { viewModel.updateEq(it, eqMid, eqHigh) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple),
                                    modifier = Modifier.weight(1f).height(24.dp).testTag("eq_low_slider")
                                )
                                Text("${eqLow.toInt()} dB", color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                            
                            // Mid Band
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Mids (1KHz)", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = eqMid,
                                    onValueChange = { viewModel.updateEq(eqLow, it, eqHigh) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple),
                                    modifier = Modifier.weight(1f).height(24.dp).testTag("eq_mid_slider")
                                )
                                Text("${eqMid.toInt()} dB", color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }

                            // High Band
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("High Treble (16KHz)", color = TextSoft, fontSize = 9.sp, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = eqHigh,
                                    onValueChange = { viewModel.updateEq(eqLow, eqMid, it) },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(thumbColor = CyberPurple, activeTrackColor = CyberPurple),
                                    modifier = Modifier.weight(1f).height(24.dp).testTag("eq_high_slider")
                                )
                                Text("${eqHigh.toInt()} dB", color = PureWhite, fontSize = 9.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- Voiceover Recording Section ---
                        Text("STUDIO VOICEOVER MICROPHONE", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (voiceoverRecording) {
                                        viewModel.stopVoiceoverRecordingAndInsert()
                                    } else {
                                        viewModel.startVoiceoverRecording()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(if (voiceoverRecording) CinemaCoral else ActiveGrey, CircleShape)
                                    .testTag("voiceover_record_btn")
                            ) {
                                Icon(
                                    imageVector = if (voiceoverRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = "Voiceover",
                                    tint = PureWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (voiceoverRecording) {
                                    Text("RECORDING VOICEOVER...", color = CinemaCoral, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Text("Duration: ${formatTime(recordedDuration)}", color = PureWhite, fontSize = 10.sp)
                                } else {
                                    Text("Microphone Ready (Studio Grade)", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        text = "Tap red mic button to speak & insert audio clip",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- REAL-TIME TEAM COLLABORATION WORKSPACE ---
        if (selectedProject != null) {
            item {
                val collaborators by viewModel.collaborators.collectAsState()
                val comments by viewModel.timelineComments.collectAsState()
                val versions by viewModel.projectVersions.collectAsState()
                val sessionActive by viewModel.isSyncingWithCollaborators.collectAsState()
                
                var inviteEmailInput by remember { mutableStateOf("") }
                var commentTextInput by remember { mutableStateOf("") }
                var showAddCommentForm by remember { mutableStateOf(false) }
                var showAddVersionForm by remember { mutableStateOf(false) }
                var versionNameInput by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real-Time Team Workspace & Version Vault", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Invite co-editors to your project, add time-locked markers or feedback comments, and rollback to historical layout backups.",
                            color = TextSoft,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Collaborators list & Invitation input
                        Text("TEAM EDITORS", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            // Render active co-editors using standard for loop
                            for (editor in collaborators) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(CyberPurple, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(editor.take(1).uppercase(), color = PureWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (editor == "poonamriturajseth@gmail.com" || editor.startsWith("Poonam")) "$editor (You) 👑" else editor,
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.size(6.dp).background(if (sessionActive) GlowGreen else TextMuted, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (sessionActive) "Active" else "Offline", color = if (sessionActive) GlowGreen else TextMuted, fontSize = 8.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Invite row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inviteEmailInput,
                                    onValueChange = { inviteEmailInput = it },
                                    placeholder = { Text("Enter collaborator email...", color = TextMuted, fontSize = 10.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("invite_email_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                                        focusedBorderColor = NeonCyan, unfocusedBorderColor = ActiveGrey
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (inviteEmailInput.isNotBlank()) {
                                            viewModel.inviteCollaborator(inviteEmailInput)
                                            inviteEmailInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                                    modifier = Modifier.height(40.dp).testTag("invite_collaborator_btn")
                                ) {
                                    Text("Invite", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Timeline feedback comments section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TIMELINE COMMENTS & FEEDBACK", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            TextButton(
                                onClick = { showAddCommentForm = !showAddCommentForm },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (showAddCommentForm) "Cancel" else "+ Add Comment", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            if (showAddCommentForm) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = commentTextInput,
                                        onValueChange = { commentTextInput = it },
                                        placeholder = { Text("Comment at active frame...", color = TextMuted, fontSize = 10.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .testTag("comment_text_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                                            focusedBorderColor = NeonCyan, unfocusedBorderColor = ActiveGrey
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (commentTextInput.isNotBlank()) {
                                                viewModel.addTimelineComment(commentTextInput, viewModel.currentTimeMs.value)
                                                commentTextInput = ""
                                                showAddCommentForm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen, contentColor = PitchBlack),
                                        modifier = Modifier.height(40.dp).testTag("submit_comment_btn")
                                    ) {
                                        Text("Post", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (comments.isEmpty()) {
                                Text("No timeline comments yet. Scrub playhead and add feedback!", color = TextMuted, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            } else {
                                for (comment in comments) {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(comment.author, color = GoldVip, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(ActiveGrey, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(formatTime(comment.timestampMs), color = PureWhite, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        Text(comment.text, color = PureWhite, fontSize = 10.sp)
                                        Divider(color = ActiveGrey.copy(alpha = 0.3f), modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Version history control vault
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VERSION HISTORY VAULT", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            TextButton(
                                onClick = { showAddVersionForm = !showAddVersionForm },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (showAddVersionForm) "Cancel" else "+ Save Version Checkpoint", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            if (showAddVersionForm) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = versionNameInput,
                                        onValueChange = { versionNameInput = it },
                                        placeholder = { Text("Checkpoint name (e.g., Color Graded v2)", color = TextMuted, fontSize = 10.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .testTag("version_name_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                                            focusedBorderColor = NeonCyan, unfocusedBorderColor = ActiveGrey
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (versionNameInput.isNotBlank()) {
                                                viewModel.createNewVersion(versionNameInput, "Poonam")
                                                versionNameInput = ""
                                                showAddVersionForm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen, contentColor = PitchBlack),
                                        modifier = Modifier.height(40.dp).testTag("submit_version_btn")
                                    ) {
                                        Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            for (version in versions) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(version.name, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("Saved • ${version.timestamp} • (${version.clipsCount} clips)", color = TextMuted, fontSize = 8.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.revertToVersion(version) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ActiveGrey, contentColor = PureWhite),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("restore_version_${version.id}_btn")
                                    ) {
                                        Text("Restore", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Divider(color = ActiveGrey.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Cross-Platform App Download Links (Android & iOS installer files)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepSlate),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = GoldVip)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download VeloEdit App Platforms", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scan the QR code below or tap to download installer bundles (.APK & .IPA) for Android and iOS devices.",
                        color = TextSoft,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Drawing an actual dynamic retro QR code pattern on Compose Canvas!
                        Canvas(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            drawMockQRCode()
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showDownloadCompleteMessage = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveGrey, contentColor = PureWhite),
                                modifier = Modifier.fillMaxWidth().testTag("download_android_apk_btn")
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = GlowGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Android Installer (.APK)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showDownloadCompleteMessage = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveGrey, contentColor = PureWhite),
                                modifier = Modifier.fillMaxWidth().testTag("download_ios_ipa_btn")
                            ) {
                                Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("iOS Installer Bundle (.IPA)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showDownloadCompleteMessage) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlowGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, GlowGreen, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "Downloading package bundle... High-speed mirror link active for quick project compilation access!",
                                color = GlowGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Draw a mock QR code representing direct project download links
fun DrawScope.drawMockQRCode() {
    val sizePx = size.width
    val cellSize = sizePx / 11f
    
    // Renders realistic QR modules (corners finder patterns and random modules)
    val colorPrimary = Color.Black

    // Finder Pattern (Top-Left)
    drawRect(color = colorPrimary, topLeft = Offset(0f, 0f), size = Size(cellSize * 3, cellSize * 3))
    drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize), size = Size(cellSize, cellSize))

    // Finder Pattern (Top-Right)
    drawRect(color = colorPrimary, topLeft = Offset(cellSize * 8, 0f), size = Size(cellSize * 3, cellSize * 3))
    drawRect(color = Color.White, topLeft = Offset(cellSize * 9, cellSize), size = Size(cellSize, cellSize))

    // Finder Pattern (Bottom-Left)
    drawRect(color = colorPrimary, topLeft = Offset(0f, cellSize * 8), size = Size(cellSize * 3, cellSize * 3))
    drawRect(color = Color.White, topLeft = Offset(cellSize, cellSize * 9), size = Size(cellSize, cellSize))

    // Static randomized QR cells
    val pseudoRandomPattern = listOf(
        Pair(4, 0), Pair(5, 0), Pair(6, 0),
        Pair(4, 2), Pair(6, 2), Pair(5, 3), Pair(6, 4),
        Pair(0, 5), Pair(2, 5), Pair(4, 5), Pair(7, 5), Pair(9, 5),
        Pair(3, 6), Pair(5, 6), Pair(6, 6), Pair(8, 6),
        Pair(4, 8), Pair(5, 8), Pair(6, 8), Pair(9, 8),
        Pair(5, 10), Pair(7, 10), Pair(10, 10)
    )

    pseudoRandomPattern.forEach { (x, y) ->
        drawRect(
            color = colorPrimary,
            topLeft = Offset(x * cellSize, y * cellSize),
            size = Size(cellSize, cellSize)
        )
    }
}

// ==========================================
// LOWER UTILITIES AND OVERLAYS
// ==========================================

@Composable
fun GlobalProcessOverlay(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(color = NeonCyan, strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(subtitle, color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var aspectRatio by remember { mutableStateOf("16:9") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, ActiveGrey, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text("Create Cinematic Studio Project", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title Name", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().testTag("new_project_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite, unfocusedTextColor = PureWhite,
                        focusedBorderColor = CyberPurple,
                        unfocusedBorderColor = ActiveGrey
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Select Canonical Aspect Ratio", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 16:9 Wide screen
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { aspectRatio = "16:9" }
                            .border(1.dp, if (aspectRatio == "16:9") CyberPurple else Color.Transparent, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = PitchBlack)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = if (aspectRatio == "16:9") CyberPurple else TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("16:9 FHD Wide🎬", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            Text("Long-Form Video", fontSize = 8.sp, color = TextMuted)
                        }
                    }

                    // 9:16 vertical Reel
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { aspectRatio = "9:16" }
                            .border(1.dp, if (aspectRatio == "9:16") NeonCyan else Color.Transparent, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = PitchBlack)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = if (aspectRatio == "9:16") NeonCyan else TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("9:16 vertical📱", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                            Text("Reels & Shorts", fontSize = 8.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onCreate(title, aspectRatio) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                        modifier = Modifier.testTag("create_project_confirm_btn")
                    ) {
                        Text("Compose Movie")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportVideoDialog(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    var selectedRes by remember { mutableStateOf("4K UHD (3840x2160)") }
    var selectedFps by remember { mutableStateOf(60) }
    var selectedCodec by remember { mutableStateOf("H.265 Pro (HEVC)") }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepSlate),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, ActiveGrey, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text("Export High-Res Video Studio", fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Render with custom overlays and watermark configurations", color = TextMuted, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(14.dp))

                if (!isExporting) {
                    // Resolution choice
                    Text("Select Output Resolution", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    listOf("4K UHD (3840x2160) - VIP PRO", "1080p Full HD (1920x1080)").forEach { res ->
                        val isSel = selectedRes.substring(0, 2) == res.substring(0, 2)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(if (isSel) CyberPurple.copy(alpha = 0.3f) else PitchBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSel) CyberPurple else ActiveGrey, RoundedCornerShape(8.dp))
                                .clickable { selectedRes = res }
                                .padding(12.dp)
                        ) {
                            Text(res, fontSize = 12.sp, color = if (isSel) PureWhite else TextSoft)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Frame rate choice
                    Text("Frame Rate Configuration", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 60).forEach { fps ->
                            val isSel = selectedFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) NeonCyan else PitchBlack, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) NeonCyan else ActiveGrey, RoundedCornerShape(8.dp))
                                    .clickable { selectedFps = fps }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${fps} FPS (Ultra Fluid)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) PitchBlack else PureWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Codec Choice
                    Text("Select Multiplexer Video Codec", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("H.264 Std", "H.265 Pro (HEVC)", "ProRes Lite").forEach { codec ->
                            val isSel = selectedCodec == codec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSel) GoldVip else PitchBlack, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) GoldVip else ActiveGrey, RoundedCornerShape(8.dp))
                                    .clickable { selectedCodec = codec }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(codec, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) PitchBlack else PureWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.exportVideo(selectedRes, selectedFps, selectedCodec) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldVip, contentColor = PitchBlack),
                            modifier = Modifier.testTag("export_trigger_btn")
                        ) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synthesize Movie File", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Rendering compiling monitor progress
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = GoldVip, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(exportMessage, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(PitchBlack, RoundedCornerShape(8.dp))
                                .border(1.dp, ActiveGrey, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(exportProgress)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(NeonCyan, GoldVip)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${(exportProgress * 100).toInt()}% Done", color = GoldVip, fontWeight = FontWeight.Black, fontSize = 12.sp)

                        if (exportProgress >= 1f) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                                    modifier = Modifier.weight(1f).testTag("export_finished_close_btn")
                                ) {
                                    Text("Close & Open", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                val context = LocalContext.current
                                val selectedProject = viewModel.selectedProject.value
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:")
                                            putExtra(Intent.EXTRA_SUBJECT, "Project Export Successful: ${selectedProject?.title ?: "Untitled"}")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                """
                                                    Hello!

                                                    My high-resolution video render was completed successfully!

                                                    Project Details:
                                                    - Title: ${selectedProject?.title ?: "Untitled Project"}
                                                    - Resolution: $selectedRes
                                                    - Frame Rate: $selectedFps FPS
                                                    - Codec: $selectedCodec

                                                    Sent from Visionary AI Studio
                                                """.trimIndent()
                                            )
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Send Email"))
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = PitchBlack),
                                    modifier = Modifier.weight(1f).testTag("export_finished_create_mail_btn")
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Create Mail", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Lower Utility Helpers ---

fun formatTime(timeMs: Long): String {
    val totalSec = timeMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val ms = (timeMs % 1000) / 100
    return String.format("%02d:%02d.%d", min, sec, ms)
}
