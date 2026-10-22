package com.arham.ssorganizer.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.arham.ssorganizer.data.local.dao.AppCount
import com.arham.ssorganizer.data.local.dao.ChatCount
import com.arham.ssorganizer.data.local.entity.ScreenshotEntity
import com.arham.ssorganizer.data.preferences.UserPreferences
import com.arham.ssorganizer.data.repository.ScreenshotRepository
import com.arham.ssorganizer.ui.permission.hasStoragePermission
import com.arham.ssorganizer.worker.ScanWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class ScanProgressState(
    val isScanning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentName: String = "",
    val statusText: String = "",
    val errorText: String? = null
)

class ScreenshotViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "ScreenshotVM"
    }

    private val repository = ScreenshotRepository(application)
    val prefs = UserPreferences(application)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isClassifying = MutableStateFlow(false)
    val isClassifying: StateFlow<Boolean> = _isClassifying.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val isSyncRunning = AtomicBoolean(false)

    /** All screenshots from Room DB, automatically updating whenever DB changes. */
    val screenshots: StateFlow<List<ScreenshotEntity>> = repository.allScreenshotsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val doneCount: StateFlow<Int> = repository.doneCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = repository.pendingCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = repository.failedCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastScanTime: StateFlow<Long?> = repository.lastScanTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Breakdown of counts per detected app. */
    val appCounts: StateFlow<List<AppCount>> = repository.appCountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Query screenshots filtered by specific app name */
    fun getScreenshotsForApp(appName: String): Flow<List<ScreenshotEntity>> =
        repository.getScreenshotsByAppFlow(appName)

    /** Query screenshots filtered by app name and chat name */
    fun getScreenshotsForChat(appName: String, chatName: String?): Flow<List<ScreenshotEntity>> =
        repository.getScreenshotsByChatFlow(appName, chatName)

    /** Query conversation list with screenshot counts for a chat app */
    fun getChatCounts(appName: String): Flow<List<ChatCount>> =
        repository.getChatCountsFlow(appName)

    /** Search screenshots dynamically with optional app and chat filters */
    fun searchScreenshots(
        query: String,
        appName: String? = null,
        chatName: String? = null
    ): Flow<List<ScreenshotEntity>> = repository.searchScreenshotsFlow(query, appName, chatName)

    /** Extra folders saved in DataStore. */
    val extraFolders: Flow<Set<String>> = prefs.extraFolders

    /** Live scan progress from WorkManager. */
    val scanProgress: StateFlow<ScanProgressState> = repository.scanWorkInfoFlow
        .map { workInfo ->
            if (workInfo != null) {
                when (workInfo.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        val progress = workInfo.progress
                        val current = progress.getInt(ScanWorker.KEY_CURRENT, 0)
                        val total = progress.getInt(ScanWorker.KEY_TOTAL, 0)
                        val status = progress.getString(ScanWorker.KEY_STATUS) ?: "Scanning..."
                        val name = progress.getString(ScanWorker.KEY_CURRENT_NAME) ?: ""
                        ScanProgressState(
                            isScanning = true,
                            current = current,
                            total = total,
                            currentName = name,
                            statusText = status
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        val err = workInfo.outputData.getString(ScanWorker.KEY_ERROR_MESSAGE)
                            ?: "Scanning encountered an error."
                        Log.e(TAG, "ScanWorker reported FAILED: $err")
                        ScanProgressState(isScanning = false, errorText = err)
                    }
                    else -> {
                        ScanProgressState(isScanning = false)
                    }
                }
            } else {
                ScanProgressState(isScanning = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanProgressState())

    init {
        if (hasStoragePermission(application)) {
            Log.d(TAG, "Permission granted -> syncing and classifying existing screenshots")
            syncWithMediaStore()
            runClassification()
        }
    }

    /** Re-classify all persisted screenshots using explainable rule engine. */
    fun runClassification() {
        viewModelScope.launch {
            _isClassifying.value = true
            try {
                repository.classifyAllPersistedScreenshots()
            } catch (e: Exception) {
                Log.e(TAG, "Classification error", e)
            } finally {
                _isClassifying.value = false
            }
        }
    }

    /** Save manual correction for a screenshot. */
    fun updateManualCorrection(mediaStoreId: Long, detectedApp: String, chatName: String?) {
        viewModelScope.launch {
            repository.updateManualCorrection(mediaStoreId, detectedApp, chatName)
        }
    }

    /** Synchronize MediaStore screenshots with Room database safely with mutex. */
    fun syncWithMediaStore() {
        if (!hasStoragePermission(getApplication())) return

        if (!isSyncRunning.compareAndSet(false, true)) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                val extras = prefs.extraFolders.first()
                repository.syncWithMediaStore(extras)
                // Classify any newly synced screenshots
                repository.classifyAllPersistedScreenshots()
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore sync failed", e)
                _syncError.value = e.message ?: "Failed to read screenshots"
            } finally {
                _isSyncing.value = false
                isSyncRunning.set(false)
            }
        }
    }

    fun startScan() {
        repository.startScan()
    }

    fun cancelScan() {
        repository.cancelScan()
    }

    fun addExtraFolder(path: String) {
        viewModelScope.launch {
            prefs.addExtraFolder(path)
            syncWithMediaStore()
        }
    }

    fun removeExtraFolder(path: String) {
        viewModelScope.launch {
            prefs.removeExtraFolder(path)
            syncWithMediaStore()
        }
    }
}
