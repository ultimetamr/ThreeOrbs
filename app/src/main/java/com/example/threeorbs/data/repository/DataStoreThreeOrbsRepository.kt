package com.example.threeorbs.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.threeorbs.domain.model.DayRecord
import com.example.threeorbs.domain.model.TaskSlot
import com.example.threeorbs.domain.model.TaskStatus
import com.example.threeorbs.domain.model.ThreeOrbsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.threeOrbsStore by preferencesDataStore("three_orbs")

class DataStoreThreeOrbsRepository(private val context: Context) : ThreeOrbsRepository {
    private val key = stringPreferencesKey("snapshot_v1")
    override val data: Flow<ThreeOrbsData> = context.threeOrbsStore.data
        .map { decode(it[key]) }
        .catch { emit(ThreeOrbsData()) }

    override suspend fun save(value: ThreeOrbsData) {
        context.threeOrbsStore.edit { it[key] = encode(value) }
    }

    internal fun encode(value: ThreeOrbsData): String = JSONObject().apply {
        put("x", value.groupX.toDouble()); put("y", value.groupY.toDouble()); put("z", value.groupZ.toDouble())
        put("days", JSONArray().apply { value.days.takeLast(14).forEach { day ->
            put(JSONObject().apply {
                put("date", day.date)
                put("slots", JSONArray().apply { day.slots.forEach { slot -> put(JSONObject().apply {
                    put("index", slot.index); put("text", slot.text); put("status", slot.status.name)
                    if (slot.completedAt != null) put("completedAt", slot.completedAt)
                }) } })
            })
        } })
    }.toString()

    internal fun decode(raw: String?): ThreeOrbsData {
        if (raw.isNullOrBlank()) return ThreeOrbsData()
        return runCatching {
            val root = JSONObject(raw); val daysJson = root.getJSONArray("days")
            val days = buildList { for (i in 0 until daysJson.length()) {
                val d = daysJson.getJSONObject(i); val s = d.getJSONArray("slots")
                add(DayRecord(d.getString("date"), List(3) { index ->
                    if (index >= s.length()) TaskSlot(index) else s.getJSONObject(index).let { j ->
                        TaskSlot(index, j.optString("text"), TaskStatus.valueOf(j.optString("status", "EMPTY")),
                            if (j.has("completedAt")) j.getLong("completedAt") else null)
                    }
                }))
            } }.filter { LocalDate.parse(it.date) >= LocalDate.now().minusDays(13) }
            ThreeOrbsData(
                days.ifEmpty { listOf(DayRecord(LocalDate.now().toString())) },
                root.finiteFloat("x"),
                root.finiteFloat("y"),
                root.finiteFloat("z"),
            )
        }.getOrElse { ThreeOrbsData() }
    }

    private fun JSONObject.finiteFloat(key: String): Float =
        optDouble(key, 0.0).toFloat().takeIf { it.isFinite() } ?: 0f
}
