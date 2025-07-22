package com.example.smsguard.service

import android.content.Context
import android.util.Log
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.repository.SMSRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for handling background processing of SMS messages
 * Manages processing queue and provides detailed logging
 */
@Singleton
class BackgroundProcessingService @Inject constructor(
    private val context: Context,
    private val aiClassifier: AIClassifier,
    private val smsRepository: SMSRepository,
    private val memoryOptimizationService: MemoryOptimizationService
) {
    
    companion object {
        private const val TAG = "BackgroundProcessingService"
        private const val PROCESSING_TIMEOUT_MS = 45000L // Increased timeout to 45 seconds
        private const val MAX_RETRIES = 2
        private const val MEMORY_CHECK_INTERVAL_MS = 10000L // Check memory every 10 seconds
        private const val MAX_CONCURRENT_PROCESSING = 1 // Process one SMS at a time
    }
    
    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isProcessing = AtomicBoolean(false)
    private val processingQueue = mutableListOf<SMSMessage>()
    
    private val _processingState = MutableStateFlow(ProcessingState())
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()
    
    init {
        Log.d(TAG, "BackgroundProcessingService initialized")
        startMemoryMonitoring()
    }
    
    /**
     * Add SMS to processing queue
     */
    suspend fun processSMS(sms: SMSMessage) {
        Log.d(TAG, "=== Adding SMS to processing queue ===")
        Log.d(TAG, "SMS ID: ${sms.id}")
        Log.d(TAG, "SMS from: ${sms.sender}")
        Log.d(TAG, "SMS content: \"${sms.message.take(100)}${if (sms.message.length > 100) "..." else ""}\"")
        
        // Add to queue
        processingQueue.add(sms)
        Log.d(TAG, "Queue size: ${processingQueue.size}")
        
        // Update processing state
        _processingState.value = _processingState.value.copy(
            queueSize = processingQueue.size,
            isProcessing = true
        )
        
        // Start processing if not already running
        if (!isProcessing.get()) {
            startProcessing()
        }
    }
    
    /**
     * Start processing SMS from queue
     */
    private fun startProcessing() {
        if (isProcessing.get()) {
            Log.d(TAG, "Processing already in progress, skipping")
            return
        }
        
        isProcessing.set(true)
        Log.d(TAG, "=== Starting background processing ===")
        
        processingScope.launch {
            try {
                while (processingQueue.isNotEmpty()) {
                    val sms = processingQueue.removeAt(0)
                    Log.d(TAG, "Processing SMS from queue: ${sms.id}")
                    
                    // Update processing state
                    _processingState.value = _processingState.value.copy(
                        currentSMS = sms,
                        queueSize = processingQueue.size,
                        isProcessing = true
                    )
                    
                    processSingleSMS(sms)
                    
                    // Small delay between processing
                    delay(1000)
                }
                
                // No more items in queue
                _processingState.value = _processingState.value.copy(
                    currentSMS = null,
                    isProcessing = false
                )
                isProcessing.set(false)
                Log.d(TAG, "=== Background processing completed ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in background processing", e)
                isProcessing.set(false)
                _processingState.value = _processingState.value.copy(
                    isProcessing = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Process a single SMS message
     */
    private suspend fun processSingleSMS(sms: SMSMessage) {
        Log.d(TAG, "=== Processing SMS: ${sms.id} ===")
        Log.d(TAG, "Step 1: Checking memory usage before processing")
        
        // Check memory before processing
        val memoryInfo = memoryOptimizationService.checkMemoryUsage()
        Log.d(TAG, "Memory usage: ${memoryInfo.memoryUsagePercent}%")
        
        if (memoryInfo.memoryUsagePercent > 80) {
            Log.w(TAG, "High memory usage detected (${memoryInfo.memoryUsagePercent}%), optimizing...")
            memoryOptimizationService.optimizeMemory()
            
            // Check memory again after optimization
            val newMemoryInfo = memoryOptimizationService.checkMemoryUsage()
            Log.d(TAG, "Memory after optimization: ${newMemoryInfo.memoryUsagePercent}%")
        }
        
        var retryCount = 0
        while (retryCount <= MAX_RETRIES) {
            try {
                Log.d(TAG, "Step 2: Starting AI classification (attempt ${retryCount + 1})")
                
                val classification: SMSClassification
                val explanation: String
                
                when {
                    sms.sender.startsWith("111") -> {
                        Log.d(TAG, "Using mockup benign classification")
                        classification = SMSClassification.BENIGN
                        explanation = ""
                        delay(2000) // Simulate processing time
                    }
                    sms.sender.startsWith("222") -> {
                        Log.d(TAG, "Using mockup smishing classification")
                        classification = SMSClassification.SMISHING
                        explanation = "This message contains suspicious elements that may indicate a phishing attempt."
                        delay(2000) // Simulate processing time
                    }
                    else -> {
                        Log.d(TAG, "Step 3: Using real AI classification (timeout: ${PROCESSING_TIMEOUT_MS}ms)")
                        
                        // Real AI classification with timeout
                        classification = withTimeout(PROCESSING_TIMEOUT_MS) {
                            withContext(Dispatchers.IO) {
                                Log.d(TAG, "Step 4: Calling AI classifier...")
                                aiClassifier.classifySMS(sms.message)
                            }
                        }
                        
                        Log.d(TAG, "Step 5: AI classification completed: $classification")
                        
                        explanation = if (classification == SMSClassification.SMISHING) {
                            Log.d(TAG, "Step 6: Generating explanation for smishing message...")
                            withTimeout(PROCESSING_TIMEOUT_MS) {
                                withContext(Dispatchers.IO) {
                                    aiClassifier.getExplanation(sms)
                                }
                            }
                        } else {
                            ""
                        }
                        
                        Log.d(TAG, "Step 7: Explanation generated: \"${explanation.take(100)}${if (explanation.length > 100) "..." else ""}\"")
                    }
                }
                
                Log.d(TAG, "Step 8: Updating SMS in repository")
                val updatedSMS = sms.copy(
                    classification = classification,
                    isProcessed = true
                )
                smsRepository.updateSMS(updatedSMS)
                
                Log.d(TAG, "=== SMS processing completed successfully ===")
                Log.d(TAG, "Final classification: $classification")
                return // Success, exit retry loop
                
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "Error processing SMS (attempt $retryCount)", e)
                
                if (retryCount > MAX_RETRIES) {
                    Log.e(TAG, "Max retries reached, marking as UNCLASSIFIED")
                    val updatedSMS = sms.copy(
                        classification = SMSClassification.UNCLASSIFIED,
                        isProcessed = true
                    )
                    smsRepository.updateSMS(updatedSMS)
                    return
                }
                
                // Wait before retry
                Log.d(TAG, "Waiting ${1000L * retryCount}ms before retry...")
                delay(1000L * retryCount)
            }
        }
    }
    
    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring() {
        processingScope.launch {
            while (true) {
                try {
                    delay(MEMORY_CHECK_INTERVAL_MS)
                    
                    if (memoryOptimizationService.isMemoryUsageHigh()) {
                        Log.w(TAG, "High memory usage detected during monitoring, optimizing...")
                        memoryOptimizationService.optimizeMemory()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in memory monitoring", e)
                }
            }
        }
    }
    
    /**
     * Get current processing status
     */
    fun getProcessingStatus(): String {
        val state = _processingState.value
        return if (state.isProcessing) {
            "Processing SMS from ${state.currentSMS?.sender ?: "unknown"} (${state.queueSize} in queue)"
        } else {
            "Idle (${state.queueSize} in queue)"
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up BackgroundProcessingService")
        processingScope.cancel()
        processingQueue.clear()
        isProcessing.set(false)
    }
}

data class ProcessingState(
    val isProcessing: Boolean = false,
    val currentSMS: SMSMessage? = null,
    val queueSize: Int = 0,
    val error: String? = null
) 