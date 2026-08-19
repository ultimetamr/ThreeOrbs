package com.example.threeorbs.data.repository

import com.example.threeorbs.domain.model.ThreeOrbsData
import kotlinx.coroutines.flow.Flow

interface ThreeOrbsRepository {
    val data: Flow<ThreeOrbsData>
    suspend fun save(value: ThreeOrbsData)
}
