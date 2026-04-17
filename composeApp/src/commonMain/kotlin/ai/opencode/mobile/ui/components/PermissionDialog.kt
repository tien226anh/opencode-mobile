package ai.opencode.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.model.Permission

@Composable
fun PermissionDialog(
    permission: Permission,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        title = {
            Text(text = "\u26A0 Tool Permission Request")
        },
        text = {
            Column {
                Text(
                    text = permission.title.ifBlank { permission.type },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Show the tool type
                if (permission.type.isNotBlank()) {
                    Text(
                        text = "Tool: ${permission.type}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Show metadata details (command, file, etc.)
                val command = permission.metadataCommand
                val file = permission.metadataFile
                if (command != null || file != null) {
                    val details = buildString {
                        command?.let { append("Command: $it") }
                        file?.let { if (isNotEmpty()) append("\n") ; append("File: $it") }
                    }
                    if (details.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Allow this tool to run?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("Allow", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text("Deny", color = MaterialTheme.colorScheme.error)
            }
        },
        modifier = modifier,
    )
}