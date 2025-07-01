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

class SMSReceiver : BroadcastReceiver() {
    
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
                
                // Use callback approach since BroadcastReceivers can't be injected with Hilt
                onSMSReceived?.invoke(sms)
            }
        }
    }
    
    companion object {
        var onSMSReceived: ((SMSMessage) -> Unit)? = null
    }
} 