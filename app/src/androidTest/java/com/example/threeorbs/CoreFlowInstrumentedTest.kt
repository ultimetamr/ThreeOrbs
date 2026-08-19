package com.example.threeorbs

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.threeorbs.data.repository.DataStoreThreeOrbsRepository
import com.example.threeorbs.domain.usecase.UpdateThreeOrbsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowInstrumentedTest {
    @Test fun createCompletePersistReloadCoreFlow() = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val repo = DataStoreThreeOrbsRepository(context); val rules = UpdateThreeOrbsUseCase()
        val seeded = rules.seed(repo.data.first(), listOf("整理提案", "散步", "联系家人")); repo.save(seeded)
        val completed = rules.complete(repo.data.first(), 0, System.currentTimeMillis()); repo.save(completed)
        val restored = repo.data.first(); assertEquals(3, rules.today(restored).slots.size); assertEquals(1, rules.today(restored).completedCount)
    }
}
