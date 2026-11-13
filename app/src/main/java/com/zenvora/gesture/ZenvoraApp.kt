package com.zenvora.gesture

import android.app.Application
import com.zenvora.gesture.data.local.ZenvoraDatabase
import com.zenvora.gesture.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZenvoraApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ZenvoraDatabase by lazy {
        ZenvoraDatabase.getInstance(this)
    }

    val repository: ProfileRepository by lazy {
        ProfileRepository(database.profileDao())
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.seedDefaultsIfEmpty()
        }
    }
}
