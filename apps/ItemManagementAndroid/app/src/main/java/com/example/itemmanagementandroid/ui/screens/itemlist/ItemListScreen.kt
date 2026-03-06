package com.example.itemmanagementandroid.ui.screens.itemlist

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
fun ItemListScreen(
    state: ItemListUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleIncludeDeleted: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Item List Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Loaded items: ${state.items.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Include deleted: ${state.includeDeleted}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.isLoading) {
            Text(text = "Loading items...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }
        state.items.take(3).forEach { item ->
            Text(
                text = "- ${item.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefresh
        ) {
            Text(text = "Refresh")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onToggleIncludeDeleted(!state.includeDeleted) }
        ) {
            Text(text = "Toggle Include Deleted")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemDetail) }
        ) {
            Text(text = "Go To Item Detail")
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
