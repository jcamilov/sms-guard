package com.example.smsguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.smsguard.ui.theme.SMSguardTheme
import com.example.smsguard.ui.screen.SMSScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSguardTheme {
                SMSScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SMSScreenPreview() {
    SMSguardTheme {
        SMSScreen()
    }
}