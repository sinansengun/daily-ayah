package com.cufica.dailyayah.ui.prayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cufica.dailyayah.data.PrayerTimesRepository
import com.cufica.dailyayah.data.model.PrayerTimes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cufica.dailyayah.widget.PrayerWidgetPreferences
import com.cufica.dailyayah.widget.DailyAyahWidgetProvider
import com.cufica.dailyayah.widget.PrayerOnlyWidgetProvider
import com.cufica.dailyayah.widget.AllInOneWidgetProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrayerTimesUiState(
    val city: String,
    val cities: List<String> = PrayerTimesRepository.DefaultCities,
    val times: PrayerTimes? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val repository: PrayerTimesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        PrayerTimesUiState(city = PrayerWidgetPreferences.selectedCity(context))
    )
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun selectCity(city: String) {
        PrayerWidgetPreferences.setSelectedCity(context, city)
        mutableState.value = mutableState.value.copy(city = city)
        DailyAyahWidgetProvider.requestRefresh(context)
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
            if (times != null) {
                DailyAyahWidgetProvider.requestRefresh(context)
                PrayerOnlyWidgetProvider.requestRefresh(context)
                AllInOneWidgetProvider.requestRefresh(context)
            }
        }
    }
}