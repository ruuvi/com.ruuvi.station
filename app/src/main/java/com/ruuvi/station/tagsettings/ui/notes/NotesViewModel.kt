package com.ruuvi.station.tagsettings.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NotesViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
): ViewModel() {

    private val _note = MutableStateFlow<String>(
        interactor.getFavouriteSensorById(sensorId)?.description ?: ""
    )
    val note: StateFlow<String> = _note

    private val _effects = MutableSharedFlow<NotesEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    fun onAction(action: NotesActions) {
        when (action) {
            is NotesActions.EditNote -> editNote(action.note)
            is NotesActions.UpdateNote -> updateNote()
        }
    }

    private fun editNote(newNote: String) {
        if (newNote.length <= 1000) {
            _note.value = newNote
        }
    }

    private fun updateNote() {
        interactor.updateDescription(sensorId, _note.value)
        viewModelScope.launch {
            _effects.emit(NotesEffect.NoteUpdated)
        }
    }
}