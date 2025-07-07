package com.example.smsguard.data.model

data class SMSMessage(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val classification: SMSClassification = SMSClassification.PENDING,
    val isProcessed: Boolean = false,
    val isSmishing: Boolean? = null
)

enum class SMSClassification {
    PENDING,
    BENIGN,
    SMISHING,
    UNCLASSIFIED
} 