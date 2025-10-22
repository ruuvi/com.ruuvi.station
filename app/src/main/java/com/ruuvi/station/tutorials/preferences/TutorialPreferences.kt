package com.ruuvi.station.tutorials.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.tutorialPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tutorial_prefs")

class TutorialPreferences(
    private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.tutorialPrefsDataStore

    private fun keyFor(id: String) = booleanPreferencesKey("tutorial_hide_$id")

    fun shouldShow(tutorialId: String): Flow<Boolean> =
        dataStore.data.map { prefs ->
            val hide = prefs[keyFor(tutorialId)] ?: false
            !hide
        }

    fun setDontShowAgain(tutorialId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { it[keyFor(tutorialId)] = true }
        }
    }

    suspend fun reset(tutorialId: String) {
        dataStore.edit { it[keyFor(tutorialId)] = false }
    }
}