package com.example.threeorbs.ui.today

import com.example.threeorbs.data.repository.ThreeOrbsRepository
import com.example.threeorbs.domain.model.ThreeOrbsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ThreeOrbsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() = Dispatchers.setMain(dispatcher)
    @After fun after() = Dispatchers.resetMain()

    @Test fun initShowsSetupForEmptyDay() = runTest(dispatcher) { val vm = ThreeOrbsViewModel(FakeRepo()); advanceUntilIdle(); assertEquals(Panel.Setup, vm.state.value.panel) }
    @Test fun setupPersistsThreeTasks() = runTest(dispatcher) { val repo=FakeRepo(); val vm=ThreeOrbsViewModel(repo); advanceUntilIdle(); vm.onEvent(ThreeOrbsEvent.Setup(listOf("A","B","C"))); advanceUntilIdle(); assertEquals(3, vm.state.value.today.slots.count { it.text.isNotBlank() }); assertNotNull(repo.saved) }
    @Test fun replaceRequiresExplicitSelection() = runTest(dispatcher) { val vm=readyVm(); vm.onEvent(ThreeOrbsEvent.Open(Panel.Add)); vm.onEvent(ThreeOrbsEvent.Replace("D")); assertEquals(listOf("A","B","C"), vm.state.value.today.slots.map { it.text }) }
    @Test fun completeThenUndoRestoresSnapshot() = runTest(dispatcher) { val vm=readyVm(); vm.onEvent(ThreeOrbsEvent.Complete(0)); assertEquals(1, vm.state.value.today.completedCount); vm.onEvent(ThreeOrbsEvent.Undo); assertEquals(0, vm.state.value.today.completedCount) }

    private suspend fun TestScope.readyVm(): ThreeOrbsViewModel { val vm=ThreeOrbsViewModel(FakeRepo()); advanceUntilIdle(); vm.onEvent(ThreeOrbsEvent.Setup(listOf("A","B","C"))); advanceUntilIdle(); return vm }
    private class FakeRepo(initial: ThreeOrbsData = ThreeOrbsData()) : ThreeOrbsRepository { private val flow=MutableStateFlow(initial); override val data: Flow<ThreeOrbsData> = flow; var saved: ThreeOrbsData?=null; override suspend fun save(value: ThreeOrbsData) { saved=value; flow.value=value } }
}
