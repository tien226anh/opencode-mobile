package ai.opencode.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.opencode.mobile.model.FileDiff
import ai.opencode.mobile.ui.theme.DiffAddedBackground
import ai.opencode.mobile.ui.theme.DiffAddedBackgroundLight
import ai.opencode.mobile.ui.theme.DiffAddedText
import ai.opencode.mobile.ui.theme.DiffRemovedBackground
import ai.opencode.mobile.ui.theme.DiffRemovedBackgroundLight
import ai.opencode.mobile.ui.theme.DiffRemovedText

@Composable
fun DiffViewer(
    diffText: String,
    modifier: Modifier = Modifier,
) {
    val isLight = isLightTheme()
    val addedBg = if (isLight) DiffAddedBackgroundLight else DiffAddedBackground
    val removedBg = if (isLight) DiffRemovedBackgroundLight else DiffRemovedBackground

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                diffText.lines().forEach { line ->
                    val bgColor = when {
                        line.startsWith("+") -> addedBg
                        line.startsWith("-") -> removedBg
                        line.startsWith("@@") -> MaterialTheme.colorScheme.surfaceVariant
                        else -> null
                    }

                    val textColor = when {
                        line.startsWith("+") -> DiffAddedText
                        line.startsWith("-") -> DiffRemovedText
                        line.startsWith("@@") -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (bgColor != null) Modifier.background(bgColor) else Modifier
                            ),
                    ) {
                        Text(
                            text = line.ifEmpty { " " },
                            style = textStyle,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun isLightTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val luminance = 0.2126f * bg.red + 0.7152f * bg.green + 0.0722f * bg.blue
    return luminance > 0.5f
}

/**
 * Card displaying a single file diff with filename, stats, and diff content.
 */
@Composable
fun FileDiffCard(
    diff: FileDiff,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            // File name header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = diff.file,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Additions/deletions stats
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (diff.additions > 0) {
                        Text(
                            text = "+${diff.additions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiffAddedText,
                        )
                    }
                    if (diff.deletions > 0) {
                        Text(
                            text = "-${diff.deletions}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiffRemovedText,
                        )
                    }
                }
            }
            // Show diff if there's before/after content
            if (diff.after.isNotBlank() || diff.before.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                DiffViewer(diffText = buildUnifiedDiff(diff))
            }
        }
    }
}

/**
 * Build a simple unified diff from before/after content.
 */
private fun buildUnifiedDiff(diff: FileDiff): String {
    val beforeLines = diff.before.lines().filter { it.isNotEmpty() }
    val afterLines = diff.after.lines().filter { it.isNotEmpty() }
    return buildString {
        beforeLines.forEach { line -> appendLine("-$line") }
        afterLines.forEach { line -> appendLine("+$line") }
    }.trimEnd()
}