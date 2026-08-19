package com.example.threeorbs.domain.model

import java.time.LocalDate

enum class TaskStatus { EMPTY, ACTIVE, COMPLETED, ARCHIVED }

data class TaskSlot(
    val index: Int,
    val text: String = "",
    val status: TaskStatus = TaskStatus.EMPTY,
    val completedAt: Long? = null,
)

data class DayRecord(
    val date: String,
    val slots: List<TaskSlot> = List(3) { TaskSlot(it) },
) {
    init { require(slots.size == 3) }
    val completedCount: Int get() = slots.count { it.status == TaskStatus.COMPLETED }
}

data class ThreeOrbsData(
    val days: List<DayRecord> = listOf(DayRecord(LocalDate.now().toString())),
    val groupX: Float = 0f,
    val groupY: Float = 0f,
    val groupZ: Float = 0f,
)
