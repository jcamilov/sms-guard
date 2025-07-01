package com.example.smsguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class SMSReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var smsRepository: com.example.smsguard.data.repository.SMSRepository
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                val sms = SMSMessage(
                    id = UUID.randomUUID().toString(),
                    sender = smsMessage.originatingAddress ?: "Unknown",
                    message = smsMessage.messageBody ?: "",
                    timestamp = Date(smsMessage.timestampMillis),
                    classification = SMSClassification.PENDING,
                    isProcessed = false
                )
                
                // Note: We'll need to inject the repository properly
                // For now, we'll use a callback approach
                onSMSReceived?.invoke(sms)
            }
        }
    }
    
    companion object {
        var onSMSReceived: ((SMSMessage) -> Unit)? = null
    }
} 