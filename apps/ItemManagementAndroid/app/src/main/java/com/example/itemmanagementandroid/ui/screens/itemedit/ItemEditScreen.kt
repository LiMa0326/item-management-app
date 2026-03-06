package com.example.itemmanagementandroid.ui.screens.itemedit

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
fun ItemEditScreen(
    state: ItemEditUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Item Edit Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Mode: ${state.mode.name}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Target item: ${state.targetItemName}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Available categories: ${state.availableCategoryCount}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.isLoading) {
            Text(text = "Loading edit context...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefresh
        ) {
            Text(text = "Refresh")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.Settings) }
        ) {
            Text(text = "Go To Settings")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canGoBack,
            onClick = onBack
        ) {
            Text(text = "Back")
        }
    }
}
