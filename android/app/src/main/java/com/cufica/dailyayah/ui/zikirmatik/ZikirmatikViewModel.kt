package com.cufica.dailyayah.ui.zikirmatik

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cufica.dailyayah.data.model.ZikirmatikState
import com.cufica.dailyayah.data.model.ZikirProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.cufica.dailyayah.widget.ZikirmatikWidgetUpdater
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.zikirmatikDataStore by preferencesDataStore(name = "zikirmatik")

@HiltViewModel
class ZikirmatikViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val mutableState = MutableStateFlow(ZikirmatikState())
    val state = mutableState.asStateFlow()
    private val mutableProfiles = MutableStateFlow<List<ZikirProfile>>(emptyList())
    val profiles = mutableProfiles.asStateFlow()
    private val mutableActiveProfileId = MutableStateFlow<String?>(null)
    val activeProfileId = mutableActiveProfileId.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = context.zikirmatikDataStore.data.first()
            val legacyState = ZikirmatikState(
                name = preferences[Name] ?: "Subhanallah",
                target = (preferences[Target] ?: 33).coerceAtLeast(1),
                groupCount = (preferences[GroupCount] ?: 1).coerceAtLeast(1),
                count = (preferences[Count] ?: 0).coerceAtLeast(0)
            )
            val storedProfiles = preferences[Profiles]?.let { encoded ->
                runCatching { Json.decodeFromString<List<ZikirProfile>>(encoded) }.getOrNull()
            }.orEmpty()
            val loadedProfiles = storedProfiles.ifEmpty {
                listOf(legacyState.toProfile())
            }
            mutableActiveProfileId.value = preferences[ActiveProfileId]
                ?.takeIf { id -> loadedProfiles.any { it.id == id } }
                ?: loadedProfiles.first().id
            mutableProfiles.value = loadedProfiles
            mutableState.value = loadedProfiles.first { it.id == mutableActiveProfileId.value }.toState()
            persist(mutableState.value)
        }
    }

    fun increment() = update(mutableState.value.copy(count = mutableState.value.count + 1))

    fun decrement() {
        if (mutableState.value.count > 0) update(mutableState.value.copy(count = mutableState.value.count - 1))
    }

    fun reset() = update(mutableState.value.copy(count = 0))

    fun save(name: String, target: Int, groupCount: Int) {
        val profile = ZikirProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            target = target.coerceAtLeast(1),
            groupCount = groupCount.coerceAtLeast(1)
        )
        mutableProfiles.value = mutableProfiles.value + profile
        mutableActiveProfileId.value = profile.id
        update(profile.toState())
    }

    fun selectProfile(id: String) {
        val profile = mutableProfiles.value.firstOrNull { it.id == id } ?: return
        mutableActiveProfileId.value = id
        update(profile.toState())
    }

    fun deleteProfile(id: String) {
        if (mutableProfiles.value.size <= 1) return
        val remaining = mutableProfiles.value.filterNot { it.id == id }
        mutableProfiles.value = remaining
        if (mutableActiveProfileId.value == id) {
            mutableActiveProfileId.value = remaining.first().id
            update(remaining.first().toState())
        } else {
            persist(mutableState.value)
        }
    }

    private fun update(newState: ZikirmatikState) {
        mutableState.value = newState
        mutableProfiles.value = mutableProfiles.value.map { profile ->
            if (profile.id == mutableActiveProfileId.value) profile.copy(
                name = newState.name,
                target = newState.target,
                groupCount = newState.groupCount,
                count = newState.count
            ) else profile
        }
        ZikirmatikWidgetUpdater.update(context, newState)
        persist(newState)
    }

    private fun persist(newState: ZikirmatikState) {
        viewModelScope.launch {
            context.zikirmatikDataStore.updateData { preferences ->
                preferences.toMutablePreferences().apply {
                    this[Name] = newState.name
                    this[Target] = newState.target
                    this[GroupCount] = newState.groupCount
                    this[Count] = newState.count
                    this[Profiles] = Json.encodeToString(mutableProfiles.value)
                    mutableActiveProfileId.value?.let { this[ActiveProfileId] = it }
                }
            }
        }
    }

    private fun ZikirmatikState.toProfile() = ZikirProfile(
        id = UUID.randomUUID().toString(),
        name = name,
        target = target,
        groupCount = groupCount,
        count = count
    )

    private companion object {
        val Name = stringPreferencesKey("name")
        val Target = intPreferencesKey("target")
        val GroupCount = intPreferencesKey("group_count")
        val Count = intPreferencesKey("count")
        val Profiles = stringPreferencesKey("profiles")
        val ActiveProfileId = stringPreferencesKey("active_profile_id")
    }
}