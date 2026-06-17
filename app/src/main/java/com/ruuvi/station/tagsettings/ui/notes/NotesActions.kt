package com.ruuvi.station.tagsettings.ui.notes

sealed interface NotesActions {
    data class EditNote(val note: String): NotesActions
    object UpdateNote: NotesActions
}

sealed interface NotesEffect {
    object NoteUpdated : NotesEffect
}