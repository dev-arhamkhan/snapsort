package com.arham.snapsort.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore instance — one per app process, tied to the Context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Persists user-adjustable settings using Jetpack DataStore.
 *
 * Currently stored:
 *  - extraFolders: Set of extra RELATIVE_PATH fragments the user wants scanned
 *    (in addition to the default "Screenshots" match).
 */
class UserPreferences(private val context: Context) {

    companion object {
        private val EXTRA_FOLDERS_KEY = stringSetPreferencesKey("extra_folders")
    }

    /** Emits the current set of extra folder strings (empty set by default). */
    val extraFolders: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[EXTRA_FOLDERS_KEY] ?: emptySet()
    }

    /** Replace the stored extra folders with [folders]. */
    suspend fun setExtraFolders(folders: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[EXTRA_FOLDERS_KEY] = folders
        }
    }

    /** Add a single [folder] path fragment (e.g. "DCIM/MyShots"). */
    suspend fun addExtraFolder(folder: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[EXTRA_FOLDERS_KEY] ?: emptySet()
            prefs[EXTRA_FOLDERS_KEY] = current + folder
        }
    }

    /** Remove a single [folder] path fragment. */
    suspend fun removeExtraFolder(folder: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[EXTRA_FOLDERS_KEY] ?: emptySet()
            prefs[EXTRA_FOLDERS_KEY] = current - folder
        }
    }
}
