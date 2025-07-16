package com.example.smsguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.repository.SMSRepository
import com.example.smsguard.receiver.SMSReceiver
import com.example.smsguard.service.AIClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SMSViewModel @Inject constructor(
    private val smsRepository: SMSRepository,
    private val aiClassifier: AIClassifier
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SMSUiState())
    val uiState: StateFlow<SMSUiState> = _uiState.asStateFlow()
    
    init {
        loadSMSMessages()
        setupSMSReceiver()
        // Add test messages for demonstration
        addTestMessages()
    }
    
    private fun loadSMSMessages() {
        viewModelScope.launch {
            smsRepository.getAllSMS().collect { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false
                )
            }
        }
    }
    
    private fun setupSMSReceiver() {
        SMSReceiver.onSMSReceived = { sms ->
            viewModelScope.launch {
                // Add SMS with processing state
                val processingSMS = sms.copy(isProcessed = false)
                smsRepository.addSMS(processingSMS)
                
                // Process based on sender number
                processSMSWithMockupOrAI(processingSMS)
            }
        }
    }
    
    private suspend fun processSMSWithMockupOrAI(sms: SMSMessage) {
        try {
            val classification: SMSClassification
            val explanation: String
            
            when {
                sms.sender.startsWith("111") -> {
                    // Mockup benign message
                    classification = SMSClassification.BENIGN
                    explanation = ""
                }
                sms.sender.startsWith("222") -> {
                    // Mockup malicious message
                    classification = SMSClassification.SMISHING
                    explanation = "This message contains suspicious elements that may indicate a phishing attempt."
                }
                else -> {
                    // Real AI classification
                    classification = aiClassifier.classifySMS(sms.message)
                    explanation = if (classification == SMSClassification.SMISHING) {
                        aiClassifier.getExplanation(sms)
                    } else {
                        ""
                    }
                }
            }
            
            // Simulate 3-second processing for mockup cases
            if (sms.sender.startsWith("111") || sms.sender.startsWith("222")) {
                delay(3000)
            }
            
            val updatedSMS = sms.copy(
                classification = classification,
                isProcessed = true
            )
            smsRepository.updateSMS(updatedSMS)
            
        } catch (e: Exception) {
            // Fallback to UNCLASSIFIED if processing fails
            val updatedSMS = sms.copy(
                classification = SMSClassification.UNCLASSIFIED,
                isProcessed = true
            )
            smsRepository.updateSMS(updatedSMS)
        }
    }
    
    private fun addTestMessages() {
        viewModelScope.launch {
            val testMessages = listOf(
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "+1234567890",
                    message = "Your package has been delivered. Click here to track: http://bit.ly/package123",
                    timestamp = System.currentTimeMillis(),
                    classification = SMSClassification.SMISHING,
                    isProcessed = true
                ),
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "Bank of America",
                    message = "Your account has been suspended. Call 1-800-BANK immediately to verify your identity.",
                    timestamp = System.currentTimeMillis() - 300000, // 5 minutes ago
                    classification = SMSClassification.SMISHING,
                    isProcessed = true
                ),
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "Mom",
                    message = "Hi honey, don't forget to pick up milk on your way home!",
                    timestamp = System.currentTimeMillis() - 600000, // 10 minutes ago
                    classification = SMSClassification.BENIGN,
                    isProcessed = true
                ),
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
        }
    }
    
    fun onSMSItemClick(sms: SMSMessage) {
        if (sms.classification == SMSClassification.SMISHING) {
            viewModelScope.launch {
                try {
                    val explanation = if (sms.sender.startsWith("222")) {
                        // Mockup explanation
                        "This message contains suspicious elements that may indicate a phishing attempt."
                    } else {
                        // Real AI explanation
                        aiClassifier.getExplanation(sms)
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
}

data class SMSUiState(
    val messages: List<SMSMessage> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSMS: SMSMessage? = null,
    val showExplanationDialog: Boolean = false,
    val explanationText: String = ""
) 