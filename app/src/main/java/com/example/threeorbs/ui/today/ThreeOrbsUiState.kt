package com.example.threeorbs.ui.today

import com.example.threeorbs.domain.model.DayRecord
import com.example.threeorbs.domain.model.ThreeOrbsData

data class PendingUndo(val slot: Int, val snapshot: ThreeOrbsData, val text: String)

data class ThreeOrbsUiState(
    val loading: Boolean = true,
    val data: ThreeOrbsData = ThreeOrbsData(),
    val today: DayRecord = DayRecord(java.time.LocalDate.now().toString()),
    val panel: Panel = Panel.None,
    val selectedSlot: Int? = null,
    val pendingUndo: PendingUndo? = null,
    val message: String? = null,
)

sealed interface Panel {
    data object None : Panel
    data object Setup : Panel
    data object Add : Panel
    data object History : Panel
    data class Edit(val slot: Int) : Panel
    data class Archive(val slot: Int) : Panel
    data class Carry(val text: String) : Panel
}

sealed interface ThreeOrbsEvent {
    data class Setup(val texts: List<String>) : ThreeOrbsEvent
    data class Edit(val slot: Int, val text: String) : ThreeOrbsEvent
    data class Complete(val slot: Int) : ThreeOrbsEvent
    data object Undo : ThreeOrbsEvent
    data class Archive(val slot: Int) : ThreeOrbsEvent
    data class ArchiveHistory(val date: String, val slot: Int) : ThreeOrbsEvent
    data class Open(val panel: Panel) : ThreeOrbsEvent
    data class SelectReplacement(val slot: Int) : ThreeOrbsEvent
    data class Replace(val text: String) : ThreeOrbsEvent
    data class MoveGroup(val x: Float, val y: Float, val z: Float = 0f) : ThreeOrbsEvent
    data object Dismiss : ThreeOrbsEvent
}
