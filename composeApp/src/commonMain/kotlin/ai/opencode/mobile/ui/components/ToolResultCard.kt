package ai.opencode.mobile.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.model.Part

@Composable
fun ToolResultCard(
    part: Part,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val toolName = part.tool ?: "unknown"
    val status = part.state?.status ?: ""
    val hasError = part.state?.error != null
    val output = part.state?.output ?: ""
    val error = part.state?.error
    val input = part.state?.input ?: ""

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (hasError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (expanded) "\u25BC" else "\u25B6",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = when (status) {
                        "completed" -> "\u2705 $toolName"
                        "error" -> "\u274C $toolName"
                        "running" -> "\u26A1 $toolName"
                        else -> "Tool: $toolName"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }

            if (input.isNotBlank() && input != "{}") {
                Spacer(modifier = Modifier.height(4.dp))
                val displayInput = if (expanded) input else input.take(80)
                Text(
                    text = displayInput,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (output.isNotBlank() || error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (output.isNotBlank()) {
                    val displayOutput = if (expanded) output else output.take(150)
                    Text(
                        text = displayOutput,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}