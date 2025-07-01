package com.example.smsguard.data.model

import java.util.Date

data class SMSMessage(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Date,
    val classification: SMSClassification = SMSClassification.PENDING,
    val isProcessed: Boolean = false
)

enum class SMSClassification {
    PENDING,
    BENIGN,
    SMISHING
} 