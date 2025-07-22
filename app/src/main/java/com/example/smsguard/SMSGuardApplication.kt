package com.example.smsguard

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SMSGuardApplication : Application() {
    
    companion object {
        private const val TAG = "SMSGuardApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SMSGuardApplication created")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Memory trim level: $level")
        
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App is in background, clean up UI resources
                Log.d(TAG, "App is in background, cleaning up UI resources")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is running but memory is low
                Log.d(TAG, "Memory is low, cleaning up resources")
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is in background and memory is very low
                Log.d(TAG, "Memory is very low, cleaning up all resources")
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")
        // Force garbage collection
        System.gc()
    }
} 