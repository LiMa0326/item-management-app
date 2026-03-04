package com.example.itemmanagementandroid.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@Composable
fun SettingsScreen(
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Settings Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Step 01 placeholder for settings page.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.Home) }
        ) {
            Text(text = "Go To Home")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onBack() }
        ) {
            Text(text = "Back")
        }
    }
}
