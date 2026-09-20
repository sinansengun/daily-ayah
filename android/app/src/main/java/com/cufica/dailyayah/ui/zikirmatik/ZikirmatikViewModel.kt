package com.cufica.dailyayah.ui.zikirmatik

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cufica.dailyayah.data.model.ZikirmatikState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cufica.dailyayah.widget.ZikirmatikWidgetUpdater
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.zikirmatikDataStore by preferencesDataStore(name = "zikirmatik")

@HiltViewModel
class ZikirmatikViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableState = MutableStateFlow(ZikirmatikState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = context.zikirmatikDataStore.data.first()
            mutableState.value = ZikirmatikState(
                name = preferences[Name] ?: "Subhanallah",
                target = (preferences[Target] ?: 33).coerceAtLeast(1),
                groupCount = (preferences[GroupCount] ?: 1).coerceAtLeast(1),
                count = (preferences[Count] ?: 0).coerceAtLeast(0)
            )
        }
    }

    fun increment() = update(mutableState.value.copy(count = mutableState.value.count + 1))

    fun decrement() {
        if (mutableState.value.count > 0) update(mutableState.value.copy(count = mutableState.value.count - 1))
    }

    fun reset() = update(mutableState.value.copy(count = 0))

    fun save(name: String, target: Int, groupCount: Int) = update(
        ZikirmatikState(name = name.trim(), target = target.coerceAtLeast(1), groupCount = groupCount.coerceAtLeast(1))
    )

    private fun update(newState: ZikirmatikState) {
        mutableState.value = newState
        ZikirmatikWidgetUpdater.update(context, newState)
        viewModelScope.launch {
            context.zikirmatikDataStore.updateData { preferences ->
                preferences.toMutablePreferences().apply {
                    this[Name] = newState.name
                    this[Target] = newState.target
                    this[GroupCount] = newState.groupCount
                    this[Count] = newState.count
                }
            }
        }
    }

    private companion object {
        val Name = stringPreferencesKey("name")
        val Target = intPreferencesKey("target")
        val GroupCount = intPreferencesKey("group_count")
        val Count = intPreferencesKey("count")
    }
}