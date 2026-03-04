package com.example.itemmanagementandroid.ui.screens.home

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
fun HomeScreen(
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
        Text(text = "Home Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Step 01 placeholder for home dashboard.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.Category) }
        ) {
            Text(text = "Go To Category")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemList) }
        ) {
            Text(text = "Go To Item List")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemDetail) }
        ) {
            Text(text = "Go To Item Detail")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemEdit) }
        ) {
            Text(text = "Go To Item Edit")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.Settings) }
        ) {
            Text(text = "Go To Settings")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onBack() }
        ) {
            Text(text = "Back")
        }
    }
}
