package com.oyj.skullking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oyj.skullking.data.DefaultActiveGameRepository
import com.oyj.skullking.data.RoomActiveGameStore
import com.oyj.skullking.data.SkullKingDatabase
import com.oyj.skullking.domain.ActiveGame
import com.oyj.skullking.domain.GameStatus
import com.oyj.skullking.domain.GameConstraints
import com.oyj.skullking.domain.Player
import com.oyj.skullking.domain.RoundPlayerInput
import com.oyj.skullking.domain.RuleSet
import com.oyj.skullking.presentation.GameViewModel
import com.oyj.skullking.presentation.GameViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels {
        val database = SkullKingDatabase.create(applicationContext)
        GameViewModelFactory(DefaultActiveGameRepository(RoomActiveGameStore(database)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SkullKingApp(viewModel) }
    }
}

private object ScoreboardColumns {
    val Player = 150.dp
    val Bid = 125.dp
    val Tricks = 125.dp
    val Bonus = 140.dp
    val Score = 120.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkullKingApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val game = uiState.game
    var confirmReplacement by rememberSaveable { mutableStateOf(false) }
    var editingCompletedGame by rememberSaveable(game?.id) { mutableStateOf(false) }

    MaterialTheme {
        Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("🏴‍☠️ Skull King") }) }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.error?.let { ErrorBanner(it, viewModel::clearError) }
                when {
                    uiState.isLoading -> LoadingState()
                    game == null -> SetupScreen(viewModel::startNewGame)
                    game.status == GameStatus.Completed && !editingCompletedGame -> ResultScreen(
                        game = game,
                        onEditRounds = { editingCompletedGame = true },
                        onStartReplacement = { confirmReplacement = true },
                    )
                    else -> ScoreboardScreen(
                        game = game,
                        onSaveRound = viewModel::saveRound,
                        onShowResults = { editingCompletedGame = false },
                    )
                }
            }
        }
    }

    if (confirmReplacement) {
        AlertDialog(
            onDismissRequest = { confirmReplacement = false },
            title = { Text("새 게임을 시작할까요?") },
            text = { Text("현재 완료된 게임 결과는 이 기기에서 교체됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplacement = false
                    viewModel.discardGame()
                }) { Text("새 게임 시작") }
            },
            dismissButton = { TextButton(onClick = { confirmReplacement = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun SetupScreen(onStartGame: (List<String>, Int, RuleSet) -> Unit) {
    val names = remember { mutableStateListOf("", "") }
    var totalRounds by rememberSaveable { mutableIntStateOf(GameConstraints.DefaultTotalRounds) }
    var ruleSet by rememberSaveable { mutableStateOf(RuleSet.Standard) }

    Text("게임 설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("플레이어 구성과 라운드 수는 첫 라운드가 시작되면 변경할 수 없습니다.")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(names.size) { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = names[index],
                    onValueChange = { names[index] = it },
                    label = { Text("플레이어 ${index + 1}") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                if (names.size > GameConstraints.MinPlayers) {
                    TextButton(onClick = { names.removeAt(index) }) { Text("삭제") }
                }
            }
        }
        item {
            if (names.size < GameConstraints.MaxPlayers) {
                TextButton(onClick = { names += "" }) { Text("플레이어 추가") }
            }
        }
    }
    NumberEditor(label = "총 라운드", value = totalRounds, minimum = 1, onValueChange = { totalRounds = it })
    Text("카드 보너스 규칙", fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RuleSet.values().forEach { option ->
            val label = when (option) {
                RuleSet.Standard -> "성공 시 수동 입력"
                RuleSet.NoCardBonus -> "카드 보너스 미사용"
            }
            if (ruleSet == option) {
                Button(onClick = { ruleSet = option }) { Text(label) }
            } else {
                TextButton(onClick = { ruleSet = option }) { Text(label) }
            }
        }
    }
    Button(
        onClick = { onStartGame(names.toList(), totalRounds, ruleSet) },
        enabled = names.all { it.isNotBlank() },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("게임 시작") }
}

@Composable
private fun ScoreboardScreen(
    game: ActiveGame,
    onSaveRound: (Int, List<RoundPlayerInput>) -> Unit,
    onShowResults: () -> Unit,
) {
    var roundNumber by rememberSaveable(game.id) { mutableIntStateOf(game.firstIncompleteRound()) }
    val storedRound = game.rounds.firstOrNull { it.number == roundNumber }
    var drafts by remember(game.id, roundNumber, storedRound) {
        mutableStateOf(game.players.associate { player ->
            val score = storedRound?.scores?.firstOrNull { it.playerId == player.id }
            player.id to ScoreDraft(score?.bid, score?.tricks, score?.bonusScore)
        })
    }
    val canSave = drafts.values.all { it.bid != null && it.tricks != null }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("라운드 $roundNumber / ${game.totalRounds}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { roundNumber -= 1 }, enabled = roundNumber > 1) { Text("이전") }
        TextButton(onClick = { roundNumber += 1 }, enabled = roundNumber < game.totalRounds) { Text("다음") }
    }
    Text("입찰과 획득이 같을 때만 추가점수를 입력할 수 있습니다.")
    ScoreboardHeader()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        game.players.forEach { player ->
            val draft = drafts.getValue(player.id)
            RoundInputRow(
                player = player,
                draft = draft,
                roundNumber = roundNumber,
                allowsRoundBonus = game.ruleSet.allowsRoundBonus,
                onUpdate = { updated -> drafts = drafts + (player.id to updated) },
            )
        }
    }
    Button(
        onClick = {
            onSaveRound(
                roundNumber,
                game.players.map { player -> drafts.getValue(player.id).toInput(player.id) },
            )
        },
        enabled = canSave,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (storedRound == null) "라운드 저장" else "라운드 수정 저장") }
    if (game.status == GameStatus.Completed) {
        TextButton(onClick = onShowResults, modifier = Modifier.fillMaxWidth()) { Text("결과로 돌아가기") }
    }
}

@Composable
private fun ScoreboardHeader() {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth()) {
        HeaderCell("플레이어", ScoreboardColumns.Player)
        HeaderCell("입찰", ScoreboardColumns.Bid)
        HeaderCell("획득", ScoreboardColumns.Tricks)
        HeaderCell("추가점수", ScoreboardColumns.Bonus)
        HeaderCell("예상 점수", ScoreboardColumns.Score)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.width(width).padding(8.dp))
}

@Composable
private fun RoundInputRow(
    player: Player,
    draft: ScoreDraft,
    roundNumber: Int,
    allowsRoundBonus: Boolean,
    onUpdate: (ScoreDraft) -> Unit,
) {
    val matched = draft.bid != null && draft.bid == draft.tricks
    val preview = if (draft.bid != null && draft.tricks != null) {
        ScoreCalculator.calculate(
            RoundScoreInput(
                roundNumber,
                draft.bid,
                draft.tricks,
                if (matched && allowsRoundBonus) draft.roundBonus ?: 0 else 0,
            ),
        ).totalScore
    } else null
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(player.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(ScoreboardColumns.Player))
            CompactNumberEditor(draft.bid, roundNumber, ScoreboardColumns.Bid) { onUpdate(draft.copy(bid = it)) }
            CompactNumberEditor(draft.tricks, roundNumber, ScoreboardColumns.Tricks) { onUpdate(draft.copy(tricks = it)) }
            if (matched && allowsRoundBonus) {
                CompactNumberEditor(draft.roundBonus ?: 0, 999, ScoreboardColumns.Bonus) { onUpdate(draft.copy(roundBonus = it)) }
            } else {
                val message = if (allowsRoundBonus) "입찰 성공 시 입력" else "규칙에서 사용 안 함"
                Text(message, modifier = Modifier.width(ScoreboardColumns.Bonus).padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }
            Text(preview?.let(::signedScore) ?: "—", modifier = Modifier.width(ScoreboardColumns.Score).padding(8.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactNumberEditor(value: Int?, maximum: Int, width: Dp, onValueChange: (Int) -> Unit) {
    Row(modifier = Modifier.width(width), verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onValueChange(((value ?: 0) - 1).coerceAtLeast(0)) },
            modifier = Modifier.defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
        ) { Text("−") }
        Text(value?.toString() ?: "—", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.titleMedium)
        TextButton(
            onClick = { onValueChange(((value ?: -1) + 1).coerceAtMost(maximum)) },
            modifier = Modifier.defaultMinSize(minWidth = 0.dp, minHeight = 0.dp),
        ) { Text("+") }
    }
}

@Composable
private fun NumberEditor(label: String, value: Int, minimum: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(110.dp))
        TextButton(onClick = { onValueChange((value - 1).coerceAtLeast(minimum)) }) { Text("−") }
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(40.dp))
        TextButton(onClick = { onValueChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun ResultScreen(game: ActiveGame, onEditRounds: () -> Unit, onStartReplacement: () -> Unit) {
    Text("최종 결과", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(game.players.sortedByDescending { it.totalScore }) { player ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(player.name, style = MaterialTheme.typography.titleLarge)
                    Text("${signedScore(player.totalScore)}점", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Button(onClick = onEditRounds, modifier = Modifier.fillMaxWidth()) { Text("라운드 수정") }
    Button(onClick = onStartReplacement, modifier = Modifier.fillMaxWidth()) { Text("새 게임 시작") }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    }
}

@Composable
private fun LoadingState() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

private data class ScoreDraft(val bid: Int? = null, val tricks: Int? = null, val roundBonus: Int? = null) {
    fun toInput(playerId: Long): RoundPlayerInput = RoundPlayerInput(
        playerId = playerId,
        bid = requireNotNull(bid),
        tricks = requireNotNull(tricks),
        roundBonus = if (bid == tricks) roundBonus ?: 0 else 0,
    )
}

private fun ActiveGame.firstIncompleteRound(): Int =
    (1..totalRounds).firstOrNull { number -> rounds.none { it.number == number } } ?: totalRounds

private fun signedScore(score: Int): String = if (score > 0) "+$score" else score.toString()
