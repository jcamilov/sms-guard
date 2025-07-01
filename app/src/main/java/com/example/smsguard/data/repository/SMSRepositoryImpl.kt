package com.example.smsguard.data.repository

import com.example.smsguard.data.model.SMSMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SMSRepositoryImpl @Inject constructor() : SMSRepository {
    
    private val _smsMessages = MutableStateFlow<List<SMSMessage>>(emptyList())
    private val smsMessages: Flow<List<SMSMessage>> = _smsMessages.asStateFlow()
    
    override fun getAllSMS(): Flow<List<SMSMessage>> = smsMessages
    
    override suspend fun addSMS(sms: SMSMessage) {
        val currentList = _smsMessages.value.toMutableList()
        currentList.add(0, sms) // Add new SMS at the top
        _smsMessages.value = currentList
    }
    
    override suspend fun updateSMS(sms: SMSMessage) {
        val currentList = _smsMessages.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == sms.id }
        if (index != -1) {
            currentList[index] = sms
            _smsMessages.value = currentList
        }
    }
    
    override suspend fun getSMSById(id: String): SMSMessage? {
        return _smsMessages.value.find { it.id == id }
    }
} 