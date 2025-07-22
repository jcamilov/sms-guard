package com.example.smsguard.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing memory optimization
 */
@Singleton
class MemoryOptimizationService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "MemoryOptimizationService"
        private const val MEMORY_THRESHOLD_PERCENT = 80
    }
    
    /**
     * Check current memory usage
     */
    suspend fun checkMemoryUsage(): MemoryInfo = withContext(Dispatchers.IO) {
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryUsagePercent = (usedMemory * 100 / maxMemory).toInt()
            
            MemoryInfo(
                maxMemory = maxMemory,
                totalMemory = totalMemory,
                freeMemory = freeMemory,
                usedMemory = usedMemory,
                memoryUsagePercent = memoryUsagePercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking memory usage", e)
            MemoryInfo()
        }
    }
    
    /**
     * Check if memory usage is high
     */
    suspend fun isMemoryUsageHigh(): Boolean {
        val memoryInfo = checkMemoryUsage()
        return memoryInfo.memoryUsagePercent > MEMORY_THRESHOLD_PERCENT
    }
    
    /**
     * Perform memory optimization
     */
    suspend fun optimizeMemory() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Performing memory optimization")
            
            // Force garbage collection
            System.gc()
            
            // Wait a bit for GC to complete
            kotlinx.coroutines.delay(100)
            
            // Force another GC cycle
            System.gc()
            
            val memoryInfo = checkMemoryUsage()
            Log.d(TAG, "Memory optimization completed. Usage: ${memoryInfo.memoryUsagePercent}%")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during memory optimization", e)
        }
    }
    
    /**
     * Get memory usage as a formatted string
     */
    suspend fun getMemoryUsageString(): String {
        val memoryInfo = checkMemoryUsage()
        return "Memory: ${memoryInfo.memoryUsagePercent}% used " +
               "(${formatBytes(memoryInfo.usedMemory)} / ${formatBytes(memoryInfo.maxMemory)})"
    }
    
    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024 * 1024)
        return "${mb}MB"
    }
}

data class MemoryInfo(
    val maxMemory: Long = 0,
    val totalMemory: Long = 0,
    val freeMemory: Long = 0,
    val usedMemory: Long = 0,
    val memoryUsagePercent: Int = 0
) 