package com.example.threeorbs.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.threeorbs.data.repository.ThreeOrbsRepository
import com.example.threeorbs.domain.usecase.UpdateThreeOrbsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThreeOrbsViewModel(
    private val repository: ThreeOrbsRepository,
    private val rules: UpdateThreeOrbsUseCase = UpdateThreeOrbsUseCase(),
) : ViewModel() {
    private val _state = MutableStateFlow(ThreeOrbsUiState())
    val state: StateFlow<ThreeOrbsUiState> = _state.asStateFlow()
    private var undoJob: Job? = null

    init { viewModelScope.launch {
        val data = repository.data.first(); val today = rules.today(data)
        _state.value = ThreeOrbsUiState(false, data, today, if (today.slots.all { it.text.isBlank() }) Panel.Setup else Panel.None)
    } }

    fun onEvent(event: ThreeOrbsEvent) {
        when (event) {
            is ThreeOrbsEvent.Open -> _state.update { it.copy(panel = event.panel, selectedSlot = null) }
            ThreeOrbsEvent.Dismiss -> _state.update { it.copy(panel = Panel.None, selectedSlot = null, message = null) }
            is ThreeOrbsEvent.SelectReplacement -> _state.update { it.copy(selectedSlot = event.slot) }
            is ThreeOrbsEvent.Setup -> if (event.texts.size == 3 && event.texts.all { it.isNotBlank() }) commit(rules.seed(state.value.data, event.texts))
            is ThreeOrbsEvent.Edit -> if (event.text.isNotBlank()) commit(rules.replace(state.value.data, event.slot, event.text))
            is ThreeOrbsEvent.Replace -> state.value.selectedSlot?.let { if (event.text.isNotBlank()) commit(rules.replace(state.value.data, it, event.text)) }
            is ThreeOrbsEvent.Archive -> commit(rules.archive(state.value.data, event.slot))
            is ThreeOrbsEvent.ArchiveHistory -> commit(rules.archiveHistory(state.value.data, event.date, event.slot), "已归档放下")
            is ThreeOrbsEvent.Complete -> complete(event.slot)
            ThreeOrbsEvent.Undo -> state.value.pendingUndo?.let { undoJob?.cancel(); commit(it.snapshot, "已撤销"); _state.update { s -> s.copy(pendingUndo = null) } }
            is ThreeOrbsEvent.MoveGroup -> commit(
                state.value.data.copy(
                    groupX = event.x.coerceIn(-180f, 180f),
                    groupY = event.y.coerceIn(-170f, 170f),
                    groupZ = event.z.coerceIn(-120f, 120f),
                ),
                closePanel = false,
            )
        }
    }

    private fun complete(slot: Int) {
        val snapshot = state.value.data; val text = state.value.today.slots[slot].text
        val next = rules.complete(snapshot, slot, System.currentTimeMillis())
        commit(next, "星尘已飞向日期墙", closePanel = false)
        _state.update { it.copy(pendingUndo = PendingUndo(slot, snapshot, text)) }
        undoJob?.cancel(); undoJob = viewModelScope.launch { delay(2_000); _state.update { it.copy(pendingUndo = null) } }
    }

    private fun commit(data: com.example.threeorbs.domain.model.ThreeOrbsData, message: String? = null, closePanel: Boolean = true) {
        _state.update { it.copy(data = data, today = rules.today(data), panel = if (closePanel) Panel.None else it.panel, selectedSlot = null, message = message) }
        viewModelScope.launch { runCatching { repository.save(data) }.onFailure { _state.update { s -> s.copy(message = "本地记录暂时没有保存，请再试一次") } } }
    }

    class Factory(private val repository: ThreeOrbsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ThreeOrbsViewModel(repository) as T
    }
}
