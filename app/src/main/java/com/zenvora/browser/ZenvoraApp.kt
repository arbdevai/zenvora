package com.zenvora.browser

import android.app.Application
import android.os.Build

class ZenvoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // PENTING: Jangan menginisialisasi GeckoRuntime di proses anak (:tab*, :gpu, :utility)
        // karena GeckoView menjalankan child process menggunakan Application class yang sama.
        // Inisialisasi GeckoRuntime di child process akan memicu fork-bomb proses tab0..tab39
        // dan menyebabkan aplikasi utama terkena SIGKILL setelah ~5 detik.
        val currentProcess = getCurrentProcessName()
        if (currentProcess != null && currentProcess.contains(":")) {
            return
        }
    }

    private fun getCurrentProcessName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            null
        }
    }
}
