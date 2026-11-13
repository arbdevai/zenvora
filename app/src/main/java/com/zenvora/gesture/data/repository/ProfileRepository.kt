package com.zenvora.gesture.data.repository

import com.zenvora.gesture.data.local.dao.ProfileDao
import com.zenvora.gesture.data.local.entity.GestureProfileEntity
import com.zenvora.gesture.data.local.entity.GestureStepEntity
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.data.model.ScrollDirection
import com.zenvora.gesture.data.model.StepActionType
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: ProfileDao) {

    fun observeProfiles(): Flow<List<ProfileWithSteps>> = dao.observeProfilesWithSteps()

    fun observeActiveProfile(): Flow<ProfileWithSteps?> = dao.observeActiveProfile()

    suspend fun getActiveProfileOnce(): ProfileWithSteps? = dao.getActiveProfileOnce()

    suspend fun getProfileWithSteps(id: Long): ProfileWithSteps? = dao.getProfileWithSteps(id)

    suspend fun seedDefaultsIfEmpty() {
        if (dao.getProfileCount() > 0) return

        val scrollId = dao.insertProfile(
            GestureProfileEntity(
                name = "Auto-Scroll 30 Detik",
                mode = ProfileMode.AUTO_SCROLL,
                intervalMs = 30_000L,
                intervalVarianceMs = 2_500L,
                humanizedJitterPx = 14,
                useBezierCurve = true,
                scrollDirection = ScrollDirection.DOWN,
                scrollDistancePercent = 55,
                scrollDurationMs = 480L,
                repeatCount = 0,
                isActive = true
            )
        )
        dao.insertSteps(
            listOf(
                GestureStepEntity(
                    profileId = scrollId,
                    orderIndex = 0,
                    actionType = StepActionType.TAP,
                    x = 0.5f,
                    y = 0.5f,
                    delayAfterMs = 1_500L,
                    durationMs = 80L
                )
            )
        )

        val tapId = dao.insertProfile(
            GestureProfileEntity(
                name = "Auto-Tap 5 Detik",
                mode = ProfileMode.SINGLE_TAP,
                intervalMs = 5_000L,
                intervalVarianceMs = 600L,
                humanizedJitterPx = 10,
                useBezierCurve = true,
                repeatCount = 0,
                isActive = false
            )
        )
        dao.insertSteps(
            listOf(
                GestureStepEntity(
                    profileId = tapId,
                    orderIndex = 0,
                    actionType = StepActionType.TAP,
                    x = 0.5f,
                    y = 0.62f,
                    delayAfterMs = 1_000L,
                    durationMs = 85L
                )
            )
        )
    }

    suspend fun setActiveProfile(profileId: Long) {
        dao.clearAllActive()
        dao.markProfileActive(profileId, System.currentTimeMillis())
    }

    suspend fun saveProfileWithSteps(
        profile: GestureProfileEntity,
        steps: List<GestureStepEntity>
    ): Long {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        val savedId = if (updated.id == 0L) {
            dao.insertProfile(updated)
        } else {
            dao.updateProfile(updated)
            updated.id
        }

        if (updated.isActive) {
            dao.clearAllActive()
            dao.markProfileActive(savedId, System.currentTimeMillis())
        }

        val normalizedSteps = steps.mapIndexed { index, step ->
            step.copy(
                id = if (updated.id == 0L) 0L else step.id,
                profileId = savedId,
                orderIndex = index
            )
        }
        dao.deleteStepsByProfile(savedId)
        if (normalizedSteps.isNotEmpty()) {
            dao.insertSteps(normalizedSteps)
        } else {
            dao.insertSteps(
                listOf(
                    GestureStepEntity(
                        profileId = savedId,
                        orderIndex = 0,
                        actionType = StepActionType.TAP,
                        x = 0.5f,
                        y = 0.55f
                    )
                )
            )
        }
        return savedId
    }

    suspend fun updateStepCoordinates(stepId: Long, x: Float, y: Float) {
        dao.updateStepCoordinates(
            stepId = stepId,
            x = x.coerceIn(0.05f, 0.95f),
            y = y.coerceIn(0.05f, 0.95f)
        )
    }

    suspend fun deleteProfile(profileId: Long) {
        val wasActive = dao.getProfileWithSteps(profileId)?.profile?.isActive == true
        dao.deleteProfileById(profileId)
        if (wasActive) {
            val remaining = dao.getActiveProfileOnce()
            if (remaining == null) {
                // Ensure another profile becomes active if available
                // Handled smoothly in ViewModel/Observer if needed
            }
        }
    }
}
