package com.example

import android.app.Application
import com.example.database.LimunixDatabase

class LimunixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Room Database early
        LimunixDatabase.getDatabase(this)
    }
}
