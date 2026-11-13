package com.zenvora.gesture.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zenvora.gesture.ZenvoraApp
import com.zenvora.gesture.core.engine.EngineRuntimeState
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.service.ZenvoraAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val profiles: List<ProfileWithSteps> = emptyList(),
    val activeProfile: ProfileWithSteps? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ZenvoraApp).repository

    val uiState: StateFlow<DashboardUiState> = repository.observeProfiles()
        .map { list ->
            DashboardUiState(
                isLoading = false,
                profiles = list,
                activeProfile = list.firstOrNull { it.profile.isActive } ?: list.firstOrNull()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DashboardUiState(isLoading = true)
        )

    val isAccessibilityConnected: StateFlow<Boolean> =
        ZenvoraAccessibilityService.isServiceConnected

    val runtimeState: StateFlow<EngineRuntimeState> =
        ZenvoraAccessibilityService.runtimeState

    private val _editingProfile = MutableStateFlow<ProfileWithSteps?>(null)
    val editingProfile: StateFlow<ProfileWithSteps?> = _editingProfile.asStateFlow()

    fun openNewProfileEditor() {
        _editingProfile.value = ProfileWithSteps(
            profile = GestureProfileEntity(
                name = "Profil Baru",
                intervalMs = 30_000L,
                intervalVarianceMs = 2_000L,
                isActive = uiState.value.profiles.isEmpty()
            ),
            steps = listOf(
                GestureStepEntity(profileId = 0L, orderIndex = 0, x = 0.5f, y = 0.55f)
            )
        )
    }

    fun openEditor(profileWithSteps: ProfileWithSteps) {
        _editingProfile.value = profileWithSteps
    }

    fun closeEditor() {
        _editingProfile.value = null
    }

    fun saveProfile(profile: GestureProfileEntity, steps: List<GestureStepEntity>) {
        viewModelScope.launch {
            repository.saveProfileWithSteps(profile, steps)
            _editingProfile.value = null
        }
    }

    fun selectActiveProfile(profileId: Long) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
        }
    }

    fun duplicateProfile(source: ProfileWithSteps) {
        viewModelScope.launch {
            val copyProfile = source.profile.copy(
                id = 0L,
                name = "${source.profile.name} (Salinan)",
                isActive = false
            )
            val copySteps = source.orderedSteps.map {
                it.copy(id = 0L, profileId = 0L)
            }
            repository.saveProfileWithSteps(copyProfile, copySteps)
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
        }
    }
}
