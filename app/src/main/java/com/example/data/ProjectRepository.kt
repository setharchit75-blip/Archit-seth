package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<VideoProject>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): VideoProject? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: VideoProject): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: VideoProject) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: VideoProject) {
        projectDao.deleteProject(project)
    }

    fun getTrackItemsForProject(projectId: Int): Flow<List<TimelineTrackItem>> {
        return projectDao.getTrackItemsForProject(projectId)
    }

    suspend fun insertTrackItem(item: TimelineTrackItem) {
        projectDao.insertTrackItem(item)
    }

    suspend fun insertTrackItems(items: List<TimelineTrackItem>) {
        projectDao.insertTrackItems(items)
    }

    suspend fun deleteTrackItemsForProject(projectId: Int) {
        projectDao.deleteTrackItemsForProject(projectId)
    }

    suspend fun deleteTrackItem(item: TimelineTrackItem) {
        projectDao.deleteTrackItem(item)
    }
}
