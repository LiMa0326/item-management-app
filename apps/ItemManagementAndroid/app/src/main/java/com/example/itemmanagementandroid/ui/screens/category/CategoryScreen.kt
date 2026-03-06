package com.example.itemmanagementandroid.ui.screens.category

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
fun CategoryScreen(
    state: CategoryUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleIncludeArchived: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Category Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Loaded categories: ${state.categories.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Include archived: ${state.includeArchived}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.isLoading) {
            Text(text = "Loading categories...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }
        state.categories.take(3).forEach { category ->
            Text(
                text = "- ${category.name}",
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
            onClick = { onToggleIncludeArchived(!state.includeArchived) }
        ) {
            Text(text = "Toggle Include Archived")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemList) }
        ) {
            Text(text = "Go To Item List")
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
