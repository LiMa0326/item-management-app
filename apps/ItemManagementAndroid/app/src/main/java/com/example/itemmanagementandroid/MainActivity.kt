package com.example.itemmanagementandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.itemmanagementandroid.ui.ItemManagementApp
import com.example.itemmanagementandroid.ui.theme.ItemManagementAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ItemManagementAndroidTheme {
                ItemManagementApp()
            }
        }
    }
}
