package com.cufica.dailyayah.ui.daily

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cufica.dailyayah.data.DailyAyahRepository
import com.cufica.dailyayah.data.model.DailyAyah
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cufica.dailyayah.widget.DailyAyahWidgetProvider
import com.cufica.dailyayah.widget.DailyAyahOnlyWidgetProvider
import com.cufica.dailyayah.widget.AllInOneWidgetProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailyAyahUiState(
    val isLoading: Boolean = true,
    val ayah: DailyAyah? = null,
    val history: List<DailyAyah> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class DailyAyahViewModel @Inject constructor(
    private val repository: DailyAyahRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DailyAyahUiState())
    val uiState: StateFlow<DailyAyahUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.value = mutableUiState.value.copy(isLoading = true, errorMessage = null)
            val ayah = repository.refreshNow()
            val history = repository.loadHistory(HISTORY_DAYS)
            ayah?.let {
                DailyAyahWidgetProvider.requestRefresh(context)
                DailyAyahOnlyWidgetProvider.requestRefresh(context)
                AllInOneWidgetProvider.requestRefresh(context)
            }
            mutableUiState.value = DailyAyahUiState(
                isLoading = false,
                ayah = ayah,
                history = history,
                errorMessage = if (ayah == null) "Veri alınamadı. Lütfen tekrar deneyin." else null
            )
        }
    }

    private companion object {
        const val HISTORY_DAYS = 15
    }
}