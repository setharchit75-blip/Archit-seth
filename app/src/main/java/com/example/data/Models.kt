package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val aspectRatio: String = "16:9", // "16:9" (Long-form), "9:16" (Reels)
    val durationMs: Long = 10000,
    val watermarkText: String = "VeloEdit PRO",
    val watermarkPosition: String = "BOTTOM_RIGHT", // "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"
    val watermarkOpacity: Float = 0.6f,
    val isWatermarkEnabled: Boolean = true,
    val isCloudSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timeline_track_items")
data class TimelineTrackItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val trackType: String, // "VIDEO", "AUDIO", "EFFECT"
    val clipTitle: String,
    val clipColorHex: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val filterType: String = "NONE", // "NONE", "CINEMATIC", "CYBERPUNK", "VINTAGE", "GLITCH"
    val transitionType: String = "NONE" // "NONE", "FADE", "CROSS_ZOOM", "WHIP_PAN", "GLITCH_WARP"
)
