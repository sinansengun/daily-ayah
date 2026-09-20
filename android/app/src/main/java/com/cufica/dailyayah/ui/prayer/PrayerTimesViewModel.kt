package com.cufica.dailyayah.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cufica.dailyayah.data.PrayerTimesRepository
import com.cufica.dailyayah.data.model.PrayerTimes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrayerTimesUiState(
    val city: String = "Istanbul",
    val cities: List<String> = PrayerTimesRepository.DefaultCities,
    val times: PrayerTimes? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val repository: PrayerTimesRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(PrayerTimesUiState())
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun selectCity(city: String) {
        mutableState.value = mutableState.value.copy(city = city)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val city = mutableState.value.city
            mutableState.value = mutableState.value.copy(isLoading = true, error = null)
            val cities = repository.cities()
            val times = repository.load(city)
            mutableState.value = mutableState.value.copy(
                cities = cities,
                times = times,
                isLoading = false,
                error = if (times == null) "Namaz vakitleri alınamadı." else null
            )
        }
    }
}