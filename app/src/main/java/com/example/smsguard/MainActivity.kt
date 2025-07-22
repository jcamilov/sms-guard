package com.example.smsguard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.smsguard.ui.theme.SMSguardTheme
import com.example.smsguard.ui.screen.SMSScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")
        enableEdgeToEdge()
        setContent {
            SMSguardTheme {
                SMSScreen()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity onPause")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
        
        // Clean up resources when activity is destroyed
        lifecycleScope.launch {
            try {
                // Force garbage collection
                System.gc()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "MainActivity received low memory warning")
        // Force garbage collection
        System.gc()
    }
}

@Preview(showBackground = true)
@Composable
fun SMSScreenPreview() {
    SMSguardTheme {
        SMSScreen()
    }
}