package com.example.threeorbs.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.threeorbs.data.repository.DataStoreThreeOrbsRepository
import com.example.threeorbs.spatial.ThreeOrbsScene

@Composable
fun ThreeOrbsScreen() {
    val repository = DataStoreThreeOrbsRepository(LocalContext.current.applicationContext)
    val vm: ThreeOrbsViewModel = viewModel(factory = ThreeOrbsViewModel.Factory(repository))
    val state by vm.state.collectAsStateWithLifecycle()
    ThreeOrbsScene(state, vm::onEvent)
}
