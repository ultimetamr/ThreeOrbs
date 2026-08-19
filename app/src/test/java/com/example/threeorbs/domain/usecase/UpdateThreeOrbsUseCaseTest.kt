package com.example.threeorbs.domain.usecase

import com.example.threeorbs.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class UpdateThreeOrbsUseCaseTest {
    private val rules = UpdateThreeOrbsUseCase()
    private fun seeded() = rules.seed(ThreeOrbsData(), listOf("A", "B", "C"))

    @Test fun seedAlwaysCreatesExactlyThreeActiveSlots() {
        val day = rules.today(seeded())
        assertEquals(3, day.slots.size); assertTrue(day.slots.all { it.status == TaskStatus.ACTIVE })
    }

    @Test fun fourthTaskReplacesExplicitSlotWithoutGrowing() {
        val day = rules.today(rules.replace(seeded(), 1, "D"))
        assertEquals(listOf("A", "D", "C"), day.slots.map { it.text }); assertEquals(3, day.slots.size)
    }

    @Test fun completeOnlyChangesSelectedActiveSlot() {
        val day = rules.today(rules.complete(seeded(), 2, 42L))
        assertEquals(1, day.completedCount); assertEquals(42L, day.slots[2].completedAt)
    }

    @Test fun historyIsPrunedToFourteenDays() {
        val days = (0..20).map { DayRecord(LocalDate.now().minusDays(it.toLong()).toString()) }
        val updated = rules.replace(ThreeOrbsData(days), 0, "today")
        assertTrue(updated.days.size <= 14)
    }
}
