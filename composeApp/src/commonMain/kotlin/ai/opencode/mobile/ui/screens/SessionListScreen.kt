package ai.opencode.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.navigation.SessionListComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    component: SessionListComponent,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenCode") },
                actions = {
                    IconButton(onClick = { component.onSettingsClicked() }) {
                        Text("⚙", style = MaterialTheme.typography.titleMedium)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { component.onSessionClicked("new", "New Session") },
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        // Placeholder empty state - will be replaced with real session list
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "No sessions yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Connect to an OpenCode server to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}