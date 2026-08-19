package com.example.threeorbs.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.threeorbs.domain.model.TaskStatus
import com.example.threeorbs.ui.today.Panel
import com.example.threeorbs.ui.today.ThreeOrbsEvent
import com.example.threeorbs.ui.today.ThreeOrbsUiState
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextField
import com.pico.spatial.ui.foundation.material.backgroundMaterial
import com.pico.spatial.ui.platform.Material

@Composable
fun ThreeOrbsControlLayer(state: ThreeOrbsUiState, onEvent: (ThreeOrbsEvent) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.TopStart).padding(22.dp).width(270.dp)
                .clip(RoundedCornerShape(24.dp)).backgroundMaterial(true, Material.Thin).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("今日三件事", style = PicoTheme.typography.titleLarge)
            Text("短捏球体编辑 · 长捏 0.8 秒完成", color = PicoTheme.colorScheme.labelSecondary)
            Text("抓取下方圆环可整体移动三球", color = PicoTheme.colorScheme.labelSecondary)
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onEvent(ThreeOrbsEvent.Open(Panel.Add)) }) { Text("新增任务") }
            state.pendingUndo?.let { Button(onClick = { onEvent(ThreeOrbsEvent.Undo) }) { Text("已完成 · 撤销（2 秒）") } }
        }
        state.message?.let {
            Text(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp), color = PicoTheme.colorScheme.labelPrimary)
        }
        if (state.panel != Panel.None) DecisionPanel(state, onEvent)
    }
}

@Composable
fun SpatialDateWallPanel(state: ThreeOrbsUiState, onHistory: () -> Unit) {
    Column(
        Modifier.width(270.dp).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(
            java.time.LocalDate.parse(state.today.date).let { "${it.monthValue}月${it.dayOfMonth}日" },
            style = PicoTheme.typography.titleMedium,
        )
        Text("今日 · ${state.today.completedCount}/3")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) { i -> Text(if (i < state.today.completedCount) "◆" else "○") }
        }
        Button(onClick = onHistory) { Text("最近 14 天") }
    }
}

@Composable
private fun DecisionPanel(state: ThreeOrbsUiState, onEvent: (ThreeOrbsEvent) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) { // design-style: fixed-figma-color focus scrim
        Column(Modifier.width(620.dp).clip(RoundedCornerShape(32.dp)).backgroundMaterial(true, Material.Thickest).padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (val panel = state.panel) {
                Panel.Setup -> SetupEditor(onEvent)
                Panel.Add -> ReplacementEditor(state, onEvent)
                is Panel.Carry -> ReplacementEditor(state, onEvent, panel.text)
                is Panel.Edit -> EditPanel(state, panel.slot, onEvent)
                is Panel.Archive -> ArchivePanel(state, panel.slot, onEvent)
                Panel.History -> HistoryPanel(state, onEvent)
                Panel.None -> Unit
            }
        }
    }
}

@Composable private fun SetupEditor(onEvent: (ThreeOrbsEvent) -> Unit) {
    val texts = remember { mutableStateListOf("", "", "") }; Text("今天想完成哪三件事？", style = PicoTheme.typography.titleLarge)
    repeat(3) { i -> TextField(value = texts[i], onValueChange = { texts[i] = it }, placeholder = { Text("第 ${i + 1} 件小事") }) }
    Actions({ onEvent(ThreeOrbsEvent.Dismiss) }, { onEvent(ThreeOrbsEvent.Setup(texts.toList())) }, texts.all { it.isNotBlank() })
}

@Composable private fun ReplacementEditor(state: ThreeOrbsUiState, onEvent: (ThreeOrbsEvent) -> Unit, initialText: String = "") {
    var text by remember(initialText) { mutableStateOf(initialText) }; Text(if (initialText.isBlank()) "新增任务 · 选择替换" else "延续到今天 · 选择替换", style = PicoTheme.typography.titleLarge)
    TextField(value = text, onValueChange = { text = it }, placeholder = { Text("新的任务") })
    state.today.slots.forEach { slot -> Button(onClick = { onEvent(ThreeOrbsEvent.SelectReplacement(slot.index)) }) { Text("${if (state.selectedSlot == slot.index) "◆" else "○"} 替换：${slot.text.ifBlank { "待填写" }}") } }
    Actions({ onEvent(ThreeOrbsEvent.Dismiss) }, { onEvent(ThreeOrbsEvent.Replace(text)) }, text.isNotBlank() && state.selectedSlot != null)
}

@Composable private fun EditPanel(state: ThreeOrbsUiState, slot: Int, onEvent: (ThreeOrbsEvent) -> Unit) {
    var text by remember(slot) { mutableStateOf(state.today.slots[slot].text) }; Text("编辑任务", style = PicoTheme.typography.titleLarge)
    TextField(value = text, onValueChange = { text = it }, placeholder = { Text("任务文字") })
    Button(onClick = { onEvent(ThreeOrbsEvent.Open(Panel.Archive(slot))) }) { Text("归档放下") }
    Actions({ onEvent(ThreeOrbsEvent.Dismiss) }, { onEvent(ThreeOrbsEvent.Edit(slot, text)) }, text.isNotBlank())
}

@Composable private fun ArchivePanel(state: ThreeOrbsUiState, slot: Int, onEvent: (ThreeOrbsEvent) -> Unit) {
    Text("确认归档放下？", style = PicoTheme.typography.titleLarge); Text("“${state.today.slots[slot].text}”不会算作失败，只会从今日槽位轻轻移除。")
    Actions({ onEvent(ThreeOrbsEvent.Dismiss) }, { onEvent(ThreeOrbsEvent.Archive(slot)) }, true)
}

@Composable private fun HistoryPanel(state: ThreeOrbsUiState, onEvent: (ThreeOrbsEvent) -> Unit) {
    Text("最近 14 天", style = PicoTheme.typography.titleLarge)
    state.data.days.sortedByDescending { it.date }.take(14).forEach { day ->
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Text("${day.date}  ·  ${day.completedCount}/3")
            if (day.date != state.today.date) day.slots.filter { it.status == TaskStatus.ACTIVE }.forEach { slot ->
                Text(slot.text, color = PicoTheme.colorScheme.labelSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onEvent(ThreeOrbsEvent.Open(Panel.Carry(slot.text))) }) { Text("延续到今天") }
                    Button(onClick = { onEvent(ThreeOrbsEvent.ArchiveHistory(day.date, slot.index)) }) { Text("归档放下") }
                }
            }
        }
    }
    Actions({ onEvent(ThreeOrbsEvent.Dismiss) }, { onEvent(ThreeOrbsEvent.Dismiss) }, true, "完成")
}

@Composable private fun Actions(cancel: () -> Unit, confirm: () -> Unit, enabled: Boolean, confirmLabel: String = "保存") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Button(onClick = cancel) { Text("取消") }; Spacer(Modifier.width(10.dp)); Button(onClick = confirm, enabled = enabled) { Text(confirmLabel) } }
}
