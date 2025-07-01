package com.example.smsguard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.repository.SMSRepository
import com.example.smsguard.receiver.SMSReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SMSViewModel @Inject constructor(
    private val smsRepository: SMSRepository
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
                smsRepository.addSMS(sms)
                // TODO: Trigger AI classification here
                processSMSWithAI(sms)
            }
        }
    }
    
    private suspend fun processSMSWithAI(sms: SMSMessage) {
        // TODO: Implement AI classification with Gemma 3n
        // For now, we'll simulate processing
        val updatedSMS = sms.copy(
            classification = SMSClassification.BENIGN, // Placeholder
            isProcessed = true
        )
        smsRepository.updateSMS(updatedSMS)
    }
    
    private fun addTestMessages() {
        viewModelScope.launch {
            val testMessages = listOf(
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "+1234567890",
                    message = "Your package has been delivered. Click here to track: http://bit.ly/package123",
                    timestamp = Date(),
                    classification = SMSClassification.SMISHING,
                    isProcessed = true
                ),
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "Bank of America",
                    message = "Your account has been suspended. Call 1-800-BANK immediately to verify your identity.",
                    timestamp = Date(System.currentTimeMillis() - 300000), // 5 minutes ago
                    classification = SMSClassification.SMISHING,
                    isProcessed = true
                ),
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "Mom",
                    message = "Hi honey, don't forget to pick up milk on your way home!",
                    timestamp = Date(System.currentTimeMillis() - 600000), // 10 minutes ago
                    classification = SMSClassification.BENIGN,
                    isProcessed = true
                ),
                SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = "Amazon",
                    message = "Your order #12345 has been shipped and will arrive tomorrow.",
                    timestamp = Date(System.currentTimeMillis() - 900000), // 15 minutes ago
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
            _uiState.value = _uiState.value.copy(
                selectedSMS = sms,
                showExplanationDialog = true
            )
        }
    }
    
    fun dismissExplanationDialog() {
        _uiState.value = _uiState.value.copy(
            showExplanationDialog = false,
            selectedSMS = null
        )
    }
}

data class SMSUiState(
    val messages: List<SMSMessage> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSMS: SMSMessage? = null,
    val showExplanationDialog: Boolean = false
) 