package com.oyj.skullking.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oyj.skullking.data.ActiveGameRepository
import com.oyj.skullking.domain.ActiveGame
import com.oyj.skullking.domain.RoundPlayerInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val isLoading: Boolean = true,
    val game: ActiveGame? = null,
    val error: String? = null,
)

class GameViewModel(
    private val repository: ActiveGameRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = launchAction { repository.getActiveGame() }

    fun startNewGame(playerNames: List<String>, totalRounds: Int) =
        launchAction { repository.startNewGame(playerNames, totalRounds) }

    fun saveRound(roundNumber: Int, entries: List<RoundPlayerInput>) =
        launchAction { repository.saveRound(roundNumber, entries) }

    fun discardGame() = launchAction {
        repository.clearActiveGame()
        null
    }

    fun clearError() {
        mutableUiState.update { it.copy(error = null) }
    }

    private fun launchAction(action: suspend () -> ActiveGame?) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            runCatching { action() }
                .onSuccess { game -> mutableUiState.value = GameUiState(isLoading = false, game = game) }
                .onFailure { throwable ->
                    mutableUiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "게임 기록을 저장하지 못했습니다.")
                    }
                }
        }
    }
}

class GameViewModelFactory(
    private val repository: ActiveGameRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameViewModel::class.java))
        return GameViewModel(repository) as T
    }
}
