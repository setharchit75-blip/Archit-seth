package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.AIProjectResponse
import com.example.api.GeminiClient
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.data.TimelineTrackItem
import com.example.data.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EditorViewModel"
    private val repository: ProjectRepository

    // Central state flows
    val allProjects: StateFlow<List<VideoProject>>
    
    private val _selectedProject = MutableStateFlow<VideoProject?>(null)
    val selectedProject: StateFlow<VideoProject?> = _selectedProject.asStateFlow()

    private val _timelineItems = MutableStateFlow<List<TimelineTrackItem>>(emptyList())
    val timelineItems: StateFlow<List<TimelineTrackItem>> = _timelineItems.asStateFlow()

    // Playback Engine State
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // AI Tools State
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiResult = MutableStateFlow<AIProjectResponse?>(null)
    val aiResult: StateFlow<AIProjectResponse?> = _aiResult.asStateFlow()

    private val _isSmartTrimming = MutableStateFlow(false)
    val isSmartTrimming: StateFlow<Boolean> = _isSmartTrimming.asStateFlow()

    private val _isGeneratingSubtitles = MutableStateFlow(false)
    val isGeneratingSubtitles: StateFlow<Boolean> = _isGeneratingSubtitles.asStateFlow()

    private val _isEnhancingAudio = MutableStateFlow(false)
    val isEnhancingAudio: StateFlow<Boolean> = _isEnhancingAudio.asStateFlow()

    // Cloud syncing state
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    // Exporting states
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportMessage = MutableStateFlow("")
    val exportMessage: StateFlow<String> = _exportMessage.asStateFlow()

    // Editor live controllers
    val watermarkText = MutableStateFlow("VeloEdit VIP PRO")
    val watermarkPosition = MutableStateFlow("BOTTOM_RIGHT")
    val watermarkOpacity = MutableStateFlow(0.7f)
    val isWatermarkEnabled = MutableStateFlow(true)
    val selectedAspectRatio = MutableStateFlow("16:9")

    // --- ADVANCED FEATURE SUITES ---

    // 1. AI Smart Crop & Auto-Reframe
    private val _isSmartCropping = MutableStateFlow(false)
    val isSmartCropping: StateFlow<Boolean> = _isSmartCropping.asStateFlow()
    
    val smartCropSubjectX = MutableStateFlow(0.5f) // Center coordinate x
    val smartCropSubjectY = MutableStateFlow(0.5f) // Center coordinate y
    val smartCropZoom = MutableStateFlow(1.0f) // Zoom factor
    val smartCropLabel = MutableStateFlow("Uncropped (Default)")

    // 2. AI Highlight Reel / Summaries
    private val _isGeneratingHighlightReel = MutableStateFlow(false)
    val isGeneratingHighlightReel: StateFlow<Boolean> = _isGeneratingHighlightReel.asStateFlow()
    
    private val _highlightReelMessage = MutableStateFlow("")
    val highlightReelMessage: StateFlow<String> = _highlightReelMessage.asStateFlow()

    // 3. Advanced Audio Editing Suite
    val isNoiseReductionEnabled = MutableStateFlow(false)
    val isEchoCancellationEnabled = MutableStateFlow(false)
    val eqBass = MutableStateFlow(0f)      // -12dB to 12dB
    val eqMid = MutableStateFlow(0f)       // -12dB to 12dB
    val eqTreble = MutableStateFlow(0f)    // -12dB to 12dB
    val isAudioDuckingEnabled = MutableStateFlow(true)
    val duckingLevel = MutableStateFlow(0.6f) // 0.0 to 1.0
    
    val isVoiceoverRecording = MutableStateFlow(false)
    val voiceoverDurationSec = MutableStateFlow(0)
    val voiceoverWaveform = MutableStateFlow<List<Float>>(emptyList())
    private var voiceoverJob: Job? = null

    // 4. Collaborative Teamwork Suite
    val collaborators = MutableStateFlow(listOf("Archit (Owner)", "Priya (Lead Editor)", "Sam (Sound Master)"))
    val timelineComments = MutableStateFlow(listOf(
        TimelineComment(1, 1500L, "Archit (Owner)", "That intro cinematic filter is perfect! 🔥"),
        TimelineComment(2, 6200L, "Priya", "Can we apply a cross-zoom transition here?"),
        TimelineComment(3, 11000L, "Sam", "Audio ducking is active, vocal clarity is excellent.")
    ))
    val projectVersions = MutableStateFlow(listOf(
        ProjectVersion(1, "v1.0 - Assembly Draft", "10 mins ago", 3),
        ProjectVersion(2, "v1.1 - Added VFX & AI Subtitles", "3 mins ago", 6),
        ProjectVersion(3, "v1.2 - Final Audio Mix (Current)", "Just now", 7)
    ))
    val isSyncingWithCollaborators = MutableStateFlow(false)
    val lastCollaboratorActionMessage = MutableStateFlow<String?>(null)

    // 5. Ending Slide Branding ("editor ka tadka" by Archit)
    val isEndingSlideEnabled = MutableStateFlow(true) // Toggle to burn Archit's watermark slide
    val watermarkPresetSelected = MutableStateFlow("Archit • Editor Ka Tadka ⚡") // High-contrast watermark text preset

    // 6. Ultra-High Graphics & Cinematic Asset Pack (900MB Expansion)
    val isUltraGraphicsEnabled = MutableStateFlow(true)
    val isDownloadingAssetPack = MutableStateFlow(false)
    val assetPackProgress = MutableStateFlow(0f)
    val isAssetPackInstalled = MutableStateFlow(false)

    fun startDownloadingAssetPack() {
        if (isDownloadingAssetPack.value || isAssetPackInstalled.value) return
        isDownloadingAssetPack.value = true
        viewModelScope.launch {
            for (p in 1..100) {
                delay(30) // Simulate downloading fast
                assetPackProgress.value = p / 100f
            }
            isDownloadingAssetPack.value = false
            isAssetPackInstalled.value = true
            isUltraGraphicsEnabled.value = true // Automatically enable ultra graphics
        }
    }

    fun toggleUltraGraphics() {
        isUltraGraphicsEnabled.value = !isUltraGraphicsEnabled.value
    }

    private var playbackJob: Job? = null
    private var timelineItemsJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProjectRepository(database.projectDao())
        allProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Automatically load or create a default project on first boot if list is empty
        viewModelScope.launch {
            allProjects.collectLatest { projects ->
                if (projects.isEmpty()) {
                    createAndSelectDefaultProject()
                } else if (_selectedProject.value == null) {
                    selectProject(projects.first())
                }
            }
        }
    }

    private suspend fun createAndSelectDefaultProject() {
        val defaultProj = VideoProject(
            title = "My Cinematic Vlog #1",
            aspectRatio = "16:9",
            durationMs = 15000,
            watermarkText = "VeloEdit PRO",
            isWatermarkEnabled = true
        )
        val id = repository.insertProject(defaultProj)
        val fullProj = defaultProj.copy(id = id.toInt())
        
        // Add some default multi-track tracks
        val defaultTracks = listOf(
            TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "VIDEO",
                clipTitle = "Cinematic Forest Intro",
                clipColorHex = "#6C5CE7",
                startTimeMs = 0,
                durationMs = 5000,
                filterType = "CINEMATIC",
                transitionType = "FADE"
            ),
            TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "VIDEO",
                clipTitle = "Glitch Street Motion",
                clipColorHex = "#FD79A8",
                startTimeMs = 5000,
                durationMs = 5000,
                filterType = "GLITCH",
                transitionType = "GLITCH_WARP"
            ),
            TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "VIDEO",
                clipTitle = "Neon Cyber Outro",
                clipColorHex = "#0984E3",
                startTimeMs = 10000,
                durationMs = 5000,
                filterType = "CYBERPUNK",
                transitionType = "CROSS_ZOOM"
            ),
            TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "AUDIO",
                clipTitle = "Synthwave Chill Beat.mp3",
                clipColorHex = "#00B894",
                startTimeMs = 0,
                durationMs = 15000
            ),
            TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "EFFECT",
                clipTitle = "Neon Spark VFX",
                clipColorHex = "#F1C40F",
                startTimeMs = 2000,
                durationMs = 8000
            )
        )
        repository.insertTrackItems(defaultTracks)
        selectProject(fullProj)
    }

    fun selectProject(project: VideoProject) {
        _selectedProject.value = project
        watermarkText.value = project.watermarkText
        watermarkPosition.value = project.watermarkPosition
        watermarkOpacity.value = project.watermarkOpacity
        isWatermarkEnabled.value = project.isWatermarkEnabled
        selectedAspectRatio.value = project.aspectRatio

        // Cancel previous track collector
        timelineItemsJob?.cancel()
        timelineItemsJob = viewModelScope.launch {
            repository.getTrackItemsForProject(project.id).collect { items ->
                _timelineItems.value = items
            }
        }
        
        _currentTimeMs.value = 0L
        stopPlayback()
    }

    fun createNewProject(title: String, aspectRatio: String = "16:9") {
        viewModelScope.launch {
            val newProj = VideoProject(
                title = title.ifBlank { "New Project" },
                aspectRatio = aspectRatio,
                durationMs = 10000
            )
            val id = repository.insertProject(newProj)
            val fullProj = newProj.copy(id = id.toInt())

            // Add basic video clip placeholder
            val clip = TimelineTrackItem(
                projectId = id.toInt(),
                trackType = "VIDEO",
                clipTitle = "Scenic Capture",
                clipColorHex = "#6C5CE7",
                startTimeMs = 0,
                durationMs = 10000
            )
            repository.insertTrackItem(clip)
            selectProject(fullProj)
        }
    }

    fun deleteCurrentProject() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            stopPlayback()
            repository.deleteProject(current)
            repository.deleteTrackItemsForProject(current.id)
            _selectedProject.value = null
        }
    }

    // --- Timeline Playback Engine ---

    fun togglePlayback() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        val duration = _selectedProject.value?.durationMs ?: 10000L
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value) {
                delay(33) // ~30 fps updates
                val nextTime = _currentTimeMs.value + 33
                if (nextTime >= duration) {
                    _currentTimeMs.value = 0
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun seekTo(timeMs: Long) {
        val duration = _selectedProject.value?.durationMs ?: 10000L
        _currentTimeMs.value = timeMs.coerceIn(0L, duration)
    }

    // --- Watermark Customizations ---

    fun updateWatermarkText(text: String) {
        watermarkText.value = text
        saveProjectSettings()
    }

    fun updateWatermarkPosition(position: String) {
        watermarkPosition.value = position
        saveProjectSettings()
    }

    fun updateWatermarkOpacity(opacity: Float) {
        watermarkOpacity.value = opacity
        saveProjectSettings()
    }

    fun toggleWatermark(enabled: Boolean) {
        isWatermarkEnabled.value = enabled
        saveProjectSettings()
    }

    fun updateAspectRatio(ratio: String) {
        selectedAspectRatio.value = ratio
        saveProjectSettings()
    }

    private fun saveProjectSettings() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                watermarkText = watermarkText.value,
                watermarkPosition = watermarkPosition.value,
                watermarkOpacity = watermarkOpacity.value,
                isWatermarkEnabled = isWatermarkEnabled.value,
                aspectRatio = selectedAspectRatio.value,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            _selectedProject.value = updated
        }
    }

    // --- Timeline Items Manipulation ---

    fun addClip(trackType: String, title: String, colorHex: String, durationMs: Long) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            val items = _timelineItems.value
            val maxEndTime = items.filter { it.trackType == trackType }.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
            
            val newItem = TimelineTrackItem(
                projectId = current.id,
                trackType = trackType,
                clipTitle = title,
                clipColorHex = colorHex,
                startTimeMs = maxEndTime,
                durationMs = durationMs
            )
            repository.insertTrackItem(newItem)

            // Adjust project duration if needed
            val newEndTime = maxEndTime + durationMs
            if (newEndTime > current.durationMs) {
                val updatedProj = current.copy(durationMs = newEndTime, updatedAt = System.currentTimeMillis())
                repository.updateProject(updatedProj)
                _selectedProject.value = updatedProj
            }
        }
    }

    fun updateClipFilter(clip: TimelineTrackItem, filter: String) {
        viewModelScope.launch {
            repository.insertTrackItem(clip.copy(filterType = filter))
        }
    }

    fun updateClipTransition(clip: TimelineTrackItem, transition: String) {
        viewModelScope.launch {
            repository.insertTrackItem(clip.copy(transitionType = transition))
        }
    }

    fun deleteClip(clip: TimelineTrackItem) {
        viewModelScope.launch {
            repository.deleteTrackItem(clip)
            
            // Re-calculate project duration
            val remainingItems = _timelineItems.value.filter { it.id != clip.id }
            val maxEndTime = remainingItems.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 5000L
            val current = _selectedProject.value
            if (current != null && maxEndTime != current.durationMs) {
                val updated = current.copy(durationMs = maxOf(maxEndTime, 5000L), updatedAt = System.currentTimeMillis())
                repository.updateProject(updated)
                _selectedProject.value = updated
            }
        }
    }

    // --- AI Power Editing Tools ---

    fun triggerSmartTrim() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isSmartTrimming.value = true
            stopPlayback()
            delay(2000) // Simulate AI scan
            
            // Shorten silent video gaps / speed up pauses
            val currentClips = _timelineItems.value
            repository.deleteTrackItemsForProject(current.id)

            val trimmedClips = currentClips.map { clip ->
                if (clip.trackType == "VIDEO") {
                    val trimAmount = (clip.durationMs * 0.2f).toLong().coerceAtLeast(1000L)
                    clip.copy(
                        clipTitle = clip.clipTitle + " (AI Trimmed ✨)",
                        durationMs = clip.durationMs - trimAmount
                    )
                } else clip
            }

            // Readjust start times in sequence
            var currentVideoTime = 0L
            val finalClips = trimmedClips.map { clip ->
                if (clip.trackType == "VIDEO") {
                    val processed = clip.copy(startTimeMs = currentVideoTime)
                    currentVideoTime += clip.durationMs
                    processed
                } else clip
            }

            repository.insertTrackItems(finalClips)
            val finalDuration = finalClips.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 10000L
            val updated = current.copy(durationMs = finalDuration, updatedAt = System.currentTimeMillis())
            repository.updateProject(updated)
            _selectedProject.value = updated
            
            _isSmartTrimming.value = false
        }
    }

    fun triggerAiSubtitles() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isGeneratingSubtitles.value = true
            stopPlayback()
            delay(2500) // Simulate transcript

            // Add automatic caption overlay track clips
            val subtitles = listOf(
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "EFFECT",
                    clipTitle = "💬 Captions: Cinematic vlog opens",
                    clipColorHex = "#2980B9",
                    startTimeMs = 0,
                    durationMs = 4000
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "EFFECT",
                    clipTitle = "💬 Captions: Experiencing dynamic pacing",
                    clipColorHex = "#2980B9",
                    startTimeMs = 4000,
                    durationMs = 5000
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "EFFECT",
                    clipTitle = "💬 Captions: Crafted using Gemini AI",
                    clipColorHex = "#2980B9",
                    startTimeMs = 9000,
                    durationMs = 4000
                )
            )
            repository.insertTrackItems(subtitles)
            _isGeneratingSubtitles.value = false
        }
    }

    fun triggerAudioEnhance() {
        viewModelScope.launch {
            _isEnhancingAudio.value = true
            delay(2000) // Simulate AI speech isolation
            
            // Rename audio track
            val audioClips = _timelineItems.value.filter { it.trackType == "AUDIO" }
            audioClips.forEach { clip ->
                repository.insertTrackItem(clip.copy(clipTitle = clip.clipTitle.replace(".mp3", "") + " (AI Vocal Clean ✨).mp3"))
            }
            _isEnhancingAudio.value = false
        }
    }

    // --- AI Text to Video (Gemini Integration) ---

    fun generateAiVideo(prompt: String) {
        val current = _selectedProject.value ?: return
        if (prompt.isBlank()) return
        
        viewModelScope.launch {
            _isAiGenerating.value = true
            _aiResult.value = null
            stopPlayback()
            
            try {
                // Call real Gemini scriptwriter
                val response = GeminiClient.generateStoryboardFromPrompt(prompt)
                _aiResult.value = response
                
                // Set storyboard as our active project timeline clips!
                repository.deleteTrackItemsForProject(current.id)
                
                var startTime = 0L
                val scenesClips = response.scenes.map { scene ->
                    val durationMs = scene.durationSeconds * 1000L
                    val clip = TimelineTrackItem(
                        projectId = current.id,
                        trackType = "VIDEO",
                        clipTitle = "Scene ${scene.sceneNumber}: [${scene.cinematicCamera}] ${scene.dialogueOrSubtitle}",
                        clipColorHex = when (scene.filterSuggested) {
                            "CYBERPUNK" -> "#E17055"
                            "CINEMATIC" -> "#6C5CE7"
                            "VINTAGE" -> "#FFEAA7"
                            "GLITCH" -> "#D63031"
                            else -> "#0984E3"
                        },
                        startTimeMs = startTime,
                        durationMs = durationMs,
                        filterType = scene.filterSuggested,
                        transitionType = if (scene.sceneNumber > 1) "CROSS_ZOOM" else "NONE"
                    )
                    startTime += durationMs
                    clip
                }

                repository.insertTrackItems(scenesClips)

                // Add nice ambient score audio
                repository.insertTrackItem(
                    TimelineTrackItem(
                        projectId = current.id,
                        trackType = "AUDIO",
                        clipTitle = "AI Scoring: Ambient Synth Symphony",
                        clipColorHex = "#00B894",
                        startTimeMs = 0,
                        durationMs = startTime
                    )
                )

                // Add storyboard caption text overlays
                var overlayTime = 0L
                response.scenes.forEach { scene ->
                    val durationMs = scene.durationSeconds * 1000L
                    repository.insertTrackItem(
                        TimelineTrackItem(
                            projectId = current.id,
                            trackType = "EFFECT",
                            clipTitle = "Sub: " + scene.dialogueOrSubtitle,
                            clipColorHex = "#10AC84",
                            startTimeMs = overlayTime,
                            durationMs = durationMs
                        )
                    )
                    overlayTime += durationMs
                }

                val finalProj = current.copy(
                    title = "AI Gen: " + response.scriptTitle,
                    durationMs = startTime,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateProject(finalProj)
                _selectedProject.value = finalProj

            } catch (e: Exception) {
                Log.e(TAG, "Failed AI text to video generation", e)
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    // --- Cloud Storage Syncing ---

    fun syncProjectToCloud() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            delay(1800) // Simulating cloud sync upload
            
            val updated = current.copy(isCloudSynced = true, updatedAt = System.currentTimeMillis())
            repository.updateProject(updated)
            _selectedProject.value = updated
            
            _isCloudSyncing.value = false
        }
    }

    // --- High-Quality Video Exporting Engine ---

    fun exportVideo(resolution: String, fps: Int, codec: String) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isExporting.value = true
            _exportProgress.value = 0f
            _exportMessage.value = "Initializing $resolution encoder..."
            delay(800)

            val totalFrames = current.durationMs / 33 // Number of frames
            _exportMessage.value = "Encoding with $codec codec..."
            
            for (i in 1..totalFrames) {
                delay(15) // Fast simulation steps
                val progress = i.toFloat() / totalFrames
                _exportProgress.value = progress
                
                if (i % 20L == 0L || i == totalFrames) {
                    val percentage = (progress * 100).toInt()
                    _exportMessage.value = "Rendering Frame $i of $totalFrames ($percentage%) @ ${fps}FPS"
                }
            }

            _exportMessage.value = "Finalizing $resolution multiplexing with custom VIP watermark..."
            delay(1000)
            
            _exportMessage.value = "Export Complete! File saved to Gallery/VeloEdit/"
            delay(1500)
            _isExporting.value = false
        }
    }

    // --- ADVANCED AUDIO FUNCTIONS ---
    fun toggleNoiseReduction() {
        isNoiseReductionEnabled.value = !isNoiseReductionEnabled.value
    }

    fun toggleEchoCancellation() {
        isEchoCancellationEnabled.value = !isEchoCancellationEnabled.value
    }

    fun updateEq(bass: Float, mid: Float, treble: Float) {
        eqBass.value = bass
        eqMid.value = mid
        eqTreble.value = treble
    }

    fun toggleDucking() {
        isAudioDuckingEnabled.value = !isAudioDuckingEnabled.value
    }

    fun updateDuckingLevel(level: Float) {
        duckingLevel.value = level
    }

    fun startVoiceoverRecording() {
        if (isVoiceoverRecording.value) return
        isVoiceoverRecording.value = true
        voiceoverDurationSec.value = 0
        voiceoverWaveform.value = emptyList()
        
        voiceoverJob = viewModelScope.launch {
            while (isVoiceoverRecording.value) {
                delay(200)
                voiceoverDurationSec.value += 1
                val randomAmplitude = (30..100).random() / 100f
                voiceoverWaveform.value = voiceoverWaveform.value.takeLast(30) + randomAmplitude
            }
        }
    }

    fun stopVoiceoverRecordingAndInsert() {
        if (!isVoiceoverRecording.value) return
        isVoiceoverRecording.value = false
        voiceoverJob?.cancel()
        voiceoverJob = null

        val current = _selectedProject.value ?: return
        val recordDurationMs = voiceoverDurationSec.value * 200L // Simulate record duration mapping
        if (recordDurationMs < 500L) return

        viewModelScope.launch {
            val clip = TimelineTrackItem(
                projectId = current.id,
                trackType = "AUDIO",
                clipTitle = "Voiceover Studio (Custom Recording 🎙️)",
                clipColorHex = "#E17055", // High-contrast orange for recorded audio
                startTimeMs = _currentTimeMs.value,
                durationMs = recordDurationMs
            )
            repository.insertTrackItem(clip)
            
            // Auto adjust duration of project if voiceover exceeds it
            val newEndTime = _currentTimeMs.value + recordDurationMs
            if (newEndTime > current.durationMs) {
                val updatedProj = current.copy(durationMs = newEndTime, updatedAt = System.currentTimeMillis())
                repository.updateProject(updatedProj)
                _selectedProject.value = updatedProj
            }
        }
    }

    // --- AI SMART CROP & AUTO REFRAME ---
    fun triggerSmartCrop(aspectRatio: String) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isSmartCropping.value = true
            delay(1500) // Simulate AI object & eye tracking computation
            
            selectedAspectRatio.value = aspectRatio
            
            // Reframe target focus coordinates based on aspect ratio
            when (aspectRatio) {
                "9:16" -> {
                    smartCropSubjectX.value = 0.65f // Slightly shifted right to focus on main human face
                    smartCropSubjectY.value = 0.45f
                    smartCropZoom.value = 1.35f
                    smartCropLabel.value = "AI Center Face Locked 👁️"
                }
                "1:1" -> {
                    smartCropSubjectX.value = 0.50f // Perfectly centered square crop
                    smartCropSubjectY.value = 0.50f
                    smartCropZoom.value = 1.20f
                    smartCropLabel.value = "AI Subject Centered 🎯"
                }
                "16:9" -> {
                    smartCropSubjectX.value = 0.50f // Wide-screen cinema
                    smartCropSubjectY.value = 0.50f
                    smartCropZoom.value = 1.0f
                    smartCropLabel.value = "AI Cinematic Horizon 🎥"
                }
            }
            
            val updated = current.copy(
                aspectRatio = aspectRatio,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            _selectedProject.value = updated
            
            _isSmartCropping.value = false
        }
    }

    fun adjustSmartCropOffset(x: Float, y: Float) {
        smartCropSubjectX.value = x.coerceIn(0f, 1f)
        smartCropSubjectY.value = y.coerceIn(0f, 1f)
        smartCropLabel.value = "Manual Adjustment (Custom Reframing)"
    }

    fun adjustSmartCropZoom(zoom: Float) {
        smartCropZoom.value = zoom.coerceIn(0.5f, 3.0f)
        smartCropLabel.value = "Manual Adjustment (Custom Zoom)"
    }

    // --- AI HIGHLIGHT REEL / SUMMARIES ---
    fun generateHighlightReel(platform: String) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isGeneratingHighlightReel.value = true
            _highlightReelMessage.value = "AI Scanning video timeline for motion & high audio peaks..."
            delay(1200)
            
            _highlightReelMessage.value = "Identifying facial expressions & action moments..."
            delay(1000)
            
            _highlightReelMessage.value = "Slicing key clips & compiling social highlights..."
            delay(1000)

            repository.deleteTrackItemsForProject(current.id)

            // Generate high-energy dynamic clips
            val highlights = listOf(
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "VIDEO",
                    clipTitle = "🎬 AI Highlight #1: Epic Intro Drop",
                    clipColorHex = "#9b59b6",
                    startTimeMs = 0,
                    durationMs = 2500,
                    filterType = "GLITCH",
                    transitionType = "GLITCH_WARP"
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "VIDEO",
                    clipTitle = "🎬 AI Highlight #2: Peak Action Zoom",
                    clipColorHex = "#e74c3c",
                    startTimeMs = 2500,
                    durationMs = 3000,
                    filterType = "CINEMATIC",
                    transitionType = "CROSS_ZOOM"
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "VIDEO",
                    clipTitle = "🎬 AI Highlight #3: Neon Climax",
                    clipColorHex = "#3498db",
                    startTimeMs = 5500,
                    durationMs = 2500,
                    filterType = "CYBERPUNK",
                    transitionType = "WHIP_PAN"
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "AUDIO",
                    clipTitle = "🎵 Social-Beats Highlight Track (120BPM)",
                    clipColorHex = "#2ecc71",
                    startTimeMs = 0,
                    durationMs = 8000
                ),
                TimelineTrackItem(
                    projectId = current.id,
                    trackType = "EFFECT",
                    clipTitle = "⚡ VFX Peak Glitch Overlay",
                    clipColorHex = "#f1c40f",
                    startTimeMs = 2000,
                    durationMs = 4000
                )
            )

            repository.insertTrackItems(highlights)

            val updatedProj = current.copy(
                title = "${current.title} • AI Summary Reel ✨",
                durationMs = 8000L,
                aspectRatio = if (platform.contains("9:16")) "9:16" else current.aspectRatio,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updatedProj)
            _selectedProject.value = updatedProj
            
            _highlightReelMessage.value = "Successfully generated high-impact summary reel!"
            delay(1500)
            _isGeneratingHighlightReel.value = false
        }
    }

    // --- REAL-TIME COLLABORATIVE TIMELINE & VERSION CONTROL ---
    fun inviteCollaborator(email: String) {
        if (email.isBlank()) return
        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val currentList = collaborators.value.toMutableList()
        if (!currentList.contains(name)) {
            currentList.add("$name (Editor)")
            collaborators.value = currentList
        }
    }

    fun addTimelineComment(text: String, timestampMs: Long) {
        if (text.isBlank()) return
        val currentComments = timelineComments.value.toMutableList()
        val newComment = TimelineComment(
            id = (currentComments.maxOfOrNull { it.id } ?: 0) + 1,
            timestampMs = timestampMs,
            author = "Archit (Owner)",
            text = text
        )
        currentComments.add(newComment)
        timelineComments.value = currentComments
    }

    fun deleteTimelineComment(commentId: Int) {
        timelineComments.value = timelineComments.value.filter { it.id != commentId }
    }

    fun createNewVersion(name: String, author: String) {
        if (name.isBlank()) return
        val currentVersions = projectVersions.value.toMutableList()
        val nextId = (currentVersions.maxOfOrNull { it.id } ?: 0) + 1
        val newVersion = ProjectVersion(
            id = nextId,
            name = name,
            timestamp = "Just now",
            clipsCount = _timelineItems.value.size.coerceAtLeast(3)
        )
        currentVersions.add(newVersion)
        projectVersions.value = currentVersions
    }

    fun revertToVersion(version: ProjectVersion) {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            delay(1200) // Simulate downloading cloud snapshot

            // In response to reverting, simulate changing timeline clips count and titles
            repository.deleteTrackItemsForProject(current.id)
            val restoredClips = mutableListOf<TimelineTrackItem>()
            
            for (i in 1..version.clipsCount) {
                restoredClips.add(
                    TimelineTrackItem(
                        projectId = current.id,
                        trackType = if (i == version.clipsCount) "AUDIO" else "VIDEO",
                        clipTitle = "Restored Scene #$i [from ${version.name}]",
                        clipColorHex = if (i % 2 == 0) "#1abc9c" else "#34495e",
                        startTimeMs = (i - 1) * 3000L,
                        durationMs = 3000L
                    )
                )
            }
            repository.insertTrackItems(restoredClips)

            val finalDuration = restoredClips.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 9000L
            val updatedProj = current.copy(
                title = current.title.substringBefore(" • restored") + " • restored ${version.name}",
                durationMs = finalDuration,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updatedProj)
            _selectedProject.value = updatedProj

            // Add a comment reflecting the reversion
            val currentComments = timelineComments.value.toMutableList()
            currentComments.add(
                TimelineComment(
                    id = (currentComments.maxOfOrNull { it.id } ?: 0) + 1,
                    timestampMs = 0L,
                    author = "System",
                    text = "Project state reverted to ${version.name} by Archit"
                )
            )
            timelineComments.value = currentComments
            
            _isCloudSyncing.value = false
        }
    }

    fun simulateCollaboratorAction() {
        val current = _selectedProject.value ?: return
        viewModelScope.launch {
            isSyncingWithCollaborators.value = true
            lastCollaboratorActionMessage.value = "Priya is typing a comment..."
            delay(2000)

            val currentComments = timelineComments.value.toMutableList()
            currentComments.add(
                TimelineComment(
                    id = (currentComments.maxOfOrNull { it.id } ?: 0) + 1,
                    timestampMs = _currentTimeMs.value,
                    author = "Priya",
                    text = "Just refined this exact section. Looks incredibly clean! 🙌"
                )
            )
            timelineComments.value = currentComments
            lastCollaboratorActionMessage.value = "Priya added a new comment at ${formatTime(_currentTimeMs.value)}"
            
            delay(3000)
            
            // Real-time peer timeline edit emulation!
            lastCollaboratorActionMessage.value = "Sam is tweaking the audio track..."
            delay(2000)

            val currentClips = _timelineItems.value.toMutableList()
            val audioClip = currentClips.find { it.trackType == "AUDIO" }
            if (audioClip != null) {
                val updatedAudio = audioClip.copy(clipTitle = audioClip.clipTitle + " (Sam Mix 🔊)")
                repository.insertTrackItem(updatedAudio)
            }
            
            lastCollaboratorActionMessage.value = "Sam synchronized the live audio mix!"
            delay(2500)
            lastCollaboratorActionMessage.value = null
            isSyncingWithCollaborators.value = false
        }
    }

    private fun formatTime(ms: Long): String {
        val sec = (ms / 1000) % 60
        val min = (ms / 60000) % 60
        return String.format("%02d:%02d", min, sec)
    }
}

data class TimelineComment(
    val id: Int,
    val timestampMs: Long,
    val author: String,
    val text: String
)

data class ProjectVersion(
    val id: Int,
    val name: String,
    val timestamp: String,
    val clipsCount: Int
)

