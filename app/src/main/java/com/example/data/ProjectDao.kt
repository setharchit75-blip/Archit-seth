package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): VideoProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Update
    suspend fun updateProject(project: VideoProject)

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("SELECT * FROM timeline_track_items WHERE projectId = :projectId ORDER BY startTimeMs ASC")
    fun getTrackItemsForProject(projectId: Int): Flow<List<TimelineTrackItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackItem(item: TimelineTrackItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackItems(items: List<TimelineTrackItem>)

    @Query("DELETE FROM timeline_track_items WHERE projectId = :projectId")
    suspend fun deleteTrackItemsForProject(projectId: Int)

    @Delete
    suspend fun deleteTrackItem(item: TimelineTrackItem)
}
