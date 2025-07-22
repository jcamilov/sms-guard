package com.example.smsguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.repository.SMSRepository
import com.example.smsguard.receiver.SMSReceiver
import com.example.smsguard.service.AIClassifier
import com.example.smsguard.service.MemoryOptimizationService
import com.example.smsguard.service.BackgroundProcessingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class SMSViewModel @Inject constructor(
    private val smsRepository: SMSRepository,
    private val aiClassifier: AIClassifier,
    private val memoryOptimizationService: MemoryOptimizationService,
    private val backgroundProcessingService: BackgroundProcessingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SMSUiState())
    val uiState: StateFlow<SMSUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val PROCESSING_TIMEOUT_MS = 30000L // Increased timeout to 30 seconds to match Gemma3nClassifier
        private const val MAX_PROCESSING_RETRIES = 2
        private const val MEMORY_CHECK_INTERVAL_MS = 30000L // Check memory every 30 seconds
    }
    
    init {
        loadSMSMessages()
        setupSMSReceiver()
        setupMemoryMonitoring()
        setupBackgroundProcessingMonitoring()
        // Add test messages for demonstration
        addTestMessages()
    }
    
    private fun setupMemoryMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    // Check memory usage
                    if (memoryOptimizationService.isMemoryUsageHigh()) {
                        Log.w("SMSViewModel", "High memory usage detected, optimizing...")
                        memoryOptimizationService.optimizeMemory()
                    }
                    
                    // Update memory info in UI state
                    val memoryInfo = memoryOptimizationService.getMemoryUsageString()
                    _uiState.value = _uiState.value.copy(memoryInfo = memoryInfo)
                    
                    delay(MEMORY_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e("SMSViewModel", "Error in memory monitoring", e)
                    delay(MEMORY_CHECK_INTERVAL_MS)
                }
            }
        }
    }
    
    private fun setupBackgroundProcessingMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backgroundProcessingService.processingState.collect { processingState ->
                    Log.d("SMSViewModel", "Background processing state updated: ${processingState.isProcessing}")
                    if (processingState.isProcessing) {
                        Log.d("SMSViewModel", "Currently processing: ${processingState.currentSMS?.sender}")
                        Log.d("SMSViewModel", "Queue size: ${processingState.queueSize}")
                    }
                    
                    // Update UI state with processing info
                    val processingStatus = backgroundProcessingService.getProcessingStatus()
                    _uiState.value = _uiState.value.copy(
                        processingStatus = processingStatus,
                        isProcessing = processingState.isProcessing
                    )
                }
            } catch (e: Exception) {
                Log.e("SMSViewModel", "Error monitoring background processing", e)
            }
        }
    }
    
    private fun loadSMSMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                smsRepository.getAllSMS().collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // Handle error gracefully
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }
    
    private fun setupSMSReceiver() {
        SMSReceiver.onSMSReceived = { sms ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    Log.d("SMSViewModel", "=== New SMS received ===")
                    Log.d("SMSViewModel", "SMS from: ${sms.sender}")
                    Log.d("SMSViewModel", "SMS content: \"${sms.message.take(100)}${if (sms.message.length > 100) "..." else ""}\"")
                    
                    // Add SMS with processing state
                    val processingSMS = sms.copy(isProcessed = false)
                    smsRepository.addSMS(processingSMS)
                    
                    // Send to background processing service
                    Log.d("SMSViewModel", "Sending SMS to background processing service")
                    backgroundProcessingService.processSMS(processingSMS)
                    
                } catch (e: Exception) {
                    Log.e("SMSViewModel", "Error handling new SMS", e)
                    // Handle error gracefully
                    val errorSMS = sms.copy(
                        classification = SMSClassification.UNCLASSIFIED,
                        isProcessed = true
                    )
                    smsRepository.updateSMS(errorSMS)
                }
            }
        }
    }
    

    
    private fun addTestMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val testMessages = listOf(
                    SMSMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "+1234567890",
                        message = "Your package has been delivered. Click here to track: http://bit.ly/package123",
                        timestamp = System.currentTimeMillis(),
                        classification = SMSClassification.SMISHING,
                        isProcessed = true
                    ),
                    // SMSMessage(
                    //     id = UUID.randomUUID().toString(),
                    //     sender = "Mom",
                    //     message = "Hi honey, don't forget to pick up milk on your way home!",
                    //     timestamp = System.currentTimeMillis() - 600000, // 10 minutes ago
                    //     classification = SMSClassification.BENIGN,
                    //     isProcessed = true
                    // ),
                    SMSMessage(
                        id = UUID.randomUUID().toString(),
                        sender = "Amazon",
                        message = "Your order #12345 has been shipped and will arrive tomorrow.",
                        timestamp = System.currentTimeMillis() - 900000, // 15 minutes ago
                        classification = SMSClassification.BENIGN,
                        isProcessed = true
                    )
                )
                
                testMessages.forEach { sms ->
                    smsRepository.addSMS(sms)
                }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }
    
    fun onSMSItemClick(sms: SMSMessage) {
        if (sms.classification == SMSClassification.SMISHING) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val explanation = if (sms.sender.startsWith("222")) {
                        // Mockup explanation
                        "This message contains suspicious elements that may indicate a phishing attempt."
                    } else {
                        // Real AI explanation with timeout
                        withTimeout(PROCESSING_TIMEOUT_MS) {
                            aiClassifier.getExplanation(sms)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        selectedSMS = sms,
                        showExplanationDialog = true,
                        explanationText = explanation
                    )
                } catch (e: Exception) {
                    // Fallback explanation if AI fails
                    _uiState.value = _uiState.value.copy(
                        selectedSMS = sms,
                        showExplanationDialog = true,
                        explanationText = "This message contains suspicious elements that may indicate a phishing attempt."
                    )
                }
            }
        }
    }
    
    fun dismissExplanationDialog() {
        _uiState.value = _uiState.value.copy(
            showExplanationDialog = false,
            selectedSMS = null,
            explanationText = ""
        )
    }
    
    fun optimizeMemory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                memoryOptimizationService.optimizeMemory()
            } catch (e: Exception) {
                Log.e("SMSViewModel", "Error optimizing memory", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up resources when ViewModel is cleared
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SMSViewModel", "Cleaning up ViewModel resources")
                // Clean up background processing service
                backgroundProcessingService.cleanup()
                // Clean up AI classifier if it has cleanup method
                if (aiClassifier is com.example.smsguard.service.Gemma3nClassifier) {
                    aiClassifier.cleanup()
                }
                // Force memory optimization
                memoryOptimizationService.optimizeMemory()
                Log.d("SMSViewModel", "ViewModel cleanup completed")
            } catch (e: Exception) {
                Log.e("SMSViewModel", "Error during cleanup", e)
            }
        }
    }
}

data class SMSUiState(
    val messages: List<SMSMessage> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSMS: SMSMessage? = null,
    val showExplanationDialog: Boolean = false,
    val explanationText: String = "",
    val memoryInfo: String = "",
    val processingStatus: String = "",
    val isProcessing: Boolean = false
) 