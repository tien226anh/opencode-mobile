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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ToolResultCard(
    part: Part,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(part.state?.status == "running") }
    val toolName = part.state?.title ?: part.tool ?: "unknown"
    val status = part.state?.status ?: ""
    val hasError = part.state?.error != null
    val output = part.state?.output ?: ""
    val error = part.state?.error
    // Input can be a JsonElement (object or string) — extract as text for display
    val input = try {
        when (val inputEl = part.state?.input) {
            is JsonPrimitive -> inputEl.content
            else -> inputEl?.toString() ?: ""
        }
    } catch (_: Exception) { "" }
    val toolTime = part.state?.time

    // Calculate duration if time is available
    val durationText = if (toolTime != null && toolTime.end > 0 && toolTime.start > 0) {
        val seconds = (toolTime.end - toolTime.start) / 1000.0
        if (seconds < 60) "${(seconds * 10).toInt() / 10.0}s" else "${(seconds / 60).toInt()}m"
    } else null

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            hasError -> MaterialTheme.colorScheme.errorContainer
            status == "running" -> MaterialTheme.colorScheme.tertiaryContainer
            status == "completed" -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Status icon
                when (status) {
                    "completed" -> Text("\u2705", style = MaterialTheme.typography.labelMedium)
                    "error" -> Text("\u274C", style = MaterialTheme.typography.labelMedium)
                    "running" -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    "pending" -> Text("\u23F3", style = MaterialTheme.typography.labelMedium)
                    else -> Text("\u25B6", style = MaterialTheme.typography.labelMedium)
                }
                // Tool name
                Text(
                    text = toolName,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        hasError -> MaterialTheme.colorScheme.error
                        status == "running" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Duration
                if (durationText != null) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Input display (expandable)
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

            // Output/error display
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