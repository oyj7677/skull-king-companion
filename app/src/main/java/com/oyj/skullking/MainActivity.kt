package com.oyj.skullking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Player(val id: Int, val name: String, val total: Int = 0)
data class RoundResult(val round: Int, val player: String, val bid: Int, val tricks: Int, val score: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SkullKingApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkullKingApp() {
    val players = remember { mutableStateListOf<Player>() }
    val history = remember { mutableStateListOf<RoundResult>() }
    var currentRound by remember { mutableIntStateOf(1) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var scoringPlayer by remember { mutableStateOf<Player?>(null) }

    MaterialTheme {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("🏴‍☠️ Skull King") }) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("라운드 $currentRound", style = MaterialTheme.typography.headlineSmall)
                            Text("각 플레이어의 입찰과 획득 트릭을 기록하세요.")
                        }
                        Button(onClick = { currentRound += 1 }) { Text("다음 라운드") }
                    }
                }
                item {
                    Button(onClick = { showPlayerDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("플레이어 추가")
                    }
                }
                if (players.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(players, key = { it.id }) { player ->
                        PlayerCard(player = player, onRecordScore = { scoringPlayer = player })
                    }
                }
                if (history.isNotEmpty()) {
                    item { Text("최근 기록", style = MaterialTheme.typography.titleLarge) }
                    items(history.takeLast(10).reversed()) { result ->
                        Text("${result.player}: 입찰 ${result.bid} · 획득 ${result.tricks} · ${signedScore(result.score)}점 (R${result.round})")
                    }
                }
            }
        }
    }

    if (showPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showPlayerDialog = false },
            onAdd = { name ->
                players += Player(id = (players.maxOfOrNull { it.id } ?: 0) + 1, name = name)
                showPlayerDialog = false
            }
        )
    }
    scoringPlayer?.let { player ->
        ScoreDialog(
            player = player,
            round = currentRound,
            onDismiss = { scoringPlayer = null },
            onSave = { bid, tricks ->
                val score = ScoreCalculator.calculate(currentRound, bid, tricks)
                val index = players.indexOfFirst { it.id == player.id }
                players[index] = player.copy(total = player.total + score)
                history += RoundResult(currentRound, player.name, bid, tricks, score)
                scoringPlayer = null
            }
        )
    }
}

@Composable
private fun EmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            "먼저 함께 게임할 플레이어를 추가해 보세요.",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PlayerCard(player: Player, onRecordScore: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(player.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("누적 ${signedScore(player.total)}점")
            }
            Button(onClick = onRecordScore) { Text("점수 기록") }
        }
    }
}

@Composable
private fun AddPlayerDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("플레이어 추가") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("이름") }) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onAdd(name.trim()) }) { Text("추가") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun ScoreDialog(player: Player, round: Int, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var bid by remember { mutableStateOf("") }
    var tricks by remember { mutableStateOf("") }
    val bidValue = bid.toIntOrNull()
    val tricksValue = tricks.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${player.name} · 라운드 $round") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = bid, onValueChange = { bid = it }, label = { Text("입찰 트릭") })
                OutlinedTextField(value = tricks, onValueChange = { tricks = it }, label = { Text("획득 트릭") })
                Text("정확히 맞추면 보너스 점수가 적용됩니다.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(bidValue!!, tricksValue!!) }, enabled = bidValue != null && tricksValue != null && bidValue >= 0 && tricksValue >= 0) {
                Text("저장")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

private fun signedScore(score: Int): String = if (score > 0) "+$score" else score.toString()
