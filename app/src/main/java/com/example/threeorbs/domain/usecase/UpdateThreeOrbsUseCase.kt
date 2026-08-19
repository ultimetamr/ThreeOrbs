package com.example.threeorbs.domain.usecase

import com.example.threeorbs.domain.model.*
import java.time.LocalDate

class UpdateThreeOrbsUseCase {
    fun today(data: ThreeOrbsData): DayRecord = data.days.firstOrNull { it.date == LocalDate.now().toString() }
        ?: DayRecord(LocalDate.now().toString())

    fun replace(data: ThreeOrbsData, slot: Int, text: String): ThreeOrbsData = updateToday(data) { day ->
        day.copy(slots = day.slots.map { if (it.index == slot) TaskSlot(slot, text.trim(), TaskStatus.ACTIVE) else it })
    }

    fun complete(data: ThreeOrbsData, slot: Int, now: Long): ThreeOrbsData = updateToday(data) { day ->
        day.copy(slots = day.slots.map { if (it.index == slot && it.status == TaskStatus.ACTIVE) it.copy(status = TaskStatus.COMPLETED, completedAt = now) else it })
    }

    fun archive(data: ThreeOrbsData, slot: Int): ThreeOrbsData = updateToday(data) { day ->
        day.copy(slots = day.slots.map { if (it.index == slot) TaskSlot(slot) else it })
    }

    fun archiveHistory(data: ThreeOrbsData, date: String, slot: Int): ThreeOrbsData = data.copy(days = data.days.map { day ->
        if (day.date != date) day else day.copy(slots = day.slots.map { if (it.index == slot) it.copy(status = TaskStatus.ARCHIVED) else it })
    })

    fun seed(data: ThreeOrbsData, texts: List<String>): ThreeOrbsData {
        require(texts.size == 3 && texts.all { it.isNotBlank() })
        return updateToday(data) { it.copy(slots = texts.mapIndexed { i, t -> TaskSlot(i, t.trim(), TaskStatus.ACTIVE) }) }
    }

    private fun updateToday(data: ThreeOrbsData, block: (DayRecord) -> DayRecord): ThreeOrbsData {
        val date = LocalDate.now().toString(); val existing = today(data)
        val days = (data.days.filterNot { it.date == date } + block(existing)).sortedBy { it.date }.takeLast(14)
        return data.copy(days = days)
    }
}
