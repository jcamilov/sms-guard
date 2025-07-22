package com.example.smsguard.service

import com.example.smsguard.data.model.SMSClassification
import com.example.smsguard.data.model.SMSMessage

/**
 * Interface for AI-based SMS classification
 */
interface AIClassifier {
    
    /**
     * Classify an SMS message as either BENIGN or SMISHING
     * @param message The SMS text to classify
     * @return SMSClassification result
     */
    suspend fun classifySMS(message: String): SMSClassification
    
    /**
     * Generate explanation for why an SMS was classified as smishing
     * @param sms The SMS message to explain
     * @return Explanation text
     */
    suspend fun getExplanation(sms: SMSMessage): String
    
    /**
     * Initialize the AI model
     * @return true if initialization was successful
     */
    suspend fun initialize(): Boolean
    
    /**
     * Check if the model is ready for inference
     * @return true if model is loaded and ready
     */
    fun isModelReady(): Boolean
} 