package com.example.smsguard.data.repository

import com.example.smsguard.data.model.SMSMessage
import kotlinx.coroutines.flow.Flow

interface SMSRepository {
    fun getAllSMS(): Flow<List<SMSMessage>>
    suspend fun addSMS(sms: SMSMessage)
    suspend fun updateSMS(sms: SMSMessage)
    suspend fun getSMSById(id: String): SMSMessage?
} 