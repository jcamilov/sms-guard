package com.example.smsguard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsguard.data.model.SMSMessage
import com.example.smsguard.data.model.SMSClassification
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SMSItem(
    sms: SMSMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = sms.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = sms.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = formatTimestamp(Date(sms.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Classification indicator
                ClassificationIndicator(
                    classification = sms.classification,
                    isProcessed = sms.isProcessed
                )
            }
        }
    }
}

@Composable
private fun ClassificationIndicator(
    classification: SMSClassification,
    isProcessed: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, tint, backgroundColor) = when {
        !isProcessed -> Triple(
            Icons.Filled.Schedule,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
        classification == SMSClassification.BENIGN -> Triple(
            Icons.Default.CheckCircle,
            Color.Green,
            Color(0xFFE8F5E8)
        )
        classification == SMSClassification.SMISHING -> Triple(
            Icons.Default.Warning,
            Color.Red,
            Color(0xFFFFEBEE)
        )
        classification == SMSClassification.UNCLASSIFIED -> Triple(
            Icons.Filled.Help,
            Color(0xFFFFA726), // Orange
            Color(0xFFFFF3E0)
        )
        else -> Triple(
            Icons.Filled.Schedule,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isProcessed) {
            // Animated spinner for processing
            ProcessingSpinner(tint = tint)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = when (classification) {
                    SMSClassification.BENIGN -> "Benign message"
                    SMSClassification.SMISHING -> "Smishing detected"
                    SMSClassification.PENDING -> "Processing"
                    SMSClassification.UNCLASSIFIED -> "Unable to classify"
                },
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProcessingSpinner(tint: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Icon(
        imageVector = Icons.Filled.Schedule,
        contentDescription = "Processing message",
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer(rotationZ = rotation)
    )
}

private fun formatTimestamp(timestamp: Date): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(timestamp)
} 